package net.unethicalite.plugins.prayerflicker;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.unethicalite.api.events.MenuAutomated;
import net.unethicalite.api.game.GameThread;
import net.unethicalite.api.packets.MousePackets;
import net.unethicalite.api.widgets.Prayers;
import net.unethicalite.api.widgets.Widgets;


import javax.inject.Inject;
@Slf4j
@PluginDescriptor(
        name = "Unethical Prayer Flicker",
        description = "Auto prayer flicker",
        enabledByDefault = false
)
public class PrayerFlickerPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private PrayerFlickerConfig config;

    private void invokeAction(MenuAutomated entry, int x, int y)
    {
        GameThread.invoke(() ->
        {
            MousePackets.queueClickPacket(x, y);
            client.invokeMenuAction(entry.getOption(), entry.getTarget(), entry.getIdentifier(),
                    entry.getOpcode().getId(), entry.getParam0(), entry.getParam1(), x, y);
        });
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        if (!config.enabled())
            return;
        for (Prayer pray : Prayer.values())
        {
            if (Prayers.isEnabled(pray))
            {
                Widget widget = Widgets.get(pray.getWidgetInfo());
                if (widget == null) {
                    return;
                }
                invokeAction(widget.getMenu(0), widget.getOriginalX(), widget.getOriginalY());
                try {
                    Thread.sleep((long)((Math.random() * 50) + 25));
                } catch (InterruptedException e) {
                    log.info("Sleep failed in PrayerFlicker: {}", e.toString());
                }
                invokeAction(widget.getMenu(0), widget.getOriginalX(), widget.getOriginalY());
            }
        }
    }
}