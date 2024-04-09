package net.unethicalite.plugins.flicker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("unethical-flicker")
public interface FlickerConfig extends Config
{
    @ConfigItem(
            keyName = "flickerEnabled",
            name = "Flicker Enabled",
            description = "Whether to prayer flick or not",
            position = 0
    )
    default boolean enabled()
    {
        return false;
    }

}
