package com.etheller.warsmash.viewer5.handlers.w3x.simulation.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.etheller.warsmash.testing.TestObjects;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;

public class CPlayerSelectionTest {
	private final List<String> events = new ArrayList<>();
	private final CPlayerSelection.Events recorder = (unit, selected) -> this.events
			.add((selected ? "+" : "-") + System.identityHashCode(unit));

	private static CUnit unit() {
		final CUnit unit = TestObjects.allocate(CUnit.class);
		TestObjects.set(unit, "life", 100f);
		return unit;
	}

	private static List<CUnit> list(final CUnit... units) {
		final List<CUnit> list = new ArrayList<>();
		Collections.addAll(list, units);
		return list;
	}

	private String event(final boolean selected, final CUnit unit) {
		return (selected ? "+" : "-") + System.identityHashCode(unit);
	}

	@Test
	public void scriptSelectionShowsAtOnceAndReachesTheGameWithTheCommand() {
		final CPlayerSelection selection = new CPlayerSelection();
		final CUnit a = unit();
		selection.scriptSelect(a, true);
		assertTrue(selection.isSelected(a));
		assertTrue(selection.getUnits().isEmpty());
		assertTrue(this.events.isEmpty());
		selection.apply(list(), list(a), this.recorder);
		assertEquals(list(a), selection.getUnits());
		assertEquals(List.of(event(true, a)), this.events);
	}

	@Test
	public void commandAppliedTwiceFiresOnce() {
		final CPlayerSelection selection = new CPlayerSelection();
		final CUnit a = unit();
		selection.apply(list(), list(a), this.recorder);
		selection.apply(list(), list(a), this.recorder);
		selection.apply(list(a), list(), this.recorder);
		selection.apply(list(a), list(), this.recorder);
		assertEquals(List.of(event(true, a), event(false, a)), this.events);
		assertFalse(selection.isSelected(a));
	}

	@Test
	public void selectThenDeselectInOneStepFiresBoth() {
		final CPlayerSelection selection = new CPlayerSelection();
		final CUnit c = unit();
		selection.scriptSelect(c, true);
		selection.scriptSelect(c, false);
		assertFalse(selection.isSelected(c));
		selection.apply(list(), list(c), this.recorder);
		selection.apply(list(c), list(), this.recorder);
		assertEquals(List.of(event(true, c), event(false, c)), this.events);
		assertTrue(selection.getUnits().isEmpty());
	}

	@Test
	public void deadAndHiddenUnitsLeaveAtTheNextStepWithoutEvents() {
		final CPlayerSelection selection = new CPlayerSelection();
		final CUnit dead = unit();
		final CUnit hidden = unit();
		final CUnit worker = unit();
		selection.apply(list(), list(dead, hidden, worker), this.recorder);
		this.events.clear();
		TestObjects.set(dead, "life", 0f);
		TestObjects.set(hidden, "hidden", true);
		selection.unitHidden(hidden);
		TestObjects.set(worker, "hidden", true);
		assertTrue(selection.isSelected(dead));
		selection.update(unit -> true, unit -> true, this.recorder);
		assertTrue(selection.isSelected(dead));
		assertEquals(list(dead, hidden, worker), selection.getUnits());
		selection.update(unit -> true, unit -> true, this.recorder);
		assertEquals(list(worker), selection.getUnits());
		assertFalse(selection.isSelected(dead));
		assertFalse(selection.isSelected(hidden));
		assertTrue(this.events.isEmpty());
		selection.update(unit -> false, unit -> true, this.recorder);
		selection.update(unit -> false, unit -> true, this.recorder);
		assertTrue(selection.getUnits().isEmpty());
		assertTrue(this.events.isEmpty());
	}

	@Test
	public void unitBackInSightFiresNothingMoreWhenDeselected() {
		final CPlayerSelection selection = new CPlayerSelection();
		final CUnit enemy = unit();
		selection.apply(list(), list(enemy), this.recorder);
		this.events.clear();
		for (int i = 0; i < 3; i++) {
			selection.update(unit -> true, unit -> false, this.recorder);
		}
		assertEquals(List.of(event(false, enemy)), this.events);
		for (int i = 0; i < 3; i++) {
			selection.update(unit -> true, unit -> true, this.recorder);
		}
		for (int i = 0; i < 3; i++) {
			selection.update(unit -> true, unit -> false, this.recorder);
		}
		selection.apply(list(enemy), list(), this.recorder);
		assertEquals(List.of(event(false, enemy)), this.events);
		assertTrue(selection.getUnits().isEmpty());
	}

	@Test
	public void replacedUnitLeavesWithItsReplacementTakingItsPlace() {
		final CPlayerSelection selection = new CPlayerSelection();
		final CUnit rider = unit();
		final CUnit other = unit();
		final CUnit merged = unit();
		selection.apply(list(), list(rider, other), this.recorder);
		this.events.clear();
		selection.unitReplaced(rider, merged);
		selection.unitReplaced(merged, unit());
		TestObjects.set(rider, "hidden", true);
		selection.update(unit -> false, unit -> true, this.recorder);
		assertEquals(list(rider, other), selection.getUnits());
		selection.update(unit -> false, unit -> true, this.recorder);
		assertEquals(list(other, merged), selection.getUnits());
		assertTrue(selection.isSelected(merged));
		assertTrue(this.events.isEmpty());
		selection.update(unit -> false, unit -> true, this.recorder);
		assertEquals(list(other, merged), selection.getUnits());
	}

	@Test
	public void unitOutOfSightFiresTheDeselectionOnceAndStays() {
		final CPlayerSelection selection = new CPlayerSelection();
		final CUnit enemy = unit();
		selection.apply(list(), list(enemy), this.recorder);
		this.events.clear();
		selection.update(unit -> true, unit -> false, this.recorder);
		assertTrue(this.events.isEmpty());
		selection.update(unit -> true, unit -> false, this.recorder);
		selection.update(unit -> true, unit -> false, this.recorder);
		assertEquals(List.of(event(false, enemy)), this.events);
		assertTrue(selection.isSelected(enemy));
		assertEquals(list(enemy), selection.getUnits());
		selection.apply(list(enemy), list(), this.recorder);
		assertEquals(1, this.events.size());
		assertTrue(selection.getUnits().isEmpty());
	}

	@Test
	public void deadUnitsCannotBeSelectedAndTheSelectionHasALimit() {
		final CPlayerSelection selection = new CPlayerSelection();
		final CUnit dead = unit();
		TestObjects.set(dead, "life", 0f);
		selection.scriptSelect(dead, true);
		selection.apply(list(), list(dead), this.recorder);
		assertFalse(selection.isSelected(dead));
		assertTrue(this.events.isEmpty());
		final List<CUnit> many = new ArrayList<>();
		for (int i = 0; i <= WarsmashConstants.MAX_SELECTION_SIZE; i++) {
			many.add(unit());
		}
		selection.apply(list(), many, this.recorder);
		assertEquals(WarsmashConstants.MAX_SELECTION_SIZE, selection.getUnits().size());
		assertEquals(WarsmashConstants.MAX_SELECTION_SIZE, this.events.size());
	}
}
