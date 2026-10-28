package app.schematictracker.screen;

import app.schematictracker.Notifications;
import app.schematictracker.SchematicTrackerClient;
import app.schematictracker.Texts;
import app.schematictracker.compat.Compat;
import app.schematictracker.compat.CompatScreen;
import app.schematictracker.compat.Gfx;
import app.schematictracker.core.ListSync;
import app.schematictracker.core.ListView;
import app.schematictracker.core.QuickAmount;
import app.schematictracker.core.TrackedItem;
import app.schematictracker.core.TrackedList;
import app.schematictracker.core.TrackerConfig;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Ganze Liste zum Scrollen. Linksklick auf ein Material = "Ich sammle das" setzen bzw. freigeben,
 * Rechtsklick = Menge eintragen (Feld mit Schnelleingabe und Knöpfen +1 Stack/SB/DK).
 */
public final class TrackerListScreen extends CompatScreen {
	private static final int ROW = 20;
	private static final int TOP = 56;
	/** Höhe des Eingabebereichs für Mengen unter der Liste */
	private static final int EDITOR = 36;
	/** Notizbereich unter der Liste: Überschrift + 3 Zeilen */
	private static final int NOTE_LINES = 3;
	private static final int NOTES = 12 + NOTE_LINES * 10 + 4;
	private static final int GLFW_KEY_ENTER = 257;
	private static final int GLFW_KEY_KP_ENTER = 335;
	private static final int GLFW_KEY_ESCAPE = 256;

	private final Screen parent;
	private int scroll;
	private List<TrackedItem> rows = List.of();
	private Component message = Component.empty();
	/** Material, dessen Menge gerade eingetragen wird (per ID, weil sich die Sortierung beim Eintragen ändern kann) */
	private String editingId;
	private EditBox amountField;
	private String amountText = "";
	private boolean notesShown;

	public TrackerListScreen(Screen parent) {
		super(Component.translatable("schematic-tracker.list.title"));
		this.parent = parent;
	}

	private int panelWidth() {
		return Math.min(420, width - 20);
	}

	private int left() {
		return (width - panelWidth()) / 2;
	}

	private int visibleRows() {
		return Math.max(1, (height - TOP - 36 - (editingId != null ? EDITOR : 0) - (hasNotes() ? NOTES : 0)) / ROW);
	}

	/** Gibt es überhaupt Notizen? Sonst bleibt der Bereich weg (Nur-Ansehen-Link, ältere Website). Einmal pro Tick. */
	private boolean hasNotes() {
		return notesShown;
	}

	private int notesTop() {
		return editorTop() + (editingId != null ? EDITOR : 0);
	}

	private int editorTop() {
		return TOP + visibleRows() * ROW + 6;
	}

	private TrackedItem editing() {
		TrackedList list = SchematicTrackerClient.sync().list();
		return editingId == null || list == null ? null : list.item(editingId);
	}

	@Override
	protected void init() {
		TrackerConfig c = SchematicTrackerClient.config();
		int w = (panelWidth() - 8) / 3;
		int x = left();
		addRenderableWidget(Compat.cycle(
						(ListView.Filter v) -> Component.translatable("schematic-tracker.filter." + v.name().toLowerCase(Locale.ROOT)),
						c.filter)
				.withValues(ListView.Filter.values())
				.create(x, 28, w, 20, Component.translatable("schematic-tracker.config.filter"), (b, v) -> {
					c.filter = v;
					scroll = 0;
					SchematicTrackerClient.saveConfig();
				}));
		addRenderableWidget(Compat.cycle(
						(ListView.Sort v) -> Component.translatable("schematic-tracker.sort." + v.name().toLowerCase(Locale.ROOT)),
						c.sort)
				.withValues(ListView.Sort.values())
				.create(x + w + 4, 28, w, 20, Component.translatable("schematic-tracker.config.sort"), (b, v) -> {
					c.sort = v;
					SchematicTrackerClient.saveConfig();
				}));
		addRenderableWidget(Button.builder(Component.translatable("schematic-tracker.list.settings"),
				b -> Compat.setScreen(minecraft, new TrackerConfigScreen(this))).bounds(x + 2 * (w + 4), 28, w, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
				.bounds(width / 2 - 100, height - 28, 200, 20).build());

		amountField = null;
		if (editing() != null) initEditor();
	}

	/** Eingabezeile: [Feld] [+1 St] [+1 SB] [+1 DK] [Setzen] [×] */
	private void initEditor() {
		int x = left();
		int y = editorTop() + 12;
		int small = 40;
		int fieldWidth = panelWidth() - 4 * (small + 4) - 64 - 4;
		amountField = new EditBox(font, x, y, fieldWidth, 20, Component.translatable("schematic-tracker.amount.field"));
		amountField.setMaxLength(40);
		amountField.setValue(amountText);
		amountField.setHint(Component.translatable("schematic-tracker.amount.hint"));
		amountField.setResponder(v -> amountText = v);
		addRenderableWidget(amountField);
		setInitialFocus(amountField);
		int bx = x + fieldWidth + 4;
		addRenderableWidget(Button.builder(Component.translatable("schematic-tracker.amount.plus", unitLabel("stacks")),
				b -> addUnits(1)).bounds(bx, y, small, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("schematic-tracker.amount.plus", unitLabel("shulker")),
				b -> addUnits(27)).bounds(bx + small + 4, y, small, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("schematic-tracker.amount.plus", unitLabel("chest")),
				b -> addUnits(54)).bounds(bx + 2 * (small + 4), y, small, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("schematic-tracker.amount.apply"), b -> applyField())
				.bounds(bx + 3 * (small + 4), y, 64, 20).build());
		addRenderableWidget(Button.builder(Component.literal("×"), b -> closeEditor())
				.bounds(bx + 3 * (small + 4) + 68, y, small, 20).build());
	}

	private static String unitLabel(String unit) {
		return Component.translatable("schematic-tracker.unit." + unit).getString();
	}

	private void openEditor(TrackedItem item) {
		editingId = item.id();
		amountText = "";
		rebuildWidgets();
	}

	private void closeEditor() {
		editingId = null;
		amountText = "";
		rebuildWidgets();
	}

	/** +1 Stack / Shulkerbox / Doppelkiste (stacks = Anzahl Stacks) */
	private void addUnits(int stacks) {
		TrackedItem item = editing();
		if (item == null) return;
		save(item, item.have() + stacks * Math.max(1, item.stackSize()));
	}

	/** Feld übernehmen: "3sb 5st" setzt, "+64" / "-1st" rechnet relativ (wie auf der Website). */
	private void applyField() {
		TrackedItem item = editing();
		if (item == null) return;
		OptionalInt value = QuickAmount.parse(amountText, item.have(), item.stackSize());
		if (value.isEmpty()) {
			message = Component.translatable("schematic-tracker.cmd.amount_invalid", amountText).withStyle(ChatFormatting.RED);
			return;
		}
		amountText = "";
		if (amountField != null) amountField.setValue("");
		save(item, value.getAsInt());
	}

	private void save(TrackedItem item, int have) {
		ListSync sync = SchematicTrackerClient.sync();
		if (!sync.setHave(item, have, SchematicTrackerClient.playerName())) {
			message = Component.translatable("schematic-tracker.cmd.read_only").withStyle(ChatFormatting.RED);
			return;
		}
		TrackedItem now = sync.list().item(item.id());
		message = Texts.amountSaved(now);
		tick();
	}

	@Override
	public void tick() {
		TrackerConfig c = SchematicTrackerClient.config();
		TrackedList list = SchematicTrackerClient.sync().list();
		rows = list == null
				? List.of()
				: ListView.select(list.items(), c.filter, c.sort, SchematicTrackerClient.playerName(), Texts::name);
		notesShown = list != null && (list.note() != null || list.items().stream().anyMatch(TrackedItem::hasNote));
		scroll = Math.clamp(scroll, 0, Math.max(0, rows.size() - visibleRows()));
		// Material verschwunden (z. B. Liste gewechselt): Eingabe schließen
		if (editingId != null && editing() == null) closeEditor();
	}

	@Override
	protected void draw(Gfx g, int mouseX, int mouseY, float delta) {
		drawList(g, mouseX, mouseY);
		Notifications.renderConfetti(g);
	}

	private void drawList(Gfx g, int mouseX, int mouseY) {
		TrackerConfig c = SchematicTrackerClient.config();
		ListSync sync = SchematicTrackerClient.sync();
		TrackedList list = sync.list();
		String player = SchematicTrackerClient.playerName();
		int x = left();
		int w = panelWidth();

		Component heading = list == null
				? title
				: Component.literal(String.format(Texts.locale(), "%s · %.0f %%", list.name(), Math.floor(list.percent())));
		g.centered(font, heading, width / 2, 12, 0xFFFFFFFF);

		if (!c.linked() || list == null) {
			String key = !c.linked() ? "schematic-tracker.config.not_linked"
					: "schematic-tracker.status." + sync.status().name().toLowerCase(Locale.ROOT);
			g.centered(font, Component.translatable(key).withStyle(ChatFormatting.GRAY), width / 2, TOP + 10, 0xFFFFFFFF);
			return;
		}

		g.fill(x, TOP - 2, x + w, TOP + visibleRows() * ROW + 2, 0x80000000);
		TrackedItem hovered = null;
		int end = Math.min(rows.size(), scroll + visibleRows());
		for (int i = scroll; i < end; i++) {
			TrackedItem item = rows.get(i);
			int y = TOP + (i - scroll) * ROW;
			boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + ROW;
			if (item.id().equals(editingId)) {
				g.fill(x, y, x + w, y + ROW, 0x40FFAA00);
			} else if (hover) {
				g.fill(x, y, x + w, y + ROW, 0x30FFFFFF);
			}
			if (hover) hovered = item;
			ItemStack stack = Texts.stack(item);
			if (!stack.isEmpty()) g.item(stack, x + 4, y + 2);
			boolean mine = item.claimedBy(player);
			String name = Texts.name(item);
			g.text(font, name, x + 24, y + 6, item.complete() ? 0xFF55FF55 : mine ? 0xFFFFFF80 : 0xFFFFFFFF);
			if (item.hasNote()) g.text(font, "✎", x + 24 + font.width(name) + 4, y + 6, 0xFFAAAAAA);
			String right = item.complete()
					? Component.translatable("schematic-tracker.hud.done").getString()
					: Texts.missing(item, c.unit);
			int rightX = x + w - 4 - font.width(right);
			g.text(font, right, rightX, y + 6, item.complete() ? 0xFF55FF55 : 0xFFFFAA00);
			if (item.claimedBy() != null) {
				String who = mine ? Component.translatable("schematic-tracker.list.you").getString() : item.claimedBy();
				g.text(font, who, rightX - 8 - font.width(who), y + 6, mine ? 0xFFAAAAAA : Texts.claimColor(item.claimedBy()));
			}
		}
		if (rows.isEmpty()) {
			g.centered(font, Component.translatable(c.filter == ListView.Filter.MINE
					? "schematic-tracker.hud.none_mine" : "schematic-tracker.hud.all_done"), width / 2, TOP + 10, 0xFFAAAAAA);
		}

		if (hasNotes()) drawNotes(g, list, hovered != null && hovered.hasNote() ? hovered : editing());

		TrackedItem editing = editing();
		if (editing != null) {
			g.text(font, Component.translatable("schematic-tracker.amount.editing", Texts.name(editing), editing.have(),
					editing.total()), x, editorTop(), 0xFFFFFFFF);
		}

		Component hint = list.canEdit()
				? Component.translatable("schematic-tracker.list.hint")
				: Component.translatable("schematic-tracker.cmd.read_only");
		g.centered(font, message.getString().isEmpty() ? hint : message, width / 2, height - 42, 0xFFAAAAAA);

		if (hovered != null) {
			g.tooltip(font, Component.translatable("schematic-tracker.list.tooltip",
					hovered.have(), hovered.total(),
					hovered.claimedBy() == null ? "-" : hovered.claimedBy()), mouseX, mouseY);
		}
	}

	/** Volle Notiz: zum Material unter der Maus bzw. in Bearbeitung, sonst zur Liste */
	private void drawNotes(Gfx g, TrackedList list, TrackedItem item) {
		int x = left();
		int y = notesTop();
		String text;
		Component heading;
		if (item != null && item.hasNote()) {
			heading = Component.translatable("schematic-tracker.note.item", Texts.name(item));
			text = item.note();
		} else if (list.note() != null) {
			heading = Component.translatable("schematic-tracker.note.list");
			text = list.note();
		} else {
			g.text(font, Component.translatable("schematic-tracker.note.hover"), x, y, 0xFF888888);
			return;
		}
		g.text(font, heading, x, y, 0xFFAAAAAA);
		int ly = y + 12;
		for (String line : Texts.wrap(font, text, panelWidth(), NOTE_LINES)) {
			g.text(font, line, x, ly, 0xFFFFFFFF);
			ly += 10;
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scroll = Math.clamp(scroll - (int) Math.signum(scrollY) * 3, 0, Math.max(0, rows.size() - visibleRows()));
		return true;
	}

	/** Klick, den kein Knopf genommen hat (siehe CompatScreen): links = sammeln, rechts = Menge eintragen. */
	@Override
	protected boolean clicked(double mouseX, double mouseY, int button) {
		if (button != 0 && button != 1) return false;
		TrackedList list = SchematicTrackerClient.sync().list();
		int x = left();
		if (list == null || !list.canEdit() || mouseX < x || mouseX >= x + panelWidth() || mouseY < TOP) {
			return false;
		}
		int index = scroll + (int) ((mouseY - TOP) / ROW);
		if (index < scroll || index >= Math.min(rows.size(), scroll + visibleRows())) return false;
		TrackedItem item = rows.get(index);
		if (button == 1) {
			openEditor(item);
			return true;
		}
		String player = SchematicTrackerClient.playerName();
		boolean release = item.claimedBy(player);
		SchematicTrackerClient.sync().claim(item, player).whenComplete((ok, err) -> minecraft.execute(() ->
				message = err == null
						? Component.translatable(release ? "schematic-tracker.list.released" : "schematic-tracker.list.claimed",
								Texts.name(item))
						: Component.translatable("schematic-tracker.cmd.no_connection").withStyle(ChatFormatting.RED)));
		tick();
		return true;
	}

	/** Enter übernimmt das Feld, Esc schließt erst die Eingabe und dann die Liste. */
	@Override
	protected boolean key(int keyCode) {
		if (editingId == null) return false;
		if (keyCode == GLFW_KEY_ENTER || keyCode == GLFW_KEY_KP_ENTER) {
			applyField();
			return true;
		}
		if (keyCode == GLFW_KEY_ESCAPE) {
			closeEditor();
			return true;
		}
		return false;
	}

	@Override
	public void onClose() {
		Compat.setScreen(minecraft, parent);
	}
}
