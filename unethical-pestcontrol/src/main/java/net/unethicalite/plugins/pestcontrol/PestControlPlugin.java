package net.unethicalite.plugins.pestcontrol;

import com.google.inject.Provides;
import com.openosrs.client.game.PlayerManager;
import com.openosrs.client.util.WeaponStyle;
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
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.http.api.worlds.WorldRegion;
import net.unethicalite.api.Interactable;
import net.unethicalite.api.entities.*;
import net.unethicalite.api.game.Combat;
import net.unethicalite.api.items.Equipment;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.movement.Reachable;
import net.unethicalite.api.movement.pathfinder.GlobalCollisionMap;
import net.unethicalite.api.plugins.LoopedPlugin;
import net.unethicalite.api.scene.Tiles;
import net.unethicalite.api.widgets.Prayers;
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
        name = "Unethical Pest Control",
        description = "Does pest control",
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
    private ItemManager itemManager;

    @Inject
    private PlayerManager playerManager;

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

    private static final int PEST_CONTROL_REGION = 10537;

    private static final  WorldPoint guardPoint = new WorldPoint(2657, 2590, 0);
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
        if (!event.getGroup().contains("unethical-pestcontrol"))
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

    private NPC getNearestWalkableAttackableNPC()
    {
        return Combat.getAttackableNPC(Objects::nonNull);
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
        var closestAttackable = getNearestWalkableAttackableNPC();

        if (local == null)
        {
            log.info("Local player is null");
            return 1000;
        }

        if (Prayers.getPoints() > 0 && !Prayers.isQuickPrayerEnabled())
        {
            log.info("Enabling quick prayers");
            Prayers.toggleQuickPrayer(true);
            return 500;
        }


        if (closestAttackable == null)
        {
            if (local.getWorldLocation().distanceTo(guardPoint) <= 3)
            {
                log.info("At guard point, idling.");
                return 50;
            }
            else
            {
                if (local.isMoving())
                {
                    log.info("Currently moving, idling.");
                    return 50;
                }

                Movement.walkTo(guardPoint);
                log.info("Walking to guard point");
                return 50;
            }

        }
        else
        {
            if (!targetDeadOrNoTarget())
            {
                log.info("Have target, idling");
                return 50;
            }
            else
            {
                int maxDist = Combat.getCurrentWeaponStyle() == WeaponStyle.MELEE ? 2 : 7;

                if (!Reachable.isWalled(local.getWorldLocation(), closestAttackable.getWorldLocation()))
                {
                    closestAttackable.interact("Attack");
                    log.info("Attacking closest reachable");
                    return 1000;
                }
                else if (local.getWorldLocation().distanceTo(closestAttackable.getWorldLocation()) <= maxDist)
                {
                    closestAttackable.interact("Attack");
                    log.info("Attacking closest reachable");
                    return 1000;


                }
                else
                {
                    if (local.getWorldLocation().distanceTo(Movement.getDestination()) > 3)
                    {
                        log.info("Currently moving, idling.");
                        return 50;
                    }

                    log.info("Moving to target");
                    Movement.walkTo(closestAttackable.getWorldLocation());
                    return 50;
                }
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
        if (cooldown > 0)
        {
            log.info("Cooling down");
            return -1;
        }
        if (!scriptStarted)
        {
            log.info("Script not started");
            return -1;
        }

        var local = Players.getLocal();
        if (local == null)
        {
            log.info("Local player is null in loop");
            return -1;
        }
        if (local.getWorldLocation().getRegionID() != PEST_CONTROL_REGION)
        {
            //We are currently actively playing pest control
            log.info("Handling pest control in region: {}", local.getWorldLocation().getRegionID());
            return handlePestControl();
        }
        else
        {
            boolean in_lander = inLander();
            Interactable plankInteractable = getPlankInteractable();
            if (plankInteractable == null && !in_lander)
            {
                Movement.walkTo(startLocation);
                log.info("Walking to start");
                return 1000;
            }
            else if (plankInteractable != null && !in_lander)
            {
                plankInteractable.interact("Cross");
                log.info("Interacting with plank");
                return 1000;
            }
            else
            {
                //wait for pest control to start
                log.info("Wait for pest control to start");
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

