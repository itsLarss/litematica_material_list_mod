package app.schematictracker.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Findet ein Material der Liste zu einer Eingabe aus dem Chat (/tracker add|set): Minecraft-ID mit oder ohne
 * "minecraft:", englischer Name, Name in der Spielsprache, Unterstriche statt Leerzeichen, oder ein eindeutiger Teil.
 */
public final class ItemMatch {
	private ItemMatch() {}

	public sealed interface Result {}

	public record Found(TrackedItem item) implements Result {}

	public record NotFound() implements Result {}

	/** Mehrere passen; die Namen (höchstens 5) für die Fehlermeldung */
	public record Ambiguous(List<TrackedItem> candidates) implements Result {}

	public static Result find(List<TrackedItem> items, String query, Function<TrackedItem, String> localName) {
		String q = normalize(query);
		if (q.isEmpty()) return new NotFound();
		// 1. genau: ID oder Name
		List<TrackedItem> exact = new ArrayList<>();
		for (TrackedItem item : items) {
			if (q.equals(idPath(item)) || q.equals(normalize(item.name())) || q.equals(normalize(localName.apply(item)))) {
				exact.add(item);
			}
		}
		if (exact.size() == 1) return new Found(exact.get(0));
		if (exact.size() > 1) return new Ambiguous(exact.subList(0, Math.min(5, exact.size())));
		// 2. Teil von ID oder Name
		List<TrackedItem> partial = new ArrayList<>();
		for (TrackedItem item : items) {
			if (idPath(item).contains(q) || normalize(item.name()).contains(q) || normalize(localName.apply(item)).contains(q)) {
				partial.add(item);
			}
		}
		if (partial.size() == 1) return new Found(partial.get(0));
		if (partial.isEmpty()) return new NotFound();
		return new Ambiguous(partial.subList(0, Math.min(5, partial.size())));
	}

	/** "minecraft:smooth_quartz" → "smooth_quartz" (Vorschlag im Chat) */
	public static String idPath(TrackedItem item) {
		String id = item.itemId() == null ? "" : item.itemId().toLowerCase(Locale.ROOT);
		return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
	}

	private static String normalize(String s) {
		if (s == null) return "";
		String t = s.trim().toLowerCase(Locale.ROOT);
		if (t.startsWith("minecraft:")) t = t.substring("minecraft:".length());
		return t.replace(' ', '_');
	}
}
