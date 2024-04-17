package net.unethicalite.plugins.actioner;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.World;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

    @Provides
    public ActionerConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ActionerConfig.class);
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        if (!config.isEnabled())
            return;

        Item item = Inventory.getFirst(x -> x.getName().toLowerCase().contains(config.item().toLowerCase()) && x.hasAction(config.action()));
        if (item == null)
        {
            log.info("No item found with action");
        }
        else
        {
            if (item.hasAction(config.action()))
            {
                item.interact(config.action());
            }
        }
    }
}
