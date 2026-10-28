package de.cozyverse.schematictracker.core;

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
		int position) {

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
