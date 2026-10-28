package app.schematictracker.core;

/**
 * Ziel-Datum der Liste aus API v1 ("deadline", YYYY-MM-DD) und – falls die Website es liefert – ihr berechneter
 * Plan-Status ("deadlineStatus"). Die Mod rechnet nichts selbst, sie zeigt nur an.
 *
 * @param status    done, overdue, onTrack, tight, behind, unknown; null = Website liefert keinen Status
 * @param daysLeft  Tage bis zum Ziel (negativ = vorbei); null = unbekannt
 * @param forecast  voraussichtliches Fertig-Datum (YYYY-MM-DD) oder null
 */
public record Deadline(String date, String status, Integer daysLeft, String forecast) {
	public boolean hasStatus() {
		return status != null;
	}
}
