package net.unethicalite.plugins.actioner;

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
            keyName = "dropItemsList",
            name = "dropItemsList",
            description = "dropItemsList",
            enabledBy = "dropItems",
            enabledByValue = "true",
            position = 7
    )
    default String dropItemsList()
    {
        return "list,comma,seperated";
    }

    @ConfigItem(
            keyName = "dropsPerTick",
            name = "dropsPerTick",
            description = "dropsPerTick",
            position = 8
    )
    default int dropsPerTick()
    {
        return 4;
    }

    @ConfigItem(
            keyName = "stopIfAnimating",
            name = "stopIfAnimating",
            description = "stopIfAnimating",
            position = 9
    )
    default boolean stopIfAnimating()
    {
        return false;
    }

}
