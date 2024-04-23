package net.unethicalite.plugins.buylooper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("unethical-buylooper")
public interface BuyLooperConfig extends Config
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
            keyName = "operation",
            name = "operation",
            description = "operation",
            position = 1
    )
    default int operation()
    {
        return LooperOperation.HALF_AND_HALF;
    }

    @ConfigItem(
            keyName = "liquidate",
            name = "liquidate",
            description = "liquidate",
            position = 2
    )
    default int liquidate()
    {
        return LooperLiquidate.ALCH;
    }

    @ConfigItem(
            keyName = "primary",
            name = "primary",
            description = "primary",
            position = 3
    )
    default int primary()
    {
        return -1;
    }

    @ConfigItem(
            keyName = "secondary",
            name = "secondary",
            description = "secondary",
            position = 4
    )
    default int secondary()
    {
        return -1;
    }

    @ConfigItem(
            keyName = "tertiary",
            name = "tertiary",
            description = "tertiary",
            position = 5
    )
    default int tertiary()
    {
        return -1;
    }

    @ConfigItem(
            keyName = "product",
            name = "product",
            description = "product",
            position = 6
    )
    default int product()
    {
        return -1;
    }

    @ConfigItem(
            keyName = "widgetList",
            name = "widgetList",
            description = "widgetList",
            position = 7
    )
    default String widgetList()
    {
        return "list,of,partial,matches,comma,separated";
    }



}
