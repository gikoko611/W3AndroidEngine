package com.etheller.warsmash.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** The slot line a launcher starts a custom game with. */
class LauncherSlotTest {
	@Test
	void parsesEverySlotOfALaunch() {
		final List<LauncherSlot> slots = LauncherSlot.parse("0:USER:2:0:3;1:COMPUTER_NORMAL:4:1:-1;");
		assertEquals(2, slots.size());
		final LauncherSlot user = slots.get(0);
		assertEquals(0, user.player);
		assertEquals("USER", user.type);
		assertTrue(user.isUser());
		assertEquals(2, user.race);
		assertEquals(0, user.team);
		assertEquals(3, user.color);
		final LauncherSlot computer = slots.get(1);
		assertFalse(computer.isUser());
		assertEquals("COMPUTER_NORMAL", computer.type);
		assertEquals(LauncherSlot.MAP_VALUE, computer.color);
	}

	@Test
	void leftOutAndNegativeNumbersKeepTheMapsOwn() {
		final LauncherSlot slot = LauncherSlot.parse(" 3 : USER : -1 ").get(0);
		assertEquals(3, slot.player);
		assertEquals(LauncherSlot.MAP_VALUE, slot.race);
		assertEquals(LauncherSlot.MAP_VALUE, slot.team);
		assertEquals(LauncherSlot.MAP_VALUE, slot.color);
		assertEquals(4, LauncherSlot.orMap(slot.race, 4));
		assertEquals(0, LauncherSlot.orMap(0, 4));
	}

	@Test
	void skipsEntriesThatAreNotSlots() {
		assertTrue(LauncherSlot.parse(null).isEmpty());
		assertTrue(LauncherSlot.parse("").isEmpty());
		final List<LauncherSlot> slots = LauncherSlot.parse("x:USER:1;2;;4::1;5:OPEN:y;6:OPEN");
		assertEquals(1, slots.size());
		assertEquals(6, slots.get(0).player);
	}

	@Test
	void firstUserIsTheLocalPlayer() {
		assertNull(LauncherSlot.firstUser("0:COMPUTER_NORMAL:1"));
		assertNull(LauncherSlot.firstUser(null));
		final LauncherSlot user = LauncherSlot.firstUser("0:COMPUTER_NORMAL:1;2:USER:-1;3:USER:4");
		assertEquals(2, user.player);
		assertEquals(LauncherSlot.MAP_VALUE, user.race);
	}

	@Test
	void ofFindsAPlayersSlot() {
		final List<LauncherSlot> slots = LauncherSlot.parse("0:USER:1;1:OPEN;1:CLOSED");
		assertSame(slots.get(0), LauncherSlot.of(slots, 0));
		assertEquals("CLOSED", LauncherSlot.of(slots, 1).type);
		assertNull(LauncherSlot.of(slots, 7));
	}

	@Test
	void writtenSlotsParseBack() {
		final StringBuilder line = new StringBuilder();
		new LauncherSlot(0, "USER", 0, -1, 5).appendTo(line);
		new LauncherSlot(1, "COMPUTER_INSANE", 3, 1, -1).appendTo(line);
		assertEquals("0:USER:0:-1:5;1:COMPUTER_INSANE:3:1:-1;", line.toString());
		final List<LauncherSlot> slots = LauncherSlot.parse(line.toString());
		assertEquals(0, slots.get(0).race);
		assertEquals(LauncherSlot.MAP_VALUE, slots.get(0).team);
		assertEquals(5, slots.get(0).color);
		assertEquals(3, slots.get(1).race);
	}
}
