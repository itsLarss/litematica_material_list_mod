package de.cozyverse.schematictracker.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Unterschiede zwischen den Minecraft-Versionen (ab 26.2: Bildschirm über Gui). */
public final class Compat {
	private Compat() {}

	public static Screen screen(Minecraft mc) {
		return mc.gui.screen();
	}

	public static void setScreen(Minecraft mc, Screen screen) {
		mc.gui.setScreen(screen);
	}
}
