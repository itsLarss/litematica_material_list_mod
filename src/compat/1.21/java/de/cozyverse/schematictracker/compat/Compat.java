package de.cozyverse.schematictracker.compat;

import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Versionsunterschiede für Minecraft 1.21 – 1.21.5. */
public final class Compat {
	private Compat() {}

	public static Item item(String id) {
		ResourceLocation key = ResourceLocation.tryParse(id);
		return key == null ? Items.AIR : BuiltInRegistries.ITEM.getOptional(key).orElse(Items.AIR);
	}

	public static void registerHud(Consumer<GuiGraphics> hud) {
		HudRenderCallback.EVENT.register((g, delta) -> hud.accept(g));
	}

	public static void pushScale(GuiGraphics g, float scale) {
		g.pose().pushPose();
		g.pose().scale(scale, scale, 1);
	}

	public static void popScale(GuiGraphics g) {
		g.pose().popPose();
	}

	public static void tooltip(GuiGraphics g, Font font, Component text, int x, int y) {
		g.renderTooltip(font, text, x, y);
	}

	public static KeyMapping key(String name, int key) {
		return new KeyMapping(name, key, "key.category.schematic-tracker.main");
	}

	public static <T> CycleButton.Builder<T> cycle(Function<T, Component> label, T initial) {
		return CycleButton.builder(label).withInitialValue(initial);
	}
}
