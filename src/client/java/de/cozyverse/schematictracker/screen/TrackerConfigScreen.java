package de.cozyverse.schematictracker.screen;

import de.cozyverse.schematictracker.SchematicTrackerClient;
import de.cozyverse.schematictracker.TrackerCommands;
import de.cozyverse.schematictracker.compat.Compat;
import de.cozyverse.schematictracker.core.Amounts;
import de.cozyverse.schematictracker.core.ListSync;
import de.cozyverse.schematictracker.core.ListView;
import de.cozyverse.schematictracker.core.TrackerConfig;
import de.cozyverse.schematictracker.core.TrackerLink;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Einstellungen (auch über Mod Menu): Verbindung, Filter, Sortierung, Einheit und Aussehen des HUD. */
public final class TrackerConfigScreen extends Screen {
	private final Screen parent;
	private EditBox server;
	private EditBox code;
	private EditBox playerName;
	private Component message = Component.empty();

	public TrackerConfigScreen(Screen parent) {
		super(Component.translatable("schematic-tracker.config.title"));
		this.parent = parent;
	}

	private static TrackerConfig config() {
		return SchematicTrackerClient.config();
	}

	private static Component enumLabel(String prefix, Enum<?> value) {
		return Component.translatable(prefix + value.name().toLowerCase(Locale.ROOT));
	}

	@Override
	protected void init() {
		TrackerConfig c = config();
		int w = 150;
		int gap = 8;
		int left = width / 2 - w - gap / 2;
		int right = width / 2 + gap / 2;
		int y = 32;

		// Verbindung
		server = new EditBox(font, left, y, w * 2 + gap, 20, Component.translatable("schematic-tracker.config.server"));
		server.setMaxLength(200);
		server.setValue(c.server);
		server.setHint(Component.translatable("schematic-tracker.config.server_hint"));
		addRenderableWidget(server);
		y += 24;
		code = new EditBox(font, left, y, w, 20, Component.translatable("schematic-tracker.config.code"));
		code.setMaxLength(300);
		code.setHint(Component.translatable("schematic-tracker.config.code_hint"));
		addRenderableWidget(code);
		addRenderableWidget(Button.builder(Component.translatable("schematic-tracker.config.link"), b -> link())
				.bounds(right, y, w / 2 - 2, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("schematic-tracker.config.unlink"), b -> {
			config().unlink();
			SchematicTrackerClient.saveConfig();
			SchematicTrackerClient.sync().reset();
			message = Component.translatable("schematic-tracker.cmd.unlinked");
			rebuildWidgets();
		}).bounds(right + w / 2 + 2, y, w / 2 - 2, 20).build()).active = c.linked();
		y += 34;

		// HUD
		addRenderableWidget(CycleButton.onOffBuilder(c.hudVisible)
				.create(left, y, w, 20, Component.translatable("schematic-tracker.config.hud"), (b, v) -> c.hudVisible = v));
		addRenderableWidget(CycleButton.builder((ListView.Filter v) -> enumLabel("schematic-tracker.filter.", v), c.filter)
				.withValues(ListView.Filter.values())
				.create(right, y, w, 20, Component.translatable("schematic-tracker.config.filter"), (b, v) -> c.filter = v));
		y += 22;
		addRenderableWidget(CycleButton.builder((ListView.Sort v) -> enumLabel("schematic-tracker.sort.", v), c.sort)
				.withValues(ListView.Sort.values())
				.create(left, y, w, 20, Component.translatable("schematic-tracker.config.sort"), (b, v) -> c.sort = v));
		addRenderableWidget(CycleButton.builder((Amounts.Unit v) -> enumLabel("schematic-tracker.unit_name.", v), c.unit)
				.withValues(Amounts.Unit.values())
				.create(right, y, w, 20, Component.translatable("schematic-tracker.config.unit"), (b, v) -> c.unit = v));
		y += 22;
		addRenderableWidget(CycleButton.builder((TrackerConfig.Corner v) -> enumLabel("schematic-tracker.corner.", v), c.corner)
				.withValues(TrackerConfig.Corner.values())
				.create(left, y, w, 20, Component.translatable("schematic-tracker.config.corner"), (b, v) -> c.corner = v));
		addRenderableWidget(CycleButton.onOffBuilder(c.hideInMenus)
				.create(right, y, w, 20, Component.translatable("schematic-tracker.config.hide_in_menus"),
						(b, v) -> c.hideInMenus = v));
		y += 22;
		addRenderableWidget(new IntSlider(left, y, w, "schematic-tracker.config.rows", "", 1, 30, 1, c.rows, v -> c.rows = v));
		addRenderableWidget(new IntSlider(right, y, w, "schematic-tracker.config.scale", " %", 50, 200, 10, c.scale,
				v -> c.scale = v));
		y += 22;
		addRenderableWidget(new IntSlider(left, y, w, "schematic-tracker.config.opacity", " %", 0, 100, 5, c.opacity,
				v -> c.opacity = v));
		playerName = new EditBox(font, right, y, w, 20, Component.translatable("schematic-tracker.config.player"));
		playerName.setMaxLength(40);
		playerName.setValue(c.playerName);
		playerName.setHint(Component.translatable("schematic-tracker.config.player_hint", minecraft.getUser().getName()));
		addRenderableWidget(playerName);
		y += 26;

		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
				.bounds(width / 2 - 100, Math.max(y, Math.min(height - 28, y + 8)), 200, 20).build());
	}

	private void link() {
		String input = code.getValue();
		TrackerLink parsed = TrackerLink.parse(input);
		applyServer();
		if (parsed instanceof TrackerLink.ShareLink share) {
			config().server = share.server();
			config().token = share.token();
			config().listName = "";
			SchematicTrackerClient.saveConfig();
			SchematicTrackerClient.sync().reset();
			message = Component.translatable("schematic-tracker.cmd.linked_link").withStyle(ChatFormatting.GREEN);
			rebuildWidgets();
		} else if (parsed instanceof TrackerLink.Code c) {
			if (config().server.isBlank()) {
				message = Component.translatable("schematic-tracker.cmd.no_server").withStyle(ChatFormatting.RED);
				return;
			}
			message = Component.translatable("schematic-tracker.cmd.linking");
			TrackerCommands.pair(config().server, c.code(), ok -> {
				message = ok;
				if (minecraft != null && Compat.screen(minecraft) == this) rebuildWidgets();
			}, fail -> message = fail.copy().withStyle(ChatFormatting.RED));
		} else {
			message = Component.translatable("schematic-tracker.cmd.bad_link").withStyle(ChatFormatting.RED);
		}
	}

	/** Server aus dem Feld übernehmen; ein anderer Server trennt die alte Verbindung. */
	private void applyServer() {
		String value = server.getValue().trim();
		String normalized = value.isEmpty() ? "" : TrackerLink.normalizeServer(value);
		if (normalized == null) return;
		if (!normalized.equals(config().server)) {
			config().unlink();
			config().server = normalized;
			SchematicTrackerClient.sync().reset();
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		g.centeredText(font, title, width / 2, 8, 0xFFFFFFFF);
		TrackerConfig c = config();
		Component state;
		if (!c.linked()) {
			state = Component.translatable("schematic-tracker.config.not_linked").withStyle(ChatFormatting.GRAY);
		} else {
			ListSync sync = SchematicTrackerClient.sync();
			String name = sync.list() != null ? sync.list().name() : c.listName;
			state = Component.translatable("schematic-tracker.config.linked_to", name,
					Component.translatable("schematic-tracker.status." + sync.status().name().toLowerCase(Locale.ROOT)));
		}
		g.centeredText(font, state, width / 2, 20, 0xFFFFFFFF);
		g.centeredText(font, message, width / 2, 32 + 24 + 23, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		applyServer();
		config().playerName = playerName.getValue().trim();
		SchematicTrackerClient.saveConfig();
		Compat.setScreen(minecraft, parent);
	}
}
