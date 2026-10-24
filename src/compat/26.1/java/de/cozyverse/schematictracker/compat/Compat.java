package de.cozyverse.schematictracker.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Unterschiede zwischen den Minecraft-Versionen (26.1.x: Bildschirm direkt an Minecraft). */
public final class Compat {
	private Compat() {}

	public static Screen screen(Minecraft mc) {
		return mc.screen;
	}

	public static void setScreen(Minecraft mc, Screen screen) {
		mc.setScreen(screen);
	}
}
