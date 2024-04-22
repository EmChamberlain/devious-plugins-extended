package net.unethicalite.plugins.logout;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.Player;
import net.runelite.api.events.ClientTick;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.unethicalite.api.commons.Time;
import net.unethicalite.api.entities.Players;
import net.unethicalite.api.game.Game;
import net.unethicalite.api.items.Equipment;
import net.unethicalite.api.items.Inventory;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.util.List;

@Slf4j
@Extension
@PluginDescriptor(name = "Unethical Logout", description = "Logs you out in wildy if a dangerous player is near", enabledByDefault = false)
public class LogoutPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private LogoutConfig config;

	@Inject
	private ConfigManager configManager;

	private final List<String> teleActions30 = List.of("Edgeville", "Monastery", "Grand Exchange");
	private final List<String> teleActions20 = List.of("Ferox Enclave");

	@Provides
	LogoutConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LogoutConfig.class);
	}

	@Subscribe
	private void onClientTick(ClientTick e)
	{
		if (!config.isEnabled())
			return;

		int wildyLevel = Game.getWildyLevel();
		if (wildyLevel < 1 && !config.test())
		{
			return;
		}
		Player local = Players.getLocal();
		int combatLevel = local.getCombatLevel();
		Player pker = Players.getNearest(player -> player != local && isDangerousPlayer(wildyLevel, combatLevel, player, config.logIfSkulledOnly()));
		if (pker != null || config.test())
		{
			Item teleItem = null;
			String teleAction = null;
			if (wildyLevel <= 30)
			{
				for (String teleAction30 : teleActions30)
				{
					if (teleAction30 == "Grand Exchange")
					{
						//Have to handle this seperately for some weird reason
						teleItem = Equipment.getFirst(x -> x.getId() == 11982);
						if (teleItem != null)
						{
							teleAction = teleAction30;
							break;
						}
					}
					else
					{
						teleItem = Equipment.getFirst(x -> x.hasAction(teleAction30));
						if (teleItem != null)
						{
							teleAction = teleAction30;
							break;
						}
					}
				}
			}

			if (wildyLevel <= 20)
			{
				for (String teleAction20 : teleActions20)
				{
					teleItem = Equipment.getFirst(x -> x.hasAction(teleAction20));
					teleAction = teleAction20;
					if (teleItem != null)
					{
						teleAction = teleAction20;
						break;
					}
				}
			}

			if (teleItem == null && wildyLevel <= 20)
			{
				log.info("No equipped teles");
				String tabletTeleAction = "Break";
				teleItem = Inventory.getFirst(x -> x.hasAction(tabletTeleAction));
				teleAction = tabletTeleAction;
			}

			if (config.teleportOut() && teleItem != null && teleAction != null && !teleAction.isEmpty())
			{
				log.info("Trying to tele");
				try {
					teleItem.interact(teleAction);
				} catch (Exception ignored)
				{
					log.info("Could not teleport so logging");
					client.setMouseIdleTicks(Integer.MAX_VALUE);
					client.setKeyboardIdleTicks(Integer.MAX_VALUE);
				}
				log.info("Teled out");
			}
			else
			{
				log.info("Logging out");
				client.setMouseIdleTicks(Integer.MAX_VALUE);
				client.setKeyboardIdleTicks(Integer.MAX_VALUE);
			}

			if(config.test())
			{
				log.info("Untesting");
				configManager.setConfiguration("unethical-logout", "test", false);
				log.info("Untested");
			}
		}
	}

	private boolean isDangerousPlayer(int wildyLevel, int localCombatLevel, Player player, boolean onlyDangerousIfSkulled)
	{
		boolean isSkulled = player.getSkullIcon() != null;
		if (onlyDangerousIfSkulled && !isSkulled)
			return false;
		int playerCombatLevel = player.getCombatLevel();
		int lowerLimit = localCombatLevel - wildyLevel;
		int upperLimit = localCombatLevel + wildyLevel;
		return playerCombatLevel >= lowerLimit && playerCombatLevel <= upperLimit;
	}
}
