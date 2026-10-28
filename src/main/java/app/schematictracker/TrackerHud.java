package app.schematictracker;

import app.schematictracker.compat.Compat;
import app.schematictracker.compat.Gfx;
import app.schematictracker.core.FarmSession;
import app.schematictracker.core.ListSync;
import app.schematictracker.core.ListView;
import app.schematictracker.core.TrackedItem;
import app.schematictracker.core.TrackedList;
import app.schematictracker.core.TrackerConfig;
import app.schematictracker.screen.TrackerListScreen;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
	static final int CYAN = 0xFF55FFFF;
	/** Höchstbreite einer Material-Notiz im HUD (Kurzfassung) */
	static final int NOTE_WIDTH = 90;

	private TrackerHud() {}

	/** Wird vom Loader jedes Bild aufgerufen. */
	public static void render(Gfx g) {
		Minecraft mc = Minecraft.getInstance();
		TrackerConfig config = SchematicTrackerClient.config();
		ListSync sync = SchematicTrackerClient.sync();
		// Konfetti auch bei ausgeblendetem HUD; in der ganzen Liste zeichnet es der Bildschirm selbst
		if (!(Compat.screen(mc) instanceof TrackerListScreen)) Notifications.renderConfetti(g);
		if (!config.hudVisible || !config.linked() || mc.getDebugOverlay().showDebugScreen()) return;
		Screen screen = Compat.screen(mc);
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

		// Notizen (nur mit Mitbearbeiten-Link und neuerer Website; sonst null und still ausgeblendet)
		String listNote = config.hudNotes && list != null && list.note() != null
				? Texts.shorten(font, "✎ " + Texts.oneLine(list.note()), NOTE_WIDTH * 2) : null;

		// Spalten ausmessen
		List<String> names = new ArrayList<>();
		List<String> notes = new ArrayList<>();
		List<String> amounts = new ArrayList<>();
		List<String> claims = new ArrayList<>();
		int nameWidth = 0;
		int amountWidth = 0;
		int claimWidth = 0;
		for (TrackedItem item : shown) {
			String name = Texts.name(item);
			String amount = item.complete()
					? Component.translatable("schematic-tracker.hud.done").getString()
					: Texts.missing(item, config.unit);
			String note = config.hudNotes && item.hasNote() ? Texts.shorten(font, Texts.oneLine(item.note()), NOTE_WIDTH) : null;
			names.add(name);
			notes.add(note);
			amounts.add(amount);
			// Wer sammelt was: nur andere (eigene Claims sind schon am gelben Namen zu erkennen)
			String claim = config.hudClaims && item.claimedBy() != null && !item.claimedBy(player) ? item.claimedBy() : null;
			claims.add(claim);
			if (claim != null) claimWidth = Math.max(claimWidth, font.width(claim));
			nameWidth = Math.max(nameWidth, font.width(name) + (note == null ? 0 : 6 + font.width(note)));
			amountWidth = Math.max(amountWidth, font.width(amount));
		}
		String filterHint = config.filter == ListView.Filter.MINE
				? Component.translatable("schematic-tracker.filter.mine").getString()
				: "";
		String more = hidden > 0 ? Component.translatable("schematic-tracker.hud.more", hidden).getString() : null;
		boolean offline = list != null && sync.status() != ListSync.Status.OK;

		int titleWidth = font.width(title) + (filterHint.isEmpty() ? 0 : 8 + font.width(filterHint)) + (offline ? 10 : 0);
		int rowsWidth = shown.isEmpty() ? 0 : 16 + 4 + nameWidth + 10 + amountWidth + (claimWidth > 0 ? claimWidth + 8 : 0);
		String session = config.hudSession ? sessionLine(list) : null;
		int width = PAD * 2 + Math.max(titleWidth, Math.max(rowsWidth,
				Math.max(emptyText == null ? 0 : font.width(emptyText), more == null ? 0 : font.width(more))));
		if (session != null) width = Math.max(width, PAD * 2 + font.width(session));
		if (listNote != null) width = Math.max(width, PAD * 2 + font.width(listNote));
		int height = PAD + TITLE + (listNote != null ? 11 : 0) + shown.size() * ROW + (emptyText != null ? 11 : 0) + (more != null ? 11 : 0)
				+ (session != null ? 11 : 0) + PAD - 2;

		float scale = config.scale / 100f;
		int screenW = Math.round(g.width() / scale);
		int screenH = Math.round(g.height() / scale);
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

		g.pushScale(scale);
		int alpha = Math.round(config.opacity / 100f * 255) << 24;
		g.fill(x, y, x + width, y + height, alpha);

		int cy = y + PAD;
		g.text(font, title, x + PAD, cy, WHITE);
		int tx = x + PAD + font.width(title);
		if (offline) {
			g.text(font, "⚠", tx + 4, cy, RED);
			tx += 10;
		}
		if (!filterHint.isEmpty()) g.text(font, filterHint, x + width - PAD - font.width(filterHint), cy, GRAY);
		cy += TITLE;
		if (listNote != null) {
			g.text(font, listNote, x + PAD, cy, GRAY);
			cy += 11;
		}

		for (int i = 0; i < shown.size(); i++) {
			TrackedItem item = shown.get(i);
			ItemStack stack = Texts.stack(item);
			if (!stack.isEmpty()) g.item(stack, x + PAD, cy);
			int textY = cy + 4;
			int color = item.complete() ? GREEN : WHITE;
			g.text(font, names.get(i), x + PAD + 20, textY, item.claimedBy(player) ? 0xFFFFFF80 : color);
			if (notes.get(i) != null) g.text(font, notes.get(i), x + PAD + 20 + font.width(names.get(i)) + 6, textY, GRAY);
			String amount = amounts.get(i);
			g.text(font, amount, x + width - PAD - font.width(amount), textY, item.complete() ? GREEN : ORANGE);
			String claim = claims.get(i);
			if (claim != null) {
				g.text(font, claim, x + width - PAD - amountWidth - 8 - font.width(claim), textY, Texts.claimColor(claim));
			}
			cy += ROW;
		}
		if (emptyText != null) {
			g.text(font, emptyText, x + PAD, cy, GRAY);
			cy += 11;
		}
		if (more != null) {
			g.text(font, more, x + PAD, cy, GRAY);
			cy += 11;
		}
		if (session != null) g.text(font, session, x + PAD, cy, CYAN);
		g.popScale();
	}

	/** "Stone: 2560/h · noch ~1 h" für das Material, an dem man gerade farmt; null = keine Session */
	private static String sessionLine(TrackedList list) {
		FarmSession session = SchematicTrackerClient.session();
		if (list == null || !session.running() || session.currentId() == null) return null;
		TrackedItem item = list.item(session.currentId());
		if (item == null) return null;
		long now = System.currentTimeMillis();
		String name = Texts.name(item);
		if (item.complete()) return Component.translatable("schematic-tracker.hud.session_done", name).getString();
		double rate = session.perHour(item.id(), now);
		if (rate < 0) {
			return Component.translatable("schematic-tracker.hud.session_wait", name, session.gained(item.id())).getString();
		}
		return Component.translatable("schematic-tracker.hud.session", name, Math.round(rate),
				FarmSession.formatDuration(session.remainingMs(item, now))).getString();
	}
}
