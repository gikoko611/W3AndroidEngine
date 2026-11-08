package com.etheller.warsmash.parsers.jass.triggers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.etheller.warsmash.testing.TestObjects;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;

class UnitGroupTest {

	private static CUnit unit(final int handleId) {
		final CUnit unit = TestObjects.allocate(CUnit.class);
		TestObjects.set(unit, "handleId", handleId);
		return unit;
	}

	private static List<Integer> handleIds(final Iterable<CUnit> units) {
		final List<Integer> ids = new ArrayList<>();
		for (final CUnit unit : units) {
			ids.add(unit.getHandleId());
		}
		return ids;
	}

	@Test
	void keepsUnitsNewestFirstWhateverTheAddOrder() {
		final UnitGroup group = new UnitGroup(1);
		for (final int handleId : new int[] { 105, 101, 110, 103, 108, 102 }) {
			group.add(unit(handleId));
		}
		assertEquals(Arrays.asList(110, 108, 105, 103, 102, 101), handleIds(group));
		assertEquals(110, group.get(0).getHandleId());
	}

	@Test
	void addAllKeepsTheSameOrder() {
		final UnitGroup group = new UnitGroup(1);
		group.add(unit(104));
		group.addAll(Arrays.asList(unit(101), unit(107), unit(103)));
		assertEquals(Arrays.asList(107, 104, 103, 101), handleIds(group));
	}

	@Test
	void copyForForGroupHasTheSameOrder() {
		final UnitGroup group = new UnitGroup(1);
		for (final int handleId : new int[] { 3, 9, 1, 7 }) {
			group.add(unit(handleId));
		}
		assertEquals(handleIds(group), handleIds(new ArrayList<>(group)));
	}

	@Test
	void removedUnitsLeaveTheGroup() {
		final UnitGroup group = new UnitGroup(1);
		final CUnit removed = unit(105);
		group.add(unit(101));
		group.add(removed);
		group.add(unit(109));
		TestObjects.set(removed, "removedFromGame", true);
		assertEquals(2, group.size());
		assertFalse(group.contains(removed));
		assertEquals(Arrays.asList(109, 101), handleIds(group));
		assertEquals(Arrays.asList(109, 101), handleIds(new ArrayList<>(group)));
	}

	@Test
	void emptyAfterAllMembersAreRemoved() {
		final UnitGroup group = new UnitGroup(1);
		final CUnit only = unit(100);
		group.add(only);
		assertFalse(group.isEmpty());
		TestObjects.set(only, "removedFromGame", true);
		assertTrue(group.isEmpty());
	}
}
