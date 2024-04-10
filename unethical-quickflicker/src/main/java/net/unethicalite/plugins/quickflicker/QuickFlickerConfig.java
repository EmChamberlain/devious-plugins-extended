package net.unethicalite.plugins.quickflicker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("unethical-quickflicker")
public interface QuickFlickerConfig extends Config
{
    @ConfigItem(
            keyName = "quickFlickerEnabled",
            name = "Quick Flicker Enabled",
            description = "Whether to prayer flick quick prayers or not",
            position = 0
    )
    default boolean enabled()
    {
        return false;
    }

}
