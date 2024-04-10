package net.unethicalite.plugins.quickflicker;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
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
        name = "Unethical Quick Flicker",
        description = "Auto Quick Flicker",
        enabledByDefault = false
)
@Slf4j
public class QuickFlickerPlugin extends Plugin
{
    @Inject
    private Client client;

//    @Inject
//    private QuickFlickerConfig config;

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

        if(Prayers.isQuickPrayerEnabled())
        {
            Widget widget = Widgets.get(WidgetInfo.MINIMAP_QUICK_PRAYER_ORB);
            if (widget == null) {
                return;
            }
            invokeAction(widget.getMenu("Deactivate"), widget.getOriginalX(), widget.getOriginalY());
            try {
                Thread.sleep((long)((Math.random() * 10) + 5));
            } catch (InterruptedException e) {
                log.info("Sleep failed in PrayerFlicker: {}", e.toString());
            }
            invokeAction(widget.getMenu("Activate"), widget.getOriginalX(), widget.getOriginalY());
        }

    }
}