package net.unethicalite.plugins.quicktoggler;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Prayer;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.ComponentID;
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
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

@Extension
@PluginDescriptor(
        name = "Unethical Quick Toggler",
        description = "Quick Toggler",
        enabledByDefault = false
)
@Slf4j
public class QuickTogglerPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private QuickTogglerConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private QuickTogglerOverlay QuickTogglerOverlay;

    public NPC enemyNPC = null;

    private int nextInteractionTick = 0;

    @Override
    protected void startUp()
    {
        overlayManager.add(QuickTogglerOverlay);
    }

    @Override
    public void shutDown()
    {
        overlayManager.remove(QuickTogglerOverlay);
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
    QuickTogglerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(QuickTogglerConfig.class);
    }

    @Subscribe
    private void onGameTick(GameTick tick)
    {
        if (!config.isEnabled() || nextInteractionTick > client.getTickCount())
        {
            return;
        }

        if (client.getLocalPlayer().getWorldLocation().getRegionID() == 14131)
        {
            Widget prayersContainer = client.getWidget(ComponentID.QUICK_PRAYER_PRAYERS);
            if (prayersContainer == null)
            {
                Widget prayerOrb = Widgets.get(WidgetInfo.MINIMAP_QUICK_PRAYER_ORB);
                prayerOrb.interact("Setup");
                return;
            }
        }

        if (config.attackRepeatedly() && !config.attackOrder().isEmpty())
        {
            String[] attackListStrings = config.attackOrder().split(",");
            for (String attackString : attackListStrings)
            {
                NPC nearest = NPCs.getNearest(Integer.parseInt(attackString));
                if (nearest != null && nearest.hasAction("Attack") && nearest.getHealthRatio() != 0 && !nearest.isDead())
                {
                    nearest.interact("Attack");
                    break;
                }
            }
        }


        var meleeInts = Arrays.stream(config.meleeWhitelist().split(",")).map(x -> Integer.parseInt(x)).collect(Collectors.toList());
        var rangedInts = Arrays.stream(config.rangedWhitelist().split(",")).map(x -> Integer.parseInt(x)).collect(Collectors.toList());
        var magicInts = Arrays.stream(config.magicWhitelist().split(",")).map(x -> Integer.parseInt(x)).collect(Collectors.toList());

        Prayer prayerToPray = null;

        enemyNPC = NPCs.getNearest(x -> x != null && x.getHealthRatio() != 0 && x.hasAction("Attack") && !x.isDead() && (meleeInts.contains(x.getId()) || rangedInts.contains(x.getId()) || magicInts.contains(x.getId())));
        if (enemyNPC == null)
            return;
        if (enemyNPC != null && meleeInts.contains(enemyNPC.getId()))
        {
            prayerToPray = Prayer.PROTECT_FROM_MELEE;
        }
        if (enemyNPC != null && rangedInts.contains(enemyNPC.getId()))
        {
            prayerToPray = Prayer.PROTECT_FROM_MISSILES;
        }
        if (enemyNPC != null && magicInts.contains(enemyNPC.getId()))
        {
            prayerToPray = Prayer.PROTECT_FROM_MAGIC;
        }

        if (enemyNPC != null && prayerToPray != null)
        {
            if (!Prayers.isQuickPrayerEnabled() && Prayers.getPoints() > 0)
                Prayers.toggleQuickPrayer(true);
            if (!Prayers.isEnabled(prayerToPray))
            {
                Widget prayersContainer = client.getWidget(ComponentID.QUICK_PRAYER_PRAYERS);
                if (prayersContainer == null)
                {
                    return;
                }
                else
                {
                    Widget[] prayerWidgets = prayersContainer.getDynamicChildren();
                    for (Widget prayerWidget : prayerWidgets)
                    {
                        String widgetString = prayerWidget.getName().toLowerCase().replace('_',' ').strip();
                        String prayerString = prayerToPray.name().toLowerCase().replace('_',' ').strip();
                        if (widgetString.contains(prayerString))
                        {
                            nextInteractionTick = client.getTickCount() + 5;
                            invokeAction(prayerWidget.getMenu(0), prayerWidget.getCanvasLocation().getX(), prayerWidget.getCanvasLocation().getY());
                            return;
                        }
                    }

                }
            }
        }
    }
}
