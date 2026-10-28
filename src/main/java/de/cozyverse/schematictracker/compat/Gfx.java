package de.cozyverse.schematictracker.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

/**
 * Zeichnen im HUD und in Bildschirmen. Versteckt, dass Minecraft das Zeichen-Objekt und seine Methoden
 * zwischen den Versionen umbenannt hat (GuiGraphics bis 1.21.x, GuiGraphicsExtractor ab 26.1).
 */
public final class Gfx {
	//? if >=26.1 {
	private final GuiGraphicsExtractor g;

	public Gfx(GuiGraphicsExtractor g) {
		this.g = g;
	}
	//?} else {
	/*private final GuiGraphics g;

	public Gfx(GuiGraphics g) {
		this.g = g;
	}
	*///?}

	public int width() {
		return g.guiWidth();
	}

	public int height() {
		return g.guiHeight();
	}

	public void fill(int x1, int y1, int x2, int y2, int color) {
		g.fill(x1, y1, x2, y2, color);
	}

	/** Text mit Schatten. */
	public void text(Font font, String text, int x, int y, int color) {
		//? if >=26.1 {
		g.text(font, text, x, y, color, true);
		//?} else {
		/*g.drawString(font, text, x, y, color, true);
		*///?}
	}

	/** Text mit Schatten. */
	public void text(Font font, Component text, int x, int y, int color) {
		//? if >=26.1 {
		g.text(font, text, x, y, color, true);
		//?} else {
		/*g.drawString(font, text, x, y, color, true);
		*///?}
	}

	public void centered(Font font, Component text, int x, int y, int color) {
		//? if >=26.1 {
		g.centeredText(font, text, x, y, color);
		//?} else {
		/*g.drawCenteredString(font, text, x, y, color);
		*///?}
	}

	/** Item-Icon ohne Anzahl und Haltbarkeit. */
	public void item(ItemStack stack, int x, int y) {
		//? if >=26.1 {
		g.fakeItem(stack, x, y);
		//?} else {
		/*g.renderFakeItem(stack, x, y);
		*///?}
	}

	public void tooltip(Font font, Component text, int x, int y) {
		//? if >=1.21.6 {
		g.setTooltipForNextFrame(font, text, x, y);
		//?} else {
		/*g.renderTooltip(font, text, x, y);
		*///?}
	}

	public void pushScale(float scale) {
		//? if >=1.21.6 {
		g.pose().pushMatrix();
		g.pose().scale(scale, scale);
		//?} else {
		/*g.pose().pushPose();
		g.pose().scale(scale, scale, 1);
		*///?}
	}

	public void popScale() {
		//? if >=1.21.6 {
		g.pose().popMatrix();
		//?} else {
		/*g.pose().popPose();
		*///?}
	}
}
