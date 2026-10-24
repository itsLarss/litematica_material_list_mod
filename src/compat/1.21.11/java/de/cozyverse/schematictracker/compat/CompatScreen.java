package de.cozyverse.schematictracker.compat;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Bildschirm mit einheitlicher Klick-Methode (ab 1.21.9: mouseClicked mit MouseButtonEvent). */
public abstract class CompatScreen extends Screen {
	protected CompatScreen(Component title) {
		super(title);
	}

	/** Linksklick, den kein Knopf genommen hat. */
	protected abstract boolean clicked(double mouseX, double mouseY);

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		return event.button() == 0 && clicked(event.x(), event.y());
	}
}
