package app.schematictracker;

import app.schematictracker.compat.Compat;
import app.schematictracker.core.Amounts;
import app.schematictracker.core.TrackedItem;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Anzeige-Hilfen: Item-Icons und -Namen in der Spielsprache, Mengen im Litematica-Stil. */
public final class Texts {
	private Texts() {}

	private static final Map<String, ItemStack> STACKS = new HashMap<>();

	/** Item zum Material; unbekannte IDs (z. B. aus neueren Versionen) ergeben ItemStack.EMPTY. */
	public static ItemStack stack(TrackedItem item) {
		return STACKS.computeIfAbsent(item.itemId() == null ? "" : item.itemId(), id -> {
			Item found = Compat.item(id);
			return found == Items.AIR ? ItemStack.EMPTY : new ItemStack(found);
		});
	}

	/** Name in der Spielsprache, sonst der englische Name aus der Liste. */
	public static String name(TrackedItem item) {
		ItemStack stack = stack(item);
		return stack.isEmpty() ? item.name() : stack.getHoverName().getString();
	}

	public static Locale locale() {
		String code = Minecraft.getInstance().options.languageCode;
		if (code == null) return Locale.ROOT;
		String[] parts = code.split("_");
		return parts.length == 2 ? Locale.of(parts[0], parts[1].toUpperCase(Locale.ROOT)) : Locale.of(parts[0]);
	}

	/** "70417 (40,75 SB)" wie bei Litematica; die Einheit kommt aus den Einstellungen. */
	public static String missing(TrackedItem item, Amounts.Unit unit) {
		int missing = item.missing();
		double inUnit = Amounts.inUnit(missing, item.stackSize(), unit);
		String label = Component.translatable("schematic-tracker.unit." + unit.name().toLowerCase(Locale.ROOT)).getString();
		return String.format(locale(), "%d (%.2f %s)", missing, inUnit, label);
	}

	/** Rückmeldung nach dem Eintragen: "Stone: 1200 / 5000 · noch 3800 (59,38 st)" bzw. "… fertig". */
	public static Component amountSaved(TrackedItem item) {
		if (item.complete()) {
			return Component.translatable("schematic-tracker.amount.saved_done", name(item), item.have(), item.total());
		}
		return Component.translatable("schematic-tracker.amount.saved", name(item), item.have(), item.total(),
				missing(item, app.schematictracker.SchematicTrackerClient.config().unit));
	}
}
