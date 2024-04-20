package net.unethicalite.plugins.motherlode;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.unethicalite.api.Interactable;
import net.unethicalite.api.SceneEntity;
import net.unethicalite.api.entities.*;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.DepositBox;
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
        name = "Unethical UnethicalMotherlode",
        description = "Chops trees",
        enabledByDefault = false
)
@Slf4j
public class UnethicalMotherlodePlugin extends LoopedPlugin
{

    private static final Set<Integer> MOTHERLODE_MAP_REGIONS = ImmutableSet.of(14679, 14680, 14681, 14935, 14936, 14937, 15191, 15192, 15193);
    private static final Set<Integer> VALID_MINABLE_IDS = ImmutableSet.of(26661, 26662, 26663, 26664);

    private static final Set<Integer> VALID_DEPOSIT_IDS = ImmutableSet.of(436, 438, 440, 442, 444, 446, 447, 449, 451, 453, 1617, 1619, 1621, 1623, 1625, 1627, 1629, 1631, 6571, 19496);

    private static final Integer BROKEN_STRUT_ID = 26670;

    private static final Integer HOPPER_ID = 26674;

    private static final Integer EMPTY_SACK_ID = 26677;
    private static final Integer FILLED_SACK_ID = 26678;

    private static final Integer PAY_DIRT_ID = 12011;

    private static final WorldPoint BANK_LOCATION = new WorldPoint(3758, 5666, 0);

    @Inject
    private Client client;
    @Inject
    private UnethicalMotherlodeConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private OverlayManager overlayManager;


    @Inject
    private GlobalCollisionMap collisionMap;

    private int fmCooldown = 0;

    @Getter(AccessLevel.PROTECTED)
    private List<Tile> fireArea;

    private WorldPoint startLocation = null;
    private WorldPoint bankLocation = null;

    @Getter(AccessLevel.PROTECTED)
    private boolean scriptStarted;

    private boolean needToEmpty = true;


    @Subscribe
    public void onConfigButtonPressed(ConfigButtonClicked event)
    {
        if (!event.getGroup().contains("unethical-UnethicalMotherlode"))
        {
            return;
        }

    }

    private boolean checkInMlm()
    {
        GameState gameState = client.getGameState();
        if (gameState != GameState.LOGGED_IN
                && gameState != GameState.LOADING)
        {
            return false;
        }

        int[] currentMapRegions = client.getMapRegions();

        // Verify that all regions exist in MOTHERLODE_MAP_REGIONS
        for (int region : currentMapRegions)
        {
            if (!MOTHERLODE_MAP_REGIONS.contains(region))
            {
                return false;
            }
        }

        return true;
    }

    @Override
    protected int loop()
    {
        if (!config.isEnabled() || !checkInMlm())
        {
            needToEmpty = true;
            return 1000;
        }

        if (client.getLocalPlayer().getAnimation() == 6752)
        {
            log.info("Currently mining so waiting for a bit longer");
            return 2500;
        }

        if (client.getLocalPlayer().isAnimating())
        {
            log.info("Currently animating so waiting");
            return 250;
        }

        if (client.getLocalPlayer().isMoving() && Movement.getDestination() != null && Movement.getDestination().distanceTo(client.getLocalPlayer().getWorldLocation()) > 3)
        {
            log.info("Currently animating so waiting");
            return 250;
        }

        if(handleDepositValidIds())
        {
            log.info("Attempted to deposit pre-emptive");
            return 250;
        }

        if(handleMine())
        {
            log.info("Attempted to mine");
            return 1000;
        }

        if(handleFixWheels())
        {
            log.info("Attempted to fix wheels");
            return 1000;
        }

        if(handleDumpPayDirt())
        {
            log.info("Attempted to fill hopper");
            return 250;
        }

        if(handleDepositValidIds())
        {
            log.info("Attempted to deposit first pass");
            return 250;
        }

        if(handleCollectOre())
        {
            log.info("Attempted to collect ore");
            return 250;
        }

        if(handleDepositValidIds())
        {
            log.info("Atempted to deposit second pass");
            return 250;
        }

        log.info("End of switch, idling");
        return 1000;
    }

    private boolean handleCollectOre()
    {
        if (!needToEmpty)
            return false;

        TileObject emptySack = TileObjects.getNearest(x -> x.getActualId() == EMPTY_SACK_ID);
        if (emptySack != null)
        {
            log.info("empty sack exists so not collecting");
            needToEmpty = false;
            return false;
        }
        TileObject filledSack = TileObjects.getNearest(x -> x.getActualId() == FILLED_SACK_ID);
        if (filledSack == null)
        {
            log.info("sack is null (lol), moving to bank spot");
            handleMoveToBankSpot();
            return true;
        }

        if (filledSack.getWorldArea().offset(1).toWorldPointList().stream().noneMatch(Reachable::isWalkable))
        {
            log.info("sack is unreachable, moving to bank spot");
            handleMoveToBankSpot();
            return true;
        }

        filledSack.interact("Search");
        return true;
    }

    private boolean handleDepositValidIds()
    {
        if(!Inventory.contains(x -> VALID_DEPOSIT_IDS.contains(x.getId())))
            return false;

        if (DepositBox.isOpen())
        {
            Widget container = client.getWidget(ComponentID.DEPOSIT_BOX_INVENTORY_ITEM_CONTAINER);
            if (container == null)
            {
                log.info("container is null");
                return false;
            }

            Widget[] widgets = container.getChildren();
            if (widgets == null)
            {
                log.info("widgets is null");
                return false;
            }

            Widget itemToDeposit = Arrays.stream(widgets).filter(x -> VALID_DEPOSIT_IDS.contains(x.getItemId())).findFirst().orElse(null);
            if (itemToDeposit == null)
            {
                log.info("no items to deposit");
                return false;
            }

            itemToDeposit.interact("Deposit-All");
            return true;
        }
        else
        {
            TileObject depositBox = TileObjects.getNearest(x -> x.hasAction("Deposit") && x.getName().toLowerCase().contains("bank"));
            if (depositBox == null)
            {
                log.info("no deposit box so attempting to move to bank location");
                handleMoveToBankSpot();
                return true;
            }

            if (depositBox.getWorldArea().offset(1).toWorldPointList().stream().noneMatch(Reachable::isWalkable))
            {
                log.info("deposit box is unreachable, moving to bank spot");
                handleMoveToBankSpot();
                return true;
            }


            depositBox.interact("Deposit");
            return true;
        }


    }

    private boolean handleFixWheels()
    {
        if (!Inventory.isFull() || !Inventory.contains(x -> x.getId() == PAY_DIRT_ID) || needToEmpty)
            return false;

        TileObject brokenStrut = TileObjects.getNearest(x -> x.getActualId() == BROKEN_STRUT_ID);
        if (brokenStrut == null)
        {
            log.info("no broken strut");
            return false;
        }

        if (brokenStrut.getWorldArea().offset(1).toWorldPointList().stream().noneMatch(Reachable::isWalkable))
        {
            log.info("strut is unreachable, moving to bank spot");
            handleMoveToBankSpot();
            return true;
        }

        brokenStrut.interact("Hammer");
        return true;
    }

    private boolean handleDumpPayDirt()
    {
        if (!Inventory.contains(x -> x.getId() == PAY_DIRT_ID))
            return false;

        TileObject hopper = TileObjects.getNearest(x -> x.getActualId() == HOPPER_ID);
        if (hopper == null)
        {
            log.info("no hopper so attempting to move to bank location");
            handleMoveToBankSpot();
            return true;
        }

        if (hopper.getWorldArea().offset(1).toWorldPointList().stream().noneMatch(Reachable::isWalkable))
        {
            log.info("hopper is unreachable, moving to bank spot");
            handleMoveToBankSpot();
            return true;
        }

        hopper.interact("Deposit");
        TileObject filledSack = TileObjects.getNearest(x -> x.getActualId() == FILLED_SACK_ID);
        if (filledSack != null)
            needToEmpty = true;
        return true;
    }

    private void handleMoveToBankSpot()
    {
        if (!Reachable.isWalkable(BANK_LOCATION))
        {
            // just get the nearest interactable rockfall and mine it
            TileObject rockFall = TileObjects.getNearest(x -> x.hasAction("Mine") && x.getName().toLowerCase().contains("rockfall") && x.getWorldArea().offset(1).toWorldPointList().stream().anyMatch(Reachable::isWalkable));
            if (rockFall == null)
            {
                log.info("Could not get any nearby rock falls so trying to walk anyways.");
                Movement.walkTo(BANK_LOCATION);
                return;
            }
            rockFall.interact("Mine");
            return;
        }
        Movement.walkTo(BANK_LOCATION);
    }

    private boolean handleMine()
    {
        if (Inventory.isFull() || needToEmpty)
            return false;

        TileObject nearestMinable = TileObjects.getNearest(x -> x.hasAction("Mine")
                && VALID_MINABLE_IDS.contains(x.getActualId())
                && x.getWorldArea().offset(1).toWorldPointList().stream().anyMatch(Reachable::isWalkable));
        if (nearestMinable == null)
        {
            log.info("Could not find mineable");
            return false;
        }

        nearestMinable.interact("Mine");
        return true;
    }

    @Subscribe
    private void onGameTick(GameTick e)
    {

    }

    @Provides
    UnethicalMotherlodeConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(UnethicalMotherlodeConfig.class);
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
