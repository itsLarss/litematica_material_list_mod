package app.schematictracker.screen;

import app.schematictracker.SchematicTrackerClient;
import app.schematictracker.Texts;
import app.schematictracker.compat.Compat;
import app.schematictracker.compat.CompatScreen;
import app.schematictracker.compat.Gfx;
import app.schematictracker.core.ListSync;
import app.schematictracker.core.ListView;
import app.schematictracker.core.TrackedItem;
import app.schematictracker.core.TrackedList;
import app.schematictracker.core.TrackerConfig;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Ganze Liste zum Scrollen; Klick auf ein Material = "Ich sammle das" setzen bzw. freigeben. */
public final class TrackerListScreen extends CompatScreen {
	private static final int ROW = 20;
	private static final int TOP = 56;

	private final Screen parent;
	private int scroll;
	private List<TrackedItem> rows = List.of();
	private Component message = Component.empty();

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
		return Math.max(1, (height - TOP - 36) / ROW);
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
	}

	@Override
	public void tick() {
		TrackerConfig c = SchematicTrackerClient.config();
		TrackedList list = SchematicTrackerClient.sync().list();
		rows = list == null
				? List.of()
				: ListView.select(list.items(), c.filter, c.sort, SchematicTrackerClient.playerName(), Texts::name);
		scroll = Math.clamp(scroll, 0, Math.max(0, rows.size() - visibleRows()));
	}

	@Override
	protected void draw(Gfx g, int mouseX, int mouseY, float delta) {
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
			if (hover) {
				g.fill(x, y, x + w, y + ROW, 0x30FFFFFF);
				hovered = item;
			}
			ItemStack stack = Texts.stack(item);
			if (!stack.isEmpty()) g.item(stack, x + 4, y + 2);
			boolean mine = item.claimedBy(player);
			g.text(font, Texts.name(item), x + 24, y + 6, item.complete() ? 0xFF55FF55 : mine ? 0xFFFFFF80 : 0xFFFFFFFF);
			String right = item.complete()
					? Component.translatable("schematic-tracker.hud.done").getString()
					: Texts.missing(item, c.unit);
			int rightX = x + w - 4 - font.width(right);
			g.text(font, right, rightX, y + 6, item.complete() ? 0xFF55FF55 : 0xFFFFAA00);
			if (item.claimedBy() != null) {
				String who = mine ? Component.translatable("schematic-tracker.list.you").getString() : item.claimedBy();
				g.text(font, who, rightX - 8 - font.width(who), y + 6, 0xFFAAAAAA);
			}
		}
		if (rows.isEmpty()) {
			g.centered(font, Component.translatable(c.filter == ListView.Filter.MINE
					? "schematic-tracker.hud.none_mine" : "schematic-tracker.hud.all_done"), width / 2, TOP + 10, 0xFFAAAAAA);
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

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scroll = Math.clamp(scroll - (int) Math.signum(scrollY) * 3, 0, Math.max(0, rows.size() - visibleRows()));
		return true;
	}

	/** Klick, den kein Knopf genommen hat (siehe CompatScreen). */
	@Override
	protected boolean clicked(double mouseX, double mouseY) {
		TrackedList list = SchematicTrackerClient.sync().list();
		int x = left();
		if (list == null || !list.canEdit() || mouseX < x || mouseX >= x + panelWidth() || mouseY < TOP) {
			return false;
		}
		int index = scroll + (int) ((mouseY - TOP) / ROW);
		if (index < scroll || index >= Math.min(rows.size(), scroll + visibleRows())) return false;
		TrackedItem item = rows.get(index);
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

	@Override
	public void onClose() {
		Compat.setScreen(minecraft, parent);
	}
}
