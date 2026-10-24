package de.cozyverse.schematictracker.compat;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Bildschirm mit einheitlicher Klick-Methode (bis 1.21.8: mouseClicked mit Koordinaten). */
public abstract class CompatScreen extends Screen {
	protected CompatScreen(Component title) {
		super(title);
	}

	/** Linksklick, den kein Knopf genommen hat. */
	protected abstract boolean clicked(double mouseX, double mouseY);

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;
		return button == 0 && clicked(mouseX, mouseY);
	}
}
