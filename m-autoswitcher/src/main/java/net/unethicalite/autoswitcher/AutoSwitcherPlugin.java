/*
 * Copyright (c) 2022, EmChamberlain <https://github.com/EmChamberlain/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.unethicalite.autoswitcher;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.unethicalite.api.items.Inventory;
import org.pf4j.Extension;
import javax.inject.Inject;
import java.awt.event.KeyEvent;

@Extension
@PluginDescriptor(
        name = "AutoSwitcher",
        description = "Auto switch gear by pressing hot key",
        enabledByDefault = false,
        tags =
                {
                        "inventory",
                        "gear",
                        "switch"
                }
)
@Slf4j
public class AutoSwitcherPlugin extends Plugin implements net.runelite.client.input.KeyListener
{
    @Inject
    private Client client;

    @Inject
    private KeyManager keyManager;

    @Inject
    private ClientThread clientThread;

    @Inject
    private AutoSwitcherConfig config;

    @Provides
    private AutoSwitcherConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AutoSwitcherConfig.class);
    }

    @Override
    protected void startUp()
    {
        keyManager.registerKeyListener(this);
    }

    @Override
    protected void shutDown()
    {
        keyManager.unregisterKeyListener(this);
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        if (KeyEvent.getKeyText(e.getKeyCode()).equalsIgnoreCase(config.hotKey()))
        {
            clientThread.invoke(() ->
            {
              log.info("Switch items: {}", config.gearSet());
              Inventory.getAll(config.gearSet().split(","))
                      .stream()
                      .forEach(i -> i.interact(x -> x != null && (x.toLowerCase().contains("wear")
                              || x.toLowerCase().contains("wield")
                              || x.toLowerCase().contains("equip"))));
            });
        }
    }

    public void keyReleased(KeyEvent e)
    {
    }

    public void keyTyped(KeyEvent e)
    {
    }
}
