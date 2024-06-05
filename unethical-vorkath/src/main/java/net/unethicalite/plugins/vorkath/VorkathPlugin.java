package net.unethicalite.plugins.vorkath;

import com.google.inject.Provides;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.events.ProjectileSpawned;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import net.unethicalite.api.game.Combat;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.movement.Reachable;
import net.unethicalite.api.plugins.LoopedPlugin;
import net.unethicalite.api.widgets.Prayers;
import net.unethicalite.api.widgets.Widgets;
import net.unethicalite.client.Static;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;


@Extension
@PluginDescriptor(
        name = "Unethical Vorkath",
        description = "Vorkath",
        enabledByDefault = false
)
@Slf4j
public class VorkathPlugin extends LoopedPlugin
{

    @Inject
    private Client client;
    @Inject
    private VorkathConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private OverlayManager overlayManager;


    private static final int VORKATH_REGION = 9023;

    @Getter(AccessLevel.PACKAGE)
    private Vorkath vorkath;

    @Getter(AccessLevel.PACKAGE)
    private NPC zombifiedSpawn;

    @Getter(AccessLevel.PACKAGE)
    private List<WorldPoint> acidSpots = new ArrayList<>();

    @Getter(AccessLevel.PACKAGE)
    private List<WorldPoint> acidFreePath = new ArrayList<>();

    @Getter(AccessLevel.PACKAGE)
    private WorldPoint[] wooxWalkPath = new WorldPoint[2];

    @Getter(AccessLevel.PACKAGE)
    private long wooxWalkTimer = -1;

    @Getter(AccessLevel.PACKAGE)
    private Rectangle wooxWalkBar;
    private int lastAcidSpotsSize = 0;

    public static final int VORKATH_WAKE_UP = 7950;
    public static final int VORKATH_DEATH = 7949;
    public static final int VORKATH_SLASH_ATTACK = 7951;
    public static final int VORKATH_ATTACK = 7952;
    public static final int VORKATH_FIRE_BOMB_OR_SPAWN_ATTACK = 7960;
    public static final int VORKATH_ACID_ATTACK = 7957;

    private WorldPoint deadlyFireballLocation;


    @Subscribe
    public void onConfigButtonPressed(ConfigButtonClicked event)
    {
        if (!event.getGroup().contains("unethical-vorkath"))
        {
            return;
        }

    }

    @Override
    protected void shutDown()
    {
        reset();
    }

    @Subscribe
    private void onNpcSpawned(NpcSpawned event)
    {
        if (!isAtVorkath())
        {
            return;
        }

        final NPC npc = event.getNpc();

        if (npc.getName() == null)
        {
            return;
        }

        if (npc.getName().equals("Vorkath"))
        {
            vorkath = new Vorkath(npc);
        }
        else if (npc.getName().equals("Zombified Spawn"))
        {
            zombifiedSpawn = npc;
        }
    }

    @Subscribe
    private void onNpcDespawned(NpcDespawned event)
    {
        if (!isAtVorkath())
        {
            return;
        }

        final NPC npc = event.getNpc();

        if (npc.getName() == null)
        {
            return;
        }

        if (npc.getName().equals("Vorkath"))
        {
            reset();
        }
        else if (npc.getName().equals("Zombified Spawn"))
        {
            zombifiedSpawn = null;
        }
    }

    @Subscribe
    private void onProjectileSpawned(ProjectileSpawned event)
    {
        if (!isAtVorkath() || vorkath == null)
        {
            return;
        }

        final Projectile proj = event.getProjectile();
        final VorkathAttack vorkathAttack = VorkathAttack.getVorkathAttack(proj.getId());

        if (vorkathAttack != null)
        {

            if (vorkathAttack == VorkathAttack.FIRE_BOMB)
            {
                deadlyFireballLocation = client.getLocalPlayer().getWorldLocation();
            }
            if (VorkathAttack.isBasicAttack(vorkathAttack.getProjectileID()) && vorkath.getAttacksLeft() > 0)
            {
                vorkath.setAttacksLeft(vorkath.getAttacksLeft() - 1);
            }
            else if (vorkathAttack == VorkathAttack.ACID)
            {
                vorkath.updatePhase(Vorkath.Phase.ACID);
                vorkath.setAttacksLeft(0);
            }
            else if (vorkathAttack == VorkathAttack.FIRE_BALL)
            {
                vorkath.updatePhase(Vorkath.Phase.FIRE_BALL);
                vorkath.setAttacksLeft(vorkath.getAttacksLeft() - 1);
            }
            else if (vorkathAttack == VorkathAttack.FREEZE_BREATH || vorkathAttack == VorkathAttack.ZOMBIFIED_SPAWN)
            {
                vorkath.updatePhase(Vorkath.Phase.SPAWN);
                vorkath.setAttacksLeft(0);
            }
            else
            {
                vorkath.updatePhase(vorkath.getNextPhase());
                vorkath.setAttacksLeft(vorkath.getAttacksLeft() - 1);
            }

            log.debug("[Vorkath ({})] {}", vorkathAttack, vorkath);
            vorkath.setLastAttack(vorkathAttack);
        }
    }

    @Subscribe
    private void onProjectileMoved(ProjectileMoved event)
    {
        if (!isAtVorkath())
        {
            return;
        }

        final Projectile proj = event.getProjectile();
        final LocalPoint loc = event.getPosition();

        if (proj.getId() == ProjectileID.VORKATH_POISON_POOL_AOE)
        {
            addAcidSpot(WorldPoint.fromLocal(client, loc));
        }
    }

    @Subscribe
    private void onGameObjectSpawned(GameObjectSpawned event)
    {
        if (!isAtVorkath())
        {
            return;
        }

        final GameObject obj = event.getGameObject();

        if (obj.getId() == ObjectID.ACID_POOL || obj.getId() == ObjectID.ACID_POOL_32000)
        {
            addAcidSpot(obj.getWorldLocation());
        }
    }

    @Subscribe
    private void onGameObjectDespawned(GameObjectDespawned event)
    {
        if (!isAtVorkath())
        {
            return;
        }

        final GameObject obj = event.getGameObject();

        if (obj.getId() == ObjectID.ACID_POOL || obj.getId() == ObjectID.ACID_POOL_32000)
        {
            acidSpots.remove(obj.getWorldLocation());
        }
    }

    @Subscribe
    private void onAnimationChanged(AnimationChanged event)
    {
        if (!isAtVorkath())
        {
            return;
        }

        final Actor actor = event.getActor();

        if (isAtVorkath() && vorkath != null && actor.equals(vorkath.getVorkath())
                && actor.getAnimation() == VorkathAttack.SLASH_ATTACK.getVorkathAnimationID())
        {
            if (vorkath.getAttacksLeft() > 0)
            {
                vorkath.setAttacksLeft(vorkath.getAttacksLeft() - 1);
            }
            else
            {
                vorkath.updatePhase(vorkath.getNextPhase());
                vorkath.setAttacksLeft(vorkath.getAttacksLeft() - 1);
            }
            log.debug("[Vorkath (SLASH_ATTACK)] {}", vorkath);
        }
    }

    @Subscribe
    private void onGameTick(GameTick event)
    {
        if (!isAtVorkath())
        {
            return;
        }

        // Update the acid free path every tick to account for player movement
        if (!acidSpots.isEmpty())
        {
            calculateAcidFreePath();
        }

        // Start the timer when the player walks into the WooxWalk zone
        if (wooxWalkPath[0] != null && wooxWalkPath[1] != null)
        {
            final WorldPoint playerLoc = client.getLocalPlayer().getWorldLocation();

            if (playerLoc.getX() == wooxWalkPath[0].getX() && playerLoc.getY() == wooxWalkPath[0].getY()
                    && playerLoc.getPlane() == wooxWalkPath[0].getPlane())
            {
                if (wooxWalkTimer == -1)
                {
                    wooxWalkTimer = System.currentTimeMillis() - 400;
                }
            }
            else if (playerLoc.getX() == wooxWalkPath[1].getX() && playerLoc.getY() == wooxWalkPath[1].getY()
                    && playerLoc.getPlane() == wooxWalkPath[1].getPlane())
            {
                if (wooxWalkTimer == -1)
                {
                    wooxWalkTimer = System.currentTimeMillis() - 1000;
                }
            }
            else if (wooxWalkTimer != -1)
            {
                wooxWalkTimer = -1;
            }
        }


        //Custom bot code
        if (deadlyFireballLocation != null)
        {
            WorldPoint worldLocation = client.getLocalPlayer().getWorldLocation();

            if (worldLocation.distanceTo(deadlyFireballLocation) >= 2)
                deadlyFireballLocation = null;
            else
            {
                WorldPoint nearestSafeInteractablePoint = Reachable.getInteractable(vorkath.getVorkath()).stream()
                        .filter(x -> x.distanceTo(worldLocation) >= 2)
                        .min(Comparator.comparingInt(x -> x.distanceTo(worldLocation)))
                        .orElse(null);
                if (nearestSafeInteractablePoint == null)
                {
                    log.info("No safe location to attack vorkath from. Trying to move away.");

                    nearestSafeInteractablePoint = client.getLocalPlayer().getWorldArea().offset(5).toWorldPointList().stream()
                            .filter(x -> x.distanceTo(worldLocation) >= 2)
                            .min(Comparator.comparingInt(x -> x.distanceTo(worldLocation)))
                            .orElse(null);
                }

                if (nearestSafeInteractablePoint == null)
                {
                    log.info("No safe location to move to. Tanking fireball");
                    deadlyFireballLocation = null;
                }
                else
                {
                    log.info("Dodging deadly fireball");
                    Movement.walkTo(nearestSafeInteractablePoint);
                    return;
                }
            }
        }

        if (!Prayers.isQuickPrayerEnabled())
        {
            log.info("Toggling quick prayers");
            Prayers.toggleQuickPrayer(true);
            return;
        }

        if (!(Combat.isSuperAntifired()))
        {
            Item antifirePotion = Inventory.getFirst(x -> x.hasAction("Drink") && x.getName().toLowerCase().contains("super antifire"));
            if (antifirePotion == null)
            {
                log.info("No antifire!");
                breakTeleportTab();
            }
            else
            {
                log.info("Drinking antifire");
                antifirePotion.interact("Drink");
            }
            return;
        }

        if (Combat.isVenomed() || Combat.isPoisoned())
        {
            Item antivenomPotion = Inventory.getFirst(x -> x.hasAction("Drink") && x.getName().toLowerCase().contains("venom"));
            if (antivenomPotion == null)
            {
                log.info("No anti venom!");
                breakTeleportTab();
            }
            else
            {
                log.info("Drinking anti venom");
                antivenomPotion.interact("Drink");
            }
            return;
        }






    }

    public void breakTeleportTab()
    {
        Item teleTab = Inventory.getFirst(x -> x.hasAction("Break"));
        if (teleTab == null)
        {
            log.info("No teletab!");
            return;
        }
        teleTab.interact("Break");
    }


    @Subscribe
    private void onClientTick(ClientTick event)
    {
        if (acidSpots.size() != lastAcidSpotsSize)
        {
            if (acidSpots.size() == 0)
            {
                acidFreePath.clear();
                Arrays.fill(wooxWalkPath, null);
                wooxWalkTimer = -1;
            }
            else
            {
                calculateAcidFreePath();
                calculateWooxWalkPath();
            }

            lastAcidSpotsSize = acidSpots.size();
        }
    }

    /**
     * @return true if the player is in the Vorkath region, false otherwise
     */
    private boolean isAtVorkath()
    {
        for (int mapRegion : client.getMapRegions())
        {
            if (mapRegion == VORKATH_REGION)
            {
                return true;
            }
        }
        return false;
    }

    private void addAcidSpot(WorldPoint acidSpotLocation)
    {
        if (!acidSpots.contains(acidSpotLocation))
        {
            acidSpots.add(acidSpotLocation);
        }
    }

    private void calculateAcidFreePath()
    {
        acidFreePath.clear();

        if (vorkath == null)
        {
            return;
        }

        final int[][][] directions = {
                {
                        {0, 1}, {0, -1} // Positive and negative Y
                },
                {
                        {1, 0}, {-1, 0} // Positive and negative X
                }
        };

        List<WorldPoint> bestPath = new ArrayList<>();
        double bestClicksRequired = 99;

        final WorldPoint playerLoc = client.getLocalPlayer().getWorldLocation();
        final WorldPoint vorkLoc = vorkath.getVorkath().getWorldLocation();
        final int maxX = vorkLoc.getX() + 14;
        final int minX = vorkLoc.getX() - 8;
        final int maxY = vorkLoc.getY() - 1;
        final int minY = vorkLoc.getY() - 8;

        // Attempt to search an acid free path, beginning at a location
        // adjacent to the player's location (including diagonals)
        for (int x = -1; x < 2; x++)
        {
            for (int y = -1; y < 2; y++)
            {
                final WorldPoint baseLocation = new WorldPoint(playerLoc.getX() + x,
                        playerLoc.getY() + y, playerLoc.getPlane());

                if (acidSpots.contains(baseLocation) || baseLocation.getY() < minY || baseLocation.getY() > maxY)
                {
                    continue;
                }

                // Search in X and Y direction
                for (int d = 0; d < directions.length; d++)
                {
                    // Calculate the clicks required to start walking on the path
                    double currentClicksRequired = Math.abs(x) + Math.abs(y);
                    if (currentClicksRequired < 2)
                    {
                        currentClicksRequired += Math.abs(y * directions[d][0][0]) + Math.abs(x * directions[d][0][1]);
                    }
                    if (d == 0)
                    {
                        // Prioritize a path in the X direction (sideways)
                        currentClicksRequired += 0.5;
                    }

                    List<WorldPoint> currentPath = new ArrayList<>();
                    currentPath.add(baseLocation);

                    // Positive X (first iteration) or positive Y (second iteration)
                    for (int i = 1; i < 25; i++)
                    {
                        final WorldPoint testingLocation = new WorldPoint(baseLocation.getX() + i * directions[d][0][0],
                                baseLocation.getY() + i * directions[d][0][1], baseLocation.getPlane());

                        if (acidSpots.contains(testingLocation) || testingLocation.getY() < minY || testingLocation.getY() > maxY
                                || testingLocation.getX() < minX || testingLocation.getX() > maxX)
                        {
                            break;
                        }

                        currentPath.add(testingLocation);
                    }

                    // Negative X (first iteration) or positive Y (second iteration)
                    for (int i = 1; i < 25; i++)
                    {
                        final WorldPoint testingLocation = new WorldPoint(baseLocation.getX() + i * directions[d][1][0],
                                baseLocation.getY() + i * directions[d][1][1], baseLocation.getPlane());

                        if (acidSpots.contains(testingLocation) || testingLocation.getY() < minY || testingLocation.getY() > maxY
                                || testingLocation.getX() < minX || testingLocation.getX() > maxX)
                        {
                            break;
                        }

                        currentPath.add(testingLocation);
                    }

                    if (currentPath.size() >= config.acidFreePathLength() && currentClicksRequired < bestClicksRequired
                            || (currentClicksRequired == bestClicksRequired && currentPath.size() > bestPath.size()))
                    {
                        bestPath = currentPath;
                        bestClicksRequired = currentClicksRequired;
                    }
                }
            }
        }

        if (bestClicksRequired != 99)
        {
            acidFreePath = bestPath;
        }
    }

    private void calculateWooxWalkPath()
    {
        wooxWalkTimer = -1;

        updateWooxWalkBar();

        if (client.getLocalPlayer() == null || vorkath.getVorkath() == null)
        {
            return;
        }

        final WorldPoint playerLoc = client.getLocalPlayer().getWorldLocation();
        final WorldPoint vorkLoc = vorkath.getVorkath().getWorldLocation();

        final int maxX = vorkLoc.getX() + 14;
        final int minX = vorkLoc.getX() - 8;
        final int baseX = playerLoc.getX();
        final int baseY = vorkLoc.getY() - 5;
        final int middleX = vorkLoc.getX() + 3;

        // Loop through the arena tiles in the x-direction and
        // alternate between positive and negative x direction
        for (int i = 0; i < 50; i++)
        {
            // Make sure we always choose the spot closest to
            // the middle of the arena
            int directionRemainder = 0;
            if (playerLoc.getX() < middleX)
            {
                directionRemainder = 1;
            }

            int deviation = (int) Math.floor(i / 2.0);
            if (i % 2 == directionRemainder)
            {
                deviation = -deviation;
            }

            final WorldPoint attackLocation = new WorldPoint(baseX + deviation, baseY, playerLoc.getPlane());
            final WorldPoint outOfRangeLocation = new WorldPoint(baseX + deviation, baseY - 1, playerLoc.getPlane());

            if (acidSpots.contains(attackLocation) || acidSpots.contains(outOfRangeLocation)
                    || attackLocation.getX() < minX || attackLocation.getX() > maxX)
            {
                continue;
            }

            wooxWalkPath[0] = attackLocation;
            wooxWalkPath[1] = outOfRangeLocation;

            break;
        }
    }

    private void updateWooxWalkBar()
    {
        // Update the WooxWalk tick indicator's dimensions
        // based on the canvas dimensions
        final Widget exp = client.getWidget(WidgetInfo.EXPERIENCE_TRACKER);

        if (exp == null)
        {
            return;
        }

        final Rectangle screen = exp.getBounds();

        int width = (int) Math.floor(screen.getWidth() / 2.0);
        if (width % 2 == 1)
        {
            width++;
        }
        int height = (int) Math.floor(width / 20.0);
        if (height % 2 == 1)
        {
            height++;
        }
        final int x = (int) Math.floor(screen.getX() + width / 2.0);
        final int y = (int) Math.floor(screen.getY() + screen.getHeight() - 2 * height);
        wooxWalkBar = new Rectangle(x, y, width, height);
    }

    private void reset()
    {
        vorkath = null;
        acidSpots.clear();
        acidFreePath.clear();
        Arrays.fill(wooxWalkPath, null);
        wooxWalkTimer = -1;
        zombifiedSpawn = null;
    }

    @Override
    protected int loop()
    {
        if (!config.isEnabled())
        {
            return 1000;
        }

        log.info("End of switch, idling");
        return 1000;
    }

    @Provides
    VorkathConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(VorkathConfig.class);
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
