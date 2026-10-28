package de.cozyverse.schematictracker;

import de.cozyverse.schematictracker.compat.Compat;
import de.cozyverse.schematictracker.core.ListSync;
import de.cozyverse.schematictracker.core.TrackerApi;
import de.cozyverse.schematictracker.core.TrackerConfig;
import de.cozyverse.schematictracker.screen.TrackerListScreen;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gemeinsamer Kern der Mod für alle Loader. Die Loader-Schichten (fabric/, neoforge/) rufen {@link #init},
 * registrieren {@link #keys()}, das HUD und die Befehle und rufen {@link #tick} am Ende jedes Client-Ticks.
 */
public final class SchematicTrackerClient {
	public static final String MOD_ID = "schematic-tracker";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

	private static final int GLFW_KEY_J = 74;
	private static final int GLFW_KEY_K = 75;

	private static Path configFile;
	private static TrackerConfig config;
	private static TrackerApi api;
	private static ListSync sync;
	/** Bildschirm, der nach dem Schließen des Chats geöffnet werden soll (Befehle laufen im Chat). */
	private static Screen pendingScreen;

	private static KeyMapping toggleHud;
	private static KeyMapping openList;

	private SchematicTrackerClient() {}

	public static void init(Path configDir, String version) {
		configFile = configDir.resolve(MOD_ID + ".json");
		config = TrackerConfig.load(configFile);
		api = new TrackerApi("SchematicTrackerMod/" + version);
		sync = new ListSync(api, () -> config, Minecraft.getInstance()::execute);
		toggleHud = Compat.key("key.schematic-tracker.toggle_hud", GLFW_KEY_J);
		openList = Compat.key("key.schematic-tracker.open_list", GLFW_KEY_K);
	}

	/** Tasten, die der Loader registrieren muss. */
	public static List<KeyMapping> keys() {
		return List.of(toggleHud, openList);
	}

	public static void tick(Minecraft mc) {
		while (toggleHud.consumeClick()) {
			config.hudVisible = !config.hudVisible;
			saveConfig();
		}
		while (openList.consumeClick()) {
			if (Compat.screen(mc) == null) Compat.setScreen(mc, new TrackerListScreen(null));
		}
		if (pendingScreen != null && Compat.screen(mc) == null) {
			Compat.setScreen(mc, pendingScreen);
			pendingScreen = null;
		}
		Screen screen = Compat.screen(mc);
		boolean listOpen = screen instanceof TrackerListScreen;
		boolean active = listOpen || (mc.level != null && !mc.isPaused() && config.hudVisible);
		sync.tick(System.currentTimeMillis(), active);
	}

	public static TrackerConfig config() {
		return config;
	}

	public static TrackerApi api() {
		return api;
	}

	public static ListSync sync() {
		return sync;
	}

	/** Öffnet einen Bildschirm, sobald der Chat zu ist. */
	public static void openLater(Screen screen) {
		pendingScreen = screen;
	}

	/** Name für "Ich sammle das" und den Filter "Meine". */
	public static String playerName() {
		if (!config.playerName.isBlank()) return config.playerName.trim();
		return Minecraft.getInstance().getUser().getName();
	}

	public static void saveConfig() {
		try {
			config.save(configFile);
		} catch (IOException e) {
			LOG.warn("Could not save {}", configFile, e);
		}
	}
}
