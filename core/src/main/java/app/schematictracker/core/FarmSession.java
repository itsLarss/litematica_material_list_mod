package app.schematictracker.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Farm-Session: zählt, was man selbst in der Mod einträgt (Liste, /tracker add|set), und rechnet daraus Items pro
 * Stunde und die geschätzte Restzeit für das Material, an dem man gerade farmt (das zuletzt erhöhte).
 *
 * Gerechnet wird pro Material vom ersten bis zum letzten Eintrag: Startet die Session von selbst (erster Eintrag),
 * ist dieser erste Eintrag nur der Ausgangspunkt und zählt nicht – sonst würde z. B. das Nachtragen von 100 000
 * schon Gesammelten ein riesiges Tempo ergeben. Nach einem Start per Befehl/Taste zählt jeder Zuwachs ab diesem
 * Moment. Zwischen zwei Einträgen bleibt die Anzeige ruhig.
 */
public final class FarmSession {
	/** Kürzere Zeitspanne zwischen erstem und letztem Eintrag ergibt keine Schätzung */
	static final long MIN_ELAPSED_MS = 60_000;

	private static final class Track {
		final long firstAt;
		long lastAt;
		/** Zuwachs, der ins Tempo eingeht (ohne den Ausgangs-Eintrag) */
		long counted;
		/** Alles, was in der Session dazukam (für die Anzeige) */
		long total;

		Track(long firstAt) {
			this.firstAt = firstAt;
			this.lastAt = firstAt;
		}
	}

	private long startedAt;
	/** true = per Befehl/Taste gestartet: dann zählt auch der erste Eintrag ab startedAt */
	private boolean explicit;
	private final Map<String, Track> tracks = new HashMap<>();
	private String currentId;

	public boolean running() {
		return startedAt != 0;
	}

	/** Start bzw. Neustart per Befehl, Taste oder Einstellungen */
	public void start(long now) {
		startedAt = now;
		explicit = true;
		tracks.clear();
		currentId = null;
	}

	public void stop() {
		startedAt = 0;
		explicit = false;
		tracks.clear();
		currentId = null;
	}

	/** Eigene Mengenänderung; nur Zuwächse zählen (Korrekturen nach unten verringern die Rate nicht ins Minus). */
	public void record(String itemId, int before, int after, long now) {
		if (after <= before) return;
		if (!running()) {
			startedAt = now;
			explicit = false;
		}
		long delta = after - before;
		Track t = tracks.get(itemId);
		if (t == null) {
			t = new Track(explicit ? startedAt : now);
			tracks.put(itemId, t);
			if (explicit) t.counted += delta;
		} else {
			t.counted += delta;
		}
		t.total += delta;
		t.lastAt = now;
		currentId = itemId;
	}

	/** Zuletzt erhöhtes Material oder null */
	public String currentId() {
		return currentId;
	}

	/** Alles, was in dieser Session für das Material dazukam */
	public long gained(String itemId) {
		Track t = tracks.get(itemId);
		return t == null ? 0 : t.total;
	}

	public long elapsedMs(long now) {
		return running() ? Math.max(0, now - startedAt) : 0;
	}

	/** Items pro Stunde für ein Material, -1 = noch keine Schätzung möglich */
	public double perHour(String itemId, long now) {
		Track t = tracks.get(itemId);
		if (t == null || t.counted <= 0) return -1;
		long span = t.lastAt - t.firstAt;
		if (span < MIN_ELAPSED_MS) return -1;
		return t.counted * 3_600_000.0 / span;
	}

	/** Geschätzte Restzeit in Millisekunden bei gleichem Tempo, -1 = unbekannt, 0 = fertig */
	public long remainingMs(TrackedItem item, long now) {
		if (item == null) return -1;
		if (item.missing() == 0) return 0;
		double rate = perHour(item.id(), now);
		if (rate <= 0) return -1;
		return Math.round(item.missing() / rate * 3_600_000.0);
	}

	/** "2 h 10 min", "45 min", "< 1 min" (Einheiten sind in Deutsch und Englisch gleich) */
	public static String formatDuration(long ms) {
		long minutes = Math.round(ms / 60_000.0);
		if (minutes < 1) return "< 1 min";
		if (minutes < 60) return minutes + " min";
		long hours = minutes / 60;
		long rest = minutes % 60;
		if (hours >= 100) return hours + " h";
		return rest == 0 ? hours + " h" : hours + " h " + rest + " min";
	}
}
