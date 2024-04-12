package net.unethicalite.plugins.armouranimator;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginDescriptor;
import net.unethicalite.api.Interactable;
import net.unethicalite.api.entities.NPCs;
import net.unethicalite.api.entities.Players;
import net.unethicalite.api.entities.TileItems;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.movement.Reachable;
import net.unethicalite.api.plugins.LoopedPlugin;
import net.unethicalite.api.widgets.Widgets;
import net.unethicalite.client.Static;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Extension
@PluginDescriptor(
        name = "Unethical Armour Animator",
        description = "Animates armour for you",
        enabledByDefault = false
)
@Slf4j
public class ArmourAnimatorPlugin extends LoopedPlugin
{
    @Inject
    private ArmourAnimatorConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private Client client;

    private static final WorldPoint bankLocation = new WorldPoint(2843, 3543, 0);
    private static final WorldPoint animatorLocation = new WorldPoint(2855, 3542, 0);


    @Override
    protected int loop()
    {
        var localPlayer = client.getLocalPlayer();
        if (localPlayer == null || (client.getBoostedSkillLevel(Skill.HITPOINTS) < config.minHealth()))
        {
            configManager.setConfiguration("unethical-armouranimator", "isEnabled", "false");
        }
        if (!config.isEnabled())
        {
            return 1000;
        }
        NPC animatedAttackableNPC = NPCs.getNearest( x -> x.hasAction("Attack") && x.getName().toLowerCase().contains("animated"));
        if(animatedAttackableNPC != null)
        {
            if (!targetDeadOrNoTarget())
            {
                log.info("We are in combat, idling");
                return 1000;
            }
            else
            {
                log.info("Attacking target");
                if(Reachable.isWalkable(animatedAttackableNPC.getWorldLocation()))
                {
                    animatedAttackableNPC.interact("Attack");
                }
                else
                {
                    Movement.walkTo(animatedAttackableNPC.getWorldLocation());
                }
                return 500;
            }
        }
        else
        {
            TileItem tokens = TileItems.getNearest("token");
            if (tokens != null)
            {
                tokens.interact("Take");
                return 50;
            }
            else
            {
                TileItem closestArmourPiece = TileItems.getNearest("platebody", "platelegs", "full helm");
                if (closestArmourPiece != null)
                {
                    closestArmourPiece.interact("Take");
                    return 50;
                }
                else
                {
                    Item foodItem = Inventory.getFirst( x -> x.hasAction("Eat"));
                    TileObject animator = TileObjects.getNearest(x -> x.hasAction("Animate"));
                    if(foodItem == null)
                    {
                        Interactable bankBooth = TileObjects.getNearest("Bank booth");
                        if(bankBooth == null)
                        {
                            log.info("Walking to bank");
                            Movement.walkTo(bankLocation);
                            return 1000;
                        }
                        else if (!Bank.isOpen())
                        {
                            log.info("Opening bank");
                            bankBooth.interact("Bank");
                            return 1000;
                        }
                        else
                        {
                            if(Bank.isNotedWithdrawMode())
                            {
                                log.info("Setting to item withdraw mode");
                                Bank.setWithdrawMode(false);
                                return 1000;
                            }
                            Item bankedFoodItem = Bank.getFirst(x -> x.getName().contains(config.foodToUse()));
                            if (bankedFoodItem == null)
                            {
                                log.info("Out of food! Idling.");
                                return 1000;
                            }
                            log.info("Attempting to withdraw previous amount");
                            Bank.withdrawLastQuantity(x -> x.getName().contains(config.foodToUse()), Bank.WithdrawMode.ITEM);
                            return 500;
                        }
                    }
                    else if(Bank.isOpen())
                    {
                        Bank.close();
                        return 1000;
                    }
                    else if (animator == null || !Reachable.isWalkable(animator.getWorldLocation()))
                    {
                        log.info("Walking to animator location");
                        Movement.walkTo(animatorLocation);
                        return 500;
                    }
                    else
                    {
                        log.info("Animating");
                        animator.interact("Animate");
                        return 5000;
                    }
                }
            }
        }
    }

    @Subscribe
    private void onGameTick(GameTick e)
    {

    }

    @Provides
    ArmourAnimatorConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ArmourAnimatorConfig.class);
    }

    private static Interactable getAnimatorInteractable()
    {
        TileObject nearestObject = TileObjects.getNearest(x -> x != null && x.hasAction("Animate"));
        if (nearestObject != null)
            return nearestObject;
        else
            return NPCs.getNearest(x -> x != null && x.hasAction("Animate"));
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

    private NPC getEligibleNpcInteractingWithUs()
    {
        return NPCs.getNearest((npc) ->
                (npc != null && npc.getName() != null &&
                        (npc.getInteracting() == client.getLocalPlayer() && npc.getHealthRatio() != 0) &&
                        npc.hasAction("Attack") &&
                        Reachable.isWalkable(npc.getWorldLocation())
                )
        );
    }

    private boolean targetDeadOrNoTarget()
    {
        NPC interactingWithUs = getEligibleNpcInteractingWithUs();

        if (client.getLocalPlayer().getInteracting() == null && interactingWithUs == null)
        {
            return true;
        }

        if (interactingWithUs != null)
        {
            return false;
        }

        if (client.getLocalPlayer().getInteracting() instanceof NPC)
        {
            NPC npcTarget = (NPC) client.getLocalPlayer().getInteracting();
            int ratio = npcTarget.getHealthRatio();

            return ratio == 0;
        }

        return false;
    }
}
