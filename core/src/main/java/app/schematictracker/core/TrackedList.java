package app.schematictracker.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aktueller Stand einer Liste. Teilantworten (?since=) werden per ID eingemischt,
 * damit nur geänderte Materialien übertragen werden müssen.
 */
public final class TrackedList {
	private final String id;
	private String name;
	private String role;
	private long version;
	private String note;
	/** Zählt jede Änderung (Abruf oder eigene Eingabe), damit die Mod nur bei Änderungen neu auswertet */
	private long revision;
	private final Map<String, TrackedItem> items = new LinkedHashMap<>();

	public TrackedList(String id, String name, String role, long version, List<TrackedItem> items) {
		this.id = id;
		this.name = name;
		this.role = role;
		this.version = version;
		for (TrackedItem item : items) this.items.put(item.id(), item);
	}

	/** Übernimmt eine Antwort von GET /api/v1/list; gibt zurück, ob sich etwas geändert hat. */
	public boolean apply(String name, String role, long version, boolean partial, List<TrackedItem> changed) {
		revision++;
		boolean changedAnything = version != this.version || !name.equals(this.name);
		this.name = name;
		this.role = role;
		this.version = version;
		if (!partial) {
			items.clear();
			changedAnything = true;
		}
		for (TrackedItem item : changed) {
			TrackedItem old = items.get(item.id());
			// Position bleibt erhalten: Teilantworten kommen in Listenreihenfolge, aber unvollständig.
			int position = old != null ? old.position() : item.position();
			items.put(item.id(), new TrackedItem(item.id(), item.name(), item.itemId(), item.total(), item.have(),
					item.stackSize(), item.farmed(), item.claimedBy(), position, item.note()));
			changedAnything = true;
		}
		return changedAnything;
	}

	/** Ändert ein Material lokal (z. B. nach einem eigenen Klick), bevor der Server antwortet. */
	public void replace(TrackedItem item) {
		if (items.containsKey(item.id())) {
			items.put(item.id(), item);
			revision++;
		}
	}

	/** Notiz zur ganzen Liste; null = keine, nicht freigegeben (Nur-Ansehen) oder Website ohne Notizen */
	public String note() {
		return note;
	}

	public boolean setNote(String note) {
		String clean = note == null || note.isBlank() ? null : note;
		if (java.util.Objects.equals(clean, this.note)) return false;
		this.note = clean;
		revision++;
		return true;
	}

	public long revision() {
		return revision;
	}

	public String id() {
		return id;
	}

	public String name() {
		return name;
	}

	public String role() {
		return role;
	}

	public boolean canEdit() {
		return "editor".equals(role);
	}

	public long version() {
		return version;
	}

	public List<TrackedItem> items() {
		List<TrackedItem> list = new ArrayList<>(items.values());
		list.sort((a, b) -> Integer.compare(a.position(), b.position()));
		return list;
	}

	public TrackedItem item(String id) {
		return items.get(id);
	}

	/** Fortschritt in Prozent: gesammelte Blöcke (höchstens Soll je Material) durch Soll. */
	public double percent() {
		long total = 0;
		long collected = 0;
		for (TrackedItem item : items.values()) {
			total += item.total();
			collected += item.farmed() ? item.total() : Math.min(item.have(), item.total());
		}
		return total == 0 ? 100 : collected * 100.0 / total;
	}
}
