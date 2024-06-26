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
            keyName = "mobWhitelist",
            name = "mobWhitelist",
            description = "mobWhitelist",
            position = 1
    )
    default String mobWhitelist()
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

    @ConfigItem(
            keyName = "quickPrayMelee",
            name = "quickPrayMelee",
            description = "quickPrayMelee",
            position = 3
    )
    default boolean quickPrayMelee()
    {
        return true;
    }

    @ConfigItem(
            keyName = "attackOrder",
            name = "attackOrder",
            description = "attackOrder",
            position = 4
    )
    default String attackOrder()
    {
        return "";
    }

    @ConfigItem(
            keyName = "attackRepeatedly",
            name = "attackRepeatedly",
            description = "attackRepeatedly",
            position = 5
    )
    default boolean attackRepeatedly()
    {
        return true;
    }



}
