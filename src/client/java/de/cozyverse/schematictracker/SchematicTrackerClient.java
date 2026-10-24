package de.cozyverse.schematictracker;

import com.mojang.blaze3d.platform.InputConstants;
import de.cozyverse.schematictracker.compat.Compat;
import de.cozyverse.schematictracker.core.ListSync;
import de.cozyverse.schematictracker.core.TrackerApi;
import de.cozyverse.schematictracker.core.TrackerConfig;
import de.cozyverse.schematictracker.screen.TrackerListScreen;
import java.io.IOException;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchematicTrackerClient implements ClientModInitializer {
	public static final String MOD_ID = "schematic-tracker";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

	private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json");
	private static TrackerConfig config;
	private static TrackerApi api;
	private static ListSync sync;
	/** Bildschirm, der nach dem Schließen des Chats geöffnet werden soll (Befehle laufen im Chat). */
	private static Screen pendingScreen;

	private static KeyMapping toggleHud;
	private static KeyMapping openList;

	@Override
	public void onInitializeClient() {
		config = TrackerConfig.load(CONFIG_FILE);
		String version = FabricLoader.getInstance().getModContainer(MOD_ID)
				.map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("dev");
		api = new TrackerApi("SchematicTrackerMod/" + version);
		sync = new ListSync(api, () -> config, Minecraft.getInstance()::execute);

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		toggleHud = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.schematic-tracker.toggle_hud", InputConstants.KEY_J, category));
		openList = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.schematic-tracker.open_list", InputConstants.KEY_K, category));

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "list"), new TrackerHud());
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> TrackerCommands.register(dispatcher));
		ClientTickEvents.END_CLIENT_TICK.register(SchematicTrackerClient::tick);
	}

	private static void tick(Minecraft mc) {
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
			config.save(CONFIG_FILE);
		} catch (IOException e) {
			LOG.warn("Could not save {}", CONFIG_FILE, e);
		}
	}
}
