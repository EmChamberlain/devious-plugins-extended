package net.unethicalite.plugins.autonightmarezone;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("autonightmarezone")
public interface AutoNightmareZoneConfig extends Config
{
    @ConfigItem(
            keyName = "isEnabled",
            name = "Enabled",
            description = "Whether the plugin is enabled or not",
            position = 1
    )
    default boolean isEnabled()
    {
        return false;
    }
    @ConfigItem(
            keyName = "foodToUse",
            name = "foodToUse",
            description = "The id of the food to use",
            position = 2
    )
    default int foodToUse()
    {
        return 373;
    }

}

