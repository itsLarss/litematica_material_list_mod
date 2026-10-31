package app.schematictracker;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import app.schematictracker.compat.Compat;
import app.schematictracker.core.FarmSession;
import app.schematictracker.core.ItemMatch;
import app.schematictracker.core.ListSync;
import app.schematictracker.core.QuickAmount;
import app.schematictracker.core.TrackedItem;
import app.schematictracker.core.TrackedList;
import app.schematictracker.core.ListView;
import app.schematictracker.core.Preset;
import app.schematictracker.core.TrackerConfig;
import app.schematictracker.core.TrackerLink;
import app.schematictracker.screen.TrackerConfigScreen;
import app.schematictracker.screen.TrackerListScreen;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * /tracker link|unlink|server|status|refresh|hud|filter|sort|list|config [preset]|add|set|session
 * Unabhängig vom Loader: der Befehlsbaum ist generisch, Rückmeldungen laufen über {@link Feedback}.
 */
public final class TrackerCommands {
	private TrackerCommands() {}

	/** Rückmeldung in den Chat; jeder Loader liefert seine eigene Umsetzung. */
	public interface Feedback {
		void ok(Component message);

		void error(Component message);
	}

	public static <S> void register(CommandDispatcher<S> dispatcher, Function<S, Feedback> feedback) {
		new Builder<>(feedback).register(dispatcher);
	}

	private static final class Builder<S> {
		private final Function<S, Feedback> feedback;

		Builder(Function<S, Feedback> feedback) {
			this.feedback = feedback;
		}

		private LiteralArgumentBuilder<S> literal(String name) {
			return LiteralArgumentBuilder.literal(name);
		}

		private <T> RequiredArgumentBuilder<S, T> argument(String name, ArgumentType<T> type) {
			return RequiredArgumentBuilder.argument(name, type);
		}

		/** Vorschläge: "hand" und die IDs der offenen Materialien, danach die fertigen. */
		private SuggestionProvider<S> items() {
			return (ctx, builder) -> {
				String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
				if ("hand".startsWith(typed)) builder.suggest("hand");
				TrackedList list = SchematicTrackerClient.sync().list();
				if (list != null) {
					List<TrackedItem> sorted = new java.util.ArrayList<>(list.items());
					sorted.sort(java.util.Comparator.comparing(TrackedItem::complete));
					for (TrackedItem item : sorted) {
						String id = ItemMatch.idPath(item);
						if (!id.isEmpty() && id.startsWith(typed)) builder.suggest(id);
					}
				}
				return builder.buildFuture();
			};
		}

		/** /tracker config preset minimal|clear|all */
		private LiteralArgumentBuilder<S> presets() {
			LiteralArgumentBuilder<S> preset = literal("preset");
			for (Preset p : Preset.values()) {
				preset.then(literal(p.id()).executes(ctx -> {
					p.apply(SchematicTrackerClient.config());
					SchematicTrackerClient.saveConfig();
					out(ctx).ok(Component.translatable("schematic-tracker.preset.applied",
							Component.translatable("schematic-tracker.preset." + p.id())));
					return 1;
				}));
			}
			return preset;
		}

		private Feedback out(CommandContext<S> ctx) {
			return feedback.apply(ctx.getSource());
		}

		void register(CommandDispatcher<S> dispatcher) {
			LiteralArgumentBuilder<S> root = literal("tracker")
					.executes(ctx -> status(out(ctx)))
					.then(literal("link")
							.then(argument("code_or_link", StringArgumentType.greedyString())
									.executes(ctx -> link(out(ctx), StringArgumentType.getString(ctx, "code_or_link")))))
					.then(literal("unlink").executes(ctx -> {
						SchematicTrackerClient.config().unlink();
						SchematicTrackerClient.saveConfig();
						SchematicTrackerClient.sync().reset();
						out(ctx).ok(Component.translatable("schematic-tracker.cmd.unlinked"));
						return 1;
					}))
					.then(literal("server")
							.then(argument("url", StringArgumentType.greedyString())
									.executes(ctx -> server(out(ctx), StringArgumentType.getString(ctx, "url")))))
					.then(literal("status").executes(ctx -> status(out(ctx))))
					.then(literal("refresh").executes(ctx -> {
						SchematicTrackerClient.sync().refresh();
						out(ctx).ok(Component.translatable("schematic-tracker.cmd.refreshing"));
						return 1;
					}))
					.then(literal("hud")
							.executes(ctx -> hud(out(ctx), !SchematicTrackerClient.config().hudVisible))
							.then(literal("on").executes(ctx -> hud(out(ctx), true)))
							.then(literal("off").executes(ctx -> hud(out(ctx), false))))
					.then(literal("add").then(argument("item", StringArgumentType.string()).suggests(items())
							.then(argument("amount", StringArgumentType.greedyString())
									.executes(ctx -> amount(out(ctx), StringArgumentType.getString(ctx, "item"),
											StringArgumentType.getString(ctx, "amount"), true)))))
					.then(literal("set").then(argument("item", StringArgumentType.string()).suggests(items())
							.then(argument("amount", StringArgumentType.greedyString())
									.executes(ctx -> amount(out(ctx), StringArgumentType.getString(ctx, "item"),
											StringArgumentType.getString(ctx, "amount"), false)))))
					.then(literal("session")
							.executes(ctx -> session(out(ctx)))
							.then(literal("start").executes(ctx -> {
								SchematicTrackerClient.restartSession(Minecraft.getInstance());
								out(ctx).ok(Component.translatable("schematic-tracker.session.started"));
								return 1;
							}))
							.then(literal("stop").executes(ctx -> {
								SchematicTrackerClient.session().stop();
								out(ctx).ok(Component.translatable("schematic-tracker.session.stopped"));
								return 1;
							})))
					.then(literal("list").executes(ctx -> {
						SchematicTrackerClient.openLater(new TrackerListScreen(null));
						return 1;
					}))
					.then(literal("config").executes(ctx -> {
						SchematicTrackerClient.openLater(new TrackerConfigScreen(null));
						return 1;
					}).then(presets()));

			var filter = literal("filter");
			for (ListView.Filter f : ListView.Filter.values()) {
				filter.then(literal(f.name().toLowerCase(Locale.ROOT)).executes(ctx -> setting(out(ctx),
						c -> c.filter = f, "schematic-tracker.filter." + f.name().toLowerCase(Locale.ROOT))));
			}
			var sort = literal("sort");
			for (ListView.Sort s : ListView.Sort.values()) {
				sort.then(literal(s.name().toLowerCase(Locale.ROOT)).executes(ctx -> setting(out(ctx),
						c -> c.sort = s, "schematic-tracker.sort." + s.name().toLowerCase(Locale.ROOT))));
			}
			dispatcher.register(root.then(filter).then(sort));
		}
	}

	private static int link(Feedback source, String input) {
		TrackerConfig config = SchematicTrackerClient.config();
		TrackerLink parsed = TrackerLink.parse(input);
		if (parsed == null) {
			source.error(Component.translatable("schematic-tracker.cmd.bad_link"));
			return 0;
		}
		if (parsed instanceof TrackerLink.ShareLink share) {
			config.server = share.server();
			config.token = share.token();
			config.listName = "";
			SchematicTrackerClient.saveConfig();
			SchematicTrackerClient.sync().reset();
			source.ok(Component.translatable("schematic-tracker.cmd.linked_link"));
			return 1;
		}
		TrackerLink.Code code = (TrackerLink.Code) parsed;
		if (config.server.isBlank()) {
			source.error(Component.translatable("schematic-tracker.cmd.no_server"));
			return 0;
		}
		source.ok(Component.translatable("schematic-tracker.cmd.linking"));
		pair(config.server, code.code(), source::ok, source::error);
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

	private static int server(Feedback source, String url) {
		String server = TrackerLink.normalizeServer(url);
		if (server == null) {
			source.error(Component.translatable("schematic-tracker.cmd.bad_server"));
			return 0;
		}
		TrackerConfig config = SchematicTrackerClient.config();
		if (!server.equals(config.server)) config.unlink();
		config.server = server;
		SchematicTrackerClient.saveConfig();
		SchematicTrackerClient.sync().reset();
		source.ok(Component.translatable("schematic-tracker.cmd.server_set", server));
		return 1;
	}

	private static int status(Feedback source) {
		TrackerConfig config = SchematicTrackerClient.config();
		ListSync sync = SchematicTrackerClient.sync();
		if (!config.linked()) {
			source.ok(Component.translatable("schematic-tracker.cmd.status_unlinked",
					config.server.isBlank() ? "-" : config.server));
			return 1;
		}
		String name = sync.list() != null ? sync.list().name() : config.listName;
		String state = "schematic-tracker.status." + sync.status().name().toLowerCase(Locale.ROOT);
		source.ok(Component.translatable("schematic-tracker.cmd.status", name, config.server,
				Component.translatable(state)));
		if (sync.status() == ListSync.Status.OFFLINE && sync.lastError() != null) {
			source.ok(Component.literal(sync.lastError()).withStyle(ChatFormatting.GRAY));
		}
		if (sync.list() != null && !sync.list().canEdit()) {
			source.ok(Component.translatable("schematic-tracker.cmd.read_only").withStyle(ChatFormatting.GRAY));
		}
		return 1;
	}

	/**
	 * /tracker add <item> <menge> addiert, /tracker set <item> <menge> setzt (Korrektur). Menge mit Schnelleingabe
	 * wie auf der Website ("3sb 5st", "2dk", "+64"); "hand" = Item in der Hand.
	 */
	private static int amount(Feedback source, String query, String amount, boolean add) {
		ListSync sync = SchematicTrackerClient.sync();
		TrackedList list = sync.list();
		if (!SchematicTrackerClient.config().linked()) {
			source.error(Component.translatable("schematic-tracker.config.not_linked"));
			return 0;
		}
		if (list == null) {
			source.error(Component.translatable("schematic-tracker.hud.loading"));
			return 0;
		}
		if (!list.canEdit()) {
			source.error(Component.translatable("schematic-tracker.cmd.read_only"));
			return 0;
		}
		String search = query;
		if (query.equalsIgnoreCase("hand")) {
			Minecraft mc = Minecraft.getInstance();
			ItemStack held = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();
			if (held.isEmpty()) {
				source.error(Component.translatable("schematic-tracker.cmd.hand_empty"));
				return 0;
			}
			search = Compat.itemId(held.getItem());
		}
		ItemMatch.Result found = ItemMatch.find(list.items(), search, Texts::name);
		if (found instanceof ItemMatch.NotFound) {
			source.error(Component.translatable("schematic-tracker.cmd.item_unknown", query));
			return 0;
		}
		if (found instanceof ItemMatch.Ambiguous many) {
			String names = many.candidates().stream().map(ItemMatch::idPath).collect(Collectors.joining(", "));
			source.error(Component.translatable("schematic-tracker.cmd.item_ambiguous", query, names));
			return 0;
		}
		TrackedItem item = ((ItemMatch.Found) found).item();
		String text = amount.trim();
		// add: ohne Vorzeichen wird addiert ("3sb" = +3sb), mit - abgezogen
		if (add && !text.startsWith("+") && !text.startsWith("-")) text = "+" + text;
		OptionalInt value = QuickAmount.parse(text, item.have(), item.stackSize());
		if (value.isEmpty()) {
			source.error(Component.translatable("schematic-tracker.cmd.amount_invalid", amount));
			return 0;
		}
		sync.setHave(item, value.getAsInt(), SchematicTrackerClient.playerName());
		TrackedItem now = list.item(item.id());
		source.ok(Texts.amountSaved(now));
		return 1;
	}

	/** /tracker session: Tempo und Restzeit des aktuellen Materials */
	private static int session(Feedback source) {
		FarmSession session = SchematicTrackerClient.session();
		TrackedList list = SchematicTrackerClient.sync().list();
		if (!session.running()) {
			source.ok(Component.translatable("schematic-tracker.session.none"));
			return 1;
		}
		long now = System.currentTimeMillis();
		String elapsed = FarmSession.formatDuration(session.elapsedMs(now));
		TrackedItem item = list == null || session.currentId() == null ? null : list.item(session.currentId());
		if (item == null) {
			source.ok(Component.translatable("schematic-tracker.session.idle", elapsed));
			return 1;
		}
		double rate = session.perHour(item.id(), now);
		long remaining = session.remainingMs(item, now);
		source.ok(Component.translatable("schematic-tracker.session.status", elapsed, Texts.name(item),
				session.gained(item.id()), rate < 0 ? "-" : String.valueOf(Math.round(rate)),
				remaining < 0 ? "-" : FarmSession.formatDuration(remaining)));
		return 1;
	}

	private static int hud(Feedback source, boolean visible) {
		return setting(source, c -> c.hudVisible = visible, visible ? "schematic-tracker.on" : "schematic-tracker.off");
	}

	private static int setting(Feedback source, Consumer<TrackerConfig> change, String valueKey) {
		change.accept(SchematicTrackerClient.config());
		SchematicTrackerClient.saveConfig();
		source.ok(Component.translatable("schematic-tracker.cmd.saved", Component.translatable(valueKey)));
		return 1;
	}
}
