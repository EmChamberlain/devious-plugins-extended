package net.unethicalite.plugins.quicktoggler;

import com.google.inject.Singleton;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
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
public class QuickTogglerOverlay extends Overlay {

    private final Client client;
    private final QuickTogglerPlugin plugin;
    private final QuickTogglerConfig config;

    @Inject
    private QuickTogglerOverlay(Client client, QuickTogglerPlugin plugin, QuickTogglerConfig config)
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

        if (!config.isEnabled())
        {
            return null;
        }

        WorldArea enemyArea = plugin.enemyNPC.getWorldArea();
        for (WorldPoint worldPoint : enemyArea.toWorldPointList())
        {
            worldPoint.outline(client, graphics2D, Color.RED, "");
        }

        return null;
    }
}
