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
import net.unethicalite.api.entities.Players;
import net.unethicalite.api.game.Game;
import net.unethicalite.api.items.Equipment;
import net.unethicalite.api.items.Inventory;
import org.pf4j.Extension;

import javax.inject.Inject;

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

	@Provides
	LogoutConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LogoutConfig.class);
	}

	@Subscribe
	private void onClientTick(ClientTick e)
	{
		int wildyLevel = Game.getWildyLevel();
		if (wildyLevel < 1)
		{
			return;
		}
		wildyLevel += 1; // Just for safety
		Player local = Players.getLocal();
		int combatLevel = local.getCombatLevel();
		final int finalWildyLevel = wildyLevel;
		Player pker = Players.getNearest(player -> player != local && isDangerousPlayer(finalWildyLevel, combatLevel, player, config.logIfSkulledOnly()));
		if (pker != null || config.test())
		{
			if(config.test())
				configManager.setConfiguration("unethical-logout", "test", false);

			String gloryTeleAction = "Edgeville";
			Item teleItem = Equipment.getFirst(x -> x.hasAction(gloryTeleAction));

			String teleAction = gloryTeleAction;

			if (teleItem == null)
				teleItem = Inventory.getFirst(x -> x.getName().toLowerCase().contains("amulet of glory("));

			if (teleItem == null)
			{
				String tabletTeleAction = "Break";
				teleItem = Inventory.getFirst(x -> x.hasAction(tabletTeleAction));
				teleAction = tabletTeleAction;
			}

			if (config.teleportOut() && teleItem != null)
			{
				log.info("Trying to tele");
				try {
					teleItem.interact(teleAction);
				} catch (Exception ignored)
				{
					log.info("Could not teleport so logging");
					client.setMouseIdleTicks(Integer.MAX_VALUE);
					client.setKeyboardIdleTicks(Integer.MAX_VALUE);
					return;
				}
				log.info("Teled out");
			}
			else
			{
				log.info("Logging out");
				client.setMouseIdleTicks(Integer.MAX_VALUE);
				client.setKeyboardIdleTicks(Integer.MAX_VALUE);
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
