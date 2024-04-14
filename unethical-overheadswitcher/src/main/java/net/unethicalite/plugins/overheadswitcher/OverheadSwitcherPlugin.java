package net.unethicalite.plugins.overheadswitcher;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Prayer;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.unethicalite.api.entities.NPCs;
import net.unethicalite.api.events.MenuAutomated;
import net.unethicalite.api.game.GameThread;
import net.unethicalite.api.packets.MousePackets;
import net.unethicalite.api.widgets.Prayers;
import net.unethicalite.api.widgets.Widgets;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.util.Objects;

@Extension
@PluginDescriptor(
        name = "Unethical Overhead Switcher",
        description = " Overhead Switcher",
        enabledByDefault = false
)
@Slf4j
public class OverheadSwitcherPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private OverheadSwitcherConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private OverheadSwitcherOverlay overheadSwitcherOverlay;

    public NPC enemyNPC = null;

    @Override
    protected void startUp()
    {
        overlayManager.add(overheadSwitcherOverlay);
    }

    @Override
    public void shutDown()
    {
        overlayManager.remove(overheadSwitcherOverlay);
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

    @Provides
    OverheadSwitcherConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(OverheadSwitcherConfig.class);
    }

    @Subscribe
    private void onGameTick(GameTick tick)
    {
        if (!config.isEnabled())
        {
            Prayers.disableAll();
            return;
        }

        enemyNPC = NPCs.getNearest(x -> x.getName().contains(config.mobWhitelist()));
        if (config.mobBlacklist() != null && !config.mobBlacklist().isEmpty())
            enemyNPC = NPCs.getNearest(x -> x.getName().contains(config.mobWhitelist()) && !x.getName().contains(config.mobBlacklist()));
        if (enemyNPC != null)
        {
            if (enemyNPC.getWorldArea().canMelee(client, client.getLocalPlayer().getWorldArea()))
            {
                if (config.quickPrayMelee())
                {
                    if (Prayers.isQuickPrayerEnabled())
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
                    else
                    {
                        Widget widget = Widgets.get(WidgetInfo.MINIMAP_QUICK_PRAYER_ORB);
                        if (widget == null) {
                            return;
                        }
                        invokeAction(widget.getMenu("Activate"), widget.getOriginalX(), widget.getOriginalY());
                        try {
                            Thread.sleep((long)((Math.random() * 10) + 5));
                        } catch (InterruptedException e) {
                            log.info("Sleep failed in PrayerFlicker: {}", e.toString());
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
                else
                {
                    Prayer prayerToUse = Prayer.PROTECT_FROM_MELEE;
                    Prayers.disableAll();
                    if (Prayers.isEnabled(prayerToUse))
                    {
                        Widget widget = Widgets.get(prayerToUse.getWidgetInfo());
                        if (widget != null) {
                            invokeAction(widget.getMenu(0), widget.getOriginalX(), widget.getOriginalY());
                            try {
                                Thread.sleep((long)((Math.random() * 10) + 5));
                            } catch (InterruptedException e) {
                                log.info("Sleep failed in PrayerFlicker: {}", e.toString());
                            }
                            invokeAction(widget.getMenu(0), widget.getOriginalX(), widget.getOriginalY());
                        }
                    }
                    else
                    {
                        Widget widget = Widgets.get(prayerToUse.getWidgetInfo());
                        if (widget != null) {
                            invokeAction(widget.getMenu(0), widget.getOriginalX(), widget.getOriginalY());
                            try {
                                Thread.sleep((long)((Math.random() * 10) + 5));
                            } catch (InterruptedException e) {
                                log.info("Sleep failed in PrayerFlicker: {}", e.toString());
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
            else
            {
                if (!config.quickPrayMelee())
                {
                    if (Prayers.isQuickPrayerEnabled())
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
                    else
                    {
                        Widget widget = Widgets.get(WidgetInfo.MINIMAP_QUICK_PRAYER_ORB);
                        if (widget == null) {
                            return;
                        }
                        invokeAction(widget.getMenu("Activate"), widget.getOriginalX(), widget.getOriginalY());
                        try {
                            Thread.sleep((long)((Math.random() * 10) + 5));
                        } catch (InterruptedException e) {
                            log.info("Sleep failed in PrayerFlicker: {}", e.toString());
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
                else
                {
                    Prayer prayerToUse = config.prayRanged() ? Prayer.PROTECT_FROM_MISSILES : Prayer.PROTECT_FROM_MAGIC;
                    Prayers.disableAll();
                    if (Prayers.isEnabled(prayerToUse))
                    {
                        Widget widget = Widgets.get(prayerToUse.getWidgetInfo());
                        if (widget != null) {
                            invokeAction(widget.getMenu(0), widget.getOriginalX(), widget.getOriginalY());
                            try {
                                Thread.sleep((long)((Math.random() * 10) + 5));
                            } catch (InterruptedException e) {
                                log.info("Sleep failed in PrayerFlicker: {}", e.toString());
                            }
                            invokeAction(widget.getMenu(0), widget.getOriginalX(), widget.getOriginalY());
                        }
                    }
                    else
                    {
                        Widget widget = Widgets.get(prayerToUse.getWidgetInfo());
                        if (widget != null) {
                            invokeAction(widget.getMenu(0), widget.getOriginalX(), widget.getOriginalY());
                            try {
                                Thread.sleep((long)((Math.random() * 10) + 5));
                            } catch (InterruptedException e) {
                                log.info("Sleep failed in PrayerFlicker: {}", e.toString());
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

        }
    }
}