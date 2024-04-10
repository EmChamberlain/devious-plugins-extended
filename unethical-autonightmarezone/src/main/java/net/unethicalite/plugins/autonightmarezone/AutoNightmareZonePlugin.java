package net.unethicalite.plugins.autonightmarezone;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginDescriptor;
import net.unethicalite.api.Interactable;
import net.unethicalite.api.entities.NPCs;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.events.MenuAutomated;
import net.unethicalite.api.game.GameThread;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.packets.MousePackets;
import net.unethicalite.api.plugins.LoopedPlugin;
import net.unethicalite.api.widgets.Prayers;
import net.unethicalite.api.widgets.Widgets;
import org.pf4j.Extension;

import java.util.ArrayList;

@Extension
@PluginDescriptor(
        name = "Unethical Auto NightmareZone",
        description = "Does NightmareZone for you",
        enabledByDefault = false
)
@Slf4j
public class AutoNightmareZonePlugin extends LoopedPlugin
{

    @Inject
    AutoNightmareZoneConfig config;

    @Provides
    AutoNightmareZoneConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AutoNightmareZoneConfig.class);
    }

    @Inject
    private Client client;

    private static final int YANILLE_REGION_ID = 10288;


    private static final WorldPoint potionLocation = new WorldPoint(2604, 3115, 0);

    private NPC lastInteracted = null;

    private ArrayList<String> toAttackNames = new ArrayList<>();

    @Override
    protected void startUp() throws Exception {
        super.startUp();
        toAttackNames.add("Trapped Soul");
        toAttackNames.add("Count Draynor");
        toAttackNames.add("Sand Snake");
        toAttackNames.add("King Roald");
        toAttackNames.add("The Kendal");
        toAttackNames.add("Me");
        toAttackNames.add("Skeleton Hellhound");
        toAttackNames.add("Tree spirit");
        toAttackNames.add("Dad");
        toAttackNames.add("Khazard Warlord");
        toAttackNames.add("Black Knight Titan");
        toAttackNames.add("Ice Troll King");
        toAttackNames.add("Bouncer");
        toAttackNames.add("Black Demon");
        toAttackNames.add("Jungle Demon");
    }
    @Override
    protected void shutDown() throws Exception {
        super.shutDown();
        return;
    }
    @Subscribe
    public void onInteractingChanged(InteractingChanged event)
    {
        var localPlayer = client.getLocalPlayer();
        if(event.getSource() != localPlayer)
        {
            return;
        }
        final Actor target = event.getTarget();
        if(!(target instanceof NPC))
        {
            return;
        }
        lastInteracted = (NPC) target;
    }

    @Override
    protected int loop() {
        if(!config.isEnabled())
            return 1000;
        var localPlayer = client.getLocalPlayer();
        if(localPlayer.getWorldLocation().getRegionID() == YANILLE_REGION_ID)
        {
            //We are in yanille and not in the nightmare zone
            NPC dominicNPC = NPCs.getNearest("Dominic Onion");
            Interactable potionInteractable = TileObjects.getNearest("Potion");
            Widget previousRumbleDialog = Widgets.get(WidgetInfo.DIALOG_OPTION_OPTIONS.getGroupId(), x -> x.getText().contains("Previous:"));
            Widget yesDialog = Widgets.get(WidgetInfo.DIALOG_OPTION_OPTIONS.getGroupId(), x -> x.getText().contains("Yes"));
            if(previousRumbleDialog != null)
            {
                previousRumbleDialog.interact(0);
                log.info("Selecting previous");
                return 1000;
            }
            if(yesDialog != null)
            {
                yesDialog.interact(0);
                log.info("Saying yes");
                return 1000;
            }

            if(dominicNPC == null)
            {
                Movement.walkTo(potionLocation);
                log.info("Moving to potion location because no dominic");
                return 250;
            }
            else if (potionInteractable == null)
            {
                dominicNPC.interact("Dream");
                log.info("Dreaming with dominic");
                return 1000;
            }
            else
            {
                if(potionInteractable == null)
                {
                    Movement.walkTo(potionLocation);
                    log.info("Moving to potion location because no potion");
                    return 250;
                }
                else
                {
                    potionInteractable.interact("Drink");
                    log.info("Drinking dream potion");
                    return 1000;
                }

            }
        }
        else
        {
            return handleNightmareZone();
        }

    }

    private boolean handlePowerUps()
    {
        Interactable powerUp = TileObjects.getNearest("Zapper", "Recurrent Damage", "Power Surge");
        if(powerUp != null)
        {
            powerUp.interact("Activate");
            return true;
        }
        return false;
    }

    private boolean handleAttackNearestWithPriority()
    {

        NPC attackable = null;
        for(String name : toAttackNames)
        {
            if(attackable == null)
            {
                attackable = NPCs.getNearest(x -> x.hasAction("Attack") && x.getName().toLowerCase().contains(name.toLowerCase()));
            }
            else
            {
                break;
            }
        }
        if(attackable != null)
        {
            if(attackable.getHealthRatio() != 0 && client.getLocalPlayer().getInteracting() == null)
            {
                attackable.interact("Attack");
                return true;
            }
        }
        return false;
    }

    private int handleNightmareZone()
    {
        if(!Prayers.isQuickPrayerEnabled())
        {
            Prayers.toggleQuickPrayer(true);
            log.info("Enabling quick prayers");
            return 250;
        }
        if(handlePowerUps())
        {
            log.info("Handling power up");
            return 1000;
        }
        if(handleAttackNearestWithPriority())
        {
            log.info("Attacking new target");
            return 250;
        }
        log.info("Idling in nightmare zone");
        return 250;
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
        if (!config.isEnabled())
            return;

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
