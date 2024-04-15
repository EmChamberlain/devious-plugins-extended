package net.unethicalite.plugins.fisher;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.unethicalite.api.entities.TileObjects;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.util.Objects;

@Singleton
public class FisherOverlay extends Overlay
{

    private final Client client;

    private final FisherPlugin plugin;

    @Inject
    public FisherOverlay(Client client, FisherPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {

        renderTile(graphics, plugin.fishingWorldPoint, Color.decode("#ff0000"), "fish");
        renderTile(graphics, plugin.bankWorldPoint, Color.decode("#00ff00"), "bank");
        renderTile(graphics, plugin.cookWorldPoint, Color.decode("#0000ff"), "cook");
        return null;
    }

    private void renderTile(Graphics2D graphics, WorldPoint wp, Color color, String stringIn)
    {
        LocalPoint lp = LocalPoint.fromWorld(client, wp);
        if (lp != null)
        {
            Polygon poly = Perspective.getCanvasTilePoly(client, lp);
            if (poly != null)
            {
                OverlayUtil.renderPolygon(graphics, poly, color);
            }
        }
    }

}
