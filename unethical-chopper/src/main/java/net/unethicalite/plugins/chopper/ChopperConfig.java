package net.unethicalite.plugins.chopper;

import net.runelite.client.config.Button;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("unethical-chopper")
public interface ChopperConfig extends Config
{
	@ConfigItem(
			keyName = "tree",
			name = "Tree type",
			description = "The type of tree to chop",
			position = 0
	)
	default Tree tree()
	{
		return Tree.REGULAR;
	}

	@ConfigItem(
			keyName = "makeFire",
			name = "Make fire",
			description = "Make fire while chopping",
			position = 1
	)
	default boolean makeFire()
	{
		return false;
	}

	@ConfigItem(
			keyName = "bank",
			name = "Bank",
			description = "Bank while chopping",
			position = 2
	)
	default boolean bank()
	{
		return false;
	}

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
