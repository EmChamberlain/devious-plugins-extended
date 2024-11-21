package net.unethicalite.pickpocket;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.unethicalite.api.movement.pathfinder.model.BankLocation;

@ConfigGroup("unethicalpickpocket")
public interface PickpocketConfig extends Config
{
	@ConfigItem(
			keyName = "bank",
			name = "Bank for food",
			description = "",
			position = 0
	)
	default boolean bank()
	{
		return true;
	}

	@ConfigItem(
			keyName = "bankLocation",
			name = "Bank Location",
			description = "",
			position = 1,
			hidden = true,
			unhide = "bank"
	)
	default BankLocation bankLocation()
	{
		return BankLocation.ARDOUGNE_SOUTH_BANK;
	}

	@ConfigItem(
			keyName = "npcName",
			name = "Npc name",
			description = "",
			position = 2
	)
	default String npcName()
	{
		return "Knight of Ardougne";
	}

	@ConfigItem(
			keyName = "eat",
			name = "Eat",
			description = "",
			position = 3
	)
	default boolean eat()
	{
		return true;
	}

	@ConfigItem(
			keyName = "foodName",
			name = "Food name",
			description = "",
			position = 4,
			hidden = true,
			unhide = "eat"
	)
	default String foodName()
	{
		return "Jug of wine";
	}

	@ConfigItem(
			keyName = "foodAmount",
			name = "Food withdraw amount",
			description = "",
			position = 5,
			hidden = true,
			unhide = "eat"
	)
	default int foodAmount()
	{
		return 10;
	}

	@ConfigItem(
			keyName = "eatHp",
			name = "Eat at X missing HP",
			description = "",
			position = 6,
			hidden = true,
			unhide = "eat"
	)
	default int eatHp()
	{
		return 11;
	}

	@ConfigItem(
			keyName = "junk",
			name = "Items to drop",
			description = "",
			position = 7
	)
	default String junk()
	{
		return "Jug, Potato seed";
	}

	@ConfigItem(
			keyName = "dodgyNecklaces",
			name = "dodgyNecklaces",
			description = "",
			position = 8
	)
	default boolean dodgyNecklaces()
	{
		return true;
	}

	@ConfigItem(
			keyName = "shadowVeil",
			name = "shadowVeil",
			description = "",
			position = 9
	)
	default boolean shadowVeil()
	{
		return true;
	}
}
