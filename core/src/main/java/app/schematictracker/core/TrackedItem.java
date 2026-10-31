package app.schematictracker.core;

/** Ein Material der Liste, wie es die API v1 liefert. */
public record TrackedItem(
		String id,
		String name,
		String itemId,
		int total,
		int have,
		int stackSize,
		boolean farmed,
		String claimedBy,
		int position,
		/** Notiz von der Website (z. B. "im Nether holen"); null = keine oder nicht freigegeben */
		String note) {

	public TrackedItem(String id, String name, String itemId, int total, int have, int stackSize, boolean farmed,
			String claimedBy, int position) {
		this(id, name, itemId, total, have, stackSize, farmed, claimedBy, position, null);
	}

	public boolean hasNote() {
		return note != null && !note.isBlank();
	}

	public int missing() {
		return farmed ? 0 : Math.max(0, total - have);
	}

	/** Fertig gesammelt oder als gefarmt markiert. */
	public boolean complete() {
		return farmed || have >= total;
	}

	public boolean claimedBy(String player) {
		return claimedBy != null && player != null && !player.isBlank() && claimedBy.equalsIgnoreCase(player.trim());
	}
}
