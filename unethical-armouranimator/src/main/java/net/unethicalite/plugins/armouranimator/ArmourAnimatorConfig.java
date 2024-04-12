package net.unethicalite.plugins.armouranimator;


import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("unethical-armouranimator")
public interface ArmourAnimatorConfig extends Config
{
    @ConfigItem(
            keyName = "isEnabled",
            name = "isEnabled",
            description = "Whether to run the plugin or not",
            position = 0
    )
    default boolean isEnabled()
    {
        return false;
    }

    @ConfigItem(
            keyName = "minHealth",
            name = "minHealth",
            description = "Below which to disable the plugin",
            position = 1
    )
    default int minHealth()
    {
        return 50;
    }

    @ConfigItem(
            keyName = "foodToUse",
            name = "foodToUse",
            description = "The food to use",
            position = 2
    )
    default String foodToUse()
    {
        return "Swordfish";
    }
}

