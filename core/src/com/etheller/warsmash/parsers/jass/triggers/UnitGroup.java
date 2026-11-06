package com.etheller.warsmash.parsers.jass.triggers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;

import com.etheller.interpreter.ast.util.CHandle;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;

/**
 * Measured on 1.29: a removed unit leaves every group it was in, so ForGroup,
 * CountUnitsInGroup, FirstOfGroup and IsUnitInGroup no longer see it. Removed members are dropped on read.
 */
public class UnitGroup extends ArrayList<CUnit> implements CHandle {
	private int handleId;

	public UnitGroup(int handleId) {
		this.handleId = handleId;
	}

	@Override
	public int getHandleId() {
		return handleId;
	}

	/**
	 * Measured on 1.29: a group keeps its units by handle id, newest first, whatever the order
	 * they were added in; ForGroup and FirstOfGroup follow it.
	 */
	@Override
	public boolean add(final CUnit unit) {
		int index = super.size();
		if (unit != null) {
			final int handleId = unit.getHandleId();
			for (int i = 0; i < super.size(); i++) {
				final CUnit other = super.get(i);
				if ((other == null) || (other.getHandleId() < handleId)) {
					index = i;
					break;
				}
			}
		}
		super.add(index, unit);
		return true;
	}

	@Override
	public boolean addAll(final Collection<? extends CUnit> units) {
		for (final CUnit unit : units) {
			add(unit);
		}
		return !units.isEmpty();
	}

	private void dropRemovedUnits() {
		removeIf(unit -> (unit != null) && unit.isRemovedFromGame());
	}

	@Override
	public int size() {
		dropRemovedUnits();
		return super.size();
	}

	@Override
	public boolean isEmpty() {
		dropRemovedUnits();
		return super.isEmpty();
	}

	@Override
	public CUnit get(final int index) {
		dropRemovedUnits();
		return super.get(index);
	}

	@Override
	public boolean contains(final Object o) {
		dropRemovedUnits();
		return super.contains(o);
	}

	@Override
	public int indexOf(final Object o) {
		dropRemovedUnits();
		return super.indexOf(o);
	}

	@Override
	public Iterator<CUnit> iterator() {
		dropRemovedUnits();
		return super.iterator();
	}

	@Override
	public ListIterator<CUnit> listIterator() {
		dropRemovedUnits();
		return super.listIterator();
	}

	@Override
	public ListIterator<CUnit> listIterator(final int index) {
		dropRemovedUnits();
		return super.listIterator(index);
	}

	@Override
	public Spliterator<CUnit> spliterator() {
		dropRemovedUnits();
		return super.spliterator();
	}

	@Override
	public void forEach(final Consumer<? super CUnit> action) {
		dropRemovedUnits();
		super.forEach(action);
	}

	@Override
	public Object[] toArray() {
		// ForGroup walks a copy (new ArrayList<>(group)), which reads toArray.
		dropRemovedUnits();
		return super.toArray();
	}

	@Override
	public <T> T[] toArray(final T[] a) {
		dropRemovedUnits();
		return super.toArray(a);
	}

}
