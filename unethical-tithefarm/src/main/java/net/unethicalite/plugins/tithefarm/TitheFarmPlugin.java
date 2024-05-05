package net.unethicalite.plugins.tithefarm;

import com.google.inject.Provides;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import net.unethicalite.api.commons.Time;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.input.Keyboard;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.movement.Reachable;
import net.unethicalite.api.plugins.LoopedPlugin;
import net.unethicalite.api.widgets.Dialog;
import net.unethicalite.api.widgets.Widgets;
import net.unethicalite.client.Static;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Extension
@PluginDescriptor(
        name = "Unethical TitheFarm",
        description = "TitheFarm",
        enabledByDefault = false
)
@Slf4j
public class TitheFarmPlugin extends LoopedPlugin
{

    @Inject
    private Client client;
    @Inject
    private TitheFarmConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ChatMessageManager chatMessageManager;


    private final List<WorldPoint> ORDERED_PLOT_LOCATIONS = List.of(



            new WorldPoint(1813, 3489, 0), new WorldPoint(1814, 3489, 0),
            new WorldPoint(1814, 3492, 0), new WorldPoint(1813, 3492, 0),

            new WorldPoint(1813, 3495, 0), new WorldPoint(1814, 3495, 0),
            new WorldPoint(1814, 3498, 0), new WorldPoint(1813, 3498, 0),

            new WorldPoint(1813, 3504, 0), new WorldPoint(1814, 3504, 0),
            new WorldPoint(1814, 3507, 0), new WorldPoint(1813, 3507, 0),

            new WorldPoint(1813, 3510, 0), new WorldPoint(1814, 3510, 0),
            new WorldPoint(1814, 3513, 0), new WorldPoint(1813, 3513, 0),

            new WorldPoint(1819, 3513, 0),
            new WorldPoint(1819, 3510, 0),
            new WorldPoint(1819, 3507, 0),
            new WorldPoint(1819, 3504, 0)
    );

    private final int SEEDS = 13424;
    private final int FRUIT = 13427;
    private final int FERTILIZER = 13420;
    private final int OPEN_PLOT = 27383;
    private final List<Integer> NEED_TO_WATER = List.of(27395, 27398, 27401);
    private final List<Integer> WATERED = List.of(27396, 27399, 27402);
    private final List<Integer> BLIGHTED = List.of(27397, 27400, 27403);
    private final int HARVESTABLE = 27404;
    private final int DEPOSIT_BIN = 27431;
    private final WorldPoint DOOR_LOC = new WorldPoint(1805, 3501, 0);
    private final int SEED_TABLE = 27430;

    private final WorldPoint START = new WorldPoint(1813, 3489, 0);

    private final WorldArea MINIGAME_AREA = new WorldArea(new WorldPoint(1805, 3486, 0), new WorldPoint(1835, 3516, 0));

    //Stage 0 | Reqs: no harvestable and have items | Actions: move to START, plant and water so all are watered index 0 | Moves to: 1
    //Stage 1 | Reqs: no NEED_TO_WATER[0] | Actions: move to START, wait for NEED_TO_WATER[1], water to WATERED[1] | Moves to: 2
    //Stage 2 | Reqs: no NEED_TO_WATER[1] | Actions: move to START, wait for NEED_TO_WATER[2], water to WATERED[2] | Moves to: 3
    //Stage 3 | Reqs: no NEED_TO_WATER[2] | Actions: move to START, wait for HARVESTABLE, harvest all | Moves to: 4, 0
    //Stage 4 | Reqs: have over 100 fruit | Actions: deposit fruit, exit, get seeds, enter, drop fertilizer | Moves to: 0
    private int stage = 0;

    //Stage 0 | Reqs: have > 100 fruit | Actions: deposit fruit
    //Stage 1 | Reqs: no fruit | Actions: drop seeds
    //Stage 2 | Reqs: no seeds and in minigame | Actions: exit
    //Stage 3 | Reqs: no seeds and outside minigame | Actions: get seeds
    //Stage 4 | Reqs: seeds and outside minigame | Actions: enter
    //Stage 5 | Reqs: seeds and inside minigame | Actions: drop fertilizer


    private int substage4 = 0;

    private int nextPlotIndex = 0;

    private final int MAX_PLOTS = 15;

    private final int ACTION_DELAY = 1000;
    private final int NO_ACTION_DELAY = 50;



    @Subscribe
    public void onConfigButtonPressed(ConfigButtonClicked event)
    {
        if (!event.getGroup().contains("unethical-tithefarm"))
        {
            return;
        }

    }

    private WorldPoint getClosestWalkableToTileObject(TileObject object)
    {
        WorldPoint localLocation = client.getLocalPlayer().getWorldLocation();
        WorldPoint closestObjectPoint = object.getWorldArea().toWorldPointList().stream().min(Comparator.comparingInt(x -> x.distanceTo(localLocation))).orElse(null);

        if (closestObjectPoint == null)
        {
            chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("No closest area point").build());

            return null;
        }

        return closestObjectPoint.toWorldArea().offset(1).toWorldPointList().stream().filter(Reachable::isWalkable).min(Comparator.comparingInt(x -> x.distanceTo(localLocation))).orElse(null);
    }

    private boolean isInMinigame()
    {
        return MINIGAME_AREA.contains(client.getLocalPlayer().getWorldLocation());
    }

    private boolean stage0Reqs()
    {
        return TileObjects.getAll(HARVESTABLE).isEmpty() && Inventory.getCount(true, SEEDS) > 100 && Inventory.getAll(FERTILIZER).isEmpty();
    }

    private boolean waterClosestIndex(int index)
    {
        assert index >= 1;
        WorldPoint plotLoc = WorldPoint.toLocalInstance(client, ORDERED_PLOT_LOCATIONS.get(nextPlotIndex)).stream().findFirst().orElse(null);
        TileObject closestPlot = TileObjects.getNearest(plotLoc, WATERED.get(index - 1), NEED_TO_WATER.get(index));

        if (plotLoc == null)
        {
            chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("No walkToLocation").build());

            return false;
        }

        if (client.getLocalPlayer().getWorldLocation().distanceTo(plotLoc) > 1)
        {
            Movement.walkTo(plotLoc);
            return true;
        }

        if (closestPlot == null)
        {
            chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("No plot").build());
            return false;
        }

        Item wateringCan = Inventory.getFirst(x -> x.getName().toLowerCase().contains("atering can("));
        if (wateringCan == null)
        {
            chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("No water").build());
            return false;
        }

        if (closestPlot.getId() == WATERED.get(index - 1))
        {
            return false;
        }
        else if (closestPlot.getId() == NEED_TO_WATER.get(index))
        {
            wateringCan.useOn(closestPlot);
            nextPlotIndex += 1;
            return true;
        }

        return false;

    }
    private boolean stage0Actions()
    {
        if (nextPlotIndex > MAX_PLOTS)
        {
            nextPlotIndex = 0;
            return false;
        }

        WorldPoint plotLoc = WorldPoint.toLocalInstance(client, ORDERED_PLOT_LOCATIONS.get(nextPlotIndex)).stream().findFirst().orElse(null);
        TileObject closestPlot = TileObjects.getNearest(plotLoc, OPEN_PLOT, NEED_TO_WATER.get(0));

        if (plotLoc == null)
        {
            chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("No walkToLocation").build());
            return false;
        }

        if (client.getLocalPlayer().getWorldLocation().distanceTo(plotLoc) > 1)
        {
            Movement.walkTo(plotLoc);
            chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("Walking to: " + plotLoc.toString()).build());
            return true;
        }

        if (closestPlot == null)
        {
            chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("No plot").build());
            return false;
        }

        Item seeds = Inventory.getFirst(SEEDS);
        if (seeds == null)
        {
            chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("No seeds").build());
            return false;
        }

        Item wateringCan = Inventory.getFirst(x -> x.getName().toLowerCase().contains("atering can("));
        if (wateringCan == null)
        {
            chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("No water").build());
            return false;
        }

        if (closestPlot.getId() == OPEN_PLOT)
        {
            seeds.useOn(closestPlot);
            return true;
        }
        else
        {
            wateringCan.useOn(closestPlot);
            nextPlotIndex += 1;
            return true;
        }
    }

    private boolean stage1Reqs()
    {
        return TileObjects.getAll(NEED_TO_WATER.get(0)).isEmpty();
    }
    private boolean stage1Actions()
    {
        if (nextPlotIndex > MAX_PLOTS)
        {
            nextPlotIndex = 0;
            return false;
        }

        if (waterClosestIndex(1))
        {
            return true;
        }

        return false;
    }

    private boolean stage2Reqs()
    {
        return TileObjects.getAll(NEED_TO_WATER.get(0)).isEmpty() && TileObjects.getAll(NEED_TO_WATER.get(1)).isEmpty();
    }
    private boolean stage2Actions()
    {
        if (nextPlotIndex > MAX_PLOTS)
        {
            nextPlotIndex = 0;
            return false;
        }

        if (waterClosestIndex(2))
        {
            return true;
        }

        return false;
    }


    private boolean stage3Reqs()
    {
        return TileObjects.getAll(NEED_TO_WATER.get(0)).isEmpty() && TileObjects.getAll(NEED_TO_WATER.get(1)).isEmpty() && TileObjects.getAll(NEED_TO_WATER.get(2)).isEmpty();
    }
    private boolean stage3Actions()
    {
        if (nextPlotIndex > MAX_PLOTS)
        {
            nextPlotIndex = 0;
            return false;
        }

        if (waterClosestIndex(3))
        {
            return true;
        }

        return false;
    }


    private boolean stage4Reqs()
    {
        return Inventory.getCount(true, FRUIT) > 100 && TileObjects.getAll(NEED_TO_WATER.get(0)).isEmpty() && TileObjects.getAll(NEED_TO_WATER.get(1)).isEmpty() && TileObjects.getAll(NEED_TO_WATER.get(2)).isEmpty() && TileObjects.getAll(HARVESTABLE).isEmpty();
    }
    private boolean stage4Actions()
    {
        if (Dialog.isEnterInputOpen())
        {
            Keyboard.type(1000);
            Keyboard.sendEnter();
            return true;
        }
        Item fruit = Inventory.getFirst(FRUIT);
        Item seeds = Inventory.getFirst(SEEDS);
        Item fert = Inventory.getFirst(FERTILIZER);
        TileObject door = TileObjects.getNearest(DOOR_LOC, x -> x.getName().toLowerCase().contains("farm door"));
        TileObject seedTable = TileObjects.getNearest(SEED_TABLE);


        if (substage4 == 0)
        {
            if (fruit == null)
            {
                substage4 = 1;
                return false;
            }

            TileObject depositObject = TileObjects.getNearest(DEPOSIT_BIN);

            if (depositObject == null)
            {
                chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("depositObject null").build());
                return false;
            }

            depositObject.interact("Deposit");
            return true;

        }
        else if (substage4 == 1)
        {
            if (seeds == null && isInMinigame())
            {
                substage4 = 2;
                return false;
            }

            if (fruit == null)
            {
                seeds.interact("Drop");
                return true;
            }

            return false;
        }
        else if (substage4 == 2)
        {
            if (seeds == null && !isInMinigame())
            {
                substage4 = 3;
                return false;
            }



            if (door == null)
            {
                chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("door null").build());
                return false;
            }

            door.interact("Use");
            return true;

        }
        else if (substage4 == 3)
        {
            if (seeds != null && !isInMinigame())
            {
                substage4 = 4;
                return false;
            }

            if (seedTable == null)
            {
                chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("seedTable null").build());
                return false;
            }

            seedTable.interact("Use");
            return true;
        }
        else if (substage4 == 4)
        {
            if (seeds != null && isInMinigame())
            {
                substage4 = 5;
                return false;
            }

            if (door == null)
            {
                chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("door null").build());
                return false;
            }

            door.interact("Use");
            return true;
        }
        else if (substage4 == 5)
        {
            if (seeds != null && isInMinigame() && fert == null)
            {
                substage4 = 0;
                stage = 0;
                return false;
            }

            fert.interact("Drop");
            return true;
        }

        return false;
    }



    @Override
    protected int loop()
    {
        if (!config.isEnabled())
        {
            return 1000;
        }

        chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("Stage: " + Integer.toString(stage)).build());

        if (stage == 0)
        {
            if (!stage0Reqs())
            {
                chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("Failed 0 reqs").build());
                return NO_ACTION_DELAY;
            }
            if (stage0Actions())
            {
                chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("Tried 0 actions").build());
                return ACTION_DELAY;
            }

            if (TileObjects.getAll(NEED_TO_WATER.get(0)).isEmpty())
            {
                stage = 1;
                return NO_ACTION_DELAY;
            }

            return NO_ACTION_DELAY;
        }
        else if (stage == 1)
        {

            if (!stage1Reqs())
            {
                chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("Failed 1 reqs").build());
                return NO_ACTION_DELAY;
            }
            if (stage1Actions())
            {
                chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("Tried 1 actions").build());
                return ACTION_DELAY;
            }

            if (TileObjects.getAll(NEED_TO_WATER.get(1)).isEmpty())
            {
                stage = 2;
                return NO_ACTION_DELAY;
            }

            return NO_ACTION_DELAY;
        }
        else if (stage == 2)
        {

            if (!stage2Reqs())
            {
                chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("Failed 2 reqs").build());
                return NO_ACTION_DELAY;
            }
            if (stage2Actions())
            {
                chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("Tried 2 actions").build());
                return ACTION_DELAY;
            }

            if (TileObjects.getAll(NEED_TO_WATER.get(2)).isEmpty())
            {
               stage = 3;
                return NO_ACTION_DELAY;
            }

            return NO_ACTION_DELAY;
        }
        else if (stage == 3)
        {
            if (!stage3Reqs())
            {
                chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("Failed 3 reqs").build());
                return NO_ACTION_DELAY;
            }
            if (stage3Actions())
            {
                chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("Tried 3 actions").build());
                return ACTION_DELAY;
            }

            if (TileObjects.getAll(HARVESTABLE).isEmpty())
            {
                if (Inventory.getCount(true, FRUIT) >= 100)
                {
                    stage = 4;
                }
                else
                {
                    stage = 0;
                }
                return NO_ACTION_DELAY;
            }


            return NO_ACTION_DELAY;
        }
        else if (stage == 4)
        {
            if (!stage4Reqs())
            {
                chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("Failed 4 reqs").build());
                return NO_ACTION_DELAY;
            }
            if (stage4Actions())
            {
                chatMessageManager.queue(QueuedMessage.builder().sender("CONSOLE").type(ChatMessageType.CONSOLE).value("Tried 4 actions").build());
                return ACTION_DELAY;
            }

            // stage4Actions() will update the state when done
            return NO_ACTION_DELAY;
        }




        log.info("End of switch, idling");
        return 1000;
    }


    @Subscribe
    private void onGameTick(GameTick e)
    {

    }

    @Provides
    TitheFarmConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(TitheFarmConfig.class);
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
