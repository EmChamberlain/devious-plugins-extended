package net.unethicalite.pickpocket;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.ObjectID;
import net.runelite.api.Player;
import net.runelite.api.TileObject;
import net.runelite.api.Varbits;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.util.Text;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.WildcardMatcher;
import net.unethicalite.api.commons.Rand;
import net.unethicalite.api.commons.Time;
import net.unethicalite.api.entities.NPCs;
import net.unethicalite.api.entities.Players;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.game.Combat;
import net.unethicalite.api.game.Vars;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Equipment;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.magic.Magic;
import net.unethicalite.api.magic.Spell;
import net.unethicalite.api.magic.SpellBook;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.movement.Reachable;
import net.unethicalite.api.movement.pathfinder.model.BankLocation;
import net.unethicalite.api.plugins.LoopedPlugin;
import net.unethicalite.api.widgets.Dialog;
import org.pf4j.Extension;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@PluginDescriptor(name = "Unethical Pickpocket", enabledByDefault = false)
@Extension
@Slf4j
public class PickpocketPlugin extends LoopedPlugin
{
	@Inject
	private PickpocketConfig config;

	private WorldPoint lastNpcPosition = null;

	private int maxPouches = 5;

	@Override
	protected int loop()
	{
		Item junk = Inventory.getFirst(item -> shouldDrop(Text.fromCSV(config.junk()), item.getName()));
		if (junk != null)
		{
			junk.interact("Drop");
			log.debug("Dropping junk");
			return -1;
		}

		Item pouch = Inventory.getFirst("Coin pouch");
		if (pouch != null && pouch.getQuantity() >= maxPouches && !Inventory.isFull())
		{
			pouch.interact("Open-all");
			maxPouches = Rand.nextInt(1, 29);
			log.debug("Opening pouches");
			return -1;
		}

		if (config.eat())
		{
			if (Combat.getMissingHealth() >= config.eatHp())
			{
				Item food = Inventory.getFirst(config.foodName());
				if (food != null)
				{
					food.interact(1);
					log.debug("Eating food");
					return -2;
				}
			}
		}

		if (config.dodgyNecklaces())
		{
			if (!Equipment.contains(x -> x.getName().toLowerCase().contains("dodgy necklace")))
			{
				Item dodgyNecklace = Inventory.getFirst(x -> x.getName().toLowerCase().contains("dodgy necklace"));
				if (dodgyNecklace != null)
				{
					dodgyNecklace.interact("Wear");
					log.debug("Equipping dodgy necklace");
					return -2;
				}
			}
		}

		if (config.shadowVeil())
		{
			if (Vars.getBit(Varbits.SHADOW_VEIL) != 1 && Vars.getBit(Varbits.SHADOW_VEIL_COOLDOWN) == 0)
			{
				Magic.cast(SpellBook.Necromancy.SHADOW_VEIL);
				log.debug("casting shadow veil");
				return -3;
			}
		}
		
		if (Bank.isOpen())
		{
			List<Item> unneeded = Inventory.getAll(item ->
					(!config.eat() || !Objects.equals(item.getName(), config.foodName()))
							&& (!item.getName().toLowerCase().contains("dodgy necklace"))
							&& (!item.getName().toLowerCase().contains("rune pouch"))
							&& item.getId() != ItemID.COINS_995
							&& !Objects.equals(item.getName(), "Coin pouch")
							&& !item.getName().toLowerCase().contains("vyre noble")
							&& !item.getName().toLowerCase().contains("rogue")
			);
			if (!unneeded.isEmpty())
			{
				for (Item item : unneeded)
				{
					Bank.depositAll(item.getId());
					Time.sleep(100);
				}

				return -1;
			}

			if (config.eat())
			{
				if (Inventory.getCount(config.foodName()) > config.foodAmount())
				{
					Bank.depositAll(config.foodName());
					return -2;
				}

				if (!Inventory.contains(config.foodName()))
				{
					Bank.withdraw(config.foodName(), config.foodAmount(), Bank.WithdrawMode.ITEM);
					log.debug("Withdrawing food");
					return -2;
				}
			}

			if (config.dodgyNecklaces())
			{
				if (Inventory.getCount(x -> x.getName().toLowerCase().contains("dodgy necklace")) < 3)
				{
					Bank.withdraw("Dodgy necklace", 5, Bank.WithdrawMode.ITEM);
					log.debug("Withdrawing dodgy necklace");
					return -2;
				}
			}
		}

		if (config.bank() && (Inventory.isFull() || !Inventory.contains(config.foodName()) || !Equipment.contains(x -> x.getName().toLowerCase().contains("dodgy necklace"))))
		{
			if (config.bankLocation() == BankLocation.DARKMEYER_BANK)
			{
				if (!Equipment.contains(ItemID.VYRE_NOBLE_TOP))
				{
					Item vyreTop = Inventory.getFirst(ItemID.VYRE_NOBLE_TOP);
					if (vyreTop != null)
					{
						log.info("Equipping vyre top");
						vyreTop.interact("Wear");
						return -2;
					}
				}

				if (!Equipment.contains(ItemID.VYRE_NOBLE_LEGS))
				{
					Item vyreLegs = Inventory.getFirst(ItemID.VYRE_NOBLE_LEGS);
					if (vyreLegs != null)
					{
						log.info("Equipping vyre legs");
						vyreLegs.interact("Wear");
						return -2;
					}
				}

				if (!Equipment.contains(ItemID.VYRE_NOBLE_SHOES))
				{
					Item vyreShoes = Inventory.getFirst(ItemID.VYRE_NOBLE_SHOES);
					if (vyreShoes != null)
					{
						log.info("Equipping vyre shoes");
						vyreShoes.interact("Wear");
						return -2;
					}
				}
			}


			if (Movement.isWalking())
			{
				return -4;
			}

			TileObject bank = TileObjects.within(config.bankLocation().getArea().offset(2), obj -> obj.hasAction("Collect"))
					.stream()
					.min(Comparator.comparingInt(obj -> obj.distanceTo(Players.getLocal())))
					.orElse(null);

			if (bank != null)
			{
				bank.interact("Bank", "Use");
				return -4;
			}

			NPC banker = NPCs.getNearest("Banker");
			if (banker != null)
			{
				banker.interact("Bank");
				return -4;
			}

			Movement.walkTo(config.bankLocation());
			return -4;
		}

		NPC target = NPCs.getNearest(config.npcName());
		if (target != null)
		{
			lastNpcPosition = target.getWorldLocation();
			if (!Reachable.isInteractable(target))
			{
				if (Movement.isWalking())
				{
					return -4;
				}

				Movement.walkTo(target);
				return -4;
			}

			Player local = Players.getLocal();
			if (local.getGraphic() == 245 && !Dialog.isOpen())
			{
				return -1;
			}

			if (local.isMoving() && target.distanceTo(local) > 3)
			{
				return -1;
			}

			if (config.rogueSet())
			{
				if (!Equipment.contains(ItemID.ROGUE_MASK))
				{
					Item rogueMask = Inventory.getFirst(ItemID.ROGUE_MASK);
					if (rogueMask != null)
					{
						log.info("Equipping rogue mask");
						rogueMask.interact("Wear");
						return -2;
					}
				}

				if (!Equipment.contains(ItemID.ROGUE_TOP))
				{
					Item rogueTop = Inventory.getFirst(ItemID.ROGUE_TOP);
					if (rogueTop != null)
					{
						log.info("Equipping rogue top");
						rogueTop.interact("Wear");
						return -2;
					}
				}

				if (!Equipment.contains(ItemID.ROGUE_TROUSERS))
				{
					Item rogueTrousers = Inventory.getFirst(ItemID.ROGUE_TROUSERS);
					if (rogueTrousers != null)
					{
						log.info("Equipping rogue trousers");
						rogueTrousers.interact("Wear");
						return -2;
					}
				}

				if (!Equipment.contains(ItemID.ROGUE_BOOTS))
				{
					Item rogueBoots = Inventory.getFirst(ItemID.ROGUE_BOOTS);
					if (rogueBoots != null)
					{
						log.info("Equipping rogue boots");
						rogueBoots.interact("Wear");
						return -2;
					}
				}

				if (!Equipment.contains(ItemID.ROGUE_GLOVES))
				{
					Item rogueGloves = Inventory.getFirst(ItemID.ROGUE_GLOVES);
					if (rogueGloves != null)
					{
						log.info("Equipping rogue gloves");
						rogueGloves.interact("Wear");
						return -2;
					}
				}
			}

			target.interact("Pickpocket");
			return Rand.nextInt(222, 999);
		}

		if (Movement.isWalking())
		{
			return -4;
		}

		if (lastNpcPosition != null)
		{
			Movement.walkTo(lastNpcPosition);
			return -4;
		}

		log.info("Idle");
		return -1;
	}

	@Provides
	PickpocketConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PickpocketConfig.class);
	}

	private boolean shouldDrop(List<String> itemNames, String itemName)
	{
		return itemNames.stream().anyMatch(name -> WildcardMatcher.matches(name, itemName));
	}
}
