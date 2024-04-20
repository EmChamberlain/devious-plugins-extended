package net.unethicalite.plugins.blastfurnace;

import net.runelite.api.ItemID;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("unethical-blastfurnace")
public interface UnethicalBlastFurnaceConfig extends Config
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
            keyName = "barsToMake",
            name = "barsToMake",
            description = "barsToMake",
            position = 1
    )
    default int barsToMake()
    {
        return ItemID.STEEL_BAR;
    }

}


