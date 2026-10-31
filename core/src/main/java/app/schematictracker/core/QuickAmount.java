package app.schematictracker.core;

import java.util.Locale;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Schnelleingabe für Mengen wie auf der Website (src/lib/quickInput.ts): "3sb 5st 12", "2dk", "1,5sb", "+64", "-1st".
 * Einheiten: dk/dc (Doppelkiste, 54 Stacks), k/c/kiste/chest (27 Stacks), sb/shulker (27 Stacks), st/stack/s (1 Stack).
 * Beginnt die Eingabe mit + oder -, wird relativ zur aktuellen Menge gerechnet.
 */
public final class QuickAmount {
	/** Wie MAX_AMOUNT auf der Website */
	public static final int MAX = 1_000_000_000;

	private static final Pattern TOKEN = Pattern.compile(
			"([+-]?)\\s*(\\d+(?:[.,]\\d+)?)\\s*(doppelkisten|doppelkiste|doublechests|doublechest|dk|dc|kisten|kiste|chests"
					+ "|chest|k|c|shulkerboxen|shulkerboxes|shulkerbox|shulker|sb|stück|stk|items|item|pcs|stacks|stack|st|s)?");

	private QuickAmount() {}

	/** Neue Menge oder leer, wenn die Eingabe nicht verstanden wird. */
	public static OptionalInt parse(String input, int current, int stackSize) {
		String text = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
		if (text.isEmpty()) return OptionalInt.empty();
		boolean relative = text.startsWith("+") || text.startsWith("-");
		Matcher m = TOKEN.matcher(text);

		double sum = 0;
		int pos = 0;
		boolean matched = false;
		while (pos < text.length()) {
			while (pos < text.length() && text.charAt(pos) == ' ') pos++;
			if (pos >= text.length()) break;
			m.region(pos, text.length());
			if (!m.lookingAt() || m.group().trim().isEmpty()) return OptionalInt.empty();
			double value = Double.parseDouble(m.group(2).replace(',', '.'));
			int sign = "-".equals(m.group(1)) ? -1 : 1;
			sum += sign * value * unitSize(m.group(3) == null ? "" : m.group(3), stackSize);
			pos = m.end();
			matched = true;
		}
		if (!matched || !Double.isFinite(sum)) return OptionalInt.empty();
		long result = Math.round(relative ? current + sum : sum);
		return OptionalInt.of((int) Math.max(0, Math.min(MAX, result)));
	}

	private static int unitSize(String unit, int stackSize) {
		int s = Math.max(1, stackSize);
		if (unit.startsWith("doppel") || unit.startsWith("double") || unit.equals("dk") || unit.equals("dc")) return 54 * s;
		if (unit.startsWith("kiste") || unit.startsWith("chest") || unit.equals("k") || unit.equals("c")) return 27 * s;
		if (unit.startsWith("shulker") || unit.equals("sb")) return 27 * s;
		if (unit.startsWith("stack") || unit.equals("st") || unit.equals("s")) return s;
		return 1;
	}
}
