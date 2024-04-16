package net.unethicalite.plugins.zulrah.data.phases;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.runelite.api.Prayer;
import net.unethicalite.plugins.zulrah.UnethicalZulrahConfig;
import net.unethicalite.plugins.zulrah.data.GearSetup;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public enum ZulrahType
{
	RANGE(2042),
	MELEE(2043),
	MAGIC(2044),
	JAD_MAGIC_FIRST(2042),
	JAD_RANGE_FIRST(2042);

	private final int id;
	@Setter
	private GearSetup setup;

	@Setter
	private static Prayer rangedPrayer = Prayer.EAGLE_EYE;
	@Setter
	private static Prayer magePrayer = Prayer.MYSTIC_MIGHT;

	public static void setRangedMeleePhaseGear(GearSetup gearSetup)
	{
		RANGE.setSetup(gearSetup);
		MELEE.setSetup(gearSetup);
		JAD_MAGIC_FIRST.setSetup(gearSetup);
		JAD_RANGE_FIRST.setSetup(gearSetup);
	}

	public static void setMagePhaseGear(GearSetup gearSetup)
	{
		MAGIC.setSetup(gearSetup);
	}

	public int id()
	{
		return id;
	}

	public Prayer getOffensivePrayer()
	{
		if (this == ZulrahType.MAGIC)
		{
			return rangedPrayer;
		}

		return magePrayer;
	}

	public Prayer getDefensivePrayer()
	{
		switch (this)
		{
			case MAGIC:
			case JAD_MAGIC_FIRST:
				return Prayer.PROTECT_FROM_MAGIC;

			default:
				return Prayer.PROTECT_FROM_MISSILES;
		}
	}
}
