package net.unethicalite.plugins.pestcontrol;

import net.runelite.client.config.Button;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("unethical-pestcontrol")
public interface PestControlConfig extends Config
{
    @ConfigItem(
            keyName = "Start",
            name = "Start/Stop",
            description = "Start/Stop button",
            position = 2)
    default Button startStopButton()
    {
        return new Button();
    }

}

