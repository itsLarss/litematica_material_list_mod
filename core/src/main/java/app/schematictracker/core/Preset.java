package app.schematictracker.core;

import java.util.Locale;

/**
 * Voreinstellungen für Aussehen und Meldungen. Setzen nur Anzeige-Optionen; Verbindung, Filter, Sortierung, Einheit,
 * Ecke, Name und "in Menüs ausblenden" bleiben, und alles lässt sich danach einzeln weiter ändern.
 */
public enum Preset {
	/** Nur die Liste, klein und unauffällig, keine Meldungen */
	MINIMAL,
	/** Standard (wie nach der Installation): alle Zusatzinfos, 6 Zeilen */
	CLEAR,
	/** Alles an, mehr Zeilen und kräftigerer Hintergrund */
	ALL;

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public void apply(TrackerConfig c) {
		boolean extras = this != MINIMAL;
		c.rows = switch (this) {
			case MINIMAL -> 3;
			case CLEAR -> 6;
			case ALL -> 12;
		};
		c.scale = this == MINIMAL ? 90 : 100;
		c.opacity = switch (this) {
			case MINIMAL -> 40;
			case CLEAR -> 60;
			case ALL -> 70;
		};
		c.hudNotes = extras;
		c.hudClaims = extras;
		c.claimColors = extras;
		c.hudSession = extras;
		c.hudDeadline = extras;
		c.notifyToast = extras;
		c.notifySound = extras;
		c.notifyConfetti = extras;
	}

	/** Passt die Config gerade genau zu einer Voreinstellung? (für die Anzeige "aktiv") */
	public boolean matches(TrackerConfig c) {
		TrackerConfig probe = new TrackerConfig();
		apply(probe);
		return probe.rows == c.rows && probe.scale == c.scale && probe.opacity == c.opacity
				&& probe.hudNotes == c.hudNotes && probe.hudClaims == c.hudClaims && probe.claimColors == c.claimColors
				&& probe.hudSession == c.hudSession && probe.hudDeadline == c.hudDeadline
				&& probe.notifyToast == c.notifyToast && probe.notifySound == c.notifySound
				&& probe.notifyConfetti == c.notifyConfetti;
	}
}
