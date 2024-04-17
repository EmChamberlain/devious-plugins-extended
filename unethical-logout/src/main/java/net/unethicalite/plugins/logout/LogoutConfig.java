package net.unethicalite.plugins.logout;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.Button;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("unethical-logout")
public interface LogoutConfig extends Config
{

    @ConfigItem(
            keyName = "isEnabled",
            name = "isEnabled",
            description = "Whether the plugin is enabled or not",
            position = 0
    )
    default boolean isEnabled()
    {
        return false;
    }

    @ConfigItem(
            keyName = "logIfSkulledOnly",
            name = "logIfSkulledOnly",
            description = "Whether to only log out if the player is skulled",
            position = 1
    )
    default boolean logIfSkulledOnly()
    {
        return false;
    }


    @ConfigItem(
            keyName = "teleportOut",
            name = "teleportOut",
            description = "Whether to teleport or log out",
            position = 2
    )
    default boolean teleportOut()
    {
        return false;
    }

    @ConfigItem(
            keyName = "test",
            name = "test",
            description = "Whether to test next tick",
            position = 3
    )
    default boolean test()
    {
        return false;
    }

}
