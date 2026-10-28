package app.schematictracker.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Einstellungen der Mod (config/schematic-tracker.json). Der Token ist ein Geheimnis wie ein Passwort:
 * er steht nur in dieser Datei und wird nie im Chat oder Log ausgegeben.
 */
public final class TrackerConfig {
	public enum Corner {
		TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
	}

	/** Website, z. B. https://schematic-tracker.example */
	public String server = "";
	/** Mitbearbeiten- oder Ansehen-Token der verbundenen Liste */
	public String token = "";
	public String listName = "";

	/** Name für "Ich sammle das" und den Filter "Meine"; leer = Minecraft-Name */
	public String playerName = "";

	public boolean hudVisible = true;
	public ListView.Filter filter = ListView.Filter.OPEN;
	public ListView.Sort sort = ListView.Sort.MISSING;
	public Amounts.Unit unit = Amounts.Unit.SHULKER;
	public Corner corner = Corner.BOTTOM_RIGHT;
	/** Zeilen im HUD (ohne Titel) */
	public int rows = 6;
	/** Skalierung in Prozent */
	public int scale = 100;
	/** Deckkraft des Hintergrunds in Prozent */
	public int opacity = 60;
	/** HUD ausblenden, solange Chat oder ein anderes Menü offen ist */
	public boolean hideInMenus = true;

	/** Meldung oben rechts, wenn ein Material oder die Liste fertig wird */
	public boolean notifyToast = true;
	/** Ton dazu */
	public boolean notifySound = true;
	/** Konfetti, wenn die ganze Liste fertig ist */
	public boolean notifyConfetti = true;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public boolean linked() {
		return server != null && !server.isBlank() && token != null && !token.isBlank();
	}

	public void unlink() {
		token = "";
		listName = "";
	}

	/** Werte in sinnvolle Grenzen bringen (Datei kann von Hand bearbeitet worden sein). */
	public TrackerConfig sanitize() {
		if (server == null) server = "";
		if (token == null) token = "";
		if (listName == null) listName = "";
		if (playerName == null) playerName = "";
		if (filter == null) filter = ListView.Filter.OPEN;
		if (sort == null) sort = ListView.Sort.MISSING;
		if (unit == null) unit = Amounts.Unit.SHULKER;
		if (corner == null) corner = Corner.BOTTOM_RIGHT;
		rows = Math.clamp(rows, 1, 30);
		scale = Math.clamp(scale, 50, 200);
		opacity = Math.clamp(opacity, 0, 100);
		return this;
	}

	public static TrackerConfig load(Path file) {
		try {
			if (Files.exists(file)) {
				TrackerConfig config = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), TrackerConfig.class);
				if (config != null) return config.sanitize();
			}
		} catch (IOException | RuntimeException e) {
			// Kaputte Datei: mit Standardwerten weitermachen, beim nächsten Speichern wird sie ersetzt.
		}
		return new TrackerConfig();
	}

	public void save(Path file) throws IOException {
		Files.createDirectories(file.getParent());
		Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
		Files.writeString(tmp, GSON.toJson(sanitize()), StandardCharsets.UTF_8);
		Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}
}
