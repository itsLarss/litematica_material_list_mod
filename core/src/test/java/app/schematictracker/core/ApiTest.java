package app.schematictracker.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Gegen einen kleinen Fake-Server mit denselben Antworten wie die echte API v1. */
class ApiTest {
	private HttpServer server;
	private String base;
	private final Queue<String> seen = new ConcurrentLinkedQueue<>();
	private volatile long version = 1;
	private volatile int glassHave = 0;

	private static final String TOKEN = "0QCN-ET1SdFps2fxvILJ2B3N";

	@BeforeEach
	void start() throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/api/v1/", ex -> {
			String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			String path = ex.getRequestURI().toString();
			String token = ex.getRequestHeaders().getFirst("X-Share-Token");
			seen.add(ex.getRequestMethod() + " " + path + " " + body + " ua=" + ex.getRequestHeaders().getFirst("User-Agent"));
			int status = 200;
			String out;
			if (path.equals("/api/v1/pair")) {
				out = body.contains("K7QM4XPA")
						? "{\"apiVersion\":1,\"token\":\"" + TOKEN + "\",\"listId\":\"p1\",\"name\":\"Kirche\",\"role\":\"editor\"}"
						: null;
				if (out == null) {
					status = 404;
					out = "{\"error\":\"Not found.\"}";
				}
			} else if (!TOKEN.equals(token)) {
				status = 404;
				out = "{\"error\":\"Not found.\"}";
			} else if (path.startsWith("/api/v1/list/items")) {
				java.util.regex.Matcher have = java.util.regex.Pattern.compile("\"have\":(\\d+)").matcher(body);
				glassHave = have.find() ? Integer.parseInt(have.group(1)) : 64;
				version++;
				out = "{\"ok\":true}";
			} else if (path.startsWith("/api/v1/list")) {
				boolean partial = path.contains("since=");
				String glass = "{\"id\":\"i2\",\"name\":\"Glass\",\"itemId\":\"minecraft:glass\",\"total\":640,\"have\":" + glassHave
						+ ",\"stackSize\":64,\"farmed\":false,\"claimedBy\":null}";
				String stone = "{\"id\":\"i1\",\"name\":\"Stone\",\"itemId\":\"minecraft:stone\",\"total\":100,\"have\":0,"
						+ "\"stackSize\":64,\"farmed\":false,\"claimedBy\":\"Lars\",\"note\":\"im Nether holen\"}";
				String items = partial ? (path.endsWith("since=" + version) ? "" : glass) : stone + "," + glass;
				out = "{\"apiVersion\":1,\"id\":\"p1\",\"name\":\"Kirche\",\"role\":\"editor\",\"version\":" + version
						+ ",\"partial\":" + partial + ",\"note\":\"Basis bei 120 64 -300\",\"deadline\":\"2026-12-24\""
						+ ",\"deadlineStatus\":{\"status\":\"tight\",\"daysLeft\":57,\"forecast\":\"2026-12-28\"}"
						+ ",\"items\":[" + items + "]}";
			} else {
				status = 404;
				out = "{\"error\":\"Not found.\"}";
			}
			byte[] bytes = out.getBytes(StandardCharsets.UTF_8);
			ex.getResponseHeaders().add("Content-Type", "application/json");
			ex.sendResponseHeaders(status, bytes.length);
			ex.getResponseBody().write(bytes);
			ex.close();
		});
		server.start();
		base = "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@AfterEach
	void stop() {
		server.stop(0);
	}

	@Test
	void pairsReadsAndWrites() throws Exception {
		TrackerApi api = new TrackerApi("SchematicTrackerTest/1.0");
		TrackerApi.Pairing pairing = api.pair(base, "K7QM4XPA").get(5, TimeUnit.SECONDS);
		assertEquals(TOKEN, pairing.token());
		assertEquals("editor", pairing.role());

		try {
			api.pair(base, "AAAAAAAA").get(5, TimeUnit.SECONDS);
		} catch (ExecutionException e) {
			assertEquals(404, ListSync.statusOf(e.getCause()));
		}

		TrackerApi.ListResponse full = api.list(base, TOKEN, -1).get(5, TimeUnit.SECONDS);
		assertEquals(2, full.items().size());
		assertEquals("minecraft:glass", full.items().get(1).itemId());
		assertEquals("Lars", full.items().get(0).claimedBy());
		// Notizen (später in v1 dazugekommen); Glass hat kein Feld → null wie bei älteren Websites
		assertEquals("Basis bei 120 64 -300", full.note());
		assertEquals("im Nether holen", full.items().get(0).note());
		assertEquals(null, full.items().get(1).note());
		assertEquals(new Deadline("2026-12-24", "tight", 57, "2026-12-28"), full.deadline());
		// Website ohne Status-Feld: nur das Datum; ohne Deadline: null
		com.google.gson.JsonObject plain = com.google.gson.JsonParser.parseString("{\"deadline\":\"2026-12-24\"}").getAsJsonObject();
		assertEquals(new Deadline("2026-12-24", null, null, null), TrackerApi.parseDeadline(plain));
		assertEquals(null, TrackerApi.parseDeadline(com.google.gson.JsonParser.parseString("{\"deadline\":null}").getAsJsonObject()));
		assertEquals(null, TrackerApi.parseDeadline(new com.google.gson.JsonObject()));

		api.saveAmounts(base, TOKEN, List.of(new TrackerApi.AmountChange("i2", 64, false)), "Steve").get(5, TimeUnit.SECONDS);
		TrackerApi.ListResponse changes = api.list(base, TOKEN, 1).get(5, TimeUnit.SECONDS);
		assertTrue(changes.partial());
		assertEquals(1, changes.items().size());
		assertEquals(64, changes.items().get(0).have());
		assertTrue(seen.stream().anyMatch(s -> s.startsWith("PATCH /api/v1/list/items") && s.contains("\"actor\":\"Steve\"")));
		assertTrue(seen.stream().allMatch(s -> s.endsWith("ua=SchematicTrackerTest/1.0")));
		// Der Token steht nie in der URL.
		assertTrue(seen.stream().noneMatch(s -> s.contains(TOKEN)));
	}

	@Test
	void syncPollsOnlyChangesAndMarksGone() throws Exception {
		TrackerApi api = new TrackerApi("SchematicTrackerTest/1.0");
		TrackerConfig config = new TrackerConfig();
		config.server = base;
		config.token = TOKEN;
		ListSync sync = new ListSync(api, () -> config, Runnable::run);

		sync.tick(0, true);
		waitFor(() -> sync.list() != null);
		assertEquals(ListSync.Status.OK, sync.status());
		assertEquals(0, sync.list().item("i2").have());
		assertEquals("Basis bei 120 64 -300", sync.list().note());
		assertTrue(sync.list().item("i1").hasNote());

		glassHave = 128;
		version++;
		sync.refresh();
		sync.tick(0, true);
		waitFor(() -> sync.list().item("i2").have() == 128);
		assertTrue(seen.stream().anyMatch(s -> s.startsWith("GET /api/v1/list?since=1")));

		config.token = "xxxxxxxxxxxxxxxxxxxxxxxxxxxx";
		sync.tick(0, true); // anderer Token: alter Stand wird verworfen
		waitFor(() -> sync.status() == ListSync.Status.GONE);
		assertEquals(null, sync.list());

		config.token = "";
		sync.tick(0, true);
		assertEquals(ListSync.Status.UNLINKED, sync.status());
		assertNotNull(sync);
	}

	@Test
	void savesOwnAmountsBatchedAndKeepsThemOverPolls() throws Exception {
		TrackerApi api = new TrackerApi("SchematicTrackerTest/1.0");
		TrackerConfig config = new TrackerConfig();
		config.server = base;
		config.token = TOKEN;
		ListSync sync = new ListSync(api, () -> config, Runnable::run);
		sync.tick(System.currentTimeMillis(), true);
		waitFor(() -> sync.list() != null);

		TrackedItem glass = sync.list().item("i2");
		assertTrue(sync.setHave(glass, 100, "Steve"));
		assertTrue(sync.setHave(sync.list().item("i2"), 200, "Steve"));
		// Sofort lokal sichtbar, noch nichts gesendet
		assertEquals(200, sync.list().item("i2").have());
		assertTrue(sync.savePending());
		assertTrue(seen.stream().noneMatch(s -> s.startsWith("PATCH")));

		// Ein Abruf mit altem Serverstand überschreibt die eigene Eingabe nicht
		sync.refresh();
		sync.tick(System.currentTimeMillis(), true);
		waitFor(() -> seen.stream().filter(s -> s.startsWith("GET")).count() >= 2);
		assertEquals(200, sync.list().item("i2").have());

		// Nach der Verzögerung: ein PATCH mit dem letzten Wert
		Thread.sleep(ListSync.SAVE_DELAY_MS + 50);
		sync.tick(System.currentTimeMillis(), true);
		waitFor(() -> !sync.savePending());
		List<String> patches = seen.stream().filter(s -> s.startsWith("PATCH /api/v1/list/items")).toList();
		assertEquals(1, patches.size());
		assertTrue(patches.get(0).contains("\"have\":200") && patches.get(0).contains("\"actor\":\"Steve\""));
		assertEquals(200, glassHave);
		for (int i = 0; i < 3; i++) {
			sync.tick(System.currentTimeMillis(), true);
			Thread.sleep(100);
		}
		assertEquals(200, sync.list().item("i2").have());

		// Nur mit Mitbearbeiten-Recht
		TrackerConfig viewer = new TrackerConfig();
		assertEquals(false, new ListSync(api, () -> viewer, Runnable::run).setHave(glass, 5, "Steve"));
	}

	private static void waitFor(java.util.function.BooleanSupplier condition) throws InterruptedException {
		for (int i = 0; i < 200 && !condition.getAsBoolean(); i++) Thread.sleep(25);
		assertTrue(condition.getAsBoolean(), "condition not reached");
	}
}
