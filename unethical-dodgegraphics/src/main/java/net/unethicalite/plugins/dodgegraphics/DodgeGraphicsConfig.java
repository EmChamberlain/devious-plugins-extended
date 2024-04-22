package net.unethicalite.plugins.dodgegraphics;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("unethical-dodgegraphics")
public interface DodgeGraphicsConfig extends Config
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
            keyName = "idBlacklist",
            name = "idBlacklist",
            description = "idBlacklist",
            position = 1
    )
    default String idBlacklist()
    {
        return "-1,-2,-3,-4";
    }

    @ConfigItem(
            keyName = "radius",
            name = "radius",
            description = "radius with 0 being only the graphical location is dangerous, 1 being surrounding tiles, etc",
            position = 2
    )
    default int radius()
    {
        return 1;
    }

    @ConfigItem(
            keyName = "repeatedlyAttack",
            name = "repeatedlyAttack",
            description = "repeatedlyAttack",
            position = 3
    )
    default boolean repeatedlyAttack()
    {
        return true;
    }

    @ConfigItem(
            keyName = "repeatedlyAttackList",
            name = "repeatedlyAttackList",
            description = "repeatedlyAttackList",
            position = 4
    )
    default String repeatedlyAttackList()
    {
        return "-1,-2,-3";
    }
}
