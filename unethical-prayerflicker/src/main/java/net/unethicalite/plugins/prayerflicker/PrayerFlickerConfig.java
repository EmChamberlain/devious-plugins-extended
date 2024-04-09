package net.unethicalite.plugins.prayerflicker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("unethical-prayerflicker")
public interface PrayerFlickerConfig extends Config
{

    @ConfigItem(
            keyName = "enabled",
            name = "Enabled",
            description = "Whether to prayer flick or not",
            position = 0
    )
    default boolean enabled()
    {
        return false;
    }

}
