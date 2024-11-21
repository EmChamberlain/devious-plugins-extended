package net.unethicalite.pickpocket;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.util.Text;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.WildcardMatcher;
import net.unethicalite.api.commons.Rand;
import net.unethicalite.api.commons.Time;
import net.unethicalite.api.entities.NPCs;
import net.unethicalite.api.entities.Players;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.game.Combat;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Equipment;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.movement.Reachable;
import net.unethicalite.api.plugins.LoopedPlugin;
import net.unethicalite.api.widgets.Dialog;
import org.pf4j.Extension;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@PluginDescriptor(name = "Unethical Pickpocket", enabledByDefault = false)
@Extension
@Slf4j
public class PickpocketPlugin extends LoopedPlugin
{
	@Inject
	private PickpocketConfig config;

	private WorldPoint lastNpcPosition = null;

	private int maxPouches = 5;

	@Override
	protected int loop()
	{
		Item junk = Inventory.getFirst(item -> shouldDrop(Text.fromCSV(config.junk()), item.getName()));
		if (junk != null)
		{
			junk.interact("Drop");
			log.debug("Dropping junk");
			return -1;
		}

		Item pouch = Inventory.getFirst("Coin pouch");
		if (pouch != null && pouch.getQuantity() >= maxPouches && !Inventory.isFull())
		{
			pouch.interact("Open-all");
			maxPouches = Rand.nextInt(1, 29);
			log.debug("Opening pouches");
			return -1;
		}

		if (config.eat())
		{
			if (Combat.getMissingHealth() >= config.eatHp())
			{
				Item food = Inventory.getFirst(config.foodName());
				if (food != null)
				{
					food.interact(1);
					log.debug("Eating food");
					return -2;
				}
			}
		}

		if (config.dodgyNecklaces())
		{
			if (!Equipment.contains(x -> x.getName().toLowerCase().contains("dodgy necklace")))
			{
				Item dodgyNecklace = Inventory.getFirst(x -> x.getName().toLowerCase().contains("dodgy necklace"));
				if (dodgyNecklace != null)
				{
					dodgyNecklace.interact("Wear");
					log.debug("Equipping dodgy necklace");
					return -2;
				}
			}
		}
		
		if (Bank.isOpen())
		{
			List<Item> unneeded = Inventory.getAll(item ->
					(!config.eat() || !Objects.equals(item.getName(), config.foodName()))
							&& item.getId() != ItemID.COINS_995
							&& !Objects.equals(item.getName(), "Coin pouch")
			);
			if (!unneeded.isEmpty())
			{
				for (Item item : unneeded)
				{
					Bank.depositAll(item.getId());
					Time.sleep(100);
				}

				return -1;
			}

			if (config.eat())
			{
				if (Inventory.getCount(config.foodName()) > config.foodAmount())
				{
					Bank.depositAll(config.foodName());
					return -2;
				}

				if (!Inventory.contains(config.foodName()))
				{
					Bank.withdraw(config.foodName(), config.foodAmount(), Bank.WithdrawMode.ITEM);
					log.debug("Withdrawing food");
					return -2;
				}
			}

			if (config.dodgyNecklaces())
			{
				if (Inventory.getCount(x -> x.getName().toLowerCase().contains("dodgy necklace")) < 3)
				{
					Bank.withdraw("Dodgy necklace", 5, Bank.WithdrawMode.ITEM);
					log.debug("Withdrawing dodgy necklace");
					return -2;
				}
			}
		}

		if (config.bank() && (Inventory.isFull() || !Inventory.contains(config.foodName()) || !Equipment.contains(x -> x.getName().toLowerCase().contains("dodgy necklace"))))
		{
			if (Movement.isWalking())
			{
				return -4;
			}

			TileObject bank = TileObjects.within(config.bankLocation().getArea().offset(2), obj -> obj.hasAction("Collect"))
					.stream()
					.min(Comparator.comparingInt(obj -> obj.distanceTo(Players.getLocal())))
					.orElse(null);
			if (bank != null)
			{
				bank.interact("Bank", "Use");
				return -4;
			}

			NPC banker = NPCs.getNearest("Banker");
			if (banker != null)
			{
				banker.interact("Bank");
				return -4;
			}

			Movement.walkTo(config.bankLocation());
			return -4;
		}

		NPC target = NPCs.getNearest(config.npcName());
		if (target != null)
		{
			lastNpcPosition = target.getWorldLocation();
			if (!Reachable.isInteractable(target))
			{
				if (Movement.isWalking())
				{
					return -4;
				}

				Movement.walkTo(target);
				return -4;
			}

			Player local = Players.getLocal();
			if (local.getGraphic() == 245 && !Dialog.isOpen())
			{
				return -1;
			}

			if (local.isMoving() && target.distanceTo(local) > 3)
			{
				return -1;
			}

			target.interact("Pickpocket");
			return Rand.nextInt(222, 333);
		}

		if (Movement.isWalking())
		{
			return -4;
		}

		if (lastNpcPosition != null)
		{
			Movement.walkTo(lastNpcPosition);
			return -4;
		}

		log.info("Idle");
		return -1;
	}

	@Provides
	PickpocketConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PickpocketConfig.class);
	}

	private boolean shouldDrop(List<String> itemNames, String itemName)
	{
		return itemNames.stream().anyMatch(name -> WildcardMatcher.matches(name, itemName));
	}
}
