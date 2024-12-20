package net.unethicalite.plugins.gotr;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.OPRSExternalPluginManager;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import net.unethicalite.api.entities.Entities;
import net.unethicalite.api.entities.NPCs;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.game.Game;
import net.unethicalite.api.input.Keyboard;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.movement.Reachable;
import net.unethicalite.api.plugins.LoopedPlugin;
import net.unethicalite.api.widgets.Dialog;
import net.unethicalite.api.widgets.Widgets;
import net.unethicalite.client.Static;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Extension
@PluginDescriptor(
        name = "Unethical GOTR",
        description = "GOTR",
        enabledByDefault = false
)

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

    private STATE state = STATE.ENTER_AREA;

    private static final int MINIGAME_MAIN_REGION = 14484;

    private static final Set<Integer> GUARDIAN_IDS = ImmutableSet.of(
            ObjectID.GUARDIAN_OF_AIR,
            ObjectID.GUARDIAN_OF_WATER,
            ObjectID.GUARDIAN_OF_EARTH,
            ObjectID.GUARDIAN_OF_FIRE,
            ObjectID.GUARDIAN_OF_MIND,
            ObjectID.GUARDIAN_OF_CHAOS,
            ObjectID.GUARDIAN_OF_DEATH,
            ObjectID.GUARDIAN_OF_BLOOD,
            ObjectID.GUARDIAN_OF_BODY,
            ObjectID.GUARDIAN_OF_COSMIC,
            ObjectID.GUARDIAN_OF_NATURE,
            ObjectID.GUARDIAN_OF_LAW);

    private static final Set<Integer> ELEMENTAL_GUARDIAN_IDS = ImmutableSet.of(
            ObjectID.GUARDIAN_OF_AIR,
            ObjectID.GUARDIAN_OF_WATER,
            ObjectID.GUARDIAN_OF_EARTH,
            ObjectID.GUARDIAN_OF_FIRE
    );

    private static final Map<Integer, Integer> CATALYTIC_GUARDIAN_IDS_MAP = ImmutableMap.<Integer, Integer>builder()
            .put(ObjectID.GUARDIAN_OF_MIND, 2)
            .put(ObjectID.GUARDIAN_OF_CHAOS, 35)
            .put(ObjectID.GUARDIAN_OF_DEATH, 65)
            .put(ObjectID.GUARDIAN_OF_BLOOD, 77)
            .put(ObjectID.GUARDIAN_OF_BODY, 20)
            .put(ObjectID.GUARDIAN_OF_COSMIC, 27)
            .put(ObjectID.GUARDIAN_OF_NATURE, 44)
            .put(ObjectID.GUARDIAN_OF_LAW, 54)
            .build();

    private static final Map<Integer, Integer> SPRITE_GUARDIAN_IDS_MAP = ImmutableMap.<Integer, Integer>builder()
            .put(4353, ObjectID.GUARDIAN_OF_AIR)
            .put(4354, ObjectID.GUARDIAN_OF_MIND)
            .put(4355, ObjectID.GUARDIAN_OF_WATER)
            .put(4356, ObjectID.GUARDIAN_OF_EARTH)
            .put(4357, ObjectID.GUARDIAN_OF_FIRE)
            .put(4358, ObjectID.GUARDIAN_OF_BODY)
            .put(4359, ObjectID.GUARDIAN_OF_COSMIC)
            .put(4360, ObjectID.GUARDIAN_OF_CHAOS)
            .put(4361, ObjectID.GUARDIAN_OF_NATURE)
            .put(4362, ObjectID.GUARDIAN_OF_LAW)
            .put(4363, ObjectID.GUARDIAN_OF_DEATH)
            .put(4364, ObjectID.GUARDIAN_OF_BLOOD)
            .put(4369, 0)
            .put(4370, 0)
            .build();


    private static final Set<Integer> TALISMAN_IDS = GuardianInfo.ALL.stream().mapToInt(x -> x.talismanId).boxed().collect(Collectors.toSet());
    private static final int GREAT_GUARDIAN_ID = 11403;

    private static final int CATALYTIC_GUARDIAN_STONE_ID = 26880;
    private static final int ELEMENTAL_GUARDIAN_STONE_ID = 26881;
    private static final int POLYELEMENTAL_GUARDIAN_STONE_ID = 26941;

    private static final int ELEMENTAL_ESSENCE_PILE_ID = 43722;
    private static final int CATALYTIC_ESSENCE_PILE_ID = 43723;

    private static final int UNCHARGED_CELL_ITEM_ID = 26882;
    private static final int WEAK_CELL_ITEM_ID = ItemID.WEAK_CELL;

    private static final int UNCHARGED_CELL_GAMEOBJECT_ID = 43732;
    private static final int WEAK_CELL_GAMEOBJECT_ID = ObjectID.WEAK_CELLS;
    private static final int CHISEL_ID = 1755;
    private static final int OVERCHARGED_CELL_ID = 26886;

    private static final int GUARDIAN_ACTIVE_ANIM = 9363;

    private static final int PARENT_WIDGET_ID = 48889857;
    private static final int CATALYTIC_RUNE_WIDGET_ID = 48889879;
    private static final int ELEMENTAL_RUNE_WIDGET_ID = 48889876;
    private static final int GUARDIAN_COUNT_WIDGET_ID = 48889886;
    private static final int PORTAL_WIDGET_ID = 48889884;

    private final static int PORTAL_SPRITE_ID = 4368;

    private static final int PORTAL_ID = 43729;

    private static final int MINE_FRAGS_SHORTCUT_ENTER_GAMEOBJECT_ID = 43724;
    private static final int MINE_FRAGS_SHORTCUT_EXIT_GAMEOBJECT_ID = 43726;


    private static final int ENTRY_BARRIER_GAMEOBJECT_ID = ObjectID.BARRIER_43700;

    private static final int FRAG_ITEM_ID = ItemID.GUARDIAN_FRAGMENTS;
    private static final int ESSENCE_ITEM_ID = ItemID.GUARDIAN_ESSENCE;
    private static final int WORKBENCH_GAMEOBJECT_ID = ObjectID.WORKBENCH_43754;



    private static final String REWARD_POINT_REGEX = "Total elemental energy:[^>]+>([\\d,]+).*Total catalytic energy:[^>]+>([\\d,]+).";
    private static final Pattern REWARD_POINT_PATTERN = Pattern.compile(REWARD_POINT_REGEX);
    private static final String CHECK_POINT_REGEX = "You have (\\d+) catalytic energy and (\\d+) elemental energy";
    private static final Pattern CHECK_POINT_PATTERN = Pattern.compile(CHECK_POINT_REGEX);

    private static final int DIALOG_WIDGET_GROUP = 229;
    private static final int DIALOG_WIDGET_MESSAGE = 1;
    private static final String BARRIER_DIALOG_FINISHING_UP = "It looks like the adventurers within are just finishing up. You must<br>wait until they are done to join.";

    private static final WorldArea MINIGAME_AREA = new WorldArea(new WorldPoint(3597,9484,0), new WorldPoint(3634,9517,0));

    private static final WorldArea FRAG_SHORTCUT_AREA = new WorldArea(new WorldPoint(3637,9494,0), new WorldPoint(3644,9513,0));
    private static final WorldArea ENTRY_PORTAL_AREA = new WorldArea(new WorldPoint(3611,9471,0), new WorldPoint(3619,9481,0));

    private static final WorldPoint FRAG_SHORTCUT_POINT = new WorldPoint(3632,9503,0);


    private static final WorldArea FRAG_PORTAL_AREA = new WorldArea(new WorldPoint(3586,9494,0), new WorldPoint(3594,9514,0));
    private static final int LARGE_GUARDIAN_REMAINS = 43719;
    private static final int HUGE_GUARDIAN_REMAINS = 43720;

    private TileObject barrierObject;



    @Getter(AccessLevel.PACKAGE)
    private final Set<GameObject> guardians = new HashSet<>();
    @Getter(AccessLevel.PACKAGE)
    private TileObject activeElementalGuardian;
    @Getter(AccessLevel.PACKAGE)
    private TileObject activeCatalyticGuardian;
    @Getter(AccessLevel.PACKAGE)
    private final Set<Integer> inventoryTalismans = new HashSet<>();
    @Getter(AccessLevel.PACKAGE)
    private NPC greatGuardian;
    @Getter(AccessLevel.PACKAGE)
    private GameObject unchargedCellTable;
    @Getter(AccessLevel.PACKAGE)
    private GameObject weakCellTable;
    @Getter(AccessLevel.PACKAGE)
    private GameObject catalyticEssencePile;
    @Getter(AccessLevel.PACKAGE)
    private GameObject elementalEssencePile;
    @Getter(AccessLevel.PACKAGE)
    private GameObject portal;

    @Getter(AccessLevel.PACKAGE)
    private boolean isInMinigame;
    @Getter(AccessLevel.PACKAGE)
    private boolean isInMainRegion;
    @Getter(AccessLevel.PACKAGE)
    private boolean outlineGreatGuardian = false;
    @Getter(AccessLevel.PACKAGE)
    private boolean outlineUnchargedCellTable = false;
    @Getter(AccessLevel.PACKAGE)
    private boolean shouldMakeGuardian = false;
    @Getter(AccessLevel.PACKAGE)
    private boolean isFirstPortal = false;

    @Getter(AccessLevel.PACKAGE)
    private int elementalRewardPoints;
    @Getter(AccessLevel.PACKAGE)
    private int catalyticRewardPoints;
    @Getter(AccessLevel.PACKAGE)
    private int currentElementalRewardPoints;
    @Getter(AccessLevel.PACKAGE)
    private int currentCatalyticRewardPoints;

    @Getter(AccessLevel.PACKAGE)
    private Optional<Instant> portalSpawnTime = Optional.empty();
    @Getter(AccessLevel.PACKAGE)
    private Optional<Instant> lastPortalDespawnTime = Optional.empty();
    @Getter(AccessLevel.PACKAGE)
    private Optional<Instant> nextGameStart = Optional.empty();
    @Getter(AccessLevel.PACKAGE)
    private int lastRewardUsage;


    private String portalLocation;
    private int lastElementalRuneSprite;
    private int lastCatalyticRuneSprite;
    private boolean areGuardiansNeeded = false;
    private boolean haveCrafted = false;

    private final Map<String, String> expandCardinal = new HashMap<>();



    @Subscribe
    public void onConfigButtonPressed(ConfigButtonClicked event)
    {
        if (!event.getGroup().contains("unethical-gotr"))
        {
            return;
        }

    }

    private boolean checkInMinigame()
    {
        GameState gameState = client.getGameState();
        if (gameState != GameState.LOGGED_IN
                && gameState != GameState.LOADING)
        {
            return false;
        }

        Widget elementalRuneWidget = client.getWidget(PARENT_WIDGET_ID);
        return elementalRuneWidget != null;
    }

    private boolean checkInMainRegion()
    {
        int[] currentMapRegions = client.getMapRegions();
        return Arrays.stream(currentMapRegions).anyMatch(x -> x == MINIGAME_MAIN_REGION);
    }

    @Override
    protected void startUp()
    {
        isInMinigame = true;
        expandCardinal.put("S",  "south");
        expandCardinal.put("SW", "south west");
        expandCardinal.put("W",  "west");
        expandCardinal.put("NW", "north west");
        expandCardinal.put("N",  "north");
        expandCardinal.put("NE", "north east");
        expandCardinal.put("E",  "east");
        expandCardinal.put("SE", "south east");
    }

    @Override
    protected void shutDown() {
        reset();
    }

    @Override
    protected int loop()
    {
        if (!config.isEnabled())
        {
            state = STATE.ENTER_AREA;
            return 1000;
        }

        /*
        if ()
        {
            log.info("");
            return 500;
        }
        */

        if (Dialog.isEnterInputOpen())
        {
            Keyboard.type(11);
            Keyboard.sendEnter();
            return 1000;
        }

        /*if (Dialog.isOpen() && Dialog.hasOption("Yes"))
        {
            Dialog.chooseOption("Yes");
            return 1000;
        }*/

        if (nextGameStart.isPresent())
        {
            var start = nextGameStart.get();
            var secondsToStart = ChronoUnit.SECONDS.between(Instant.now(), start) - .5d;
            if (secondsToStart > 20 && secondsToStart <= 45) {
                state = STATE.COLLECT_MATS;
                /*if (Inventory.contains(FRAG_ITEM_ID))
                {
                    Inventory.getFirst(FRAG_ITEM_ID).interact("Destroy");
                    return 500;
                }*/
                if (!client.getLocalPlayer().getWorldLocation().isInArea(MINIGAME_AREA))
                {
                    if (leaveShortcut())
                    {
                        log.info("leaveShortcut");
                        return 500;
                    }
                    if (exitAltarPortal())
                    {
                        log.info("exitAltarPortal");
                        return 500;
                    }
                }
                log.info("Waiting for next game to almost start");
                return 1000;
            }
        }

        switch(state) {
            case ENTER_AREA:
                if (Reachable.isInteractable(unchargedCellTable))
                {
                    log.info("Changing from ENTER_AREA to COLLECT_MATS");
                    state = STATE.COLLECT_MATS;
                    return 0;
                }


                if (enterMinigame())
                {
                    log.info("enterMinigame");
                    return 2000;
                }

                break;
            case COLLECT_MATS:

                if (Inventory.contains(WEAK_CELL_ITEM_ID) && Inventory.getCount(true, UNCHARGED_CELL_ITEM_ID) >= 10)
                {
                    log.info("Changing from COLLECT_MATS to MINE_FRAGS_SHORTCUT");
                    if (Inventory.getCount(true, FRAG_ITEM_ID) <= 30)
                        state = STATE.MINE_FRAGS_SHORTCUT;
                    else
                        state = STATE.CRAFT_ESSENCE;
                    return 0;
                }

                if (getWeakCell())
                {
                    log.info("getWeakCell");
                    return 500;
                }
                if (getUnchargedCell())
                {
                    log.info("getUnchargedCell");
                    return 500;
                }

                break;
            case MINE_FRAGS_SHORTCUT:

                if (Inventory.getCount(true, FRAG_ITEM_ID) >= config.fragCount() && client.getLocalPlayer().getWorldLocation().isInArea(MINIGAME_AREA))
                {
                    log.info("Changing from MINE_FRAGS_SHORTCUT to CRAFT_ESSENCE");
                    state = STATE.CRAFT_ESSENCE;
                    return 0;
                }

                if (jumpShortcut())
                {
                    log.info("jumpShortcut");
                    return 500;
                }

                if (mineFragsShortcut())
                {
                    log.info("mineFrags shortcut");
                    return 500;
                }

                if (leaveShortcut())
                {
                    log.info("leaveShortcut");
                    return 500;
                }


                break;
            case CRAFT_ESSENCE:
                if (Inventory.getCount(true, FRAG_ITEM_ID) <= 30)
                {
                    log.info("Changing from CRAFT_ESSENCE to MINE_FRAGS_SHORTCUT");
                    state = STATE.MINE_FRAGS_SHORTCUT;
                    return 0;
                }

                if (Inventory.isFull())
                {
                    log.info("Changing from CRAFT_ESSENCE to USE_ALTAR");
                    state = STATE.USE_ALTAR;
                    return 0;
                }

                if (craftEssence())
                {
                    log.info("craftEssence");
                    return 500;
                }

                break;
            case USE_ALTAR:
                if (!Inventory.contains(ESSENCE_ITEM_ID) && client.getLocalPlayer().getWorldLocation().isInArea(MINIGAME_AREA) && !Inventory.contains(x -> x.getName().toLowerCase().contains("rune")))
                {
                    barrierObject = null;
                    if (Inventory.getCount(true, FRAG_ITEM_ID) <= 30)
                    {
                        log.info("Changing from USE_ALTAR to MINE_FRAGS_SHORTCUT");
                        state = STATE.MINE_FRAGS_SHORTCUT;
                        return 0;
                    }
                    else if (!Inventory.contains(UNCHARGED_CELL_ITEM_ID))
                    {
                        log.info("Changing from USE_ALTAR to COLLECT_MATS");
                        state = STATE.COLLECT_MATS;
                        return 0;
                    }
                    else
                    {
                        log.info("Changing from USE_ALTAR to CRAFT_ESSENCE");
                        state = STATE.CRAFT_ESSENCE;
                        return 0;
                    }

                }

                if (enterAltarPortal())
                {
                    log.info("enterAltarPortal");
                    return 500;
                }

                if (craftRunes())
                {
                    log.info("craftRunes");
                    return 500;
                }

                if (exitAltarPortal())
                {
                    log.info("exitAltarPortal");
                    return 500;
                }

                if (redeemGuardianStone())
                {
                    log.info("redeemGuardianStone USE_ALTAR");
                    return 500;
                }

                if (redeemCell())
                {
                    log.info("redeemCell USE_ALTAR");
                    return 500;
                }

                if (depositRunes())
                {
                    log.info("depositRunes");
                    return 500;
                }

                break;
            /*case MINE_FRAGS_PORTAL:
                if (Inventory.isFull() && client.getLocalPlayer().getWorldLocation().isInArea(MINIGAME_AREA))
                {
                    log.info("Changing from MINE_FRAGS_PORTAL to USE_ALTAR");
                    state = STATE.USE_ALTAR;
                    return 0;
                }

                if (enterFragsPortal())
                {
                    log.info("enterFragsPortal");
                    return 500;
                }

                if (mineFragsPortal())
                {
                    log.info("mineFrags portal");
                    return 500;
                }

                if (exitFragsPortal())
                {
                    log.info("exitFragsPortal");
                    return 500;
                }

                break;
            case REDEEM_USE_ALTAR:
                if ( haveCrafted && client.getLocalPlayer().getWorldLocation().isInArea(MINIGAME_AREA))
                {
                    log.info("Changing from REDEEM_USE_ALTAR to REDEEM_DEPOSIT");
                    state = STATE.REDEEM_DEPOSIT;
                    haveCrafted = false;
                    return 0;
                }

                if (redeem())
                {
                    log.info("redeem REDEEM_USE_ALTAR");
                    return 500;
                }

                if (enterAltarPortal())
                {
                    log.info("enterAltarPortal");
                    return 500;
                }

                if (craftRunes())
                {
                    log.info("craftRunes");
                    return 500;
                }

                if (exitAltarPortal())
                {
                    log.info("exitAltarPortal");
                    return 500;
                }
                else
                {
                    haveCrafted = true;
                }

                break;
            case REDEEM_DEPOSIT:

                if (redeem())
                {
                    log.info("redeem REDEEM_USE_ALTAR");
                    return 500;
                }

                if (depositRunes())
                {
                    log.info("depositRunes");
                    return 500;
                }

                break;*/
            default:
                log.info("End of SWITCH, idling");
                return 1000;
        }



        log.info("End of LOOP, idling");
        return 10;
    }

    private boolean redeemGuardianStone()
    {
        if (!Inventory.contains(x -> x.getName().toLowerCase().contains("guardian stone")))
            return false;

        NPC guardianObject = NPCs.getNearest(x -> x.hasAction("Power-up"));

        if (guardianObject == null)
        {
            log.info("guardianObject is null");
            return false;
        }

        guardianObject.interact("Power-up");
        return true;
    }


    private boolean redeemCell()
    {
        if (!(
                Inventory.contains(x -> x.getName().toLowerCase().contains("weak cell")) ||
                Inventory.contains(x -> x.getName().toLowerCase().contains("medium cell")) ||
                Inventory.contains(x -> x.getName().toLowerCase().contains("strong cell")) ||
                Inventory.contains(x -> x.getName().toLowerCase().contains("overcharged cell"))
        ))
            return false;

        NPC nearestBarrier = NPCs.getNearest(x ->
                (x.getId() == ObjectID.OVERCHARGED_BARRIER || x.getId() == ObjectID.OVERCHARGED_BARRIER_43751) &&
                x.getHealthRatio() != -1 &&
                x.getHealthScale() != -1 &&
                x.getHealthRatio() < x.getHealthScale()

        );
        if (nearestBarrier == null)
            nearestBarrier = NPCs.getNearest(ObjectID.OVERCHARGED_BARRIER, ObjectID.OVERCHARGED_BARRIER_43751);

        WorldArea validArea;
        if (nearestBarrier != null)
            validArea = nearestBarrier.getWorldArea().offset(2);
        else
            validArea = null;

        if (validArea != null)
        {
            barrierObject = TileObjects.getNearest(x ->
                    validArea.contains(x.getWorldLocation()) &&
                            x.hasAction("Place-cell")
            );
            log.info("Setting barrier object from world area: " + validArea.toString());
        }
        else
        {
            barrierObject = TileObjects.getNearest(x -> x.hasAction("Place-cell"));
            log.info("validArea is null");
        }



        /*TileObject barrierObject = TileObjects.getFirstSurrounding(
                nearestBarrier.getWorldLocation(),3, x -> x.hasAction("Place-cell")
        );*/

        if (barrierObject == null)
        {
            log.info("Barrier object is null");
            barrierObject = TileObjects.getNearest(x -> x.hasAction("Place-cell"));
        }
        if (barrierObject == null)
        {
            log.info("Barrier object is null after retry");
            return false;
        }
        else
        {
            barrierObject.interact("Place-cell");
            return true;
        }
    }

    private boolean exitFragsPortal()
    {
        if (client.getLocalPlayer().getWorldLocation().isInArea(MINIGAME_AREA))
            return false;

        TileObject exitPortal = TileObjects.getNearest(x -> x.hasAction("Enter"));
        if (exitPortal == null)
        {
            log.info("exitPortal is null");
            return false;
        }

        exitPortal.interact("Enter");
        return true;
    }

    private boolean leaveShortcut()
    {
        if (client.getLocalPlayer().getWorldLocation().isInArea(MINIGAME_AREA))
            return false;


        //TileObject fragsShortcut = TileObjects.getNearest(MINE_FRAGS_SHORTCUT_EXIT_GAMEOBJECT_ID);
        TileObject fragsShortcut = TileObjects.getNearest(x -> x.hasAction("Climb"));
        if (fragsShortcut == null)
        {
            log.info("fragsShortcut is null");
            return false;
        }

        if (!Reachable.isInteractable(fragsShortcut))
        {
            log.info("Frags shortcut is unreachable shortcut leave");
            return false;
        }

        fragsShortcut.interact("Climb");
        return true;

    }

    private boolean mineFragsPortal()
    {
        if (!client.getLocalPlayer().getWorldLocation().isInArea(FRAG_PORTAL_AREA))
            return false;

        if (Inventory.isFull())
            return false;

        if (client.getLocalPlayer().isAnimating())
            return true;

        TileObject fragsPortalObject = TileObjects.getNearest(HUGE_GUARDIAN_REMAINS);
        if (fragsPortalObject == null)
        {
            log.info("fragsPortalObject is null");
            return false;
        }

        fragsPortalObject.interact("Mine");
        return true;
    }

    private boolean enterFragsPortal()
    {
        if (client.getLocalPlayer().getWorldLocation().isInArea(FRAG_PORTAL_AREA))
            return false;

        TileObject portalObject = TileObjects.getNearest(ObjectID.PORTAL_43729);

        if (portalObject == null)
        {
            log.info("portal is null");
            return false;
        }

        if (!Reachable.isInteractable(portalObject))
        {
            log.info("portalObject is not reachable");
            return false;
        }


        portalObject.interact("Enter");
        return true;

    }

    private boolean depositRunes()
    {
        if (!Inventory.contains(x -> x.getName().toLowerCase().contains("rune")) && client.getLocalPlayer().getWorldLocation().isInArea(MINIGAME_AREA))
            return false;

        TileObject depositPool = TileObjects.getNearest(ObjectID.DEPOSIT_POOL);
        if (depositPool == null)
        {
            log.info("depositPool is null");
            return false;
        }

        depositPool.interact("Deposit-runes");
        return true;

    }

    private boolean exitAltarPortal()
    {
        if (!Inventory.contains(ESSENCE_ITEM_ID) && client.getLocalPlayer().getWorldLocation().isInArea(MINIGAME_AREA))
            return false;

        TileObject exitAltarPortal = TileObjects.getNearest(x -> x.hasAction("Use"));
        if (exitAltarPortal == null)
        {
            log.info("exitAltarPortal is null");
            return false;
        }

        exitAltarPortal.interact("Use");
        return true;
    }

    private boolean craftRunes()
    {
        if (!Inventory.contains(ESSENCE_ITEM_ID))
            return false;

        TileObject altarObject = TileObjects.getNearest(x -> x.hasAction("Craft-rune"));
        if (altarObject == null)
        {
            log.info("altarObject is null");
            return false;
        }

        altarObject.interact("Craft-rune");
        return true;
    }

    private boolean enterAltarPortal()
    {
        if (Inventory.contains(ESSENCE_ITEM_ID) && !client.getLocalPlayer().getWorldLocation().isInArea(MINIGAME_AREA))
            return false;

        if (!Inventory.contains(ESSENCE_ITEM_ID))
            return false;

        TileObject altarPortal = getAltarPortal();
        if (altarPortal == null)
        {
            log.info("altarPortal is null");
            return false;
        }

        altarPortal.interact("Enter");
        return true;

    }

    private TileObject getAltarPortal()
    {
        log.info("Elemental: {}", activeElementalGuardian == null ? 0 : activeElementalGuardian.getId());
        log.info("Catalytic: {}", activeCatalyticGuardian == null ? 0 : activeCatalyticGuardian.getId());


        if (currentElementalRewardPoints >= currentCatalyticRewardPoints && activeCatalyticGuardian != null)
        {
            if (CATALYTIC_GUARDIAN_IDS_MAP.get(activeCatalyticGuardian.getId()) <= client.getRealSkillLevel(Skill.RUNECRAFT))
            {
                return activeCatalyticGuardian;
            }
            else
            {
                return activeElementalGuardian;
            }
        }
        else
        {
                return activeElementalGuardian;
        }
    }

    private boolean craftEssence()
    {
        if (client.getLocalPlayer().isAnimating())
            return false;


        TileObject workbench = TileObjects.getNearest(WORKBENCH_GAMEOBJECT_ID);
        if (workbench == null)
        {
            log.info("workbench is null");
            return false;
        }

        workbench.interact("Work-at");
        return true;
    }

    private boolean mineFragsShortcut()
    {
        if (Inventory.getCount(true, FRAG_ITEM_ID) >= config.fragCount())
            return false;

        if (client.getLocalPlayer().isAnimating())
            return true;

        TileObject fragObject = TileObjects.getNearest(LARGE_GUARDIAN_REMAINS);
        if (fragObject == null)
        {
            log.info("No fragObject");
            return false;
        }

        if (!Reachable.isInteractable(fragObject))
        {
            log.info("fragObject is not reachable");
            return false;
        }

        fragObject.interact("Mine");
        return true;
    }

    private boolean jumpShortcut()
    {
        TileObject fragsShortcut = TileObjects.getNearest(MINE_FRAGS_SHORTCUT_ENTER_GAMEOBJECT_ID);

        if (fragsShortcut == null)
        {
            log.info("fragsShortcut is null, walking to shortcut");
            Movement.walkTo(FRAG_SHORTCUT_POINT);
            return true;
        }

        if (!Reachable.isInteractable(fragsShortcut))
        {
            log.info("Frags shortcut is unreachable shortcut enter");
            return false;
        }

        if (client.getLocalPlayer().getWorldLocation().isInArea(FRAG_SHORTCUT_AREA))
        {
            log.info("Currently in frag shortcut area");
            return false;
        }

        fragsShortcut.interact("Climb");
        return true;
    }

    private boolean getUnchargedCell()
    {
        if (!Reachable.isInteractable(unchargedCellTable) || Inventory.getCount(true, UNCHARGED_CELL_ITEM_ID) >= 10)
            return false;
        unchargedCellTable.interact("Take-10");
        return true;
    }

    private boolean getWeakCell()
    {
        if (!Reachable.isInteractable(weakCellTable) || Inventory.contains(WEAK_CELL_ITEM_ID))
            return false;
        weakCellTable.interact("Take");
        return true;
    }

    private boolean enterMinigame()
    {
        boolean isInArea = client.getLocalPlayer().getWorldLocation().isInArea(MINIGAME_AREA);
        if (!isInMinigame && isInArea)
            return true;
        if (!isInArea)
        {
            TileObject entryBarrier = TileObjects.getNearest(ENTRY_BARRIER_GAMEOBJECT_ID);
            if (entryBarrier == null)
            {
                log.info("Could not find entry barrier");
                return false;
            }
            entryBarrier.interact("Quick-pass");
            return true;
        }
        return false;
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (!isInMainRegion || event.getItemContainer() != client.getItemContainer(InventoryID.INVENTORY))
        {
            return;
        }

        Item[] items = event.getItemContainer().getItems();
        outlineGreatGuardian = Arrays.stream(items).anyMatch(x -> x.getId() == ELEMENTAL_GUARDIAN_STONE_ID || x.getId() == CATALYTIC_GUARDIAN_STONE_ID || x.getId() == POLYELEMENTAL_GUARDIAN_STONE_ID);		outlineUnchargedCellTable = Arrays.stream(items).noneMatch(x -> x.getId() == UNCHARGED_CELL_ITEM_ID);
        shouldMakeGuardian = Arrays.stream(items).anyMatch(x -> x.getId() == CHISEL_ID) && Arrays.stream(items).anyMatch(x -> x.getId() == OVERCHARGED_CELL_ID) && areGuardiansNeeded;

        List<Integer> invTalismans = Arrays.stream(items).mapToInt(x -> x.getId()).filter(x -> TALISMAN_IDS.contains(x)).boxed().collect(Collectors.toList());
        if(invTalismans.stream().count() != inventoryTalismans.stream().count()){
            inventoryTalismans.clear();
            inventoryTalismans.addAll(invTalismans);
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        isInMinigame = checkInMinigame();
        isInMainRegion = checkInMainRegion();

        /*activeGuardians.removeIf(ag -> {
            Animation anim = ((DynamicObject)ag.getRenderable()).getAnimation();
            return anim == null || anim.getId() != GUARDIAN_ACTIVE_ANIM;
        });

        for(GameObject guardian : guardians){
            Animation animation = ((DynamicObject) guardian.getRenderable()).getAnimation();
            log.info("Guardian: {} | Animation: {}", guardian.getId(), animation.getId());
            if(animation != null && animation.getId() == GUARDIAN_ACTIVE_ANIM) {
                activeGuardians.add(guardian);
            }
        }*/

        Widget elementalRuneWidget = client.getWidget(ELEMENTAL_RUNE_WIDGET_ID);
        Widget catalyticRuneWidget = client.getWidget(CATALYTIC_RUNE_WIDGET_ID);

        if (elementalRuneWidget != null)
        {
            int guardianId = SPRITE_GUARDIAN_IDS_MAP.getOrDefault(elementalRuneWidget.getSpriteId(), 0);
            //log.info("Elemental guardian id: {}", guardianId);
            if (guardianId != 0)
                activeElementalGuardian = TileObjects.getNearest(guardianId);
            else
                activeElementalGuardian = null;
        }
        else
        {
            log.info("No elemental rune widget");
            activeElementalGuardian = null;
        }


        if (catalyticRuneWidget != null)
        {
            int guardianId = SPRITE_GUARDIAN_IDS_MAP.getOrDefault(catalyticRuneWidget.getSpriteId(), 0);
            //log.info("Catalytic guardian id: {}", guardianId);
            if (guardianId != 0)
                activeCatalyticGuardian = TileObjects.getNearest(guardianId);
            else
                activeCatalyticGuardian = null;


        }
        else
        {
            log.info("No catalytic rune widget");
            activeCatalyticGuardian = null;
        }

        Widget guardianCountWidget = client.getWidget(GUARDIAN_COUNT_WIDGET_ID);
        Widget portalWidget = client.getWidget(PORTAL_WIDGET_ID);

        lastElementalRuneSprite = parseRuneWidget(elementalRuneWidget, lastElementalRuneSprite);
        lastCatalyticRuneSprite = parseRuneWidget(catalyticRuneWidget, lastCatalyticRuneSprite);

        if(guardianCountWidget != null) {
            String text = guardianCountWidget.getText();
            areGuardiansNeeded = text != null && !text.contains("10/10");
        }

        if(portalWidget != null && !portalWidget.isHidden()){
            if(!portalSpawnTime.isPresent() && lastPortalDespawnTime.isPresent()) {
                lastPortalDespawnTime = Optional.empty();
                if (isFirstPortal) {
                    isFirstPortal = false;
                }
            }
            portalLocation = portalWidget.getText();
            portalSpawnTime = portalSpawnTime.isPresent() ? portalSpawnTime : Optional.of(Instant.now());
        } else if(elementalRuneWidget != null && !elementalRuneWidget.isHidden()) {
            if(portalSpawnTime.isPresent()){
                lastPortalDespawnTime = Optional.of(Instant.now());
            }
            portalLocation = null;
            portalSpawnTime = Optional.empty();
        }

        Widget dialog = client.getWidget(DIALOG_WIDGET_GROUP, DIALOG_WIDGET_MESSAGE);
        if (dialog != null)
        {
            String dialogText = dialog.getText();
            if (dialogText.equals(BARRIER_DIALOG_FINISHING_UP)) {
                // Allow one click per tick while the portal is closed
                //entryBarrierClickCooldown = 0;
            }
            else
            {
                final Matcher checkMatcher = CHECK_POINT_PATTERN.matcher(dialogText);
                if (checkMatcher.find(0))
                {
                    //For some reason these are reversed compared to everything else
                    catalyticRewardPoints = Integer.parseInt(checkMatcher.group(1));
                    elementalRewardPoints = Integer.parseInt(checkMatcher.group(2));
                }
            }
        }
    }

    int parseRuneWidget(Widget runeWidget, int lastSpriteId){
        if(runeWidget != null) {
            int spriteId = runeWidget.getSpriteId();
            if(spriteId != lastSpriteId) {
                if(lastSpriteId > 0) {
                    Optional<GuardianInfo> lastGuardian = GuardianInfo.ALL.stream().filter(g -> g.spriteId == lastSpriteId).findFirst();
                    if(lastGuardian.isPresent()) {
                        lastGuardian.get().despawn();
                    }
                }

                Optional<GuardianInfo> currentGuardian = GuardianInfo.ALL.stream().filter(g -> g.spriteId == spriteId).findFirst();
                if(currentGuardian.isPresent()) {
                    currentGuardian.get().spawn();
                }
            }

            return spriteId;
        }
        return lastSpriteId;
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        GameObject gameObject = event.getGameObject();
        if(GUARDIAN_IDS.contains(event.getGameObject().getId())) {
            guardians.removeIf(g -> g.getId() == gameObject.getId());
            guardians.add(gameObject);
        }

        if(gameObject.getId() == UNCHARGED_CELL_GAMEOBJECT_ID){
            unchargedCellTable = gameObject;
        }

        if(gameObject.getId() == WEAK_CELL_GAMEOBJECT_ID){
            weakCellTable = gameObject;
        }

        if(gameObject.getId() == ELEMENTAL_ESSENCE_PILE_ID){
            elementalEssencePile = gameObject;
        }

        if(gameObject.getId() == CATALYTIC_ESSENCE_PILE_ID){
            catalyticEssencePile = gameObject;
        }

        if(gameObject.getId() == PORTAL_ID){
            portal = gameObject;
        }

    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned npcSpawned) {
        NPC npc = npcSpawned.getNpc();
        if(npc.getId() == GREAT_GUARDIAN_ID){
            greatGuardian = npc;
        }
    }


    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOADING)
        {
            // on region changes the tiles get set to null
            reset();
        }
        else if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            isInMinigame = false;
        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        if(!isInMainRegion) return;
        currentElementalRewardPoints = client.getVarbitValue(13686);
        currentCatalyticRewardPoints = client.getVarbitValue(13685);
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage)
    {
        if(!isInMainRegion) return;
        if(chatMessage.getType() != ChatMessageType.SPAM && chatMessage.getType() != ChatMessageType.GAMEMESSAGE) return;

        String msg = chatMessage.getMessage();
        if(msg.contains("The rift becomes active!")) {
            lastPortalDespawnTime = Optional.of(Instant.now());
            nextGameStart = Optional.empty();
            isFirstPortal = true;
        } else if(msg.contains("The rift will become active in 30 seconds.")) {
            nextGameStart = Optional.of(Instant.now().plusSeconds(30));
        } else if(msg.contains("The rift will become active in 10 seconds.")) {
            nextGameStart = Optional.of(Instant.now().plusSeconds(10));
        } else if(msg.contains("The rift will become active in 5 seconds.")) {
            nextGameStart = Optional.of(Instant.now().plusSeconds(5));
        } else if(msg.contains("The Portal Guardians will keep their rifts open for another 30 seconds.")){
            nextGameStart = Optional.of(Instant.now().plusSeconds(60));
        } else if(msg.contains("You found some loot:")){
            elementalRewardPoints--;
            catalyticRewardPoints--;
        }

        Matcher rewardPointMatcher = REWARD_POINT_PATTERN.matcher(msg);
        if(rewardPointMatcher.find()) {
            // Use replaceAll to remove thousands separators from the text
            elementalRewardPoints = Integer.parseInt(rewardPointMatcher.group(1).replaceAll(",", ""));
            catalyticRewardPoints = Integer.parseInt(rewardPointMatcher.group(2).replaceAll(",", ""));
        }
        //log.info(msg);
    }

    private void reset() {
        guardians.clear();
        activeElementalGuardian = null;
        activeCatalyticGuardian = null;
        unchargedCellTable = null;
        greatGuardian = null;
        catalyticEssencePile = null;
        elementalEssencePile = null;
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
