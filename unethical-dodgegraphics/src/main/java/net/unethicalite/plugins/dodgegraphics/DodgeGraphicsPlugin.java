package net.unethicalite.plugins.dodgegraphics;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.unethicalite.api.entities.NPCs;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.movement.Reachable;
import net.unethicalite.api.plugins.LoopedPlugin;
import net.unethicalite.api.widgets.Widgets;
import net.unethicalite.client.Static;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.runelite.api.GraphicID.GRAPHICS_OBJECT_ROCKFALL;


@Extension
@PluginDescriptor(
        name = "Unethical DodgeGraphics",
        description = "DodgeGraphics",
        enabledByDefault = false
)
@Slf4j
public class DodgeGraphicsPlugin extends LoopedPlugin
{

    @Inject
    private Client client;
    @Inject
    private DodgeGraphicsConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private DodgeGraphicsOverlay dodgeGraphicsOverlay;


    public final List<GraphicsObject> graphics = new ArrayList<>();

    public WorldPoint closestPoint = null;

    public List<WorldPoint> safePoints = new ArrayList<>();


    @Override
    protected void startUp()
    {
        overlayManager.add(dodgeGraphicsOverlay);
    }

    @Override
    public void stop()
    {
        super.stop();
        overlayManager.remove(dodgeGraphicsOverlay);
    }


    @Subscribe
    public void onConfigButtonPressed(ConfigButtonClicked event)
    {
        if (!event.getGroup().contains("unethical-dodgegraphics"))
        {
            return;
        }

    }

    @Override
    protected int loop()
    {
        if (!config.isEnabled())
        {
            return 1000;
        }

        NPC closestNPC = NPCs.getNearest(x -> !x.isDead());
        var localPlayer = client.getLocalPlayer();

        graphics.clear();
        client.getGraphicsObjects().forEach(graphics::add);

        safePoints = client.getLocalPlayer().getWorldArea().offset(4).toWorldPointList().stream()
                .filter(this::isLocationSafe)
                .filter(Reachable::isWalkable)
                .filter(x -> closestNPC == null || !closestNPC.getWorldArea().toWorldPointList().contains(x))
                .collect(Collectors.toList());


        int lowestDist = Integer.MAX_VALUE;
        for (WorldPoint safePoint : safePoints)
        {
            int distance = safePoint.distanceTo(localPlayer.getWorldLocation());
            if (closestPoint == null || distance < lowestDist)
            {
                lowestDist = distance;
                closestPoint = safePoint;
            }
        }

        if (closestPoint != null && closestPoint != localPlayer.getWorldLocation() && lowestDist > 0)
        {
            log.info("Walking");
            Movement.walkTo(closestPoint);
            return 250;
        }

        if (config.repeatedlyAttack())
        {
            List<Integer> attackIds = Arrays.stream(config.repeatedlyAttackList().split(",")).map(Integer::parseInt).collect(Collectors.toList());
            for (int attackId : attackIds)
            {
                NPC closestAttackable = NPCs.getNearest(x -> x.hasAction("Attack") && x.getId() == attackId && x.getHealthRatio() != 0);
                if (closestAttackable != null && !localPlayer.isMoving() && localPlayer.getInteracting() == null)
                {
                    log.info("Attacking");
                    closestAttackable.interact("Attack");
                    return 250;
                }
            }
        }


        log.info("End of switch, idling");
        return 50;
    }



    private boolean isLocationSafe(WorldPoint pointToCheck)
    {
        for (var gObject : graphics)
        {
            if (pointToCheck.distanceTo(WorldPoint.fromLocal(client, gObject.getLocation())) <= config.radius())
            {
                return false;
            }
        }
        return true;
    }

    @Subscribe
    private void onGameTick(GameTick e)
    {


    }

    @Provides
    DodgeGraphicsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(DodgeGraphicsConfig.class);
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
