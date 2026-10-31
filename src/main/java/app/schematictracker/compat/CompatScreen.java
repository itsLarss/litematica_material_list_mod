package app.schematictracker.compat;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}

/** Bildschirm mit einheitlichem Zeichnen und Klicken über alle Minecraft-Versionen. */
public abstract class CompatScreen extends Screen {
	protected CompatScreen(Component title) {
		super(title);
	}

	/** Eigene Inhalte über den Knöpfen zeichnen. */
	protected abstract void draw(Gfx g, int mouseX, int mouseY, float delta);

	/** Klick, den kein Knopf genommen hat; button mit InputConstants.MOUSE_BUTTON_… vergleichen (ab 26.3 andere Zahlen). */
	protected boolean clicked(double mouseX, double mouseY, int button) {
		return false;
	}

	/** Taste gedrückt (Code wie InputConstants.KEY_…, ab 26.3 SDL statt GLFW), bevor der Bildschirm sie verarbeitet; true = verbraucht. */
	protected boolean key(int keyCode) {
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
		return clicked(event.x(), event.y(), event.button());
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		return key(event.key()) || super.keyPressed(event);
	}
	//?} else {
	/*@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;
		return clicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return key(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
	}
	*///?}
}
