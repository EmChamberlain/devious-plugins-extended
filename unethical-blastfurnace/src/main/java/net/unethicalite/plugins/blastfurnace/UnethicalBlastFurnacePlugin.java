package net.unethicalite.plugins.blastfurnace;

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
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.DepositBox;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.movement.Reachable;
import net.unethicalite.api.movement.pathfinder.GlobalCollisionMap;
import net.unethicalite.api.plugins.LoopedPlugin;
import net.unethicalite.api.widgets.Widgets;
import net.unethicalite.client.Static;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Extension
@PluginDescriptor(
        name = "Unethical UnethicalBlastFurnace",
        description = "UnethicalBlastFurnace",
        enabledByDefault = false
)
@Slf4j
public class UnethicalBlastFurnacePlugin extends LoopedPlugin
{

    private static final Set<Integer> VALID_DEPOSIT_IDS = ImmutableSet.of(436, 438, 440, 442, 444, 446, 447, 449, 451, 453, 1617, 1619, 1621, 1623, 1625, 1627, 1629, 1631, 6571, 19496);

    private static final WorldPoint BANK_LOCATION = new WorldPoint(3758, 5666, 0);

    @Inject
    private Client client;
    @Inject
    private UnethicalBlastFurnaceConfig config;

    @Inject
    private ConfigManager configManager;

    private boolean needToEmpty = true;


    @Subscribe
    public void onConfigButtonPressed(ConfigButtonClicked event)
    {
        if (!event.getGroup().contains("unethical-UnethicalBlastFurnace"))
        {
            return;
        }

    }

    private boolean checkInBlastfurnace()
    {
        GameState gameState = client.getGameState();
        if (gameState != GameState.LOGGED_IN
                && gameState != GameState.LOADING)
        {
            return false;
        }

        int[] currentMapRegions = client.getMapRegions();

        TODO

        return true;
    }

    @Override
    protected int loop()
    {
        if (!config.isEnabled() || !checkInBlastfurnace())
        {
            needToEmpty = true;
            return 1000;
        }

        if(client.getLocalPlayer().isAnimating())
        {
            log.info("Animating so idling for a bit");
            return 250;
        }

        if(client.getLocalPlayer().isMoving() && Movement.getDestination().distanceTo(client.getLocalPlayer().getWorldLocation()) > 3)
        {
            log.info("Moving so idling for a bit");
            return 250;
        }

        if(handleDepositValidIds())
        {
            log.info("Attempted to deposit pre-emptive pass");
            return 250;
        }

        if(handleCollectOre())
        {
            log.info("Attempted to collect ore");
            return 250;
        }

        if(handlePlaceOre())
        {
            log.info("Attempted to place ore");
            return 250;
        }

        if(handleMoveToDispenser())
        {
            log.info("Attempted to move to dispenser");
            return 250;
        }

        if(handleEquipIceGloves())
        {
            log.info("Attempted to equip gloves");
            return 250;
        }

        if(handleCollectBars())
        {
            log.info("Attempted to collect bars");
            return 250;
        }

        if(handleMoveToBank())
        {
            log.info("Attempted to move to bank");
            return 250;
        }

        if(handleSwapGlovesBack())
        {
            log.info("Attempted to swap gloves back");
            return 250;
        }

        log.info("End of switch, idling");
        return 1000;
    }

    private boolean handleCollectOre()
    {
        if (!Inventory.isFull())
        {

        }
    }

    private boolean handleDepositValidIds()
    {
        if (!Inventory.contains(x -> VALID_DEPOSIT_IDS.contains(x.getId())))
            return false;

        if (Bank.isOpen())
        {
            Item bankItemToDeposit = Bank.Inventory.getFirst(x -> VALID_DEPOSIT_IDS.contains(x.getId()));

            if (bankItemToDeposit == null)
            {
                log.info("no bank item to deposit");
                return false;
            }

            bankItemToDeposit.interact("Deposit-All");
            return true;
        }
        else
        {
            TileObject bank = TileObjects.getNearest(x -> x.hasAction("Bank") && x.getName().toLowerCase().contains("bank"));
            if (bank == null)
            {
                log.info("no bank so attempting to move to bank location");
                return handleMoveToBank();
            }

            if (bank.getWorldArea().offset(1).toWorldPointList().stream().noneMatch(Reachable::isWalkable))
            {
                log.info("bank is not walkable so attempting to walk to bank");
                return handleMoveToBank();
            }

            bank.interact("Bank");
            return true;
        }


    }

    private boolean handleMoveToBank()
    {
        TODO
        return Movement.walkTo(BANK_LOCATION);
    }

    @Subscribe
    private void onGameTick(GameTick e)
    {

    }

    @Provides
    UnethicalBlastFurnaceConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(UnethicalBlastFurnaceConfig.class);
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
