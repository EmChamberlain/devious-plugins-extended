/*
 * Copyright (c) 2022, EmChamberlain <https://github.com/EmChamberlain/>,
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
package net.unethicalite.powerfisher;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.unethicalite.api.items.Inventory;
import org.pf4j.Extension;
import javax.inject.Inject;

@Extension
@PluginDescriptor(
        name = "mPower Fisher",
        description = "Power fish",
        enabledByDefault = false,
        tags =
                {
                        "Fishing",
                        "auto",
                        "fish",
                        "shrimp",
                        "anchovies",
                        "trout",
                        "salmon",
                        "barbarian"
                }
)
@Slf4j
public class PowerFisherPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private PowerFisherConfig config;

    @Provides
    private PowerFisherConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(PowerFisherConfig.class);
    }

    private boolean fishing;

    @Override
    protected void startUp()
    {
    }

    @Override
    protected void shutDown()
    {
    }

    @Subscribe
    public void onConfigButtonPressed(ConfigButtonClicked event)
    {
            if (!event.getGroup().contains("powerfisher") || !event.getKey().toLowerCase().contains("start"))
            {
                return;
            }

            if (fishing)
            {
                reset();
            }
            else
            {
                this.fishing = true;
            }
    }

    /**
     * Reset/stop fishing
     */
    private void reset()
    {
        this.fishing = false;
    }

    @Subscribe
    private void onGameTick(GameTick event)
    {
        // fishing
        if (!fishing || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        // Stop when level is reached..
        if (client.getBoostedSkillLevel(Skill.FISHING) >= config.destinationLevel())
        {
            reset();
            return;
        }

        FishingType fishingType = config.fishingType();

        // No required items found
        if (!Inventory.contains(fishingType.getRequiredItems()))
        {
            log.error("Make sure you have required items in inventory: {}", fishingType.getRequiredItems().toString());
            reset();
            return;
        }

        // Idle
        if (client.getLocalPlayer().getAnimation() == -1)
        {
            // Drop fish
            if (Inventory.contains(fishingType.getFishToDrop()))
            {
                Inventory.getAll(fishingType.getFishToDrop()).forEach(Item::drop);
                return;
            }

            // Fish
            NPC fishingSpot = client.getNpcs()
                    .stream()
                    .filter(n -> n.getName().contains("spot"))
                    .findFirst()
                    .get();

            if (fishingSpot == null)
            {
                return;
            }

            fishingSpot.interact(fishingType.getAction());
        }
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            reset();
        }
    }
}
