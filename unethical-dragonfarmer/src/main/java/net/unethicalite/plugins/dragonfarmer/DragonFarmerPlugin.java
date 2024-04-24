package net.unethicalite.plugins.dragonfarmer;

import com.google.inject.Provides;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.game.Combat;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.plugins.LoopedPlugin;
import net.unethicalite.api.widgets.Prayers;
import net.unethicalite.api.widgets.Widgets;
import net.unethicalite.client.Static;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


@Extension
@PluginDescriptor(
        name = "Unethical DragonFarmer",
        description = "DragonFarmer",
        enabledByDefault = false
)
@Slf4j
public class DragonFarmerPlugin extends LoopedPlugin
{

    @Inject
    private Client client;
    @Inject
    private DragonFarmerConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private OverlayManager overlayManager;

    private boolean needToMove = false;
    private int state = 0; // 0 is needing to bank, 1 is needing to kill dragons, 2 is needing to restore prayer

    private final WorldPoint DRAGON_LOCATION = new WorldPoint(1946, 8994, 1);
    private final WorldPoint BANK_LOCATION = new WorldPoint(2465, 2848, 1);


    @Subscribe
    public void onConfigButtonPressed(ConfigButtonClicked event)
    {
        if (!event.getGroup().contains("unethical-dragonfarmer"))
        {
            return;
        }

    }

    @Override
    protected int loop()
    {
        if (!config.isEnabled())
        {
            state = 0;
            return 1000;
        }

        Player localPlayer = client.getLocalPlayer();

        if (localPlayer == null)
        {
            log.info("Local player is null");
            return 1000;
        }

        if (handleEmergencyCheck())
        {
            log.info("EMERGENCY TRIGGERED!");
            return 500;
        }

        log.info("Current state: {}", state);

        if (handleMoveToStateLocation()) // This will update the state for us
        {
            log.info("Attempted to move to state location: {}", state);
            return 500;
        }

        if(state == 0)
        {
            if (Prayers.isQuickPrayerEnabled())
            {
                Prayers.toggleQuickPrayer(false);
                log.info("Attempted to turn off prayers");
                return 500;
            }

            if (!Bank.isOpen())
            {
                TileObject bankChest = TileObjects.getNearest(x -> x.getId() == 30087);
                if (bankChest == null)
                {
                    log.info("Could not find bank chest");
                    return 500;
                }

                bankChest.interact("Use");
                return 500;
            }
            else
            {
                Item cape = Inventory.getFirst(x -> x.getName().toLowerCase().contains("mythical cape"));
                Item antifire = Inventory.getFirst(x -> x.getName().toLowerCase().contains("antifire"));
                Item dagger = Inventory.getFirst(x -> x.getName().toLowerCase().contains("dragon dagger"));
                Item whip = Inventory.getFirst(x -> x.getName().toLowerCase().contains("whip"));
                Item itemToDeposit = Inventory.getFirst(x -> x.getId() != cape.getId() && x.getId() != antifire.getId() && x.getId() != dagger.getId() && x.getId() != whip.getId());

                if (cape == null)
                {
                    log.info("Attempted to withdraw cape");
                    Bank.withdraw(x -> x.getId() == cape.getId(), 1, Bank.WithdrawMode.ITEM);
                    return 500;
                }

                if (antifire == null)
                {
                    log.info("Attempted to withdraw antifire");
                    Bank.withdraw(x -> x.getName().toLowerCase().contains("antifire"), 1, Bank.WithdrawMode.ITEM);
                    return 500;
                }

                if (itemToDeposit != null)
                {
                    log.info("Attempted to deposit item");
                    Bank.depositAll(x -> x.getId() == itemToDeposit.getId());
                    return 500;
                }

                log.info("Nothing to deposit or withdraw, moving on");
                if (Prayers.getPoints() <= 10)
                    state = 2;
                else
                    state = 1;
                needToMove = true;
                cape.interact("Teleport");
                return 500;

            }

        }
        else if (state == 1)
        {
            if (!Prayers.isQuickPrayerEnabled())
            {
                Prayers.toggleQuickPrayer(true);
                return 500;
            }

            if (!Combat.isAntifired())
            {
                Item antifire = Inventory.getFirst(x -> x.getName().toLowerCase().contains("antifire"));
                if (antifire == null)
                {
                    log.info("antifire is null so we are banking early");
                    state = 0;
                    Item cape = Inventory.getFirst(x -> x.getName().toLowerCase().contains("mythical cape"));
                    if (cape == null)
                    {
                        log.info("Cape is null");
                        return 500;
                    }

                    cape.interact("Teleport");
                    return 500;
                }

                antifire.interact("Drink");
                return 500;
            }

            if (Prayers.getPoints() < 5)
            {
                log.info("Prayer is low, banking early");
                state = 0;

                Item cape = Inventory.getFirst(x -> x.getName().toLowerCase().contains("mythical cape"));
                if (cape == null)
                {
                    log.info("Cape is null");
                    return 500;
                }

                cape.interact("Teleport");
                return 500;
            }

            if (Inventory.isFull())
            {
                state = 0;
                Item cape = Inventory.getFirst(x -> x.getName().toLowerCase().contains("mythical cape"));
                if (cape == null)
                {
                    log.info("Cape is null");
                    return 1000;
                }

                cape.interact("Teleport");
                return 500;
            }

        }
        else if (state == 2)
        {
            if (Prayers.isQuickPrayerEnabled())
            {
                Prayers.toggleQuickPrayer(false);
                return 500;
            }

            Item houseTab = Inventory.getFirst(x -> x.getId() == ItemID.TELEPORT_TO_HOUSE);
            if (houseTab != null)
            {
                houseTab.interact("Break");
                return 500;
            }

            if (Prayers.getPoints() > 10)
            {
                state = 1;

                Item cape = Inventory.getFirst(x -> x.getName().toLowerCase().contains("mythical cape"));
                if (cape == null)
                {
                    log.info("Cape is null");
                    return 500;
                }

                cape.interact("Teleport");
                return 500;
            }

            TileObject altar = TileObjects.getNearest(x -> x.hasAction("Pray"));
            if (altar != null)
            {
                altar.interact("Pray");
                return 500;
            }
        }
        else
        {
            log.info("Unknown state.");
            return 1000;
        }

        log.info("End of switch, idling");
        return 1000;
    }

    private boolean handleMoveToStateLocation()
    {
        Player localPlayer = client.getLocalPlayer();
        if (state == 0)
        {
            TileObject bankEntrance = TileObjects.getNearest(x -> x.getId() == 31627);
            if (bankEntrance != null)
            {
                bankEntrance.interact("Climb-up");
                return true;
            }

            if (needToMove && localPlayer.getWorldLocation().distanceTo(BANK_LOCATION) > 3)
            {
                Movement.walkTo(BANK_LOCATION);
                return true;
            }

            if (needToMove)
            {
                needToMove = false;
                return false;
            }

            return false;
        }
        else if (state == 1)
        {
            TileObject basementEntrance = TileObjects.getNearest(x -> x.getId() == 31626);
            if (basementEntrance != null)
            {
                basementEntrance.interact("Enter");
                return true;
            }

            if (!Prayers.isQuickPrayerEnabled())
            {
                Prayers.toggleQuickPrayer(true);
                return true;
            }

            if (needToMove && localPlayer.getWorldLocation().distanceTo(DRAGON_LOCATION) > 3)
            {
                Movement.walkTo(DRAGON_LOCATION);
                return true;
            }

            if (needToMove)
            {
                configManager.setConfiguration("lucid-combat", "enabledByConfig", true);
                needToMove = false;
                return false;
            }

            return false;
        }
        else if (state == 2)
        {
            return false;
        }
        else
        {
            log.info("Unknown move state");
            return false;
        }
    }
    private boolean handleEmergencyCheck()
    {

        if (Combat.getCurrentHealth() < 50)
        {
            configManager.setConfiguration("unethical-dragonfarmer", "isEnabled", false);
            Item cape = Inventory.getFirst(x -> x.getName().toLowerCase().contains("mythical cape"));
            if (cape == null)
            {
                log.info("Cape is null");
                return false;
            }

            cape.interact("Teleport");
            return true;
        }
        return false;
    }


    @Subscribe
    private void onGameTick(GameTick e)
    {

        if (!config.isEnabled())
        {
            return;
        }

    }

    @Provides
    DragonFarmerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(DragonFarmerConfig.class);
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
