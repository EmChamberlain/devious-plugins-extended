package net.unethicalite.plugins.overheadswitcher;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("unethical-overheadswitcher")
public interface OverheadSwitcherConfig extends Config
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
            keyName = "mobToTest",
            name = "mobToTest",
            description = "mobToTest",
            position = 1
    )
    default String mobToTest()
    {
        return "";
    }

    @ConfigItem(
            keyName = "prayRanged",
            name = "prayRanged",
            description = "prayRanged",
            position = 2
    )
    default boolean prayRanged()
    {
        return true;
    }

}
