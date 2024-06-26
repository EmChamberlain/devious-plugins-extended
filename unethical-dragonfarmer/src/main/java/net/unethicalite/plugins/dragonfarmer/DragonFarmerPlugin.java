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

    private final WorldPoint GREEN_DRAGON_LOCATION = new WorldPoint(1943, 8996, 1);
    private final WorldPoint BLUE_DRAGON_LOCATION = new WorldPoint(1932, 8975, 1);

    private final WorldPoint BANK_LOCATION = new WorldPoint(2465, 2848, 1);
    private boolean needToSetConfig = true;


    @Subscribe
    public void onConfigButtonPressed(ConfigButtonClicked event)
    {
        if (!event.getGroup().contains("unethical-dragonfarmer"))
        {
            return;
        }

    }

    public void setConfigForLucidCombat()
    {
        configManager.setConfiguration("lucid-combat", "npcToFight", "Blue dragon");
        configManager.setConfiguration("lucid-combat", "maxRange", 10);
        configManager.setConfiguration("lucid-combat", "antilureProtection", true);
        configManager.setConfiguration("lucid-combat", "antilureProtectionRange", 3);
        configManager.setConfiguration("lucid-combat", "useSafespot", false);
        configManager.setConfiguration("lucid-combat", "buryScatter", false);
        configManager.setConfiguration("lucid-combat", "enableAutoSpec", true);
        configManager.setConfiguration("lucid-combat", "mainWeapon", "lance");
        configManager.setConfiguration("lucid-combat", "specWeapon", "gon dagger");

    }

    @Override
    protected int loop()
    {
        if (!config.isEnabled())
        {
            state = 0;
            needToSetConfig = true;
            return 1000;
        }

        if (needToSetConfig)
        {
            setConfigForLucidCombat();
            needToSetConfig = false;
        }


        if (Math.random() < 0.1)
        {
            if (Math.random() < 0.5)
            {
                client.setMouseIdleTicks(0);
            }
            else
            {
                client.setKeyboardIdleTicks(0);
            }
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
            configManager.setConfiguration("unethical-dragonfarmer", "isEnabled", false);//we want to stop doing what we were just doing after teleporting out
            configManager.setConfiguration("hootautologin",  "neverIdle", false);//this is just so we actually log out eventually
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
                Item hasta = Inventory.getFirst(x -> x.getName().toLowerCase().contains("hasta"));
                Item fang = Inventory.getFirst(x -> x.getName().toLowerCase().contains("fang"));
                Item lance = Inventory.getFirst(x -> x.getName().toLowerCase().contains("lance"));
                List<Integer> validIds = List.of(cape == null ? -1 : cape.getId(), antifire == null ? -1 : antifire.getId(), dagger == null ? -1 : dagger.getId(), whip == null ? -1 : whip.getId(), hasta == null ? -1 : hasta.getId(), fang == null ? -1 : fang.getId(), ItemID.OSMUMTENS_FANG, lance == null ? -1 : lance.getId());
                Item itemToDeposit = Inventory.getFirst(x -> !validIds.contains(x.getId()));

                if (itemToDeposit != null)
                {
                    log.info("Attempted to deposit item");
                    Bank.depositAll(x -> x.getId() == itemToDeposit.getId());
                    return 500;
                }

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



                log.info("Nothing to deposit or withdraw, moving on");
                needToMove = true;
                if (Prayers.getPoints() <= 10)
                {
                    state = 2;
                    Bank.withdraw(x -> x.getId() == ItemID.TELEPORT_TO_HOUSE, 1, Bank.WithdrawMode.ITEM);
                    return 500;

                }
                else
                {
                    state = 1;
                    needToMove = true;
                    cape.interact("Teleport");
                    return 2500;
                }

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

                    needToMove = true;
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

                needToMove = true;
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

                needToMove = true;
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
                needToMove = true;
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

                needToMove = true;
                cape.interact("Teleport");
                return 500;
            }

            TileObject altar = TileObjects.getNearest(x -> x.hasAction("Drink"));
            if (altar != null)
            {
                altar.interact("Drink");
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

    private WorldPoint getDragonLocation()
    {
        return BLUE_DRAGON_LOCATION;//TODO expand this
    }

    private boolean handleMoveToStateLocation()
    {
        Player localPlayer = client.getLocalPlayer();
        if (state == 0)
        {
            configManager.setConfiguration("lucid-combat", "disabledByConfig", true);
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

            if (needToMove && localPlayer.getWorldLocation().distanceTo(getDragonLocation()) > 3)
            {
                Movement.walkTo(getDragonLocation());
                return true;
            }

            if (needToMove)
            {
                configManager.setConfiguration("lucid-combat", "enabledByConfig", true);
                needToMove = false;
                return false;
            }

            if (localPlayer.getWorldLocation().distanceTo(getDragonLocation()) > 50)
            {
                Movement.walkTo(getDragonLocation());
                return true;
            }

            return false;
        }
        else if (state == 2)
        {
            configManager.setConfiguration("lucid-combat", "disabledByConfig", true);
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
