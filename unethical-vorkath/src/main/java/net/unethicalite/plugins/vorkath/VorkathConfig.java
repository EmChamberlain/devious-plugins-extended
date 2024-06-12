package net.unethicalite.plugins.vorkath;

import net.runelite.api.ItemID;
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
            keyName = "foodToUse",
            name = "foodToUse",
            description = "foodToUse",
            position = 1
    )
    default int foodToUse()
    {
        return ItemID.COOKED_KARAMBWAN;
    }

    @ConfigItem(
            keyName = "specWeapon",
            name = "specWeapon",
            description = "specWeapon",
            position = 2
    )
    default int specWeapon()
    {
        return ItemID.BANDOS_GODSWORD;
    }

    @ConfigItem(
            keyName = "acidFreePathMinLength",
            name = "Minimum Length Acid Free Path",
            description = "The minimum length of an acid free path",
            position = 3,
            hidden = true,
            unhide = "indicateAcidFreePath"
    )
    default int acidFreePathLength()
    {
        return 5;
    }
}
