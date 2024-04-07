package net.unethicalite.plugins.pestcontrol;

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
class PestControlOverlay extends Overlay
{
    private final Client client;
    private final PestControlPlugin plugin;
    private final PestControlConfig config;

    @Inject
    private PestControlOverlay(Client client, PestControlPlugin plugin, PestControlConfig config)
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
        if (plugin.guardPoint != null)
        {
            Tile tile = Tiles.getAt(plugin.guardPoint.getWorldLocation());
            tile.getWorldLocation().outline(client, graphics2D, Color.RED, "Guard Point");
        }
        return null;
    }
}