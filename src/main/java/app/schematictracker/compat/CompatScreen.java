package app.schematictracker.compat;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
//? if >=1.21.9 {
import net.minecraft.client.input.MouseButtonEvent;
//?}

/** Bildschirm mit einheitlichem Zeichnen und Klicken über alle Minecraft-Versionen. */
public abstract class CompatScreen extends Screen {
	protected CompatScreen(Component title) {
		super(title);
	}

	/** Eigene Inhalte über den Knöpfen zeichnen. */
	protected abstract void draw(Gfx g, int mouseX, int mouseY, float delta);

	/** Linksklick, den kein Knopf genommen hat. */
	protected boolean clicked(double mouseX, double mouseY) {
		return false;
	}

	//? if >=26.1 {
	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		draw(new Gfx(g), mouseX, mouseY, delta);
	}
	//?} else {
	/*@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		draw(new Gfx(g), mouseX, mouseY, delta);
	}
	*///?}

	//? if >=1.21.9 {
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		return event.button() == 0 && clicked(event.x(), event.y());
	}
	//?} else {
	/*@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;
		return button == 0 && clicked(mouseX, mouseY);
	}
	*///?}
}
