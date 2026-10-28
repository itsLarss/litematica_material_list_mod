package app.schematictracker.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Erkennt, wann Materialien oder die ganze Liste fertig werden – egal ob durch eigene Eingaben, die Website oder
 * Freunde. Beim ersten Laden und beim Wechsel der Liste wird nur der Stand gemerkt, ohne Meldung.
 */
public final class Completions {
	/** Was seit dem letzten Aufruf fertig geworden ist; listDone = jetzt alles fertig, vorher nicht. */
	public record Update(List<TrackedItem> completed, boolean listDone) {
		public static final Update NONE = new Update(List.of(), false);

		public boolean isEmpty() {
			return completed.isEmpty() && !listDone;
		}
	}

	private String listId;
	private final Set<String> done = new HashSet<>();
	private boolean allDone;

	/** Mit dem aktuellen Stand aufrufen (null = keine Liste). */
	public Update update(TrackedList list) {
		if (list == null) {
			listId = null;
			done.clear();
			return Update.NONE;
		}
		List<TrackedItem> items = list.items();
		boolean all = !items.isEmpty() && items.stream().allMatch(TrackedItem::complete);
		if (!list.id().equals(listId)) {
			listId = list.id();
			done.clear();
			for (TrackedItem item : items) if (item.complete()) done.add(item.id());
			allDone = all;
			return Update.NONE;
		}
		List<TrackedItem> completed = new ArrayList<>();
		Set<String> now = new HashSet<>();
		for (TrackedItem item : items) {
			if (!item.complete()) continue;
			now.add(item.id());
			if (!done.contains(item.id())) completed.add(item);
		}
		done.clear();
		done.addAll(now);
		boolean listDone = all && !allDone;
		allDone = all;
		return completed.isEmpty() && !listDone ? Update.NONE : new Update(completed, listDone);
	}
}
