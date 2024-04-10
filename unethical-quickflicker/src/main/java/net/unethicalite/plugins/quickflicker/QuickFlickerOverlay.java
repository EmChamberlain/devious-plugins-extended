package net.unethicalite.plugins.quickflicker;

import com.google.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;

@Singleton
class QuickFlickerOverlay extends Overlay
{
    private final Client client;
    private final QuickFlickerPlugin plugin;
    private final QuickFlickerConfig config;

    @Inject
    private QuickFlickerOverlay(Client client, QuickFlickerPlugin plugin, QuickFlickerConfig config)
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