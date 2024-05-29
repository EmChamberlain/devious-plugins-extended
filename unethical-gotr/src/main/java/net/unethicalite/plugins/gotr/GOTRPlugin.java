package net.unethicalite.plugins.gotr;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
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

import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Reachable;
import net.unethicalite.api.plugins.LoopedPlugin;
import net.unethicalite.api.widgets.Widgets;
import net.unethicalite.client.Static;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;
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

    private static final Set<Integer> GUARDIAN_IDS = ImmutableSet.of(43705, 43701, 43710, 43702, 43703, 43711, 43704, 43708, 43712, 43707, 43706, 43709, 43702);
    private static final Set<Integer> TALISMAN_IDS = GuardianInfo.ALL.stream().mapToInt(x -> x.talismanId).boxed().collect(Collectors.toSet());
    private static final int GREAT_GUARDIAN_ID = 11403;

    private static final int CATALYTIC_GUARDIAN_STONE_ID = 26880;
    private static final int ELEMENTAL_GUARDIAN_STONE_ID = 26881;
    private static final int POLYELEMENTAL_GUARDIAN_STONE_ID = 26941;

    private static final int ELEMENTAL_ESSENCE_PILE_ID = 43722;
    private static final int CATALYTIC_ESSENCE_PILE_ID = 43723;

    private static final int UNCHARGED_CELL_ITEM_ID = 26882;
    private static final int UNCHARGED_CELL_GAMEOBJECT_ID = 43732;
    private static final int TODO_WEAK_CELL_GAMEOBJECT_ID = 43732;
    private static final int CHISEL_ID = 1755;
    private static final int OVERCHARGED_CELL_ID = 26886;

    private static final int GUARDIAN_ACTIVE_ANIM = 9363;

    private static final int PARENT_WIDGET_ID = 48889857;
    private static final int CATALYTIC_RUNE_WIDGET_ID = 48889876;
    private static final int ELEMENTAL_RUNE_WIDGET_ID = 48889879;
    private static final int GUARDIAN_COUNT_WIDGET_ID = 48889886;
    private static final int PORTAL_WIDGET_ID = 48889884;

    private final static int PORTAL_SPRITE_ID = 4368;

    private static final int PORTAL_ID = 43729;

    private static final String REWARD_POINT_REGEX = "Total elemental energy:[^>]+>([\\d,]+).*Total catalytic energy:[^>]+>([\\d,]+).";
    private static final Pattern REWARD_POINT_PATTERN = Pattern.compile(REWARD_POINT_REGEX);
    private static final String CHECK_POINT_REGEX = "You have (\\d+) catalytic energy and (\\d+) elemental energy";
    private static final Pattern CHECK_POINT_PATTERN = Pattern.compile(CHECK_POINT_REGEX);

    private static final int DIALOG_WIDGET_GROUP = 229;
    private static final int DIALOG_WIDGET_MESSAGE = 1;
    private static final String BARRIER_DIALOG_FINISHING_UP = "It looks like the adventurers within are just finishing up. You must<br>wait until they are done to join.";

    @Getter(AccessLevel.PACKAGE)
    private final Set<GameObject> guardians = new HashSet<>();
    @Getter(AccessLevel.PACKAGE)
    private final Set<GameObject> activeGuardians = new HashSet<>();
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

        if (!isInMainRegion)
        {
            log.info("You are not in the main region for GOTR, idling");
            return 1000;
        }
        /*
        if ()
        {
            log.info("");
            return 500;
        }
        */


        switch(state) {
            case ENTER_AREA:
                if (enterMinigame())
                {
                    log.info("enterMinigame");
                    return 500;
                }

                if (Reachable.isInteractable(unchargedCellTable))
                {
                    log.info("Changing from ENTER_AREA to COLLECT_MATS");
                    state = STATE.COLLECT_MATS;
                    return 0;
                }

                break;
            case COLLECT_MATS:
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
                break;
            case CRAFT_ESSENCE:
                break;
            case USE_ALTAR:
                break;
            case MINE_FRAGS_PORTAL:
                break;
            case REDEEM_USE_ALTAR:
                break;
            case REDEEM_DEPOSIT:
                break;
            default:
                log.info("End of SWITCH, idling");
                return 1000;
        }



        log.info("End of LOOP, idling");
        return 1000;
    }

    private boolean getUnchargedCell()
    {
        if (!Reachable.isInteractable(unchargedCellTable) || Inventory.contains())
            return false;
        unchargedCellTable.interact(TODO);
        return true;
    }

    private boolean getWeakCell()
    {
        if (!Reachable.isInteractable(weakCellTable))
            return false;
        weakCellTable.interact(TODO);
        return true;
    }

    private boolean enterMinigame()
    {
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

        activeGuardians.removeIf(ag -> {
            Animation anim = ((DynamicObject)ag.getRenderable()).getAnimation();
            return anim == null || anim.getId() != GUARDIAN_ACTIVE_ANIM;
        });

        for(GameObject guardian : guardians){
            Animation animation = ((DynamicObject) guardian.getRenderable()).getAnimation();
            if(animation != null && animation.getId() == GUARDIAN_ACTIVE_ANIM) {
                activeGuardians.add(guardian);
            }
        }

        Widget elementalRuneWidget = client.getWidget(ELEMENTAL_RUNE_WIDGET_ID);
        Widget catalyticRuneWidget = client.getWidget(CATALYTIC_RUNE_WIDGET_ID);
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
            activeGuardians.removeIf(g -> g.getId() == gameObject.getId());
            guardians.add(gameObject);
        }

        if(gameObject.getId() == UNCHARGED_CELL_GAMEOBJECT_ID){
            unchargedCellTable = gameObject;
        }

        if(gameObject.getId() == WEAK_CELL_GAMEOBJECT_ID){
            unchargedCellTable = gameObject;
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
        activeGuardians.clear();
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
