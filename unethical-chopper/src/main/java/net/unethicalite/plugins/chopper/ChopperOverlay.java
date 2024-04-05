package net.unethicalite.plugins.chopper;

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
class ChopperOverlay extends Overlay
{
	private final Client client;
	private final ChopperPlugin plugin;
	private final ChopperConfig config;

	@Inject
	private ChopperOverlay(Client client, ChopperPlugin plugin, ChopperConfig config)
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
		List<Tile> fireArea = plugin.getFireArea();

		if (!plugin.isScriptStarted()
			|| !config.makeFire()
			|| fireArea == null
			|| fireArea.isEmpty())
		{
			return null;
		}

		for (Tile t : plugin.getFireArea())
		{
			Tile tile = Tiles.getAt(t.getWorldLocation());
			if (plugin.isEmptyTile(tile))
			{
				tile.getWorldLocation().outline(client, graphics2D, Color.GREEN, "");
			}
		}

		return null;
	}
}