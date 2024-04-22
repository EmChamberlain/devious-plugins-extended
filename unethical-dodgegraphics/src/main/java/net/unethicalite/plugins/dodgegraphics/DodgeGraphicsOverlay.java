package net.unethicalite.plugins.dodgegraphics;

import com.google.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.unethicalite.api.scene.Tiles;

import javax.inject.Inject;
import java.awt.*;
import java.util.List;

@Singleton
class DodgeGraphicsOverlay extends Overlay
{
    @Inject
    private Client client;
    @Inject
    private DodgeGraphicsPlugin plugin;

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
            Polygon poly = Perspective.getCanvasTilePoly(client, safePoint);
            if (poly != null)
            {
                OverlayUtil.renderPolygon(graphics2D, poly, Color.YELLOW);
            }
        }

        for (GraphicsObject graphicsObject : plugin.graphics)
        {
            Polygon poly = Perspective.getCanvasTilePoly(client, graphicsObject.getLocation());
            if (poly != null)
            {
                OverlayUtil.renderPolygon(graphics2D, poly, Color.RED);
            }
        }

        if (plugin.closestPoint != null)
        {
            Polygon poly = Perspective.getCanvasTilePoly(client, plugin.closestPoint);
            if (poly != null)
            {
                OverlayUtil.renderPolygon(graphics2D, poly, Color.GREEN);
            }
        }

        return null;
    }
}