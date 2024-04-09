package net.unethicalite.plugins.prayerflicker;

import com.google.inject.Singleton;
import net.unethicalite.api.scene.Tiles;
import net.runelite.api.Client;
import net.runelite.api.Tile;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;

@Singleton
class PrayerFlickerOverlay extends Overlay
{
    private final Client client;
    private final PrayerFlickerPlugin plugin;
    private final PrayerFlickerConfig config;

    @Inject
    private PrayerFlickerOverlay(Client client, PrayerFlickerPlugin plugin, PrayerFlickerConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics2D)
    {
        return null;
    }
}