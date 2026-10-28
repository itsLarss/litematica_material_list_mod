package de.cozyverse.schematictracker.core;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/** Auswahl und Reihenfolge der angezeigten Materialien (HUD und Listen-Bildschirm). */
public final class ListView {
	private ListView() {}

	public enum Filter {
		/** Noch nicht fertig */
		OPEN,
		/** Noch nicht fertig und von mir übernommen ("Ich sammle das") */
		MINE,
		/** Alles */
		ALL
	}

	public enum Sort {
		/** Am meisten fehlend zuerst */
		MISSING,
		/** Alphabetisch nach angezeigtem Namen */
		NAME,
		/** Reihenfolge der Liste */
		LIST
	}

	public static List<TrackedItem> select(List<TrackedItem> items, Filter filter, Sort sort, String player,
			Function<TrackedItem, String> displayName) {
		Comparator<TrackedItem> order = switch (sort) {
			case MISSING -> Comparator.comparingInt(TrackedItem::missing).reversed()
					.thenComparingInt(TrackedItem::position);
			case NAME -> Comparator.comparing((TrackedItem i) -> displayName.apply(i), String.CASE_INSENSITIVE_ORDER);
			case LIST -> Comparator.comparingInt(TrackedItem::position);
		};
		return items.stream()
				.filter(i -> switch (filter) {
					case OPEN -> !i.complete();
					case MINE -> !i.complete() && i.claimedBy(player);
					case ALL -> true;
				})
				// Fertige immer ans Ende, auch bei "Alles".
				.sorted(Comparator.comparing(TrackedItem::complete).thenComparing(order))
				.toList();
	}
}
