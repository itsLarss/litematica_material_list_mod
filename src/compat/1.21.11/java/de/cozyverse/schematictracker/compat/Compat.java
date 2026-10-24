package de.cozyverse.schematictracker.compat;

import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Versionsunterschiede für Minecraft 1.21.11 (Identifier heißt Identifier, neuer CycleButton-Builder). */
public final class Compat {
	private static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath("schematic-tracker", "main"));

	private Compat() {}

	public static Item item(String id) {
		Identifier key = Identifier.tryParse(id);
		return key == null ? Items.AIR : BuiltInRegistries.ITEM.getOptional(key).orElse(Items.AIR);
	}

	public static void registerHud(Consumer<GuiGraphics> hud) {
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("schematic-tracker", "list"), (g, delta) -> hud.accept(g));
	}

	public static void pushScale(GuiGraphics g, float scale) {
		g.pose().pushMatrix();
		g.pose().scale(scale, scale);
	}

	public static void popScale(GuiGraphics g) {
		g.pose().popMatrix();
	}

	public static void tooltip(GuiGraphics g, Font font, Component text, int x, int y) {
		g.setTooltipForNextFrame(font, text, x, y);
	}

	public static KeyMapping key(String name, int key) {
		return new KeyMapping(name, key, CATEGORY);
	}

	public static <T> CycleButton.Builder<T> cycle(Function<T, Component> label, T initial) {
		return CycleButton.builder(label, initial);
	}
}
