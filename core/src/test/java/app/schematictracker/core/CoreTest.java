package app.schematictracker.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CoreTest {
	private static TrackedItem item(String id, String name, int total, int have, boolean farmed, String claim, int pos) {
		return new TrackedItem(id, name, "minecraft:" + name.toLowerCase().replace(' ', '_'), total, have, 64, farmed, claim, pos);
	}

	@Test
	void parsesCodesAndLinks() {
		assertEquals(new TrackerLink.Code("K7QM4XPA"), TrackerLink.parse(" k7qm-4xpa "));
		assertEquals(new TrackerLink.Code("K7QM4XPA"), TrackerLink.parse("K7QM4XPA"));
		TrackerLink link = TrackerLink.parse("https://tracker.example/s/0QCN-ET1SdFps2fxvILJ2B3N");
		assertEquals(new TrackerLink.ShareLink("https://tracker.example", "0QCN-ET1SdFps2fxvILJ2B3N"), link);
		assertInstanceOf(TrackerLink.ShareLink.class, TrackerLink.parse("http://localhost:3000/s/0QCN-ET1SdFps2fxvILJ2B3N/"));
		assertNull(TrackerLink.parse("K7QM-4XP0")); // 0 kommt in Codes nicht vor
		assertNull(TrackerLink.parse("https://tracker.example/e/0QCN-ET1SdFps2fxvILJ2B3N")); // Besitzer-Link nie
		assertNull(TrackerLink.parse("hello"));
		assertEquals("https://tracker.example", TrackerLink.normalizeServer("tracker.example/"));
		assertNull(TrackerLink.normalizeServer("ftp://x"));
	}

	@Test
	void parsesQuickAmountsLikeTheWebsite() {
		// Gleiche Fälle wie tests/features.test.ts der Website
		Object[][] cases = {
				{"3sb 5st 12", 0, 64, 3 * 1728 + 5 * 64 + 12},
				{"2dk", 0, 64, 2 * 54 * 64},
				{"1 kiste", 0, 16, 27 * 16},
				{"1,5sb", 0, 64, 2592},
				{"+64", 100, 64, 164},
				{"-1st", 100, 64, 36},
				{"-5st", 100, 64, 0},
				{"12 stk", 0, 64, 12},
				{"42", 7, 64, 42},
				{"+1 SB", 0, 16, 27 * 16},
		};
		for (Object[] c : cases) {
			assertEquals((int) c[3], QuickAmount.parse((String) c[0], (int) c[1], (int) c[2]).orElse(-1), (String) c[0]);
		}
		assertTrue(QuickAmount.parse("abc", 0, 64).isEmpty());
		assertTrue(QuickAmount.parse("3xx", 0, 64).isEmpty());
		assertTrue(QuickAmount.parse("", 0, 64).isEmpty());
		assertEquals(QuickAmount.MAX, QuickAmount.parse("99999999999", 0, 64).orElse(-1));
	}

	@Test
	void findsItemsForCommands() {
		List<TrackedItem> items = List.of(
				item("a", "Smooth Quartz Block", 10, 0, false, null, 0),
				item("b", "Stone", 10, 0, false, null, 1),
				item("c", "Stone Bricks", 10, 0, false, null, 2));
		java.util.function.Function<TrackedItem, String> german = i -> i.name().equals("Stone") ? "Stein" : i.name();
		assertEquals("a", ((ItemMatch.Found) ItemMatch.find(items, "minecraft:smooth_quartz_block", german)).item().id());
		assertEquals("a", ((ItemMatch.Found) ItemMatch.find(items, "Smooth Quartz", german)).item().id());
		// Genauer Treffer schlägt Teiltreffer ("stone" steckt auch in "stone_bricks")
		assertEquals("b", ((ItemMatch.Found) ItemMatch.find(items, "stone", german)).item().id());
		assertEquals("b", ((ItemMatch.Found) ItemMatch.find(items, "Stein", german)).item().id());
		assertEquals("c", ((ItemMatch.Found) ItemMatch.find(items, "bricks", german)).item().id());
		assertInstanceOf(ItemMatch.Ambiguous.class, ItemMatch.find(items, "st", german));
		assertInstanceOf(ItemMatch.NotFound.class, ItemMatch.find(items, "glass", german));
		assertInstanceOf(ItemMatch.NotFound.class, ItemMatch.find(items, " ", german));
	}

	@Test
	void detectsCompletedItemsAndList() {
		TrackedList list = new TrackedList("p1", "Kirche", "editor", 1, List.of(
				item("a", "Stone", 10, 10, false, null, 0),
				item("b", "Glass", 10, 0, false, null, 1),
				item("c", "Sand", 10, 0, false, null, 2)));
		Completions completions = new Completions();
		// Erstes Laden: schon fertige Materialien melden sich nicht
		assertTrue(completions.update(list).isEmpty());

		list.replace(item("b", "Glass", 10, 10, false, null, 1));
		Completions.Update u = completions.update(list);
		assertEquals(List.of("b"), u.completed().stream().map(TrackedItem::id).toList());
		assertFalse(u.listDone());
		assertTrue(completions.update(list).isEmpty()); // nur einmal

		// Wieder weniger, dann erneut voll: meldet sich wieder
		list.replace(item("b", "Glass", 10, 5, false, null, 1));
		assertTrue(completions.update(list).isEmpty());
		list.replace(item("b", "Glass", 10, 10, false, null, 1));
		assertEquals(1, completions.update(list).completed().size());

		// Letztes Material (als gefarmt markiert) → Liste fertig
		list.replace(item("c", "Sand", 10, 3, true, null, 2));
		u = completions.update(list);
		assertEquals(1, u.completed().size());
		assertTrue(u.listDone());
		assertFalse(completions.update(list).listDone());

		// Andere Liste: nur merken
		TrackedList other = new TrackedList("p2", "Turm", "editor", 1, List.of(item("x", "Stone", 1, 1, false, null, 0)));
		assertTrue(completions.update(other).isEmpty());
		assertTrue(completions.update(null).isEmpty());
	}

	@Test
	void estimatesFarmRateAndRemainingTime() {
		FarmSession session = new FarmSession();
		assertFalse(session.running());
		long t0 = 1_000_000;
		// Erster eigener Eintrag startet die Session
		session.record("stone", 0, 640, t0);
		assertTrue(session.running());
		assertEquals("stone", session.currentId());
		assertEquals(-1, session.perHour("stone", t0 + 30_000), 1e-9); // unter einer Minute keine Schätzung

		session.record("stone", 640, 1280, t0 + 30 * 60_000);
		assertEquals(2560, session.perHour("stone", t0 + 30 * 60_000), 1e-6); // 1280 in 30 min
		session.record("stone", 1280, 1000, t0 + 30 * 60_000); // Korrektur nach unten zählt nicht
		assertEquals(1280, session.gained("stone"));

		TrackedItem stone = new TrackedItem("stone", "Stone", "minecraft:stone", 3840, 1280, 64, false, null, 0);
		assertEquals(60 * 60_000, session.remainingMs(stone, t0 + 30 * 60_000)); // 2560 fehlen bei 2560/h
		assertEquals(0, session.remainingMs(new TrackedItem("x", "X", null, 5, 5, 64, false, null, 0), t0));
		assertEquals(-1, session.remainingMs(new TrackedItem("glass", "Glass", null, 5, 0, 64, false, null, 0), t0));

		session.record("glass", 0, 64, t0 + 31 * 60_000);
		assertEquals("glass", session.currentId());
		session.start(t0 + 40 * 60_000); // Reset
		assertEquals(0, session.gained("stone"));
		assertNull(session.currentId());

		assertEquals("< 1 min", FarmSession.formatDuration(20_000));
		assertEquals("45 min", FarmSession.formatDuration(45 * 60_000));
		assertEquals("2 h 10 min", FarmSession.formatDuration(130 * 60_000));
		assertEquals("3 h", FarmSession.formatDuration(180 * 60_000));
	}

	@Test
	void presetsChangeOnlyDisplayOptions() {
		TrackerConfig c = new TrackerConfig();
		c.server = "https://tracker.example";
		c.token = "t";
		c.filter = ListView.Filter.MINE;
		c.corner = TrackerConfig.Corner.TOP_LEFT;
		c.playerName = "Steve";
		// Ohne Änderung entspricht die Standard-Config "Übersichtlich"
		assertTrue(Preset.CLEAR.matches(new TrackerConfig()));

		Preset.MINIMAL.apply(c);
		assertEquals(3, c.rows);
		assertFalse(c.notifyToast || c.notifySound || c.notifyConfetti || c.hudClaims || c.hudSession || c.hudNotes);
		assertTrue(Preset.MINIMAL.matches(c));
		assertFalse(Preset.ALL.matches(c));

		Preset.ALL.apply(c);
		assertEquals(12, c.rows);
		assertTrue(c.hudNotes && c.hudClaims && c.hudSession && c.hudDeadline && c.notifyConfetti);
		// Danach einzeln änderbar; Verbindung, Filter, Ecke und Name bleiben unberührt
		c.rows = 8;
		assertFalse(Preset.ALL.matches(c));
		assertEquals("https://tracker.example", c.server);
		assertEquals(ListView.Filter.MINE, c.filter);
		assertEquals(TrackerConfig.Corner.TOP_LEFT, c.corner);
		assertEquals("Steve", c.playerName);
		assertEquals("minimal", Preset.MINIMAL.id());
	}

	@Test
	void convertsUnits() {
		assertEquals(40.75, Amounts.inUnit(70416, 64, Amounts.Unit.SHULKER), 0.01);
		assertEquals(2.0, Amounts.inUnit(128, 64, Amounts.Unit.STACKS), 1e-9);
		assertEquals(1.0, Amounts.inUnit(54 * 16, 16, Amounts.Unit.CHEST), 1e-9);
	}

	@Test
	void filtersAndSorts() {
		List<TrackedItem> items = List.of(
				item("a", "Stone", 100, 10, false, null, 0),
				item("b", "Glass", 500, 0, false, "Lars", 1),
				item("c", "Dirt", 50, 50, false, null, 2),
				item("d", "Sand", 70, 70, true, "lars", 3));
		List<TrackedItem> open = ListView.select(items, ListView.Filter.OPEN, ListView.Sort.MISSING, "Lars", TrackedItem::name);
		assertEquals(List.of("b", "a"), open.stream().map(TrackedItem::id).toList());
		List<TrackedItem> mine = ListView.select(items, ListView.Filter.MINE, ListView.Sort.MISSING, "lars", TrackedItem::name);
		assertEquals(List.of("b"), mine.stream().map(TrackedItem::id).toList());
		List<TrackedItem> all = ListView.select(items, ListView.Filter.ALL, ListView.Sort.NAME, "Lars", TrackedItem::name);
		assertEquals(List.of("b", "a", "c", "d"), all.stream().map(TrackedItem::id).toList());
	}

	@Test
	void mergesPartialUpdates() {
		TrackedList list = new TrackedList("p", "Kirche", "editor", 1, List.of(
				item("a", "Stone", 100, 0, false, null, 0),
				item("b", "Glass", 10, 0, false, null, 1)));
		assertFalse(list.apply("Kirche", "editor", 1, true, List.of()));
		assertTrue(list.apply("Kirche", "editor", 2, true, List.of(item("b", "Glass", 10, 10, false, "Mia", 0))));
		assertEquals(List.of("a", "b"), list.items().stream().map(TrackedItem::id).toList());
		assertEquals(10, list.item("b").have());
		assertEquals("Mia", list.item("b").claimedBy());
		assertEquals(10 * 100.0 / 110, list.percent(), 1e-9);
		assertTrue(list.canEdit());
	}

	@Test
	void sanitizesConfig() {
		TrackerConfig c = new TrackerConfig();
		c.rows = 999;
		c.scale = 1;
		c.filter = null;
		c.sanitize();
		assertEquals(30, c.rows);
		assertEquals(50, c.scale);
		assertEquals(ListView.Filter.OPEN, c.filter);
		assertFalse(c.linked());
	}
}
