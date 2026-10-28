package app.schematictracker.screen;

import java.util.function.IntConsumer;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/** Schieberegler für ganze Zahlen mit Schrittweite, z. B. "Zeilen: 8". */
final class IntSlider extends AbstractSliderButton {
	private final String labelKey;
	private final String suffix;
	private final int min;
	private final int max;
	private final int step;
	private final IntConsumer onChange;

	IntSlider(int x, int y, int width, String labelKey, String suffix, int min, int max, int step, int value,
			IntConsumer onChange) {
		super(x, y, width, 20, Component.empty(), (value - min) / (double) (max - min));
		this.labelKey = labelKey;
		this.suffix = suffix;
		this.min = min;
		this.max = max;
		this.step = step;
		this.onChange = onChange;
		updateMessage();
	}

	private int current() {
		int raw = min + (int) Math.round(value * (max - min));
		return Math.clamp(Math.round(raw / (float) step) * step, min, max);
	}

	@Override
	protected void updateMessage() {
		setMessage(Component.translatable(labelKey).append(": " + current() + suffix));
	}

	@Override
	protected void applyValue() {
		onChange.accept(current());
	}
}
