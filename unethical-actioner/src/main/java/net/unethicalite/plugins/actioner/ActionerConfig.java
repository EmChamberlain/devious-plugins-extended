package net.unethicalite.plugins.actioner;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("unethical-actioner")
public interface ActionerConfig extends Config
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
            keyName = "item",
            name = "item",
            description = "item",
            position = 1
    )
    default String item()
    {
        return "item";
    }

    @ConfigItem(
            keyName = "action",
            name = "action",
            description = "action",
            position = 2
    )
    default String action()
    {
        return "action";
    }

}
