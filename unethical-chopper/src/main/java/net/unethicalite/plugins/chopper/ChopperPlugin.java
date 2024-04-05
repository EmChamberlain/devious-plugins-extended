package net.unethicalite.plugins.chopper;

import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.unethicalite.api.Interactable;
import net.unethicalite.api.SceneEntity;
import net.unethicalite.api.entities.*;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.movement.Reachable;
import net.unethicalite.api.movement.pathfinder.GlobalCollisionMap;
import net.unethicalite.api.plugins.LoopedPlugin;
import net.unethicalite.api.scene.Tiles;
import net.unethicalite.api.widgets.Widgets;
import net.unethicalite.client.Static;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.unethicalite.api.entities.TileObjects.getNearest;

@Extension
@PluginDescriptor(
		name = "Unethical Chopper",
		description = "Chops trees",
		enabledByDefault = false
)
@Slf4j
public class ChopperPlugin extends LoopedPlugin
{
	@Inject
	private ChopperConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ChopperOverlay chopperOverlay;

	@Inject
	private GlobalCollisionMap collisionMap;

	private int fmCooldown = 0;

	@Getter(AccessLevel.PROTECTED)
	private List<Tile> fireArea;

	private WorldPoint startLocation = null;
	private WorldPoint bankLocation = null;

	@Getter(AccessLevel.PROTECTED)
	private boolean scriptStarted;

	@Override
	protected void startUp()
	{
		overlayManager.add(chopperOverlay);
	}

	@Override
	public void stop()
	{
		super.stop();
		overlayManager.remove(chopperOverlay);
	}

	@Subscribe
	public void onConfigButtonPressed(ConfigButtonClicked event)
	{
		if (!event.getGroup().contains("unethical-chopper"))
		{
			return;
		}

		if (event.getKey().toLowerCase().contains("start"))
		{
			if (scriptStarted)
			{
				scriptStarted = false;
			}
			else
			{
				var local = Players.getLocal();
				if (local == null)
				{
					return;
				}
				startLocation = local.getWorldLocation();
				String[] split = config.bankTile().split(" ");
				bankLocation = new WorldPoint(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
				fireArea = generateFireArea(3);
				this.scriptStarted = true;
				log.info("Script started");
			}
		}
		else if(event.getKey().toLowerCase().contains("banktile"))
		{
			bankLocation = Players.getLocal().getWorldLocation();
			configManager.setConfiguration("unethical-chopper", "bankTile", String.format("%s %s %s", bankLocation.getX(), bankLocation.getY(), bankLocation.getPlane()));
		}

	}

	protected boolean isEmptyTile(Tile tile)
	{
		return tile != null
			&& TileObjects.getFirstAt(tile, a -> a instanceof GameObject) == null
			&& !collisionMap.fullBlock(tile.getWorldLocation());
	}

	@Override
	protected int loop()
	{
		if (fmCooldown > 0 || !scriptStarted)
		{
			return -1;
		}

		var local = Players.getLocal();
		if (local == null)
		{
			return -1;
		}

		var tree = TileObjects
				.getSurrounding(startLocation, 8, config.tree().getNames())
				.stream()
				.min(Comparator.comparing(x -> x.distanceTo(local.getWorldLocation())))
				.orElse(null);

		var logs = Inventory.getFirst(x -> x.getName().toLowerCase(Locale.ROOT).contains("logs"));
		if (config.makeFire())
		{
			var tinderbox = Inventory.getFirst("Tinderbox");
			if (logs != null && tinderbox != null)
			{
				var emptyTile = fireArea == null || fireArea.isEmpty() ? null : fireArea.stream()
						.filter(t ->
						{
							Tile tile = Tiles.getAt(t.getWorldLocation());
							return tile != null && isEmptyTile(tile);
						})
						.min(Comparator.comparingInt(wp -> wp.distanceTo(local)))
						.orElse(null);

				if (fireArea.isEmpty() || emptyTile == null)
				{
					fireArea = generateFireArea(3);
					log.debug("Generating fire area");
					return 1000;
				}

				if (emptyTile != null)
				{
					if (!emptyTile.getWorldLocation().equals(local.getWorldLocation()))
					{
						if (local.isMoving())
						{
							return 333;
						}

						Movement.walk(emptyTile);
						return 1000;
					}

					if (local.isAnimating())
					{
						return 333;
					}

					fmCooldown = 4;
					tinderbox.useOn(logs);
					return 500;
				}
			}
		}
		else if (config.bank())
		{
			if (Inventory.isFull())
			{
				Interactable bankInteractable = getBankInteractable();
				if (bankInteractable == null)
				{
					Movement.walk(startLocation);
					return 444;
				}
				else
				{
					Widget depositInventoryWidget = getWidget(x -> x != null && x.equals("Deposit inventory"));
					log.info("Deposit widget: {}", depositInventoryWidget);
					if (depositInventoryWidget != null)
						log.info("	{}", (Object) depositInventoryWidget.getActions());
					if (depositInventoryWidget == null)
					{
						bankInteractable.interact("Bank", "Deposit");
						return 555;
					}
					else
					{
						depositInventoryWidget.interact("Deposit inventory");
						return 666;
					}

				}
			}
			else
			{
				Widget closeWidget = getWidget(WidgetID.BANK_GROUP_ID, x -> x != null &&  x.equals("Close"));
				log.info("Close widget: {}", closeWidget);
				if(closeWidget != null)
				{
					closeWidget.interact();
					return 777;
				}
			}
		}
		else
		{
			if (logs != null && !local.isAnimating())
			{
				logs.drop();
				return 500;
			}
		}

		if (local.isMoving() || local.isAnimating())
		{
			return 333;
		}

		if (tree == null)
		{
			log.debug("Could not find any trees");
			Movement.walk(startLocation);
			return 1000;
		}

		tree.interact("Chop down");
		return 1000;
	}

	@Subscribe
	private void onGameTick(GameTick e)
	{
		if (fmCooldown > 0)
		{
			fmCooldown--;
		}
	}

	@Provides
	ChopperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ChopperConfig.class);
	}

	private List<Tile> generateFireArea(int radius)
	{
		return Tiles.getSurrounding(Players.getLocal().getWorldLocation(), radius).stream()
				.filter(tile -> tile != null
					&& isEmptyTile(tile)
					&& Reachable.isWalkable(tile.getWorldLocation()))
				.collect(Collectors.toUnmodifiableList());
	}

	private static Interactable getBankInteractable()
	{
		TileObject nearestObject = TileObjects.getNearest(x -> x != null && x.hasAction("Bank", "Deposit"));
		if (nearestObject != null)
			return nearestObject;
		else
			return NPCs.getNearest(x -> x != null && x.hasAction("Bank", "Deposit"));
	}

	private static Widget getWidget(Predicate<String> predicate)
	{
		return Arrays.stream(Static.getClient().getWidgets())
				.filter(Objects::nonNull)
				.flatMap(Arrays::stream)
				.filter(
						w -> w.getActions() != null
								&& Arrays.stream(w.getActions()).anyMatch(predicate)
				)
				.findFirst().orElse(null);
	}

	private static List<Widget> getFlatChildren(Widget widget)
	{
		final var list = new ArrayList<Widget>();
		list.add(widget);
		if (widget.getChildren() != null)
		{
			list.addAll(
					Arrays.stream(widget.getChildren())
							.flatMap(w -> getFlatChildren(w).stream())
							.collect(Collectors.toList())
			);
		}

		return list;
	}

	private static Widget getWidget(int groupId, Predicate<String> predicate)
	{
		return Widgets.get(groupId).stream().flatMap(w -> getFlatChildren(w).stream())
				.collect(Collectors.toList())
				.stream()
				.filter(Objects::nonNull)
				.filter(
						w -> w.getActions() != null
								&& Arrays.stream(w.getActions()).anyMatch(predicate)
				)
				.findFirst().orElse(null);
	}
}
