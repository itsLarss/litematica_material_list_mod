package app.schematictracker.core;

/** Umrechnung von Stückzahlen in Stacks, Shulkerboxen und Doppelkisten. */
public final class Amounts {
	private Amounts() {}

	public enum Unit {
		STACKS(1),
		SHULKER(27),
		CHEST(54);

		/** Anzahl Stacks pro Behälter */
		public final int stacks;

		Unit(int stacks) {
			this.stacks = stacks;
		}
	}

	/** Menge in der Einheit, z. B. 70417 Stone in Shulkerboxen = 40,75. */
	public static double inUnit(int amount, int stackSize, Unit unit) {
		return amount / (double) (Math.max(1, stackSize) * unit.stacks);
	}
}
