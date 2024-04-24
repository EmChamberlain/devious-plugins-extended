package net.unethicalite.plugins.actioner;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.unethicalite.api.EntityNameable;
import net.unethicalite.api.Interactable;
import net.unethicalite.api.entities.NPCs;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.events.LobbyWorldSelectToggled;
import net.unethicalite.api.events.LoginIndexChanged;
import net.unethicalite.api.events.MenuAutomated;
import net.unethicalite.api.events.WorldHopped;
import net.unethicalite.api.game.Game;
import net.unethicalite.api.game.Worlds;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.script.blocking_events.WelcomeScreenEvent;
import net.unethicalite.api.widgets.Widgets;
import org.pf4j.Extension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@PluginDescriptor(name = "Unethical Actioner", enabledByDefault = false)
@Extension
@Slf4j
public class ActionerPlugin extends Plugin
{
    @Inject
    private ActionerConfig config;

    @Inject
    private Client client;

    @Inject
    private ConfigManager configManager;
    private boolean droppingItems = false;


    int state = 0; // 0 for actioning, 1 for banking

    @Provides
    public ActionerConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ActionerConfig.class);
    }

    private List<Integer> getIntListOfConfigString(String confString)
    {
        return Stream.of(confString.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    private List<String> getStringListOfConfigString(String confString)
    {
        return Stream.of(confString.split(","))
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    private boolean handleDropItems()
    {
        ListIterator<Item> matchingItemsIterator = null;
        if (config.isId())
        {
            matchingItemsIterator = Inventory.getAll(x -> getIntListOfConfigString(config.dropItemsList()).contains(x.getId())).listIterator();
        }
        else
        {
            for (String dropString : getStringListOfConfigString(config.dropItemsList()))
            {
                matchingItemsIterator = Inventory.getAll(x -> x.getName().toLowerCase().contains(dropString)).listIterator();
                if (matchingItemsIterator.hasNext())
                    break;
            }
        }

        if (matchingItemsIterator == null)
        {
            return false;
        }

        boolean didDrop = false;
        for (int i = 0; i < config.dropsPerTick(); i++)
        {

            if (!matchingItemsIterator.hasNext())
                break;

            Item itemToDrop = matchingItemsIterator.next();
            log.info("Dropping item: {}", itemToDrop.getName());
            itemToDrop.interact("Drop");
            didDrop = true;
        }
        return didDrop;
    }

    private int getInvalidIdCount()
    {
        if (config.isId())
        {
           return Inventory.getCount(x -> {
               var validItems = getIntListOfConfigString(config.validItemsList());
               var depositItems = getIntListOfConfigString(config.depositItemsList());
               validItems.addAll(depositItems);
               return !validItems.contains(x.getId());
           });
        }
        else
        {
            var validItems = getStringListOfConfigString(config.validItemsList());
            var depositItems = getStringListOfConfigString(config.depositItemsList());
            validItems.addAll(depositItems);

            int count = 0;
            for (String validItem : validItems)
            {
                count += Inventory.getCount(x -> x.getName().toLowerCase().contains(validItem));
            }
            return count;
        }
    }

    private Interactable getBankInteractable()
    {
        TileObject bankChest = TileObjects.getNearest(x -> x.getName().toLowerCase().contains("bank chest") && x.hasAction( "Use"));
        if (bankChest != null)
            return bankChest;

        TileObject bankObject = TileObjects.getNearest(x -> x.hasAction( "Bank"));
        if (bankObject != null)
            return bankObject;

        return NPCs.getNearest(x -> x.hasAction( "Bank"));
    }

    private List<MenuAutomated> getMenuAutomatedList()
    {
        return Arrays.stream(config.widgetActionsToDo().split(",")).map(this::getMenuAutomated).collect(Collectors.toList());
    }

    private MenuAutomated getMenuAutomated(String stringIn)
    {
        String[] splitString = stringIn.split("\\|");
        return MenuAutomated.builder().option(splitString[0]).target(splitString[1]).identifier(Integer.parseInt(splitString[2])).opcode(MenuAction.of(Integer.parseInt(splitString[3]))).param0(Integer.parseInt(splitString[4])).param1(Integer.parseInt(splitString[5])).build();
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        Player localPlayer = client.getLocalPlayer();


        if (config.setNewBank())
        {
            configManager.setConfiguration("unethical-actioner", "setNewBank", false);
            configManager.setConfiguration("unethical-actioner", "bankLocation", localPlayer.getWorldLocation());
        }

        if (!config.isEnabled())
        {
            configManager.setConfiguration("unethical-actioner", "startLocation", null);
            state = 0;
            return;
        }

        if (config.startLocation() == null)
        {
            configManager.setConfiguration("unethical-actioner", "startLocation", localPlayer.getWorldLocation());
        }


        if (config.use() && !config.isItem())
            configManager.setConfiguration("unethical-actioner", "isItem", true);

        var widgetList = getIntListOfConfigString(config.widgetsToSelect());
        var menuAutomatedList = getMenuAutomatedList();
        for (int i = 0; i <  widgetList.size(); i++)
        {

            Widget widget = Widgets.fromId(widgetList.get(i));
            if (widget != null)
            {
                client.interact(menuAutomatedList.get(i));
                log.info("Tried to interact with menu: {}", menuAutomatedList.get(i).toString());
                return;
            }
        }




        if (state == 0)
        {
            if ((config.dropItemsIfFull() && Inventory.isFull()) || droppingItems)
            {
                droppingItems = handleDropItems();
                if (droppingItems)
                    return;
            }

            if (Inventory.isFull() && config.toBank() && getInvalidIdCount() <= 0)
            {
                state = 1;
                Movement.walkTo(config.bankLocation());
                return;
            }

            Interactable interactable = null;

            if (config.isItem())
            {
                if (config.isId())
                {
                    interactable = Inventory.getFirst(x -> getIntListOfConfigString(config.interactable()).contains(x.getId()));
                }
                else
                {
                    for (String interactableString : getStringListOfConfigString(config.interactable()))
                    {
                        interactable = Inventory.getFirst(x -> x.getName().toLowerCase().contains(interactableString));
                        if (interactable != null)
                            break;
                    }
                }
            }
            else
            {
                if (config.isId())
                {
                    interactable = TileObjects.getNearest(x -> getIntListOfConfigString(config.interactable()).contains(x.getId()));
                    if (interactable == null)
                    {
                        interactable = NPCs.getNearest(x -> getIntListOfConfigString(config.interactable()).contains(x.getId()));
                    }
                }
                else
                {
                    for (String interactableString : getStringListOfConfigString(config.interactable()))
                    {
                        interactable = TileObjects.getNearest(x -> x.getName().toLowerCase().contains(interactableString));
                        if (interactable == null)
                        {
                            interactable = NPCs.getNearest(x -> x.getName().toLowerCase().contains(interactableString));
                        }

                        if (interactable != null)
                            break;

                    }
                }
                if (interactable != null && ((Locatable) interactable).getWorldLocation().distanceTo(client.getLocalPlayer().getWorldLocation()) > config.maxRange())
                {
                    log.info("Found one but not in range");
                    interactable = null;
                }
            }

            if (config.stopIfAnimating())
            {
                if (localPlayer != null)
                {
                    if (localPlayer.isAnimating())
                        return;
                }
                else
                {
                    log.info("local is null so stopping");
                    return;
                }
            }

            if (interactable != null)
            {
                if (config.use() && config.isItem())
                {
                    // We enforce that it must be the item branch, so we should be able to cast here
                    Item item = (Item) interactable;
                    TileObject nearObject = TileObjects.getNearest(x -> x.getName().toLowerCase().contains(config.action().toLowerCase()));
                    if (nearObject != null)
                    {
                        item.useOn(nearObject);
                        return;
                    }

                    NPC nearNPC = NPCs.getNearest(x -> x.getName().toLowerCase().contains(config.action().toLowerCase()));
                    if (nearNPC != null)
                    {
                        item.useOn(nearNPC);
                        return;
                    }
                }
                else
                {
                    if (interactable.hasAction(config.action()))
                    {
                        interactable.interact(config.action());
                        return;
                    }
                    else
                    {
                        log.info("No action for item: {} which has actions: {}", ((EntityNameable) interactable).getName(), interactable.getActions());
                    }
                }
            }
            else
            {
                log.info("No interactable");

                if (localPlayer.getWorldLocation().distanceTo(config.startLocation()) > 5)
                {
                    log.info("Moving to start location");
                    Movement.walkTo(config.startLocation());
                    return;
                }
            }

            handleDropItems();

        }
        else if (state == 1)
        {
            if (Bank.isOpen())
            {
                if (handleDepositItems())
                {
                    log.info("Attempted to deposit items");
                    return;
                }

                if (handleWithdrawItems())
                {
                    log.info("Attempted to deposit items");
                    return;
                }

                log.info("Done depositing and withdrawing so moving to next state.");
                state = 0;
                Movement.walkTo(config.startLocation());
                return;

            }
            else
            {
                Interactable bankInteractable = getBankInteractable();
                if (bankInteractable == null)
                {
                    log.info("Bank is null");
                    if (localPlayer.getWorldLocation().distanceTo(config.bankLocation()) > 5)
                    {
                        log.info("Moving to bank location");
                        Movement.walkTo(config.bankLocation());
                    }
                    return;
                }
                bankInteractable.interact("Bank", "Use");
            }
        }
        else
        {
            log.info("Unknown state");
        }
    }

    private boolean handleWithdrawItems()
    {
        Item withdrawItem = null;
        if (config.isId())
        {
            for (Integer withdrawInt : getIntListOfConfigString(config.withdrawItemsList()))
            {
                withdrawItem = Bank.Inventory.getFirst(x -> x.getId() == withdrawInt);
                if (withdrawItem == null)
                {
                    if (Bank.getCount(true, x -> x.getId() == withdrawInt) > 0)
                    {
                        Bank.withdrawLastQuantity(x -> x.getId() == withdrawInt, Bank.WithdrawMode.ITEM);
                        return true;
                    }
                    else
                    {
                        log.info("Missing withdraw item: {}", withdrawInt);
                        return true;
                    }
                }
            }

        }
        else
        {
            for (String withdrawString : getStringListOfConfigString(config.withdrawItemsList()))
            {
                withdrawItem = Bank.Inventory.getFirst(x -> x.getName().toLowerCase().contains(withdrawString));
                if (withdrawItem == null)
                {
                    Item bankWithdrawItem = Bank.getFirst(x -> x.getName().toLowerCase().contains(withdrawString));
                    if (bankWithdrawItem == null)
                    {
                        log.info("Missing withdraw item: {}", withdrawItem);
                        return true;
                    }
                    int withdrawInt = bankWithdrawItem.getId();
                    if (Bank.getCount(true, x -> x.getId() == withdrawInt) > 0)
                    {
                        Bank.withdrawLastQuantity(x -> x.getId() == withdrawInt, Bank.WithdrawMode.ITEM);
                        return true;
                    }
                    else
                    {
                        log.info("Missing withdraw item: {}", withdrawInt);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean handleDepositItems()
    {
        Item depositItem = null;
        if (config.isId())
        {
            depositItem = Bank.Inventory.getFirst(x -> getIntListOfConfigString(config.depositItemsList()).contains(x.getId()));
        }
        else
        {
            for (String depositString : getStringListOfConfigString(config.depositItemsList()))
            {
                depositItem = Inventory.getFirst(x -> x.getName().toLowerCase().contains(depositString));
                if (depositItem != null)
                    break;
            }
        }

        if (depositItem == null)
        {
            return false;
        }

        int idToDeposit = depositItem.getId();
        Bank.depositAll(x -> x.getId() == idToDeposit);
        return true;
    }
}
