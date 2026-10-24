package de.cozyverse.schematictracker;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.cozyverse.schematictracker.core.ListSync;
import de.cozyverse.schematictracker.core.ListView;
import de.cozyverse.schematictracker.core.TrackerConfig;
import de.cozyverse.schematictracker.core.TrackerLink;
import de.cozyverse.schematictracker.screen.TrackerConfigScreen;
import de.cozyverse.schematictracker.screen.TrackerListScreen;
import java.util.Locale;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** /tracker link|unlink|server|status|refresh|hud|filter|sort|list|config */
public final class TrackerCommands {
	private TrackerCommands() {}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		LiteralArgumentBuilder<FabricClientCommandSource> root = literal("tracker")
				.executes(ctx -> status(ctx.getSource()))
				.then(literal("link")
						.then(argument("code_or_link", StringArgumentType.greedyString())
								.executes(ctx -> link(ctx.getSource(), StringArgumentType.getString(ctx, "code_or_link")))))
				.then(literal("unlink").executes(ctx -> {
					SchematicTrackerClient.config().unlink();
					SchematicTrackerClient.saveConfig();
					SchematicTrackerClient.sync().reset();
					ctx.getSource().sendFeedback(Component.translatable("schematic-tracker.cmd.unlinked"));
					return 1;
				}))
				.then(literal("server")
						.then(argument("url", StringArgumentType.greedyString())
								.executes(ctx -> server(ctx.getSource(), StringArgumentType.getString(ctx, "url")))))
				.then(literal("status").executes(ctx -> status(ctx.getSource())))
				.then(literal("refresh").executes(ctx -> {
					SchematicTrackerClient.sync().refresh();
					ctx.getSource().sendFeedback(Component.translatable("schematic-tracker.cmd.refreshing"));
					return 1;
				}))
				.then(literal("hud")
						.executes(ctx -> hud(ctx.getSource(), !SchematicTrackerClient.config().hudVisible))
						.then(literal("on").executes(ctx -> hud(ctx.getSource(), true)))
						.then(literal("off").executes(ctx -> hud(ctx.getSource(), false))))
				.then(literal("list").executes(ctx -> {
					SchematicTrackerClient.openLater(new TrackerListScreen(null));
					return 1;
				}))
				.then(literal("config").executes(ctx -> {
					SchematicTrackerClient.openLater(new TrackerConfigScreen(null));
					return 1;
				}));

		var filter = literal("filter");
		for (ListView.Filter f : ListView.Filter.values()) {
			filter.then(literal(f.name().toLowerCase(Locale.ROOT)).executes(ctx -> setting(ctx.getSource(),
					c -> c.filter = f, "schematic-tracker.filter." + f.name().toLowerCase(Locale.ROOT))));
		}
		var sort = literal("sort");
		for (ListView.Sort s : ListView.Sort.values()) {
			sort.then(literal(s.name().toLowerCase(Locale.ROOT)).executes(ctx -> setting(ctx.getSource(),
					c -> c.sort = s, "schematic-tracker.sort." + s.name().toLowerCase(Locale.ROOT))));
		}
		dispatcher.register(root.then(filter).then(sort));
	}

	private static int link(FabricClientCommandSource source, String input) {
		TrackerConfig config = SchematicTrackerClient.config();
		TrackerLink parsed = TrackerLink.parse(input);
		if (parsed == null) {
			source.sendError(Component.translatable("schematic-tracker.cmd.bad_link"));
			return 0;
		}
		if (parsed instanceof TrackerLink.ShareLink share) {
			config.server = share.server();
			config.token = share.token();
			config.listName = "";
			SchematicTrackerClient.saveConfig();
			SchematicTrackerClient.sync().reset();
			source.sendFeedback(Component.translatable("schematic-tracker.cmd.linked_link"));
			return 1;
		}
		TrackerLink.Code code = (TrackerLink.Code) parsed;
		if (config.server.isBlank()) {
			source.sendError(Component.translatable("schematic-tracker.cmd.no_server"));
			return 0;
		}
		source.sendFeedback(Component.translatable("schematic-tracker.cmd.linking"));
		pair(config.server, code.code(), msg -> source.sendFeedback(msg), msg -> source.sendError(msg));
		return 1;
	}

	/** Kopplungscode einlösen; Rückmeldung kommt auf dem Client-Thread. */
	public static void pair(String server, String code, Consumer<Component> ok, Consumer<Component> fail) {
		Minecraft mc = Minecraft.getInstance();
		SchematicTrackerClient.api().pair(server, code).whenComplete((pairing, err) -> mc.execute(() -> {
			if (err != null) {
				int status = ListSync.statusOf(err);
				if (status == 0) SchematicTrackerClient.LOG.warn("Pairing with {} failed: {}", server, err.getMessage());
				fail.accept(Component.translatable(status == 404
						? "schematic-tracker.cmd.code_invalid"
						: status == 429 ? "schematic-tracker.cmd.too_many" : "schematic-tracker.cmd.no_connection"));
				return;
			}
			TrackerConfig config = SchematicTrackerClient.config();
			config.token = pairing.token();
			config.listName = pairing.name() == null ? "" : pairing.name();
			SchematicTrackerClient.saveConfig();
			SchematicTrackerClient.sync().reset();
			ok.accept(Component.translatable("schematic-tracker.cmd.linked", config.listName).withStyle(ChatFormatting.GREEN));
		}));
	}

	private static int server(FabricClientCommandSource source, String url) {
		String server = TrackerLink.normalizeServer(url);
		if (server == null) {
			source.sendError(Component.translatable("schematic-tracker.cmd.bad_server"));
			return 0;
		}
		TrackerConfig config = SchematicTrackerClient.config();
		if (!server.equals(config.server)) config.unlink();
		config.server = server;
		SchematicTrackerClient.saveConfig();
		SchematicTrackerClient.sync().reset();
		source.sendFeedback(Component.translatable("schematic-tracker.cmd.server_set", server));
		return 1;
	}

	private static int status(FabricClientCommandSource source) {
		TrackerConfig config = SchematicTrackerClient.config();
		ListSync sync = SchematicTrackerClient.sync();
		if (!config.linked()) {
			source.sendFeedback(Component.translatable("schematic-tracker.cmd.status_unlinked",
					config.server.isBlank() ? "-" : config.server));
			return 1;
		}
		String name = sync.list() != null ? sync.list().name() : config.listName;
		String state = "schematic-tracker.status." + sync.status().name().toLowerCase(Locale.ROOT);
		source.sendFeedback(Component.translatable("schematic-tracker.cmd.status", name, config.server,
				Component.translatable(state)));
		if (sync.status() == ListSync.Status.OFFLINE && sync.lastError() != null) {
			source.sendFeedback(Component.literal(sync.lastError()).withStyle(ChatFormatting.GRAY));
		}
		if (sync.list() != null && !sync.list().canEdit()) {
			source.sendFeedback(Component.translatable("schematic-tracker.cmd.read_only").withStyle(ChatFormatting.GRAY));
		}
		return 1;
	}

	private static int hud(FabricClientCommandSource source, boolean visible) {
		return setting(source, c -> c.hudVisible = visible, visible ? "schematic-tracker.on" : "schematic-tracker.off");
	}

	private static int setting(FabricClientCommandSource source, Consumer<TrackerConfig> change, String valueKey) {
		change.accept(SchematicTrackerClient.config());
		SchematicTrackerClient.saveConfig();
		source.sendFeedback(Component.translatable("schematic-tracker.cmd.saved", Component.translatable(valueKey)));
		return 1;
	}
}
