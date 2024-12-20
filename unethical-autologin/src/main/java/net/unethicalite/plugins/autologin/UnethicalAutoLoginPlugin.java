package net.unethicalite.plugins.autologin;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.World;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.unethicalite.api.events.LobbyWorldSelectToggled;
import net.unethicalite.api.events.LoginIndexChanged;
import net.unethicalite.api.events.WorldHopped;
import net.unethicalite.api.game.Game;
import net.unethicalite.api.game.Worlds;
import net.unethicalite.api.script.blocking_events.WelcomeScreenEvent;
import net.unethicalite.api.widgets.Widgets;
import org.jboss.aerogear.security.otp.Totp;
import org.pf4j.Extension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@PluginDescriptor(name = "Unethical Auto Login", enabledByDefault = false)
@Extension
@Slf4j
public class UnethicalAutoLoginPlugin extends Plugin
{
	@Inject
	private UnethicalAutoLoginConfig config;

	@Inject
	private Client client;

	@Inject
	private ConfigManager configManager;

	@Provides
	public UnethicalAutoLoginConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(UnethicalAutoLoginConfig.class);
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged e)
	{
		if (!config.doLogin())
			return;

		if (List.of(GameState.LOGGING_IN, GameState.LOADING, GameState.LOGGED_IN, GameState.CONNECTION_LOST, GameState.HOPPING).contains(e.getGameState()))
		{
			return;
		}
		if (config.useWorld())
		{
			World selectedWorld = Worlds.getFirst(config.world());
			if (selectedWorld != null)
			{
				client.changeWorld(selectedWorld);
			}
		}
		if (e.getGameState() == GameState.LOGIN_SCREEN && client.getLoginIndex() == 0)
		{
			prepareLogin();
		}
		else
		{
			client.setGameState(GameState.LOGIN_SCREEN);
		}
		if (client.getCurrentLoginField() == 1)
		{
			log.info("Logging in in 30 seconds");
            try {
                Thread.sleep(1000 * 30);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
			login();
			log.info("Logging in!");
		}

	}

	@Subscribe
	private void onGameTick(GameTick event)
	{
		if (config.neverIdle())
		{
			if (client.getMouseIdleTicks() > Math.random() * 600)
				client.setMouseIdleTicks(0);

			if (client.getKeyboardIdleTicks() > Math.random() * 600)
				client.setKeyboardIdleTicks(0);
		}
	}

	@Subscribe
	private void onLoginIndexChanged(LoginIndexChanged e)
	{
		if (!config.doLogin())
			return;
		switch (e.getIndex())
		{
			case 2:
				login();
				break;
			case 4:
				enterAuth();
				break;
			case 24:
				prepareLogin();
				client.getCallbacks().post(new LoginIndexChanged(2));
				break;
		}
	}


	@Subscribe
	private void onWidgetHiddenChanged(WidgetLoaded e)
	{
		if (!config.welcomeScreen())
		{
			return;
		}

		int group = e.getGroupId();
		if (group == 378 || group == 413)
		{
			Widget playButton = WelcomeScreenEvent.getPlayButton();
			if (Widgets.isVisible(playButton))
			{
				client.invokeWidgetAction(1, playButton.getId(), -1, -1, "");
			}
		}
	}

	/*@Subscribe
	private void onLobbyWorldSelectToggled(LobbyWorldSelectToggled e)
	{
		if (e.isOpened())
		{
			client.setWorldSelectOpen(false);

			if (config.useWorld())
			{
				World selectedWorld = Worlds.getFirst(config.world());
				if (selectedWorld != null)
				{
					client.changeWorld(selectedWorld);
				}
			}
		}

		client.promptCredentials(false);
	}*/

	@Subscribe
	private void onPluginChanged(PluginChanged e)
	{
		if (e.getPlugin() != this)
		{
			return;
		}
		if (!config.doLogin())
			return;

		if (e.isLoaded() && Game.getState() == GameState.LOGIN_SCREEN)
		{
			prepareLogin();
			client.getCallbacks().post(new LoginIndexChanged(2));
		}
	}

	private void prepareLogin()
	{
		if (config.useWorld() && client.getWorld() != config.world())
		{
			client.loadWorlds();
		}
		else
		{
			client.promptCredentials(false);
		}
	}

	private void login()
	{
		client.setUsername(config.username());
		client.setPassword(config.password());
		client.login(false);
		client.setGameState(GameState.LOGGING_IN);
		//Mouse.click(299, 322, true);
	}

	private void enterAuth()
	{
		client.setOtp(new Totp(config.auth()).now());
		client.login(true);
		//Keyboard.sendEnter();
	}
}
