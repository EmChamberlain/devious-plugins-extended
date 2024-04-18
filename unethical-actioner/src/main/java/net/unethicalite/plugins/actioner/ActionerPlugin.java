package net.unethicalite.plugins.actioner;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
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
import net.unethicalite.api.events.WorldHopped;
import net.unethicalite.api.game.Game;
import net.unethicalite.api.game.Worlds;
import net.unethicalite.api.items.Inventory;
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
        ListIterator<Item> matchingItemsIterator;
        if (config.isId())
        {
            matchingItemsIterator = Inventory.getAll(x -> getIntListOfConfigString(config.dropItemsList()).contains(x.getId())).listIterator();
        }
        else
        {
            matchingItemsIterator = Inventory.getAll(x -> getStringListOfConfigString(config.dropItemsList()).contains(x.getName().toLowerCase())).listIterator();
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

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        if (!config.isEnabled())
            return;

        if (config.stopIfAnimating())
        {
            var local = client.getLocalPlayer();
            if (local != null)
            {
                if (local.isAnimating())
                    return;
            }
            else
            {
                log.info("local is null so stopping");
                return;
            }
        }

        if (config.use() && !config.isItem())
            configManager.setConfiguration("unethical-actioner", "isItem", true);

        Interactable interactable = null;

        if ((config.dropItemsIfFull() && Inventory.isFull()) || droppingItems)
        {
            droppingItems = handleDropItems();
            if (droppingItems)
                return;
        }

        if (config.isItem())
        {
            if (config.isId())
            {
                interactable = Inventory.getFirst(x -> getIntListOfConfigString(config.interactable()).contains(x.getId()));
            }
            else
            {
                interactable = Inventory.getFirst(x -> getStringListOfConfigString(config.interactable()).contains(x.getName().toLowerCase()));
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
                interactable = TileObjects.getNearest(x -> getStringListOfConfigString(config.interactable()).contains(x.getName().toLowerCase()));
                if (interactable == null)
                {
                    interactable = NPCs.getNearest(x -> getStringListOfConfigString(config.interactable()).contains(x.getName().toLowerCase()));
                }
            }
            if (interactable != null && ((Locatable) interactable).getWorldLocation().distanceTo(client.getLocalPlayer().getWorldLocation()) > config.maxRange())
            {
                log.info("Found one but not in range");
                interactable = null;
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
        }

       handleDropItems();
    }
}
