package com.etheller.warsmash.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The places of the options radio choices on the device boards and in a narrow window. */
class LauncherBoardTest {
	private static float rowWidth(final LauncherBoard board) {
		return board.contentW - (2 * board.pageFrame);
	}

	private static void assertInside(final LauncherBoard board, final LauncherBoard.RadioPlaces places,
			final float[] names) {
		final float end = rowWidth(board);
		for (int i = 0; i < names.length; i++) {
			assertTrue(places.x[i] + places.w[i] <= end, "choice " + i + " ends inside the row");
			if (i > 0) {
				assertTrue(places.x[i] >= places.x[i - 1] + places.w[i - 1], "choice " + i + " after the one before");
			}
		}
	}

	@Test
	void shortNamesKeepTheDesignSpacing() {
		final LauncherBoard tablet = new LauncherBoard(true);
		final LauncherBoard.RadioPlaces places = tablet.radioPlaces(new float[] { 60, 70, 60 }, rowWidth(tablet));
		assertFalse(places.ownLine);
		assertEquals(tablet.controlX - tablet.pageFrame, places.x[0], 0.01f);
		assertEquals(340, places.x[1] - places.x[0], 0.01f);
		assertEquals(tablet.rowH, places.height, 0.01f);
	}

	@Test
	void lockedGraphicsSetsFitTheTablet() {
		// "Classic", "Reforged HD 🔒", "Reforged DE 🔒" in the 32 px body font.
		final LauncherBoard tablet = new LauncherBoard(true);
		final float[] names = { 110, 250, 250 };
		final LauncherBoard.RadioPlaces places = tablet.radioPlaces(names, rowWidth(tablet));
		assertFalse(places.ownLine);
		for (int i = 0; i < names.length; i++) {
			assertTrue(places.w[i] >= tablet.control + LauncherBoard.RADIO_NAME_GAP + names[i], "name " + i + " whole");
		}
		assertInside(tablet, places, names);
	}

	@Test
	void lockedGraphicsSetsMoveUnderTheLabelOnAPhone() {
		// The same names in the 28 px body font of the 1600 x 720 phone board.
		final LauncherBoard phone = new LauncherBoard(false);
		final float[] names = { 96, 218, 218 };
		final LauncherBoard.RadioPlaces places = phone.radioPlaces(names, rowWidth(phone));
		assertTrue(places.ownLine);
		assertEquals(phone.labelX, places.x[0], 0.01f);
		assertTrue(places.height > phone.rowH);
		assertEquals(phone.rowH, places.height - places.choicesY, 0.01f);
		for (int i = 0; i < names.length; i++) {
			assertTrue(places.w[i] >= phone.control + LauncherBoard.RADIO_NAME_GAP + names[i], "name " + i + " whole");
		}
		assertInside(phone, places, names);
	}

	@Test
	void namesTooLongForAnyLineShareIt() {
		final LauncherBoard phone = new LauncherBoard(false);
		final float[] names = { 600, 600, 600 };
		final LauncherBoard.RadioPlaces places = phone.radioPlaces(names, rowWidth(phone));
		assertTrue(places.ownLine);
		assertInside(phone, places, names);
	}

	@Test
	void narrowWindowFollowsThePhoneRules() {
		final LauncherBoard window = LauncherBoard.window(1600, 900, 150);
		assertFalse(window.tablet);
		final float[] names = { 96, 218, 218 };
		final LauncherBoard.RadioPlaces places = window.radioPlaces(names, rowWidth(window));
		assertTrue(places.ownLine);
		assertInside(window, places, names);
	}
}
