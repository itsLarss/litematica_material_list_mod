package app.schematictracker;

import app.schematictracker.compat.Compat;
import app.schematictracker.compat.Gfx;
import app.schematictracker.core.Completions;
import app.schematictracker.core.TrackedList;
import app.schematictracker.core.TrackerConfig;
import java.util.Random;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * Fertig-Meldungen: Toast und Ton, wenn ein Material voll ist; bei "Liste fertig" zusätzlich Konfetti über dem
 * Bildschirm (eigene Animation, keine Partikel). Alles einzeln in den Einstellungen abschaltbar.
 */
public final class Notifications {
	private static final long CONFETTI_MS = 4_000;
	private static final int PIECES = 140;
	private static final int[] COLORS = {0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55, 0xFF55FF55, 0xFF55FFFF, 0xFF5555FF, 0xFFFF55FF};

	private static final Completions COMPLETIONS = new Completions();
	private static long lastRevision = -1;
	private static String lastListId;
	private static long confettiStart;
	private static long confettiSeed;

	private Notifications() {}

	/** Jeden Client-Tick: nur auswerten, wenn sich die Liste geändert hat. */
	public static void tick(Minecraft mc, TrackedList list) {
		String id = list == null ? null : list.id();
		long revision = list == null ? -1 : list.revision();
		if (revision == lastRevision && java.util.Objects.equals(id, lastListId)) return;
		lastRevision = revision;
		lastListId = id;
		Completions.Update update = COMPLETIONS.update(list);
		if (update.isEmpty()) return;

		TrackerConfig config = SchematicTrackerClient.config();
		if (update.listDone()) {
			if (config.notifyToast) {
				Compat.toast(mc, Component.translatable("schematic-tracker.notify.list_done"), Component.literal(list.name()));
			}
			if (config.notifySound) mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f));
			if (config.notifyConfetti) startConfetti();
			return;
		}
		if (config.notifyToast) {
			if (update.completed().size() == 1) {
				Compat.toast(mc, Component.translatable("schematic-tracker.notify.item_done"),
						Component.literal(Texts.name(update.completed().get(0))));
			} else {
				String names = update.completed().stream().limit(3).map(Texts::name).collect(Collectors.joining(", "));
				if (update.completed().size() > 3) names += ", …";
				Compat.toast(mc, Component.translatable("schematic-tracker.notify.items_done", update.completed().size()),
						Component.literal(names));
			}
		}
		if (config.notifySound) mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.4f));
	}

	public static void startConfetti() {
		confettiStart = System.currentTimeMillis();
		confettiSeed = confettiStart;
	}

	/** Konfetti über den ganzen Bildschirm zeichnen, solange die Animation läuft (aus HUD und Liste aufgerufen). */
	public static void renderConfetti(Gfx g) {
		long t = System.currentTimeMillis() - confettiStart;
		if (confettiStart == 0 || t > CONFETTI_MS) return;
		int w = g.width();
		int h = g.height();
		float seconds = t / 1000f;
		// Zum Ende hin ausblenden
		float fade = Math.min(1f, (CONFETTI_MS - t) / 600f);
		int alpha = Math.round(fade * 255) << 24;
		Random random = new Random(confettiSeed);
		for (int i = 0; i < PIECES; i++) {
			float startX = random.nextFloat() * w;
			float delay = random.nextFloat() * 0.8f;
			float speed = 60 + random.nextFloat() * 90;
			float sway = 8 + random.nextFloat() * 16;
			float spin = 4 + random.nextFloat() * 8;
			int color = COLORS[random.nextInt(COLORS.length)];
			float life = seconds - delay;
			if (life < 0) continue;
			float y = -10 + life * speed + life * life * 20;
			if (y > h) continue;
			float x = startX + (float) Math.sin(life * 3 + i) * sway;
			// "Drehen": Höhe pendelt zwischen 1 und 4 Pixeln
			int ph = 1 + Math.round(3 * Math.abs((float) Math.cos(life * spin)));
			int px = Math.round(x);
			int py = Math.round(y);
			g.fill(px, py, px + 3, py + ph, (color & 0x00FFFFFF) | alpha);
		}
	}

	/** Vorschau aus den Einstellungen: zeigt "Liste fertig" mit den gerade eingestellten Optionen. */
	public static void preview(Minecraft mc) {
		TrackerConfig config = SchematicTrackerClient.config();
		if (config.notifyToast) {
			Compat.toast(mc, Component.translatable("schematic-tracker.notify.list_done"),
					Component.translatable("schematic-tracker.notify.preview"));
		}
		if (config.notifySound) mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f));
		if (config.notifyConfetti) startConfetti();
	}
}
