package app.schematictracker.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Hält die verbundene Liste aktuell: fragt regelmäßig nur die Änderungen seit der letzten Version ab
 * (alle 2,5 s, nach 2 Minuten ohne Änderung alle 10 s) und nur, solange die Liste angezeigt wird.
 * Ergebnisse werden über `mainThread` angewendet, damit HUD und Menüs nie einen halben Stand sehen.
 */
public final class ListSync {
	static final long FAST_MS = 2_500;
	static final long SLOW_MS = 10_000;
	static final long IDLE_AFTER_MS = 120_000;
	static final long ERROR_MS = 10_000;
	static final long GONE_MS = 60_000;
	/** Eigene Mengen werden gesammelt und kurz verzögert gespeichert, wie auf der Website. */
	static final long SAVE_DELAY_MS = 600;

	public enum Status {
		/** Nicht verbunden */
		UNLINKED,
		/** Verbunden, erster Abruf läuft */
		LOADING,
		OK,
		/** Server nicht erreichbar; der letzte Stand bleibt sichtbar */
		OFFLINE,
		/** Liste gelöscht oder Freigabe beendet */
		GONE
	}

	private final TrackerApi api;
	private final Supplier<TrackerConfig> config;
	private final Executor mainThread;

	private TrackedList list;
	private String loadedFor = "";
	private Status status = Status.UNLINKED;
	private boolean inFlight;
	private long nextPollAt;
	private long lastChangeAt;
	private int generation;
	private String lastError;

	/**
	 * Eigene Mengen, die der Server noch nicht (sicher) hat. Sie haben Vorrang vor abgerufenen Daten, bis ein Abruf
	 * zurückkommt, der nach dem Speichern gestartet wurde.
	 */
	private final Map<String, LocalAmount> localAmounts = new LinkedHashMap<>();
	private boolean saving;
	private long saveAt;
	private long pollStartedAt;
	private String actor;
	private AmountListener amountListener = (item, before, after) -> {};

	/** Wird bei jeder eigenen Mengenänderung aufgerufen (z. B. für die Farm-Session). */
	public interface AmountListener {
		void changed(TrackedItem item, int before, int after);
	}

	public void onOwnAmount(AmountListener listener) {
		amountListener = listener;
	}

	private static final class LocalAmount {
		TrackerApi.AmountChange change;
		/** Zeitpunkt der Bestätigung durch den Server, 0 = noch nicht gespeichert */
		long savedAt;

		LocalAmount(TrackerApi.AmountChange change) {
			this.change = change;
		}
	}

	public ListSync(TrackerApi api, Supplier<TrackerConfig> config, Executor mainThread) {
		this.api = api;
		this.config = config;
		this.mainThread = mainThread;
	}

	public TrackedList list() {
		return list;
	}

	public Status status() {
		return status;
	}

	/** Letzte Fehlermeldung (ohne Token), für /tracker status. */
	public String lastError() {
		return lastError;
	}

	/** Sofort neu abrufen (z. B. /tracker refresh oder nach dem Verbinden). */
	public void refresh() {
		nextPollAt = 0;
	}

	/** Verbindung geändert: alten Stand verwerfen. */
	public void reset() {
		generation++;
		list = null;
		loadedFor = "";
		inFlight = false;
		nextPollAt = 0;
		localAmounts.clear();
		saving = false;
		status = config.get().linked() ? Status.LOADING : Status.UNLINKED;
	}

	/** Jeden Client-Tick aufrufen; `active` = Liste wird gerade angezeigt. */
	public void tick(long now, boolean active) {
		TrackerConfig c = config.get();
		String key = c.server + "|" + c.token;
		if (!c.linked()) {
			if (list != null || status != Status.UNLINKED) reset();
			return;
		}
		if (!key.equals(loadedFor) && list != null) reset();
		if (!saving && now >= saveAt && localAmounts.values().stream().anyMatch(l -> l.savedAt == 0)) saveAmounts(c);
		if (!active || inFlight || now < nextPollAt) return;

		inFlight = true;
		int gen = generation;
		long pollStart = System.currentTimeMillis();
		long since = list == null ? -1 : list.version();
		api.list(c.server, c.token, since).whenComplete((res, err) -> mainThread.execute(() -> {
			if (gen != generation) return;
			inFlight = false;
			long done = System.currentTimeMillis();
			if (err != null) {
				int code = statusOf(err);
				Throwable cause = err.getCause() != null ? err.getCause() : err;
				lastError = cause.getMessage();
				status = code == 404 ? Status.GONE : Status.OFFLINE;
				nextPollAt = done + (code == 404 ? GONE_MS : ERROR_MS);
				return;
			}
			boolean changed;
			if (list == null || !list.id().equals(res.id())) {
				list = new TrackedList(res.id(), res.name(), res.role(), res.version(), res.items());
				loadedFor = key;
				changed = true;
			} else {
				changed = list.apply(res.name(), res.role(), res.version(), res.partial(), res.items());
			}
			pollStartedAt = pollStart;
			applyLocalAmounts();
			if (changed || lastChangeAt == 0) lastChangeAt = done;
			status = Status.OK;
			lastError = null;
			nextPollAt = done + (done - lastChangeAt < IDLE_AFTER_MS ? FAST_MS : SLOW_MS);
		}));
	}

	/** "Ich sammle das" setzen oder freigeben; sofort lokal sichtbar, bei Fehler zurückgenommen. */
	public CompletableFuture<Void> claim(TrackedItem item, String player) {
		TrackerConfig c = config.get();
		if (list == null || !list.canEdit()) return CompletableFuture.failedFuture(new TrackerApi.ApiException(404, "read only"));
		String next = item.claimedBy(player) ? null : player;
		list.replace(withClaim(item, next));
		lastChangeAt = System.currentTimeMillis();
		int gen = generation;
		return api.saveClaim(c.server, c.token, item.id(), next, player).whenComplete((ok, err) -> mainThread.execute(() -> {
			if (gen != generation || list == null) return;
			if (err != null) list.replace(item);
			refresh();
		}));
	}

	/**
	 * Eigene Menge für ein Material setzen (volle Menge, kein Unterschied). Sofort lokal sichtbar, gespeichert wird
	 * gesammelt nach {@link #SAVE_DELAY_MS}. "Gefarmt" bleibt nur, solange die Menge reicht (wie auf der Website).
	 */
	public boolean setHave(TrackedItem item, int have, String player) {
		if (list == null || !list.canEdit() || list.item(item.id()) == null) return false;
		TrackedItem current = list.item(item.id());
		int value = Math.clamp(have, 0, QuickAmount.MAX);
		boolean farmed = current.farmed() && value >= current.total();
		TrackerApi.AmountChange change = new TrackerApi.AmountChange(current.id(), value, farmed);
		LocalAmount local = localAmounts.computeIfAbsent(current.id(), id -> new LocalAmount(change));
		local.change = change;
		local.savedAt = 0;
		list.replace(withAmount(current, value, farmed));
		amountListener.changed(current, current.have(), value);
		actor = player;
		long now = System.currentTimeMillis();
		saveAt = now + SAVE_DELAY_MS;
		lastChangeAt = now;
		return true;
	}

	/** true, solange eigene Mengen noch nicht gespeichert sind (z. B. für einen Hinweis im HUD). */
	public boolean savePending() {
		return saving || localAmounts.values().stream().anyMatch(l -> l.savedAt == 0);
	}

	private void saveAmounts(TrackerConfig c) {
		List<TrackerApi.AmountChange> batch = new ArrayList<>();
		for (LocalAmount l : localAmounts.values()) if (l.savedAt == 0) batch.add(l.change);
		saving = true;
		int gen = generation;
		api.saveAmounts(c.server, c.token, batch, actor).whenComplete((ok, err) -> mainThread.execute(() -> {
			if (gen != generation) return;
			saving = false;
			long done = System.currentTimeMillis();
			if (err != null) {
				// Bleibt lokal sichtbar und wird später erneut gesendet
				Throwable cause = err.getCause() != null ? err.getCause() : err;
				lastError = cause.getMessage();
				int code = statusOf(err);
				if (code == 404) {
					localAmounts.clear();
					status = Status.GONE;
				}
				saveAt = done + ERROR_MS;
				return;
			}
			for (TrackerApi.AmountChange sent : batch) {
				LocalAmount l = localAmounts.get(sent.id());
				if (l != null && l.change.equals(sent)) l.savedAt = done;
			}
			refresh();
		}));
	}

	/** Nach einem Abruf: eigene, noch nicht bestätigte Mengen wieder darüberlegen. */
	private void applyLocalAmounts() {
		localAmounts.values().removeIf(l -> l.savedAt != 0 && l.savedAt <= pollStartedAt);
		for (LocalAmount l : localAmounts.values()) {
			TrackedItem item = list.item(l.change.id());
			if (item != null) list.replace(withAmount(item, l.change.have(), l.change.farmed()));
		}
	}

	private static TrackedItem withAmount(TrackedItem i, int have, boolean farmed) {
		return new TrackedItem(i.id(), i.name(), i.itemId(), i.total(), have, i.stackSize(), farmed, i.claimedBy(),
				i.position());
	}

	private static TrackedItem withClaim(TrackedItem i, String claimedBy) {
		return new TrackedItem(i.id(), i.name(), i.itemId(), i.total(), i.have(), i.stackSize(), i.farmed(), claimedBy,
				i.position());
	}

	public static int statusOf(Throwable err) {
		Throwable t = err instanceof CompletionException && err.getCause() != null ? err.getCause() : err;
		return t instanceof TrackerApi.ApiException e ? e.status : 0;
	}
}
