package net.unethicalite.plugins.explorer;

import net.runelite.client.config.Button;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("unethicalexplorer")
public interface ExplorerConfig extends Config
{
	@ConfigItem(
			keyName = "coords",
			name = "Custom coords",
			description = "Walk to the specified coordinates",
			position = 0
	)
	default String coords()
	{
		return "3220 3220 0";
	}

	@ConfigItem(
			keyName = "walk",
			name = "Walk to",
			description = "Walk to set coordinates",
			position = 1
	)
	default Button walk()
	{
		return new Button();
	}

	@ConfigItem(
		keyName = "keyBind",
		name = "Stop explorer hotkey",
		description = "Hotkey to stop the explorer",
		position = 2
	)
	default Keybind stopKeyBind()
	{
		return Keybind.SHIFT;
	}
}
