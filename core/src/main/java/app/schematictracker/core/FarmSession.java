package app.schematictracker.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Farm-Session: zählt, was man selbst in der Mod einträgt (Liste, /tracker add|set), und rechnet daraus Items pro
 * Stunde und die geschätzte Restzeit für das Material, an dem man gerade farmt (das zuletzt erhöhte).
 * Start und Reset per Befehl oder Taste; ohne Start beginnt die Session beim ersten eigenen Eintrag.
 */
public final class FarmSession {
	/** Unter einer Minute ist die Rate zu wackelig für eine Schätzung */
	static final long MIN_ELAPSED_MS = 60_000;

	private long startedAt;
	private final Map<String, Long> gained = new HashMap<>();
	private String currentId;

	public boolean running() {
		return startedAt != 0;
	}

	public void start(long now) {
		startedAt = now;
		gained.clear();
		currentId = null;
	}

	public void stop() {
		startedAt = 0;
		gained.clear();
		currentId = null;
	}

	/** Eigene Mengenänderung; nur Zuwächse zählen (Korrekturen nach unten verringern die Rate nicht ins Minus). */
	public void record(String itemId, int before, int after, long now) {
		if (after <= before) return;
		if (!running()) start(now);
		gained.merge(itemId, (long) (after - before), Long::sum);
		currentId = itemId;
	}

	/** Zuletzt erhöhtes Material oder null */
	public String currentId() {
		return currentId;
	}

	public long gained(String itemId) {
		return gained.getOrDefault(itemId, 0L);
	}

	public long elapsedMs(long now) {
		return running() ? Math.max(0, now - startedAt) : 0;
	}

	/** Items pro Stunde für ein Material, -1 = noch keine Schätzung möglich */
	public double perHour(String itemId, long now) {
		long elapsed = elapsedMs(now);
		long amount = gained(itemId);
		if (elapsed < MIN_ELAPSED_MS || amount <= 0) return -1;
		return amount * 3_600_000.0 / elapsed;
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
