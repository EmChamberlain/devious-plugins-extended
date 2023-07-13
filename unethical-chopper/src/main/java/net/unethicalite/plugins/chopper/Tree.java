package net.unethicalite.plugins.chopper;

import lombok.Getter;

@Getter
public enum Tree
{
	REGULAR(1, "Tree", "Evergreen tree"),
	OAK(15, "Oak tree"),
	WILLOW(30, "Willow tree"),
	TEAK(35, "Teak tree"),
	MAPLE(45, "Maple tree"),
	MAHOGANY(50, "Mahogany tree"),
	YEW(60, "Yew tree"),
	MAGIC(75, "Magic tree");

	private final int level;
	private final String[] names;

	Tree(int level, String... names)
	{
		this.level = level;
		this.names = names;
	}
}
