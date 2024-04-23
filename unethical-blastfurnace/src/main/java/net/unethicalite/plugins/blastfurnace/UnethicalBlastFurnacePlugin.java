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
import net.unethicalite.api.input.Keyboard;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.DepositBox;
import net.unethicalite.api.items.Equipment;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.movement.Reachable;
import net.unethicalite.api.movement.pathfinder.GlobalCollisionMap;
import net.unethicalite.api.plugins.LoopedPlugin;
import net.unethicalite.api.widgets.Dialog;
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

    private static final Set<Integer> VALID_DEPOSIT_IDS = ImmutableSet.of(ItemID.IRON_BAR, ItemID.STEEL_BAR, ItemID.MITHRIL_BAR, ItemID.ADAMANTITE_BAR, ItemID.RUNITE_BAR, ItemID.GOLD_BAR, ItemID.SILVER_BAR);

    private static final WorldPoint BANK_LOCATION = new WorldPoint(1948, 4957, 0);

    private static final WorldPoint DISPENSER_LOCATION = new WorldPoint(1939, 4963, 0);

    private static final WorldPoint CONVEYOR_LOCATION = new WorldPoint(1941, 4968, 0);

    @Inject
    private Client client;
    @Inject
    private UnethicalBlastFurnaceConfig config;
    @Inject
    private ConfigManager configManager;

    int state = 0; // this is 0 for needing to move to bank, 1 for needing to move to conveyor, 2 for needing to move to dispenser and then repeat


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

        return client.getLocalPlayer().getWorldLocation().getRegionID() == 7757;
    }

    @Override
    protected int loop()
    {
        if (!config.isEnabled() || !checkInBlastfurnace())
        {
            return 1000;
        }

        log.info("Current state: {}", state);

        if(handleMoveToNextLocation()) // This will update the state variable for us
        {
            log.info("Attempted to move to the next location in the rotation");
            return 1000;
        }

        log.info("State now: {}", state);

        Player localPlayer = client.getLocalPlayer();

        if (state == 0)
        {
            if (localPlayer.getWorldLocation().distanceTo(BANK_LOCATION) > 1)
            {
                log.info("Waiting until at bank");
                return 500;
            }

            if(handleDepositValidIds())
            {
                log.info("Attempted to deposit pre-emptive pass");
                return 1000;
            }

            if(handleCollectOre())
            {
                log.info("Attempted to collect ore");
                return 1000;
            }

        }
        else if (state == 1)
        {

            if (localPlayer.getWorldLocation().distanceTo(CONVEYOR_LOCATION) > 1)
            {
                log.info("Waiting until at conveyor");
                return 500;
            }

            if(handlePlaceOre())
            {
                log.info("Attempted to place ore");
                return 1000;
            }
        }
        else if (state == 2)
        {
            if (localPlayer.getWorldLocation().distanceTo(DISPENSER_LOCATION) > 1)
            {
                log.info("Waiting until at dispenser");
                return 500;
            }

            if(handleCollectBars())
            {
                log.info("Attempted to collect bars");
                return 1000;
            }
        }

        log.info("End of switch, idling");
        return 1000;
    }

    private boolean handleMoveToNextLocation()
    {
        if (state == 0)
        {
            if (Inventory.contains(x -> x.getId() == config.oreToUse()))
            {
                state = 1;
                log.info("Walking to conveyor");
                Movement.walkTo(CONVEYOR_LOCATION);
                return true;
            }

            if (client.getLocalPlayer().getWorldLocation().distanceTo(BANK_LOCATION) > 1)
            {
                log.info("Walking to bank from location: {}", client.getLocalPlayer().getWorldLocation());
                Movement.walkTo(BANK_LOCATION);
                return true;
            }

            return false;
        }
        else if (state == 1)
        {
            if (!Inventory.contains(x -> x.getId() == config.oreToUse()))
            {
                state = 2;
                log.info("Walking to dispenser");
                Movement.walkTo(DISPENSER_LOCATION);
                return true;
            }

            if (client.getLocalPlayer().getWorldLocation().distanceTo(CONVEYOR_LOCATION) > 1)
            {
                log.info("Walking to conveyor");
                Movement.walkTo(CONVEYOR_LOCATION);
                return true;
            }

            return false;
        }
        else if (state == 2)
        {
            if (Inventory.contains(x -> VALID_DEPOSIT_IDS.contains(x.getId())))
            {
                state = 0;
                log.info("Walking to bank");
                Movement.walkTo(BANK_LOCATION);
                return true;
            }

            if (client.getLocalPlayer().getWorldLocation().distanceTo(DISPENSER_LOCATION) > 1)
            {
                log.info("Walking to dispenser");
                Movement.walkTo(DISPENSER_LOCATION);
                return true;
            }

            return false;
        }
        else
        {
            log.info("Unknown state. Doing nothing.");
            return false;
        }
    }

    private boolean handleCollectBars()
    {
        if (Inventory.contains(x -> x.getId() == config.oreToUse()) || Inventory.contains(x -> VALID_DEPOSIT_IDS.contains(x.getId())))
            return false;


        TileObject dispenserObject = TileObjects.getNearest(x -> x.getName().toLowerCase().contains("bar dispenser"));
        if (dispenserObject == null)
        {
            log.info("dispenser is null");
            return false;
        }

        if (!dispenserObject.hasAction("Take"))
        {
            log.info("dispenser not ready");
            return false;
        }

        Item equippedIce = Equipment.getFirst(x -> x.getId() == ItemID.ICE_GLOVES);
        Item inventoryIce = Inventory.getFirst(x -> x.getId() == ItemID.ICE_GLOVES);

        if (equippedIce == null)
        {
            if (inventoryIce == null)
            {
                log.info("No ice gloves");
                return false;
            }

            inventoryIce.interact("Wear");
            return true;
        }

        dispenserObject.interact("Take");
        return true;
    }

    private boolean handlePlaceOre()
    {
        if (!Inventory.contains(x -> x.getId() == config.oreToUse()))
            return false;

        TileObject conveyorBeltObject = TileObjects.getNearest(x -> x.hasAction("Put-ore-on"));
        if (conveyorBeltObject == null)
        {
            log.info("Conveyor is null");
            return false;
        }

        if (config.oreToUse() == ItemID.GOLD_ORE)
        {
            Item equippedGold = Equipment.getFirst(x -> x.getId() == ItemID.GOLDSMITH_GAUNTLETS);
            Item inventoryGold = Inventory.getFirst(x -> x.getId() == ItemID.GOLDSMITH_GAUNTLETS);

            if (equippedGold == null)
            {
                if (inventoryGold == null)
                {
                    log.info("No gold gloves");
                    return false;
                }

                inventoryGold.interact("Wear");
                return true;
            }
        }

        conveyorBeltObject.interact("Put-ore-on");
        return true;
    }

    private int getCoalToWithdraw()
    {

        if (config.oreToUse() == ItemID.IRON_ORE)
        {
            return 1;
        }
        else if (config.oreToUse() == ItemID.GOLD_ORE)
        {
            return 0;
        }
        else if (config.oreToUse() == ItemID.SILVER_ORE)
        {
            return 0;
        }
        else if (config.oreToUse() == ItemID.MITHRIL_ORE)
        {
            return 2;
        }
        else if (config.oreToUse() == ItemID.ADAMANTITE_ORE)
        {
            return 3;
        }
        else if (config.oreToUse() == ItemID.RUNITE_ORE)
        {
            return 4;
        }
        else return -1;
    }

    private boolean handleCollectOre()
    {
        TileObject dispenserObject = TileObjects.getNearest(x -> x.getName().toLowerCase().contains("bar dispenser"));

        if (Inventory.isFull() || (dispenserObject != null && dispenserObject.hasAction("Take")))
            return false;

        if (Bank.isOpen())
        {
            Item oreBankItem = Bank.Inventory.getFirst(x -> x.getId() == config.oreToUse());
            Item coalBankItem = Bank.Inventory.getFirst(x ->  x.getId() == ItemID.COAL);

            int slotsPer = getCoalToWithdraw() + 1;
            int barsCanMake = (int) Math.floor((double) Bank.Inventory.getFreeSlots() / slotsPer);
            int coalNeeded = barsCanMake * getCoalToWithdraw();

            if (getCoalToWithdraw() > 0 && Bank.getCount(ItemID.COAL) >= coalNeeded && !Inventory.contains(x -> x.getId() == ItemID.COAL))
            {
                log.info("Withdrawing {} amount of coal for the {} amount of ore", coalNeeded, barsCanMake);
                Bank.withdraw(x -> x.getId() == ItemID.COAL, coalNeeded, Bank.WithdrawMode.ITEM);
                return true;
            }

            if (Bank.getCount(config.oreToUse()) >= barsCanMake && !Inventory.contains(x -> x.getId() == config.oreToUse()))
            {
                log.info("Withdrawing {} amount of ore for the {} amount of coal", barsCanMake, coalNeeded);
                Bank.withdraw(x -> x.getId() == config.oreToUse(), barsCanMake, Bank.WithdrawMode.ITEM);
                return true;
            }

            if (oreBankItem != null && coalBankItem != null)
            {
                if (Bank.Inventory.getCount(x -> x.getId() == ItemID.COAL) / Bank.Inventory.getCount(x -> x.getId() == oreBankItem.getId()) != getCoalToWithdraw())
                {
                    log.info("Something went wrong and we don't have the right amount of ore and coal, depositing");
                    Bank.depositAll(x -> x.getId() == oreBankItem.getId() || x.getId() == coalBankItem.getId());
                    return true;
                }
                else
                {
                    log.info("Proper amount of ore in inventory so no need to do anything with the bank");
                    return false;
                }
            }
        }
        else
        {
            TileObject bankObject = TileObjects.getNearest(x -> x.hasAction("Use") && x.getName().toLowerCase().contains("bank"));
            if (bankObject == null)
            {
                log.info("bank object null");
                return false;
            }

            bankObject.interact("Use");
            return true;
        }
        return false;
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
            TileObject bank = TileObjects.getNearest(x -> x.hasAction("Use") && x.getName().toLowerCase().contains("bank"));
            if (bank == null)
            {
                log.info("no bank");
                return false;
            }

            if (bank.getWorldArea().offset(1).toWorldPointList().stream().noneMatch(Reachable::isWalkable))
            {
                log.info("bank is not walkable");
                return false;
            }

            bank.interact("Use");
            return true;
        }


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
