package net.unethicalite.plugins.fisher;

import net.runelite.client.config.Button;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ConfigGroup("unethical-fisher")
public interface FisherConfig extends Config
{

    @ConfigItem(
            keyName = "isEnabled",
            name = "isEnabled",
            description = "Whether the plugin is enabled or not",
            position = 0
    )
    default boolean isEnabled()
    {
        return false;
    }
    @ConfigItem(
            keyName = "identifierAction",
            name = "identifierAction",
            description = "Name of action to fish at",
            position = 1
    )
    default String identifierAction()
    {
        return null;
    }
    @ConfigItem(
            keyName = "fishingAction",
            name = "fishingAction",
            description = "Name of action to use at fishing spot",
            position = 2
    )
    default String fishingAction()
    {
        return null;
    }

    @ConfigItem(
            keyName = "fishingItem",
            name = "fishingItem",
            description = "Name of item to use at fishing spot",
            position = 3
    )
    default String fishingItem()
    {
        return null;
    }

    @ConfigItem(
            keyName = "setBankTile",
            name = "setBankTile",
            description = "Sets bank tile",
            position = 4)
    default Button setBankTile()
    {
        return new Button();
    }

    @ConfigItem(
            keyName = "setFishTile",
            name = "setFishTile",
            description = "Sets fish tile",
            position = 5)
    default Button setFishTile()
    {
        return new Button();
    }

    @ConfigItem(
            keyName = "setCookTile",
            name = "setCookTile",
            description = "Sets cook tile",
            position = 6)
    default Button setCookTile()
    {
        return new Button();
    }


    @ConfigItem(
            keyName = "cookedFish",
            name = "cookedFish",
            description = "Name of cooked fish seperated by comma",
            position = 7)
    default String cookedFish(){return "swordfish,tuna";}

    @ConfigItem(
            keyName = "toCook",
            name = "toCook",
            description = "Whether to cook the fish or not",
            position = 8)
    default boolean toCook(){return true;}

    @ConfigItem(
            keyName = "toBank",
            name = "toBank",
            description = "Whether to bank the fish or not",
            position = 9)
    default boolean toBank(){return true;}

}
