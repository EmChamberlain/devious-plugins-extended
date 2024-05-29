package com.datbear;

import com.google.inject.Provides;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.OPRSExternalPluginManager;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.xptracker.XpTrackerPlugin;
import net.runelite.client.ui.overlay.OverlayManager;

import net.unethicalite.api.plugins.LoopedPlugin;
import net.unethicalite.api.widgets.Widgets;
import net.unethicalite.client.Static;
import org.pf4j.Extension;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

import javax.inject.Inject;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


@Extension
@PluginDescriptor(
        name = "Unethical GOTR",
        description = "GOTR",
        enabledByDefault = false
)
@PluginDependency(GuardiansOfTheRiftHelperPlugin.class)
@Slf4j
public class GOTRPlugin extends LoopedPlugin
{

    private enum STATE
    {
        ENTER_AREA,
        COLLECT_MATS,
        MINE_FRAGS_SHORTCUT,
        CRAFT_ESSENCE,
        USE_ALTAR,
        MINE_FRAGS_PORTAL,
        REDEEM_USE_ALTAR,
        REDEEM_DEPOSIT
    }

    @Inject
    private Client client;
    @Inject
    private GOTRConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private OPRSExternalPluginManager pluginManager;


    private STATE state = STATE.ENTER_AREA;

    @Inject
    private GuardiansOfTheRiftHelperPlugin helperPlugin;


    @Subscribe
    public void onConfigButtonPressed(ConfigButtonClicked event)
    {
        if (!event.getGroup().contains("unethical-gotr"))
        {
            return;
        }

    }



    @Override
    protected int loop()
    {
        if (!config.isEnabled())
        {
            state = STATE.ENTER_AREA;
            return 1000;
        }

        if (helperPlugin == null)
        {
            log.info("Helper plugin is null attempting to get helper plugin");
            //helperPlugin = getHelperPlugin();
            if (helperPlugin == null)
                log.info("Unable to get helper plugin!");
            return 1000;
        }

        log.info("In minigame?: {}", helperPlugin.isInMinigame());

        log.info("End of switch, idling");
        return 1000;
    }

    /*private GuardiansOfTheRiftHelperPlugin getHelperPlugin()
    {
        List<PluginWrapper> plugins = pluginManager.getStartedPlugins();
        for (var pluginWrapper : plugins)
        {
            Plugin plugin = pluginWrapper.getPlugin();
            if (plugin instanceof GuardiansOfTheRiftHelperPlugin)
            {
                log.info("Found helper plugin");
                return (GuardiansOfTheRiftHelperPlugin) plugin;
            }
        }
        log.info("Did NOT find helper plugin");
        return null;
    }*/


    @Subscribe
    private void onGameTick(GameTick e)
    {

    }

    @Provides
    GOTRConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(GOTRConfig.class);
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
