package net.unethicalite.plugins.vorkath;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("unethical-vorkath")
public interface VorkathConfig extends Config
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
            keyName = "acidFreePathMinLength",
            name = "Minimum Length Acid Free Path",
            description = "The minimum length of an acid free path",
            position = 1,
            hidden = true,
            unhide = "indicateAcidFreePath"
    )
    default int acidFreePathLength()
    {
        return 5;
    }
}
