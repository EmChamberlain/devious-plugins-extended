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
import net.unethicalite.api.items.Equipment;
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

    private static final WorldPoint BANK_LOCATION = new WorldPoint(1948, 4957, 0);

    private static final WorldPoint DISPENSER_LOCATION = new WorldPoint(1942, 4967, 0);


    @Inject
    private Client client;
    @Inject
    private UnethicalBlastFurnaceConfig config;

    @Inject
    private ConfigManager configManager;

    private boolean needToEmpty = true;

    private boolean needToBeWearingIce = false;

    int gloveIdToSwapBackTo = -1;


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
            needToEmpty = true;
            return 1000;
        }

        if(handleEquipIceGloves())
        {
            log.info("Attempted to equip gloves");
            return 250;
        }

        if(handleSwapGlovesBack())
        {
            log.info("Attempted to swap gloves back");
            return 250;
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

        log.info("End of switch, idling");
        return 1000;
    }

    private boolean handleCollectBars()
    {
        if (Inventory.contains(x -> x.getId() == config.oreToUse()) || Inventory.contains(x -> VALID_DEPOSIT_IDS.contains(x.getId())))
            return false;


        TileObject dispenserObject = TileObjects.getNearest(x -> x.getName().toLowerCase().contains("bar dispenser"));
        if (dispenserObject == null)
        {
            log.info("dispenser is null, moving to dispenser");
            return handleMoveToDispenser();
        }

        dispenserObject.interact("Check");
        return true;
    }

    private boolean handlePlaceOre()
    {
        if (!Inventory.contains(x -> x.getId() == config.oreToUse()) || !Inventory.contains(x -> x.getId() == ItemID.COAL))
            return false;

        TileObject conveyorBeltObject = TileObjects.getNearest(x -> x.hasAction("Put-ore-on"));
        if (conveyorBeltObject == null)
        {
            log.info("Conveyor is null, moving to dispenser");
            return handleMoveToDispenser();
        }

        conveyorBeltObject.interact("Put-ore-on");
        return true;
    }

    private boolean handleSwapGlovesBack()
    {
        if (needToBeWearingIce)
            return false;

        Item glovesToSwapTo = Inventory.getFirst(x -> x.getId() == gloveIdToSwapBackTo);
        if (glovesToSwapTo == null)
        {
            log.info("No gloves to swap back to, shit is gonna break");
            return false;
        }

        gloveIdToSwapBackTo = -1;
        glovesToSwapTo.interact("Wear");
        return true;
    }

    private boolean handleEquipIceGloves()
    {
        if (!needToBeWearingIce || Equipment.contains(x -> x.getId() == ItemID.ICE_GLOVES))
            return false;

        Item iceGloves = Inventory.getFirst(x -> x.getId() == ItemID.ICE_GLOVES);
        if (iceGloves == null)
        {
            log.info("No ice gloves, shit is gonna break");
            return false;
        }

        gloveIdToSwapBackTo = Equipment.fromSlot(EquipmentInventorySlot.GLOVES).getId();
        iceGloves.interact("Wear");
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
        if (Inventory.isFull())
            return false;

        if (Bank.isOpen())
        {
            Item oreBankItem = Bank.Inventory.getFirst(x -> x.getId() == config.oreToUse());
            Item coalBankItem = Bank.Inventory.getFirst(x ->  x.getId() == ItemID.COAL);

            int slotsPer = getCoalToWithdraw() + 1;
            int barsCanMake = (int) Math.floor((double) Bank.Inventory.getFreeSlots() / slotsPer);

            if (oreBankItem == null && coalBankItem != null)
            {
                log.info("Something weird happened so depositing coal");
                Bank.depositAll(x -> x.getId() == coalBankItem.getId());
                return true;
            }

            if (oreBankItem == null && coalBankItem == null)
            {
                log.info("Withdrawing {} amount of ore for the {} amount of coal", barsCanMake, barsCanMake * getCoalToWithdraw());
                Bank.withdraw(x -> x.getId() == config.oreToUse(), barsCanMake, Bank.WithdrawMode.ITEM);
                return true;
            }

            if (oreBankItem != null && coalBankItem == null)
            {
                log.info("Withdrawing {} amount of coal for the ore", barsCanMake * getCoalToWithdraw());
                Bank.withdrawAll(x -> x.getId() == ItemID.COAL, Bank.WithdrawMode.ITEM);
                return true;
            }

            if (oreBankItem != null && coalBankItem != null)
            {
                if (Bank.Inventory.getCount(x -> x.getId() == ItemID.COAL) / Bank.Inventory.getCount(x -> x.getId() == oreBankItem.getId()) != getCoalToWithdraw())
                {
                    log.info("Something went wrong and we don't have the right amount of ore and coal, depositing ore");
                    Bank.depositAll(x -> x.getId() == oreBankItem.getId());
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
                log.info("bank object null, moving to bank");
                return handleMoveToBank();
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
                log.info("no bank so attempting to move to bank location");
                return handleMoveToBank();
            }

            if (bank.getWorldArea().offset(1).toWorldPointList().stream().noneMatch(Reachable::isWalkable))
            {
                log.info("bank is not walkable so attempting to walk to bank");
                return handleMoveToBank();
            }

            bank.interact("Use");
            return true;
        }


    }

    private boolean handleMoveToBank()
    {
        TileObject bankObject = TileObjects.getNearest(x -> x.hasAction("Use") && x.getName().toLowerCase().contains("bank"));
        if (bankObject != null || Bank.isOpen() || !Inventory.contains(x -> VALID_DEPOSIT_IDS.contains(x.getId())))
            return false;
        return Movement.walkTo(BANK_LOCATION);
    }

    private boolean handleMoveToDispenser()
    {
        TileObject dispenserObject = TileObjects.getNearest(x -> x.hasAction("Put-ore-on"));
        if (dispenserObject != null || !Inventory.contains(x -> config.oreToUse() == x.getId()))
            return false;
        return Movement.walkTo(DISPENSER_LOCATION);
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
