package app.schematictracker.core;

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
		if (!active || inFlight || now < nextPollAt) return;

		inFlight = true;
		int gen = generation;
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

	private static TrackedItem withClaim(TrackedItem i, String claimedBy) {
		return new TrackedItem(i.id(), i.name(), i.itemId(), i.total(), i.have(), i.stackSize(), i.farmed(), claimedBy,
				i.position());
	}

	public static int statusOf(Throwable err) {
		Throwable t = err instanceof CompletionException && err.getCause() != null ? err.getCause() : err;
		return t instanceof TrackerApi.ApiException e ? e.status : 0;
	}
}
