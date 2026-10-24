package de.cozyverse.schematictracker.core;

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
