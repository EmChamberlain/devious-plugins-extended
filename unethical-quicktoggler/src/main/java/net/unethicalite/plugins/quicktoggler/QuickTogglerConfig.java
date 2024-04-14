package net.unethicalite.plugins.quicktoggler;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("unethical-quicktoggler")
public interface QuickTogglerConfig extends Config
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
            keyName = "meleeWhitelist",
            name = "meleeWhitelist",
            description = "meleeWhitelist",
            position = 1
    )
    default String meleeWhitelist()
    {
        return "";
    }

    @ConfigItem(
            keyName = "rangedWhitelist",
            name = "rangedWhitelist",
            description = "rangedWhitelist",
            position = 2
    )
    default String rangedWhitelist()
    {
        return "";
    }

    @ConfigItem(
            keyName = "magicWhitelist",
            name = "magicWhitelist",
            description = "magicWhitelist",
            position = 3
    )
    default String magicWhitelist()
    {
        return "";
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

