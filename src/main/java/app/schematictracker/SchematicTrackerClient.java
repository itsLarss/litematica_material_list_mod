package app.schematictracker;

import app.schematictracker.compat.Compat;
import com.mojang.blaze3d.platform.InputConstants;
import app.schematictracker.core.FarmSession;
import app.schematictracker.core.ListSync;
import app.schematictracker.core.TrackerApi;
import app.schematictracker.core.TrackerConfig;
import app.schematictracker.screen.TrackerListScreen;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gemeinsamer Kern der Mod für alle Loader. Die Loader-Schichten (fabric/, neoforge/) rufen {@link #init},
 * registrieren {@link #keys()}, das HUD und die Befehle und rufen {@link #tick} am Ende jedes Client-Ticks.
 */
public final class SchematicTrackerClient {
	public static final String MOD_ID = "schematic-tracker";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);


	private static Path configFile;
	private static TrackerConfig config;
	private static TrackerApi api;
	private static ListSync sync;
	private static final FarmSession SESSION = new FarmSession();
	/** Bildschirm, der nach dem Schließen des Chats geöffnet werden soll (Befehle laufen im Chat). */
	private static Screen pendingScreen;

	private static KeyMapping toggleHud;
	private static KeyMapping openList;
	private static KeyMapping restartSession;

	private SchematicTrackerClient() {}

	public static void init(Path configDir, String version) {
		configFile = configDir.resolve(MOD_ID + ".json");
		config = TrackerConfig.load(configFile);
		api = new TrackerApi("SchematicTrackerMod/" + version);
		// Minecraft erst beim Aufruf holen: NeoForge erzeugt die Mod, bevor es Minecraft gibt
		sync = new ListSync(api, () -> config, task -> Minecraft.getInstance().execute(task));
		toggleHud = Compat.key("key.schematic-tracker.toggle_hud", InputConstants.KEY_J);
		openList = Compat.key("key.schematic-tracker.open_list", InputConstants.KEY_K);
		// Ohne Standardtaste. Tastencodes immer über InputConstants: ab 26.3 sind es SDL- statt GLFW-Codes.
		restartSession = Compat.key("key.schematic-tracker.restart_session", InputConstants.UNKNOWN.getValue());
		sync.onOwnAmount((item, before, after) -> SESSION.record(item.id(), before, after, System.currentTimeMillis()));
	}

	/** Tasten, die der Loader registrieren muss. */
	public static List<KeyMapping> keys() {
		return List.of(toggleHud, openList, restartSession);
	}

	public static void tick(Minecraft mc) {
		while (toggleHud.consumeClick()) {
			config.hudVisible = !config.hudVisible;
			saveConfig();
		}
		while (restartSession.consumeClick()) {
			restartSession(mc);
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
		Notifications.tick(mc, sync.list());
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

	public static FarmSession session() {
		return SESSION;
	}

	/** Farm-Session (neu) starten; Rückmeldung als Toast */
	public static void restartSession(Minecraft mc) {
		SESSION.start(System.currentTimeMillis());
		Compat.toast(mc, Component.translatable("schematic-tracker.session.started"),
				Component.translatable("schematic-tracker.session.started_hint"));
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
