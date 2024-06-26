package net.unethicalite.plugins.actioner;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("unethical-actioner")
public interface ActionerConfig extends Config
{
    @ConfigItem(
            keyName = "isEnabled",
            name = "isEnabled",
            description = "isEnabled",
            position = 0
    )
    default boolean isEnabled()
    {
        return false;
    }

    @ConfigItem(
            keyName = "interactable",
            name = "interactable",
            description = "interactable",
            position = 1
    )
    default String interactable()
    {
        return "interactable,list,comma,seperated";
    }

    @ConfigItem(
            keyName = "isId",
            name = "isId",
            description = "is isId vs being a partial string match",
            position = 2
    )
    default boolean isId()
    {
        return false;
    }

    @ConfigItem(
            keyName = "isItem",
            name = "isItem",
            description = "is Item vs being an npc/game object",
            position = 3
    )
    default boolean isItem()
    {
        return false;
    }

    @ConfigItem(
            keyName = "action",
            name = "action",
            description = "action",
            position = 4
    )
    default String action()
    {
        return "action";
    }

    @ConfigItem(
            keyName = "use",
            name = "use",
            description = "use",
            position = 5
    )
    default boolean use()
    {
        return true;
    }
    @ConfigItem(
            keyName = "dropItemsIfFull",
            name = "dropItemsIfFull",
            description = "dropItemsIfFull",
            position = 6
    )
    default boolean dropItemsIfFull()
    {
        return true;
    }

    @ConfigItem(
            keyName = "dropItemsPriority",
            name = "dropItemsPriority",
            description = "dropItemsPriority",
            position = 7
    )
    default boolean dropItemsPriority()
    {
        return false;
    }

    @ConfigItem(
            keyName = "dropItemsList",
            name = "dropItemsList",
            description = "dropItemsList",
            enabledBy = "dropItems",
            enabledByValue = "true",
            position = 8
    )
    default String dropItemsList()
    {
        return "list,comma,seperated";
    }

    @ConfigItem(
            keyName = "dropsPerTick",
            name = "dropsPerTick",
            description = "dropsPerTick",
            position = 9
    )
    default int dropsPerTick()
    {
        return 4;
    }

    @ConfigItem(
            keyName = "stopIfAnimating",
            name = "stopIfAnimating",
            description = "stopIfAnimating",
            position = 10
    )
    default boolean stopIfAnimating()
    {
        return false;
    }

    @ConfigItem(
            keyName = "toBank",
            name = "toBank",
            description = "toBank",
            position = 11
    )
    default boolean toBank()
    {
        return false;
    }

    @ConfigItem(
            keyName = "validItemsList",
            name = "validItemsList",
            description = "validItemsList",
            position = 12
    )
    default String validItemsList()
    {
        return "list,comma,seperated";
    }

    @ConfigItem(
            keyName = "depositItemsList",
            name = "depositItemsList",
            description = "depositItemsList",
            position = 13
    )
    default String depositItemsList()
    {
        return "list,comma,seperated";
    }

    @ConfigItem(
            keyName = "withdrawItemsList",
            name = "withdrawItemsList",
            description = "withdrawItemsList",
            position = 14
    )
    default String withdrawItemsList()
    {
        return "list,comma,seperated";
    }


    @ConfigItem(
            keyName = "maxRange",
            name = "maxRange",
            description = "maxRange",
            position = 15
    )
    default int maxRange()
    {
        return 1;
    }

    @ConfigItem(
            keyName = "setNewBank",
            name = "setNewBank",
            description = "setNewBank",
            position = 16
    )
    default boolean setNewBank()
    {
        return false;
    }

    @ConfigItem(
            keyName = "bankLocationString",
            name = "bankLocationString",
            description = "bankLocationString",
            position = 17
    )
    default String bankLocationString()
    {
        return "0,0,0";
    }

    @ConfigItem(
            keyName = "startLocationString",
            name = "startLocationString",
            description = "startLocationString",
            position = 18
    )
    default String startLocationString()
    {
        return "0,0,0";
    }

    @ConfigItem(
            keyName = "widgetsToSelect",
            name = "widgetsToSelect",
            description = "widgetsToSelect",
            position = 19
    )
    default String widgetsToSelect()
    {
        return "list,comma,seperated";
    }

    @ConfigItem(
            keyName = "widgetActionsToDo",
            name = "widgetActionsToDo",
            description = "widgetActionsToDo",
            position = 20
    )
    default String widgetActionsToDo()
    {
        return "option|target|id|type|p0|p1,option|target|id|type|p0|p1,option|target|id|type|p0|p1";
    }

    @ConfigItem(
            keyName = "pickUpItems",
            name = "pickUpItems",
            description = "pickUpItems",
            position = 21
    )
    default boolean pickUpItems()
    {
        return false;
    }

    @ConfigItem(
            keyName = "pickUpList",
            name = "pickUpList",
            description = "pickUpList",
            position = 22
    )
    default String pickUpList()
    {
        return "list,comma,seperated";
    }

    @ConfigItem(
            keyName = "delayList",
            name = "delayList",
            description = "delayList",
            position = 23
    )
    default String delayList()
    {
        return "0";
    }
}
