package net.unethicalite.plugins.flicker;

import com.google.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;

@Singleton
class FlickerOverlay extends Overlay
{
    private final Client client;
    private final FlickerPlugin plugin;
    private final FlickerConfig config;

    @Inject
    private FlickerOverlay(Client client, FlickerPlugin plugin, FlickerConfig config)
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