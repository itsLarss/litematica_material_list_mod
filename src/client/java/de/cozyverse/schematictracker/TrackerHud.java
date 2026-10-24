package de.cozyverse.schematictracker;

import de.cozyverse.schematictracker.compat.Compat;
import de.cozyverse.schematictracker.core.ListSync;
import de.cozyverse.schematictracker.core.ListView;
import de.cozyverse.schematictracker.core.TrackedItem;
import de.cozyverse.schematictracker.core.TrackedList;
import de.cozyverse.schematictracker.core.TrackerConfig;
import de.cozyverse.schematictracker.screen.TrackerListScreen;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Kompaktes Kästchen mit den offenen Materialien, im Stil der Litematica-Materialliste:
 * Icon, Name und rechts die fehlende Menge. Zeigt nur so viele Zeilen wie eingestellt.
 */
public final class TrackerHud {
	static final int ROW = 18;
	static final int PAD = 4;
	static final int TITLE = 13;
	static final int WHITE = 0xFFFFFFFF;
	static final int GRAY = 0xFFAAAAAA;
	static final int ORANGE = 0xFFFFAA00;
	static final int GREEN = 0xFF55FF55;
	static final int RED = 0xFFFF5555;

	public void render(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		TrackerConfig config = SchematicTrackerClient.config();
		ListSync sync = SchematicTrackerClient.sync();
		if (!config.hudVisible || !config.linked() || mc.getDebugOverlay().showDebugScreen()) return;
		Screen screen = mc.screen;
		if (screen instanceof TrackerListScreen || (config.hideInMenus && screen != null)) return;

		Font font = mc.font;
		TrackedList list = sync.list();
		String player = SchematicTrackerClient.playerName();

		Component title;
		List<TrackedItem> shown = List.of();
		int hidden = 0;
		String emptyText = null;
		if (list == null) {
			title = Component.literal(config.listName.isBlank() ? "Schematic Tracker" : config.listName);
			emptyText = Component.translatable(sync.status() == ListSync.Status.GONE
					? "schematic-tracker.hud.gone"
					: sync.status() == ListSync.Status.OFFLINE ? "schematic-tracker.hud.offline" : "schematic-tracker.hud.loading")
					.getString();
		} else {
			title = Component.literal(String.format(Texts.locale(), "%s · %.0f %%", list.name(), Math.floor(list.percent())));
			List<TrackedItem> selected = ListView.select(list.items(), config.filter, config.sort, player, Texts::name);
			shown = selected.subList(0, Math.min(config.rows, selected.size()));
			hidden = selected.size() - shown.size();
			if (selected.isEmpty()) {
				emptyText = Component.translatable(config.filter == ListView.Filter.MINE
						? "schematic-tracker.hud.none_mine"
						: "schematic-tracker.hud.all_done").getString();
			}
		}

		// Spalten ausmessen
		List<String> names = new ArrayList<>();
		List<String> amounts = new ArrayList<>();
		int nameWidth = 0;
		int amountWidth = 0;
		for (TrackedItem item : shown) {
			String name = Texts.name(item);
			String amount = item.complete()
					? Component.translatable("schematic-tracker.hud.done").getString()
					: Texts.missing(item, config.unit);
			names.add(name);
			amounts.add(amount);
			nameWidth = Math.max(nameWidth, font.width(name));
			amountWidth = Math.max(amountWidth, font.width(amount));
		}
		String filterHint = config.filter == ListView.Filter.MINE
				? Component.translatable("schematic-tracker.filter.mine").getString()
				: "";
		String more = hidden > 0 ? Component.translatable("schematic-tracker.hud.more", hidden).getString() : null;
		boolean offline = list != null && sync.status() != ListSync.Status.OK;

		int titleWidth = font.width(title) + (filterHint.isEmpty() ? 0 : 8 + font.width(filterHint)) + (offline ? 10 : 0);
		int rowsWidth = shown.isEmpty() ? 0 : 16 + 4 + nameWidth + 10 + amountWidth;
		int width = PAD * 2 + Math.max(titleWidth, Math.max(rowsWidth,
				Math.max(emptyText == null ? 0 : font.width(emptyText), more == null ? 0 : font.width(more))));
		int height = PAD + TITLE + shown.size() * ROW + (emptyText != null ? 11 : 0) + (more != null ? 11 : 0) + PAD - 2;

		float scale = config.scale / 100f;
		int screenW = Math.round(g.guiWidth() / scale);
		int screenH = Math.round(g.guiHeight() / scale);
		int margin = 4;
		int x = switch (config.corner) {
			case TOP_LEFT, BOTTOM_LEFT -> margin;
			case TOP_RIGHT, BOTTOM_RIGHT -> screenW - width - margin;
		};
		int y = switch (config.corner) {
			case TOP_LEFT, TOP_RIGHT -> margin;
			// Unten etwas Abstand, damit die Hotbar-Zeile frei bleibt.
			case BOTTOM_LEFT, BOTTOM_RIGHT -> screenH - height - margin - 24;
		};

		Compat.pushScale(g, scale);
		int alpha = Math.round(config.opacity / 100f * 255) << 24;
		g.fill(x, y, x + width, y + height, alpha);

		int cy = y + PAD;
		g.drawString(font, title, x + PAD, cy, WHITE, true);
		int tx = x + PAD + font.width(title);
		if (offline) {
			g.drawString(font, "⚠", tx + 4, cy, RED, true);
			tx += 10;
		}
		if (!filterHint.isEmpty()) g.drawString(font, filterHint, x + width - PAD - font.width(filterHint), cy, GRAY, true);
		cy += TITLE;

		for (int i = 0; i < shown.size(); i++) {
			TrackedItem item = shown.get(i);
			ItemStack stack = Texts.stack(item);
			if (!stack.isEmpty()) g.renderFakeItem(stack, x + PAD, cy);
			int textY = cy + 4;
			int color = item.complete() ? GREEN : WHITE;
			g.drawString(font, names.get(i), x + PAD + 20, textY, item.claimedBy(player) ? 0xFFFFFF80 : color, true);
			String amount = amounts.get(i);
			g.drawString(font, amount, x + width - PAD - font.width(amount), textY, item.complete() ? GREEN : ORANGE, true);
			cy += ROW;
		}
		if (emptyText != null) {
			g.drawString(font, emptyText, x + PAD, cy, GRAY, true);
			cy += 11;
		}
		if (more != null) g.drawString(font, more, x + PAD, cy, GRAY, true);
		Compat.popScale(g);
	}
}
