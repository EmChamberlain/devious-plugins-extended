package net.unethicalite.plugins.autonightmarezone;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("unethical-autonightmarezone")
public interface AutoNightmareZoneConfig extends Config
{
    @ConfigItem(
            keyName = "isEnabled",
            name = "Enabled",
            description = "Whether the plugin is enabled or not",
            position = 0
    )
    default boolean isEnabled()
    {
        return false;
    }
}

