package net.unethicalite.plugins.flicker;

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
import org.pf4j.Extension;


import javax.inject.Inject;
@Extension
@PluginDescriptor(
        name = "Unethical Flicker",
        description = "Auto flicker",
        enabledByDefault = false
)
@Slf4j
public class FlickerPlugin extends Plugin
{
    @Inject
    private Client client;

//    @Inject
//    private FlickerConfig config;

    private Prayer lastTickOverhead = null;

    @Override
    protected void startUp()
    {
        return;
    }

    @Override
    public void shutDown()
    {
        return;
    }

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
    private void onGameTick(GameTick tick)
    {
//        if (!config.enabled())
//            return;
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
                    Thread.sleep((long)((Math.random() * 10) + 5));
                } catch (InterruptedException e) {
                    log.info("Sleep failed in PrayerFlicker: {}", e.toString());
                }
                invokeAction(widget.getMenu(0), widget.getOriginalX(), widget.getOriginalY());
            }
        }
    }
}