package net.unethicalite.plugins.roguesden;

import com.google.inject.Provides;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import net.unethicalite.api.entities.NPCs;
import net.unethicalite.api.entities.TileItems;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.movement.Reachable;
import net.unethicalite.api.plugins.LoopedPlugin;
import net.unethicalite.api.widgets.Prayers;
import net.unethicalite.api.widgets.Widgets;
import net.unethicalite.client.Static;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


@Extension
@PluginDescriptor(
        name = "Unethical RoguesDen",
        description = "RoguesDen",
        enabledByDefault = false
)
@Slf4j
public class RoguesDenPlugin extends LoopedPlugin
{

    private enum STATE
    {
        GET_STAMINA_DOSE,
        ENTER_DOORWAY,
        ENTER_CONTORTION_BARS,
        STAND_0,
        RUN_0,
        OPEN_GRILL,
        RUN_1,
        RUN_2,
        CLIMB_LEDGE,
        STAND_SAW,
        CROSS_LEDGE,
        STAND_1,
        RUN_3,
        SHORTCUT,
        ENTER_SHORTCUT_BARS,
        OPEN_SHORTCUT_GRILL,
        STAND_2,
        RUN_4,
        GET_FLASH_POWDER,
        STUN_NPC,
        RUN_5,
        WALK,
        CRACK
    }

    @Inject
    private Client client;
    @Inject
    private RoguesDenConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private OverlayManager overlayManager;

    private STATE state = STATE.GET_STAMINA_DOSE;

    private final WorldPoint BANK_LOCATION = new WorldPoint(3040, 4969, 1);

    private final WorldPoint DOORWAY_LOCATION = new WorldPoint(3056, 4991, 1);

    private final WorldPoint STAND_0_LOCATION = new WorldPoint(3039, 4999, 1);
    private final WorldPoint RUN_0_LOCATION = new WorldPoint(3029, 5003, 1);
    private final WorldPoint RUN_1_LOCATION = new WorldPoint(3011, 5005, 1);
    private final WorldPoint RUN_2_LOCATION = new WorldPoint(3004, 5003, 1);
    private final WorldPoint STAND_SAW_LOCATION = new WorldPoint(2969, 5018, 1);
    private final WorldPoint STAND_1_LOCATION = new WorldPoint(2962, 5050, 1);
    private final WorldPoint RUN_3_LOCATION = new WorldPoint(2963, 5056, 1);
    private final WorldPoint STAND_2_LOCATION = new WorldPoint(2992, 5067, 1);
    private final WorldPoint RUN_4_LOCATION = new WorldPoint(2992, 5075, 1);
    private final WorldPoint FLASH_POWDER_LOCATION = new WorldPoint(3009, 5063, 1);

    private final WorldPoint RUN_5_LOCATION = new WorldPoint(3028, 5056, 1);
    private final WorldPoint WALK_LOCATION = new WorldPoint(3028, 5047, 1);

    private static final WorldArea MINIGAME_AREA = new WorldArea(new WorldPoint(3051,4992,1), new WorldPoint(3061,5001,1));

    private static final WorldArea CONTORTION_BARS_AREA = new WorldArea(new WorldPoint(3047,4996,1), new WorldPoint(3048,4998,1));
    private static final WorldArea GRILL_AREA = new WorldArea(new WorldPoint(3022,5000,1), new WorldPoint(3023,5002,1));
    private static final WorldArea CLIMB_LEDGE_AREA = new WorldArea(new WorldPoint(2986,5003,1), new WorldPoint(2990,5006,1));

    private static final WorldArea STAND_SAW_AREA = new WorldArea(new WorldPoint(2966,5016,1), new WorldPoint(2967,5020,1));

    private static final WorldArea CROSS_LEDGE_AREA = new WorldArea(new WorldPoint(2954,5034,1), new WorldPoint(2962,5036,1));
    private static final WorldArea SHORTCUT_AREA = new WorldArea(new WorldPoint(2968,5060,1), new WorldPoint(2969,5063,1));
    private static final WorldArea SHORTCUT_BARS_AREA = new WorldArea(new WorldPoint(2973,5058,1), new WorldPoint(2975,5059,1));
    private static final WorldArea SHORTCUT_GRILL_AREA = new WorldArea(new WorldPoint(2990,5056,1), new WorldPoint(2992,5060,1));
    private static final WorldArea CRACK_AREA = new WorldArea(new WorldPoint(3054,4986,1), new WorldPoint(3059,4991,1));





    @Subscribe
    public void onConfigButtonPressed(ConfigButtonClicked event)
    {
        if (!event.getGroup().contains("unethical-roguesden"))
        {
            return;
        }

    }

    @Override
    protected int loop()
    {
        if (!config.isEnabled())
        {
            state = STATE.GET_STAMINA_DOSE;
            return 1000;
        }

        Player localPlayer = client.getLocalPlayer();

        if (localPlayer == null)
        {
            log.info("Local player is null");
            return 1000;
        }

        switch(state)
        {
            case GET_STAMINA_DOSE:
                Item potion = Inventory.getFirst(x -> x.getName().toLowerCase().contains("stamina potion"));

                if (potion != null)
                {
                    if (Bank.isOpen())
                    {
                        log.info("Closing bank");
                        Bank.close();
                        return 500;
                    }

                    log.info("Drinking potion");
                    potion.interact("Drink");
                    return 500;
                }
                else
                {
                    if (Movement.isStaminaBoosted())
                    {
                        log.info("Changing from GET_STAMINA_DOSE to ENTER_DOORWAY");
                        state = STATE.ENTER_DOORWAY;
                        return 0;
                    }

                    if (localPlayer.getWorldLocation().distanceTo(BANK_LOCATION) > 5)
                    {
                        log.info("Moving to bank");
                        Movement.walkTo(BANK_LOCATION);
                        return 500;
                    }

                    if (!Bank.isOpen())
                    {
                        log.info("Opening bank");
                        TileObject bankChest = TileObjects.getNearest(x -> x.getName().toLowerCase().contains("bank chest") && x.hasAction("Use"));

                        if (bankChest == null)
                        {
                            log.info("Could not find bank chest");
                            return 500;
                        }

                        bankChest.interact("Use");
                        return 500;
                    }
                    else
                    {
                        if (Inventory.contains(x -> !x.getName().toLowerCase().contains("stamina potion")))
                        {
                            log.info("depositing inventory");
                            Bank.depositInventory();
                            return 1500;
                        }

                        log.info("Attempted to withdraw potion");
                        Bank.withdraw(x -> x.getName().toLowerCase().contains("stamina potion(1)"), 1, Bank.WithdrawMode.ITEM);
                        return 1500;
                    }
                }

            case ENTER_DOORWAY:
                if (client.getLocalPlayer().getWorldLocation().isInArea(MINIGAME_AREA))
                {
                    log.info("Changing from ENTER_DOORWAY to ENTER_CONTORTION_BARS");
                    state = STATE.ENTER_CONTORTION_BARS;
                    return 0;
                }

                if (localPlayer.getWorldLocation().distanceTo(DOORWAY_LOCATION) > 5)
                {
                    log.info("Moving to DOORWAY_LOCATION");
                    Movement.walkTo(DOORWAY_LOCATION);
                    return 500;
                }

                TileObject doorway = TileObjects.getFirstSurrounding(DOORWAY_LOCATION, 5, x -> x.hasAction("Open"));

                if(doorway == null)
                {
                    log.info("doorway is null");
                    return 500;
                }

                doorway.interact("Open");
                return 500;

            case ENTER_CONTORTION_BARS:
                if (client.getLocalPlayer().getWorldLocation().isInArea(CONTORTION_BARS_AREA))
                {
                    log.info("Changing from ENTER_CONTORTION_BARS to STAND_0");
                    state = STATE.STAND_0;
                    return 0;
                }
                TileObject contortionBars = TileObjects.getNearest(x -> x.getName().toLowerCase().contains("contortion bars") && x.hasAction("Enter"));

                if(contortionBars == null)
                {
                    log.info("contortionBars is null");
                    return 500;
                }

                contortionBars.interact("Enter");
                return 1500;

            case STAND_0:
                if (client.getLocalPlayer().getWorldLocation().distanceTo(STAND_0_LOCATION) <= 0)
                {
                    log.info("Changing from STAND_0 to RUN_0");
                    state = STATE.RUN_0;
                    return 0;
                }

                Movement.walkTo(STAND_0_LOCATION);
                return 500;

            case RUN_0:
                if (client.getLocalPlayer().getWorldLocation().distanceTo(RUN_0_LOCATION) <= 0)
                {
                    log.info("Changing from RUN_0 to OPEN_GRILL");
                    state = STATE.OPEN_GRILL;
                    return 0;
                }

                Movement.walkTo(RUN_0_LOCATION);
                return 500;

            case OPEN_GRILL:
                if (client.getLocalPlayer().getWorldLocation().isInArea(GRILL_AREA))
                {
                    log.info("Changing from OPEN_GRILL to RUN_1");
                    state = STATE.RUN_1;
                    return 0;
                }
                TileObject grill = TileObjects.getNearest(x -> x.getName().toLowerCase().contains("grill") && x.hasAction("Open"));

                if(grill == null)
                {
                    log.info("grill is null");
                    return 500;
                }

                grill.interact("Open");
                return 1500;

            case RUN_1:
                if (client.getLocalPlayer().getWorldLocation().distanceTo(RUN_1_LOCATION) <= 0)
                {
                    log.info("Changing from RUN_1 to RUN_2");
                    state = STATE.RUN_2;
                    return 0;
                }

                Movement.walkTo(RUN_1_LOCATION);
                return 500;

            case RUN_2:
                if (client.getLocalPlayer().getWorldLocation().distanceTo(RUN_2_LOCATION) <= 0)
                {
                    log.info("Changing from RUN_2 to CLIMB_LEDGE");
                    state = STATE.CLIMB_LEDGE;
                    return 0;
                }

                Movement.walkTo(RUN_2_LOCATION);
                return 500;

            case CLIMB_LEDGE:
                if (client.getLocalPlayer().getWorldLocation().isInArea(CLIMB_LEDGE_AREA))
                {
                    log.info("Changing from CLIMB_LEDGE to STAND_1");
                    state = STATE.STAND_1;
                    return 0;
                }
                TileObject ledge = TileObjects.getNearest(x -> x.getName().toLowerCase().contains("ledge") && x.hasAction("Climb"));

                if(ledge == null)
                {
                    log.info("ledge is null");
                    return 500;
                }

                ledge.interact("Climb");
                return 1500;

            case STAND_SAW:
                if (client.getLocalPlayer().getWorldLocation().isInArea(CLIMB_LEDGE_AREA))
                {
                    log.info("Changing from STAND_SAW to CROSS_LEDGE");
                    state = STATE.CROSS_LEDGE;
                    return 0;
                }

                Movement.walkTo(STAND_SAW_LOCATION);
                return 1500;

            case CROSS_LEDGE:
                if (client.getLocalPlayer().getWorldLocation().isInArea(CROSS_LEDGE_AREA))
                {
                    log.info("Changing from CROSS_LEDGE to STAND_1");
                    state = STATE.STAND_1;
                    return 0;
                }

                TileObject cross_ledge = TileObjects.getNearest(x -> x.getName().toLowerCase().contains("ledge") && x.hasAction("Climb"));

                if(cross_ledge == null)
                {
                    log.info("cross_ledge is null");
                    return 500;
                }

                cross_ledge.interact("Climb");
                return 1500;

            case STAND_1:
                if (client.getLocalPlayer().getWorldLocation().distanceTo(STAND_1_LOCATION) <= 0)
                {
                    log.info("Changing from STAND_1 to RUN_3");
                    state = STATE.RUN_3;
                    return 0;
                }

                Movement.walkTo(STAND_1_LOCATION);
                return 500;

            case RUN_3:
                if (client.getLocalPlayer().getWorldLocation().distanceTo(RUN_3_LOCATION) <= 0)
                {
                    log.info("Changing from RUN_3 to SHORTCUT");
                    state = STATE.SHORTCUT;
                    return 0;
                }

                Movement.walkTo(RUN_3_LOCATION);
                return 500;

            case SHORTCUT:
                if (client.getLocalPlayer().getWorldLocation().isInArea(SHORTCUT_AREA))
                {
                    log.info("Changing from SHORTCUT to ENTER_SHORTCUT_BARS");
                    state = STATE.ENTER_SHORTCUT_BARS;
                    return 0;
                }

                TileObject shortcut_door = TileObjects.getNearest(x -> x.getName().toLowerCase().contains("door") && x.hasAction("Pick-lock"));

                if(shortcut_door == null)
                {
                    log.info("shortcut_door is null");
                    return 500;
                }

                shortcut_door.interact("Pick-lock");
                return 1500;

            case ENTER_SHORTCUT_BARS:
                if (client.getLocalPlayer().getWorldLocation().isInArea(SHORTCUT_BARS_AREA))
                {
                    log.info("Changing from ENTER_SHORTCUT_BARS to OPEN_SHORTCUT_GRILL");
                    state = STATE.OPEN_SHORTCUT_GRILL;
                    return 0;
                }

                TileObject shortcut_bars = TileObjects.getNearest(x -> x.getName().toLowerCase().contains("contortion bars") && x.hasAction("Enter"));

                if(shortcut_bars == null)
                {
                    log.info("shortcut_bars is null");
                    return 500;
                }

                shortcut_bars.interact("Enter");
                return 1500;

            case OPEN_SHORTCUT_GRILL:
                if (client.getLocalPlayer().getWorldLocation().isInArea(SHORTCUT_GRILL_AREA))
                {
                    log.info("Changing from OPEN_SHORTCUT_GRILL to STAND_2");
                    state = STATE.STAND_2;
                    return 0;
                }

                TileObject shortcut_grill = TileObjects.getNearest(x -> x.getName().toLowerCase().contains("grill") && x.hasAction("Open"));

                if(shortcut_grill == null)
                {
                    log.info("shortcut_grill is null");
                    return 500;
                }

                shortcut_grill.interact("Open");
                return 1500;

            case STAND_2:
                if (client.getLocalPlayer().getWorldLocation().distanceTo(STAND_2_LOCATION) <= 0)
                {
                    log.info("Changing from STAND_2 to RUN_4");
                    state = STATE.RUN_4;
                    return 0;
                }

                Movement.walkTo(STAND_2_LOCATION);
                return 500;

            case RUN_4:
                if (client.getLocalPlayer().getWorldLocation().distanceTo(RUN_4_LOCATION) <= 0)
                {
                    log.info("Changing from RUN_4 to GET_FLASH_POWDER");
                    state = STATE.GET_FLASH_POWDER;
                    return 0;
                }

                Movement.walkTo(RUN_4_LOCATION);
                return 500;

            case GET_FLASH_POWDER:
                if (client.getLocalPlayer().getWorldLocation().distanceTo(RUN_4_LOCATION) <= 0 && Inventory.contains(x -> x.getName().toLowerCase().contains("flash powder")))
                {
                    log.info("Changing from GET_FLASH_POWDER to STUN_NPC");
                    state = STATE.STUN_NPC;
                    return 0;
                }

                if (client.getLocalPlayer().getWorldLocation().distanceTo(RUN_4_LOCATION) > 0)
                {
                    log.info("walking to flash powder is null");
                    Movement.walkTo(FLASH_POWDER_LOCATION);
                    return 500;
                }

                TileItem flashPowder = TileItems.getNearest(x -> x.getName().toLowerCase().contains("flash powder") && x.hasAction("Take"));

                if(flashPowder == null)
                {
                    log.info("flashPowder is null");
                    return 500;
                }

                flashPowder.interact("Take");
                return 1500;

            case STUN_NPC:
                if (client.getLocalPlayer().getAnimation() == 929)
                {
                    log.info("Changing from STUN_NPC to RUN_5");
                    state = STATE.RUN_5;
                    return 0;
                }

                Item flashPowderInventory = Inventory.getFirst(x -> x.getName().toLowerCase().contains("flash powder"));

                if(flashPowderInventory == null)
                {
                    log.info("flashPowderInventory is null");
                    return 500;
                }

                NPC rogueGuard = NPCs.getNearest(3191);
                if (rogueGuard == null)
                {
                    log.info("rogueGuard is null");
                    return 500;
                }

                flashPowderInventory.useOn(rogueGuard);
                return 100;

            case RUN_5:
                if (client.getLocalPlayer().getWorldLocation().distanceTo(RUN_5_LOCATION) <= 0)
                {
                    log.info("Changing from RUN_5 to WALK");
                    state = STATE.WALK;
                    return 0;
                }

                Movement.walkTo(RUN_5_LOCATION);
                return 500;

            case WALK:
                if (client.getLocalPlayer().getWorldLocation().distanceTo(WALK_LOCATION) <= 0)
                {
                    log.info("Changing from WALK to CRACK");
                    state = STATE.CRACK;
                    return 0;
                }

                Movement.walkTo(WALK_LOCATION);
                return 500;

            case CRACK:
                if (client.getLocalPlayer().getWorldLocation().isInArea(CRACK_AREA))
                {
                    log.info("Changing from CRACK to GET_STAMINA_DOSE");
                    state = STATE.GET_STAMINA_DOSE;
                    return 0;
                }

                TileObject crackTarget = TileObjects.getNearest(x -> x.getName().toLowerCase().contains("wall safe") && x.hasAction("Crack"));

                if(crackTarget == null)
                {
                    log.info("crackTarget is null");
                    return 500;
                }

                crackTarget.interact("Crack");
                return 1500;

            default:
                log.info("End of SWITCH, idling");
                return 1000;
        }

    }




    @Subscribe
    private void onGameTick(GameTick e)
    {

    }

    @Provides
    RoguesDenConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(RoguesDenConfig.class);
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
