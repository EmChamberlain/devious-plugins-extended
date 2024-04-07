package net.unethicalite.plugins.pestcontrol;

import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.http.api.worlds.WorldRegion;
import net.unethicalite.api.Interactable;
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
        name = "Unethical PestControl",
        description = "Chops trees",
        enabledByDefault = false
)
@Slf4j
public class PestControlPlugin extends LoopedPlugin
{
    @Inject
    private PestControlConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private Client client;

    @Inject
    private PestControlOverlay PestControlOverlay;

    @Inject
    private GlobalCollisionMap collisionMap;

    private int cooldown = 0;

    private WorldPoint startLocation = null;

    @Getter(AccessLevel.PROTECTED)
    private boolean scriptStarted;

    private static final int PEST_CONTROL_REGION = 10536;

    private static final  WorldPoint guardPoint = new WorldPoint(2656, 2591, 0);
    private static final WorldPoint noviceLanderCorner = new WorldPoint(2661, 2639, 0);
    private static final WorldPoint intermediateLanderCorner = new WorldPoint(2639, 2643, 0);
    private static final WorldPoint expertLanderCorner = new WorldPoint(2633, 2650, 0);

    @Override
    protected void startUp()
    {
        overlayManager.add(PestControlOverlay);
    }

    @Override
    public void stop()
    {
        super.stop();
        overlayManager.remove(PestControlOverlay);
    }

    @Subscribe
    public void onConfigButtonPressed(ConfigButtonClicked event)
    {
        if (!event.getGroup().contains("unethical-PestControl"))
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
                this.scriptStarted = true;
                log.info("Script started");
            }
        }

    }

    protected boolean isEmptyTile(Tile tile)
    {
        return tile != null
                && TileObjects.getFirstAt(tile, a -> a instanceof GameObject) == null
                && !collisionMap.fullBlock(tile.getWorldLocation());
    }

    private NPC getNearestAttackableNPC()
    {
        return NPCs.getNearest(x -> x != null && x.hasAction("Attack"));
    }


    private boolean targetDeadOrNoTarget()
    {
        if (client.getLocalPlayer().getInteracting() == null)
        {
            return true;
        }

        if (client.getLocalPlayer().getInteracting() instanceof NPC)
        {
            NPC npcTarget = (NPC) client.getLocalPlayer().getInteracting();
            int ratio = npcTarget.getHealthRatio();

            return ratio == 0;
        }

        return false;
    }

    private int handlePestControl()
    {
        var local = Players.getLocal();
        var closestAttackable = getNearestAttackableNPC();

        if (local == null)
            return 1000;


        if (closestAttackable == null)
        {
            Movement.walkTo(guardPoint);
            return 1000;
        }
        else
        {
            if (!targetDeadOrNoTarget())
                return 1000;
            else
            {
                closestAttackable.interact("Attack");
                return 1000;
            }
        }
    }


    private boolean inLander()
    {
        var localPoint = client.getLocalPlayer().getWorldLocation();
        return ((localPoint.getX() >= noviceLanderCorner.getX() && localPoint.getX() <= noviceLanderCorner.getX() + 2) && ((localPoint.getY() >= noviceLanderCorner.getY() && localPoint.getY() <= noviceLanderCorner.getY() + 4)))
                || ((localPoint.getX() >= intermediateLanderCorner.getX() && localPoint.getX() <= intermediateLanderCorner.getX() + 2) && ((localPoint.getY() >= intermediateLanderCorner.getY() && localPoint.getY() <= intermediateLanderCorner.getY() + 4)))
                || ((localPoint.getX() >= expertLanderCorner.getX() && localPoint.getX() <= expertLanderCorner.getX() + 2) && ((localPoint.getY() >= expertLanderCorner.getY() && localPoint.getY() <= expertLanderCorner.getY() + 4)));
    }


    @Override
    protected int loop()
    {
        if (cooldown > 0 || !scriptStarted)
        {
            return -1;
        }

        var local = Players.getLocal();
        if (local == null)
        {
            return -1;
        }

        if (local.getWorldLocation().getRegionID() == PEST_CONTROL_REGION)
        {
            //We are currently actively playing pest control
            return handlePestControl();
        }
        else
        {
            boolean in_lander = inLander();
            Interactable plankInteractable = getPlankInteractable();
            if (plankInteractable == null && !in_lander)
            {
                Movement.walkTo(startLocation);
                return 1000;
            }
            else if (plankInteractable != null && !in_lander)
            {
                plankInteractable.interact();
                return 1000;
            }
            else
            {
                //wait for pest control to start
                return 1000;
            }
        }
    }

    @Subscribe
    private void onGameTick(GameTick e)
    {
        if (cooldown > 0)
        {
            cooldown--;
        }
    }

    @Provides
    PestControlConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(PestControlConfig.class);
    }

    private List<Tile> generateFireArea(int radius)
    {
        return Tiles.getSurrounding(Players.getLocal().getWorldLocation(), radius).stream()
                .filter(tile -> tile != null
                        && isEmptyTile(tile)
                        && Reachable.isWalkable(tile.getWorldLocation()))
                .collect(Collectors.toUnmodifiableList());
    }

    private Interactable getPlankInteractable()
    {
        return TileObjects.getFirstSurrounding(client.getLocalPlayer().getWorldLocation(), 3, x -> x != null && x.hasAction("Cross"));
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
        return Widgets.get(groupId).stream()
                .filter(Objects::nonNull)
                .flatMap(w -> getFlatChildren(w).stream())
                .filter(
                        w -> w.getActions() != null
                                && Arrays.stream(w.getActions()).anyMatch(predicate)
                )
                .findFirst().orElse(null);
    }
}

