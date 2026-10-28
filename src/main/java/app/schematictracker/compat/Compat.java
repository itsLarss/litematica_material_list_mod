package app.schematictracker.compat;

import java.util.function.Function;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Kleine Unterschiede zwischen den Minecraft-Versionen. Gemeinsamer Code ruft nur diese Methoden auf.
 * Bis 1.21.10 heißt Identifier noch Identifier (ersetzt Stonecutter automatisch).
 */
public final class Compat {
	public static final String NAMESPACE = "schematic-tracker";

	//? if >=1.21.9 {
	// NeoForge registriert Kategorien selbst (RegisterKeyMappingsEvent), doppelt registriert wirft Vanilla einen Fehler
	//? if fabric {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(id("main"));
	//?} else {
	/*private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(id("main"));
	*///?}
	//?}

	/** Eigene Kennung, damit Meldungen der Mod nicht mit Minecraft-Meldungen zusammengelegt werden */
	private static final SystemToast.SystemToastId TOAST = new SystemToast.SystemToastId();

	private Compat() {}

	/** Kleine Meldung oben rechts (Toast) */
	public static void toast(Minecraft mc, Component title, Component message) {
		//? if >=26.2 {
		SystemToast.add(mc.gui.toastManager(), TOAST, title, message);
		//?} elif >=1.21.2 {
		/*SystemToast.add(mc.getToastManager(), TOAST, title, message);
		*///?} else {
		/*SystemToast.add(mc.getToasts(), TOAST, title, message);
		*///?}
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(NAMESPACE, path);
	}

	/** Item zur Minecraft-ID; unbekannte IDs ergeben Items.AIR. */
	public static Item item(String id) {
		Identifier key = Identifier.tryParse(id);
		return key == null ? Items.AIR : BuiltInRegistries.ITEM.getOptional(key).orElse(Items.AIR);
	}

	/** Minecraft-ID eines Items, z. B. "minecraft:stone". */
	public static String itemId(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).toString();
	}

	/** Tastenbelegung in der Kategorie "Schematic Tracker"; key immer als InputConstants.KEY_… (ab 26.3 SDL-Codes). */
	public static KeyMapping key(String name, int key) {
		//? if >=1.21.9 {
		return new KeyMapping(name, key, CATEGORY);
		//?} else {
		/*return new KeyMapping(name, key, "key.category.schematic-tracker.main");
		*///?}
	}

	//? if >=1.21.9 {
	public static KeyMapping.Category keyCategory() {
		return CATEGORY;
	}
	//?}

	public static <T> CycleButton.Builder<T> cycle(Function<T, Component> label, T initial) {
		//? if >=1.21.11 {
		return CycleButton.builder(label, initial);
		//?} else {
		/*return CycleButton.builder(label).withInitialValue(initial);
		*///?}
	}

	/** Gerade offener Bildschirm (ab 26.2 über Gui). */
	public static Screen screen(Minecraft mc) {
		//? if >=26.2 {
		return mc.gui.screen();
		//?} else {
		/*return mc.screen;
		*///?}
	}

	public static void setScreen(Minecraft mc, Screen screen) {
		//? if >=26.2 {
		mc.gui.setScreen(screen);
		//?} else {
		/*mc.setScreen(screen);
		*///?}
	}
}
