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
import java.util.stream.Stream;


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

    @Inject private ConfigManager configManager;
    @Inject public FisherConfig config;
    @Inject private FisherOverlay overlay;

    @Inject private OverlayManager overlayManager;
    private boolean overlayEnabled;

    private static final List<Integer> fishingAnimations = List.of(618, 619, 622, 6703, 6704, 6707, 6708, 6709, 7261, 1193, 9349, 9350);
    private static final List<Integer> cookingAnimations = List.of(883, 896, 897);

    private int cookingUntil = 0;

    private int fishingUntil = 0;

    private boolean droppingFish = false;

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
        if (nearestNPC == null)
            nearestNPC = TileObjects.getNearest(object -> object != null && object.getActions() != null && object.getName().toLowerCase().contains("bank chest") && Reachable.isInteractable(object));
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
        for (var child : widget.getDynamicChildren())
            list.addAll(getFlatChildren(child));
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

    private List<String> getCookedFishList()
    {
        return Arrays.stream(config.cookedFish().split(",")).map(String::toLowerCase).collect(Collectors.toList());
    }

    private boolean handleDropFish()
    {
        if (isCooking() || isFishing() || Bank.isOpen())
        {
            droppingFish = false;
            return false;
        }
        for (String fish : getCookedFishList())
        {
            var fishToDrop = Inventory.getAll(x -> x.getName().toLowerCase().contains(fish) && x.hasAction("Drop"));
            if (fishToDrop != null && !fishToDrop.isEmpty())
            {
                for (int i = 0; i < config.dropsPerTick(); i++)
                {
                    fishToDrop.get(i).interact("Drop");
                }
                droppingFish = true;
                return true;
            }
        }
        droppingFish = false;
        return false;
    }

    private boolean handleClickWidget()
    {
        var containerWidget = client.getWidget(17694733);

        if (containerWidget == null)
            return false;

        var children = containerWidget.getStaticChildren();

        if (children.length == 0)
            return false;
        //log.info("Children not empty");
        Widget widget = Arrays.stream(children).filter(x -> {
            //log.info("Handling ID: {} | Name: {} | Text: {}", x.getId(), x.getName(), x.getText());
            for (String fish : getCookedFishList())
            {
                if (x.getName().toLowerCase().contains(fish.toLowerCase()) && !x.getName().toLowerCase().contains("poison"))
                    return true;
            }
            return false;
        }).findFirst().orElse(null);
        if (widget != null)
        {
            //log.info("Found widget ID: {} | Name: {} | Text: {}", widget.getId(), widget.getName(), widget.getText());
            widget.interact(0);
            return true;
        }
        //log.info("Did not find widget");
        return false;
    }

    private boolean handleMoveToFishingSpot()
    {
        NPC fishingNPC = getNearestFishingSpotNPC();
        if (fishingNPC != null || Inventory.isFull() || isFishing() || !Inventory.contains(x -> x.getName().toLowerCase().contains(config.fishingItem().toLowerCase())) || Bank.isOpen())
            return false;

        Movement.walkTo(config.fishWorldPoint());
        return true;
    }

    private boolean handleFishAtSpot()
    {
        NPC fishingNPC = getNearestFishingSpotNPC();
        if (fishingNPC == null || Inventory.isFull() || isFishing() || !Inventory.contains(x -> x.getName().toLowerCase().contains(config.fishingItem().toLowerCase())) || Bank.isOpen())
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
        if (isFishing() || isCooking() || Bank.isOpen())
            return false;
        Interactable cookInteractable = getNearestRangeObject();
        if (cookInteractable == null && Inventory.isFull() && Inventory.contains(x -> x.getName().toLowerCase().contains("raw") && !x.getName().toLowerCase().contains("karambwanji")))
        {
            Movement.walkTo(config.cookWorldPoint());
            return true;
        }
        return false;
    }

    private boolean handleCook()
    {
        if (isFishing() || isCooking() || Bank.isOpen())
            return false;
        Interactable cookInteractable = getNearestRangeObject();
        if (cookInteractable != null && Inventory.isFull() && Inventory.contains(x -> x.getName().toLowerCase().contains("raw") && !x.getName().toLowerCase().contains("karambwanji")))
        {
            cookInteractable.interact("Cook");
            return true;
        }
        return false;
    }

    private boolean handleMoveToBank()
    {
        if (isFishing() || isCooking() || Bank.isOpen())
            return false;
        Interactable bankInteractable = getNearestBankNPC();
        if (bankInteractable == null && Inventory.isFull() && Inventory.contains(x -> {
            String inventoryObjectName = x.getName().toLowerCase();
            return getCookedFishList().stream().anyMatch(inventoryObjectName::contains);
        }))
        {
            Movement.walkTo(config.bankWorldPoint());
            return true;
        }
        return false;
    }

    private boolean handleOpenBank()
    {
        if (isFishing() || isCooking() || Bank.isOpen())
            return false;
        Interactable bankInteractable = getNearestBankNPC();
        if (bankInteractable != null && Inventory.isFull() && Inventory.contains(x -> {
            String inventoryObjectName = x.getName().toLowerCase();
            return getCookedFishList().stream().anyMatch(inventoryObjectName::contains);
        }))
        {
            bankInteractable.interact("Bank", "Deposit", "Use");
            return true;
        }
        return false;
    }

    private boolean handleDepositInventory()
    {
        if (!Bank.isOpen())
            return false;
        Item firstToDeposit = null;
        for (String fishString : getCookedFishList())
        {
            firstToDeposit = Bank.Inventory.getFirst(x -> x.getName().toLowerCase().contains(fishString) && !x.getName().toLowerCase().contains("vessel") && !x.getName().toLowerCase().contains("karambwanji"));
            if (firstToDeposit != null)
                break;
        }

        if (firstToDeposit != null)
        {
            firstToDeposit.interact("Deposit-All");
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
        if (closeWidget == null || Inventory.isFull() || !Bank.isOpen())
            return false;

        boolean haveFishInInventory = false;

        for (String fish : getCookedFishList())
        {
            if(Inventory.contains(x -> {
                if (x.getId() == 3159 || x.getId() == 3150)
                    return false;
                return x.getName().toLowerCase().contains(fish);
            }))
            {
                haveFishInInventory = true;
                break;
            }
        }


        if(closeWidget != null && !Inventory.isFull() && Inventory.contains(x -> x.getName().toLowerCase().contains(config.fishingItem().toLowerCase())) && !haveFishInInventory)
        {
            closeWidget.interact("Close");
            return true;
        }
        return false;
    }



    @Override
    public int loop()
    {
        if (!config.isEnabled())
            return 1000;

        if (getCookedFishList() == null || getCookedFishList().isEmpty())
        {
            log.info("No cooked fish list. Idling.");
            return 1000;
        }

        boolean actionTakenThisTick = handleClickWidget();
        if (actionTakenThisTick)
        {
            log.info("Tried to click on widget");
            return 5000;
        }

        if (droppingFish)
        {
            actionTakenThisTick = handleDropFish();
            if (actionTakenThisTick)
            {
                log.info("Continuing to drop fish");
                return 500;
            }
        }

        actionTakenThisTick = handleMoveToFishingSpot();
        if (actionTakenThisTick)
        {
            log.info("Tried to move to fish");
            return 500;
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
                return 500;
            }

            actionTakenThisTick = handleCook();
            if (actionTakenThisTick)
            {
                log.info("Tried to cook");
                return 2500;
            }
        }

        if (config.toBank())
        {
            actionTakenThisTick = handleMoveToBank();
            if (actionTakenThisTick)
            {
                log.info("Tried to move to bank");
                return 500;
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
        }
        else
        {
            actionTakenThisTick = handleDropFish();
            if (actionTakenThisTick)
            {
                log.info("Tried to drop fish");
                return 50;
            }
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
            configManager.setConfiguration("unethical-fisher", "bankWorldPoint", client.getLocalPlayer().getWorldLocation());
        }
        if (event.getKey().toLowerCase().contains("setfishtile"))
        {
            configManager.setConfiguration("unethical-fisher", "fishWorldPoint", client.getLocalPlayer().getWorldLocation());
        }
        if (event.getKey().toLowerCase().contains("setcooktile"))
        {
            configManager.setConfiguration("unethical-fisher", "cookWorldPoint", client.getLocalPlayer().getWorldLocation());
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

