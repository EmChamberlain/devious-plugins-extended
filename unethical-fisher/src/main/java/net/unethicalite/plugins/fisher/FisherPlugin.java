package net.unethicalite.plugins.fisher;



import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.NPC;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.unethicalite.api.Interactable;
import net.unethicalite.api.entities.NPCs;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.movement.Reachable;
import net.unethicalite.api.plugins.LoopedPlugin;
import net.unethicalite.api.widgets.Widgets;
import net.unethicalite.client.Static;
import org.pf4j.Extension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;


@Slf4j
@Extension
@PluginDescriptor(
        name = "Unethical fisher",
        description = "fishes and cooks for you",
        enabledByDefault = false
)
public class FisherPlugin extends LoopedPlugin
{
    @Inject private Client client;
    @Inject private FisherConfig config;
    @Inject private FisherOverlay overlay;

    @Inject private OverlayManager overlayManager;
    private boolean overlayEnabled;

    private final ArrayList<Integer> fishingAnimations = new ArrayList<>();
    private final ArrayList<Integer> cookingAnimations = new ArrayList<>();

    private int cookingUntil = 0;

    private int fishingUntil = 0;

    public FisherPlugin()
    {
        fishingAnimations.add(618);
        fishingAnimations.add(619);
        fishingAnimations.add(622);
        fishingAnimations.add(6703);
        fishingAnimations.add(6704);
        fishingAnimations.add(6707);
        fishingAnimations.add(6708);
        fishingAnimations.add(6709);
        fishingAnimations.add(7261);

        cookingAnimations.add(883);
        cookingAnimations.add(896);
        cookingAnimations.add(897);
    }

    public boolean isFishing()
    {
        if (client.getLocalPlayer().getAnimation() != -1 && fishingAnimations.contains(client.getLocalPlayer().getAnimation()))
        {
            fishingUntil = client.getTickCount() + 3;
        }

        return fishingUntil > client.getTickCount();
    }

    public boolean isCooking()
    {
        if (client.getLocalPlayer().getAnimation() != -1 && cookingAnimations.contains(client.getLocalPlayer().getAnimation()))
        {
            cookingUntil = client.getTickCount() + 3;
        }

        return cookingUntil > client.getTickCount();
    }

    private Interactable getNearestBankNPC()
    {
        Interactable nearestNPC = NPCs.getNearest(object -> object != null && object.getActions() != null && Arrays.asList(object.getActions()).contains("Bank") && Reachable.isInteractable(object));
        if (nearestNPC == null)
            nearestNPC = TileObjects.getNearest(object -> object != null && object.getActions() != null && Arrays.asList(object.getActions()).contains("Bank") && Reachable.isInteractable(object));
        // get nearest npc that has a "Bank" option
        return nearestNPC;
    }

    private NPC getNearestFishingSpotNPC()
    {
        // get nearest npc that has a "Cage" and "Harpoon" option
        return NPCs.getNearest(
                npc -> npc.getActions() != null && Arrays.asList(npc.getActions()).contains(config.identifierAction()) && Arrays.asList(npc.getActions()).contains(config.fishingAction()) && Reachable.isInteractable(npc)
        );
    }

    private TileObject getNearestRangeObject()
    {
        // get nearest npc that has a "Bank" option
        return TileObjects.getNearest(
                object -> object != null && object.getActions() != null && Arrays.asList(object.getActions()).contains("Cook") && Reachable.isInteractable(object)
        );
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

    public WorldPoint bankWorldPoint = new WorldPoint(2814, 3437, 0);

    //TODO: Implement below
    public WorldPoint cookWorldPoint = new WorldPoint(2814, 3440, 0);
    public WorldPoint fishingWorldPoint = new WorldPoint(2848, 3431, 0);

    private boolean handleDropFish()
    {
        if (isCooking() || isFishing())
            return false;
        var burntFish = Inventory.getFirst(x -> x.getName().toLowerCase().contains("burnt"));
        if (burntFish != null)
        {
            burntFish.interact("Drop");
            return true;
        }
        return false;
    }

    private boolean handleClickWidget()
    {
        var chatboxWidget = client.getWidget(ComponentID.CHATBOX_MESSAGES);
        if (chatboxWidget == null)
            return false;
        log.info("Have chatbox widget");
        var children = getFlatChildren(chatboxWidget);

        if (children.isEmpty())
            return false;
        log.info("Children not empty");
        Widget widget = children.stream().filter(x -> {
            log.info("Handling ID: {} | Name: {} | Text: {}", x.getId(), x.getName(), x.getText());
            String[] splitFish = config.cookedFish().split(",");
            for (String fish : splitFish)
            {
                if (x.getName().toLowerCase().contains(fish.toLowerCase()))
                    return true;
            }
            return false;
        }).findFirst().orElse(null);
        if (widget != null)
        {
            log.info("Found widget ID: {} | Name: {} | Text: {}", widget.getId(), widget.getName(), widget.getText());
            widget.interact(0);
            return true;
        }
        log.info("Did not find widget");
        return false;
    }

    private boolean handleMoveToFishingSpot()
    {
        NPC fishingNPC = getNearestFishingSpotNPC();
        if (fishingNPC != null || Inventory.isFull() || isFishing() || !Inventory.contains(x -> x.getName().toLowerCase().contains(config.fishingItem().toLowerCase())))
            return false;

        Movement.walkTo(fishingWorldPoint);
        return true;
    }

    private boolean handleFishAtSpot()
    {
        NPC fishingNPC = getNearestFishingSpotNPC();
        if (fishingNPC == null || Inventory.isFull() || isFishing() || !Inventory.contains(x -> x.getName().toLowerCase().contains(config.fishingItem().toLowerCase())))
            return false;

        if (fishingNPC.hasAction(config.fishingAction()))
        {
            fishingNPC.interact(config.fishingAction());
            return true;
        }
        return false;
    }

    private boolean handleMoveToCook()
    {
        if (isFishing() || isCooking())
            return false;
        Interactable cookInteractable = getNearestRangeObject();
        if (cookInteractable == null && Inventory.isFull() && Inventory.contains(x -> x.getName().toLowerCase().contains("raw")))
        {
            Movement.walkTo(cookWorldPoint);
            return true;
        }
        return false;
    }

    private boolean handleCook()
    {
        if (isFishing() || isCooking())
            return false;
        Interactable cookInteractable = getNearestRangeObject();
        if (cookInteractable != null && Inventory.isFull() && Inventory.contains(x -> x.getName().toLowerCase().contains("raw")))
        {
            cookInteractable.interact("Cook");
            return true;
        }
        return false;
    }

    private boolean handleMoveToBank()
    {
        if (isFishing() || isCooking())
            return false;
        Interactable bankInteractable = getNearestBankNPC();
        if (bankInteractable == null && Inventory.isFull() && Inventory.contains(x -> {
            String inventoryObjectName = x.getName().toLowerCase();
            return Arrays.stream(config.cookedFish().split(",")).map(String::toLowerCase).anyMatch(inventoryObjectName::contains);
        }))
        {
            Movement.walkTo(bankWorldPoint);
            return true;
        }
        return false;
    }

    private boolean handleOpenBank()
    {
        if (isFishing() || isCooking())
            return false;
        Interactable bankInteractable = getNearestBankNPC();
        if (bankInteractable != null && Inventory.isFull() && Inventory.contains(x -> {
            String inventoryObjectName = x.getName().toLowerCase();
            return Arrays.stream(config.cookedFish().split(",")).map(String::toLowerCase).anyMatch(inventoryObjectName::contains);
        }))
        {
            bankInteractable.interact("Bank");
            return true;
        }
        return false;
    }

    private boolean handleDepositInventory()
    {
        if (!Bank.isOpen())
            return false;

        Item firstToDeposit = Bank.Inventory.getFirst(x -> x.getName().toLowerCase().contains(config.cookedFish().toLowerCase()));

        if (firstToDeposit != null)
        {
            Bank.depositAll(firstToDeposit.getId());
            return true;
        }
        return false;
    }

    private boolean handleWithdrawFishingItem()
    {
        if (!Bank.isOpen())
            return false;
        if (!Inventory.contains(x -> x.getName().toLowerCase().contains(config.fishingItem().toLowerCase())))
        {
            Bank.withdraw(x -> x.getName().toLowerCase().contains(config.fishingItem().toLowerCase()), 1, Bank.WithdrawMode.ITEM);
            return true;
        }
        return false;
    }

    private boolean handleCloseBank()
    {
        Widget closeWidget = getWidget(WidgetID.BANK_GROUP_ID, x -> x != null &&  x.equals("Close"));
        if (closeWidget == null)
            closeWidget = getWidget(WidgetID.BANK_INVENTORY_GROUP_ID, x -> x != null &&  x.equals("Close"));
        if(closeWidget != null && !Inventory.isFull() && Inventory.contains(x -> x.getName().toLowerCase().contains(config.fishingItem().toLowerCase())))
        {
            closeWidget.interact("Close");
            return true;
        }
        return false;
    }



    @Override
    public int loop()
    {
        boolean actionTakenThisTick = handleDropFish();
        if (actionTakenThisTick)
        {
            log.info("Tried to drop fish");
            return 50;
        }

        actionTakenThisTick = handleClickWidget();
        if (actionTakenThisTick)
        {
            log.info("Tried to click on widget");
            return 1000;
        }

        actionTakenThisTick = handleMoveToFishingSpot();
        if (actionTakenThisTick)
        {
            log.info("Tried to move to fish");
            return 1000;
        }

        actionTakenThisTick = handleFishAtSpot();
        if (actionTakenThisTick)
        {
            log.info("Tried to fish at spot");
            return 5000;
        }

        if (config.toCook())
        {
            actionTakenThisTick = handleMoveToCook();
            if (actionTakenThisTick)
            {
                log.info("Tried to move to cook");
                return 2500;
            }

            actionTakenThisTick = handleCook();
            if (actionTakenThisTick)
            {
                log.info("Tried to cook");
                return 2500;
            }
        }

        actionTakenThisTick = handleMoveToBank();
        if (actionTakenThisTick)
        {
            log.info("Tried to move to bank");
            return 2500;
        }

        actionTakenThisTick = handleOpenBank();
        if (actionTakenThisTick)
        {
            log.info("Tried to open bank");
            return 2500;
        }

        actionTakenThisTick = handleDepositInventory();
        if (actionTakenThisTick)
        {
            log.info("Tried to deposit inventory");
            return 1000;
        }

        actionTakenThisTick = handleWithdrawFishingItem();
        if (actionTakenThisTick)
        {
            log.info("Tried to withdraw fishing item");
            return 1000;
        }

        actionTakenThisTick = handleCloseBank();
        if (actionTakenThisTick)
        {
            log.info("Tried to close bank");
            return 1000;
        }

        log.info("Nothing to do so idling.");
        return 1000;
    }

    @Provides
    FisherConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(FisherConfig.class);
    }

    @Subscribe
    public void onConfigButtonPressed(ConfigButtonClicked event)
    {
        if (!event.getGroup().contains("unethical-fisher"))
        {
            return;
        }

        if (event.getKey().toLowerCase().contains("setbanktile"))
        {
            bankWorldPoint = client.getLocalPlayer().getWorldLocation();
        }
        if (event.getKey().toLowerCase().contains("setfishtile"))
        {
            fishingWorldPoint = client.getLocalPlayer().getWorldLocation();
        }
        if (event.getKey().toLowerCase().contains("setcooktile"))
        {
            cookWorldPoint = client.getLocalPlayer().getWorldLocation();
        }

    }

    @Override
    protected void startUp()
    {
        enableOverlay();
    }

    @Override
    protected void shutDown()
    {
        disableOverlay();
    }

    private void enableOverlay()
    {
        if (overlayEnabled)
        {
            return;
        }

        overlayEnabled = true;
        overlayManager.add(overlay);
    }

    private void disableOverlay()
    {
        if (overlayEnabled)
        {
            overlayManager.remove(overlay);
        }
        overlayEnabled = false;
    }
}

