package net.unethicalite.plugins.dodgegraphics;

import com.google.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.unethicalite.api.scene.Tiles;

import javax.inject.Inject;
import java.awt.*;
import java.util.List;

@Singleton
class DodgeGraphicsOverlay extends Overlay
{
    @Inject
    private final Client client;
    @Inject
    private final DodgeGraphicsPlugin plugin;

    @Inject
    private DodgeGraphicsOverlay(Client client, DodgeGraphicsPlugin plugin, DodgeGraphicsConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics2D)
    {

        for (LocalPoint safePoint : plugin.safePoints)
        {
            Tile tile = Tiles.getAt(safePoint);
            tile.getWorldLocation().outline(client, graphics2D, Color.YELLOW, "");
        }

        if (plugin.closestPoint != null)
        {
            Tile tile = Tiles.getAt(plugin.closestPoint);
            tile.getWorldLocation().outline(client, graphics2D, Color.GREEN, "");
        }

        return null;
    }
}