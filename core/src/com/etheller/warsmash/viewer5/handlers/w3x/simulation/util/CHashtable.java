package com.etheller.warsmash.viewer5.handlers.w3x.simulation.util;

import java.util.HashMap;
import java.util.Map;

/**
 * As in the original game, one key pair holds five separate values: an integer, a real, a boolean, a string and a
 * handle (every handle type shares the handle slot). FlushParentHashtable destroys the table: later saves are
 * lost (measured on 1.29).
 */
public class CHashtable {
	public static final int INTEGER = 0;
	public static final int REAL = 1;
	public static final int BOOLEAN = 2;
	public static final int STRING = 3;
	public static final int HANDLE = 4;
	private static final int SLOTS = 5;

	private final Map<Integer, Map<Integer, Object[]>> parentKeyToChildTable = new HashMap<>();
	private boolean destroyed;

	public void save(final int slot, final Integer parentKey, final Integer childKey, final Object object) {
		if (this.destroyed) {
			return;
		}
		if (object == null) {
			remove(slot, parentKey, childKey);
			return;
		}
		Map<Integer, Object[]> childTable = this.parentKeyToChildTable.get(parentKey);
		if (childTable == null) {
			childTable = new HashMap<>();
			this.parentKeyToChildTable.put(parentKey, childTable);
		}
		Object[] values = childTable.get(childKey);
		if (values == null) {
			values = new Object[SLOTS];
			childTable.put(childKey, values);
		}
		values[slot] = object;
	}

	public Object load(final int slot, final Integer parentKey, final Integer childKey) {
		final Map<Integer, Object[]> childTable = this.parentKeyToChildTable.get(parentKey);
		if (childTable != null) {
			final Object[] values = childTable.get(childKey);
			if (values != null) {
				return values[slot];
			}
		}
		return null;
	}

	public void remove(final int slot, final Integer parentKey, final Integer childKey) {
		final Map<Integer, Object[]> childTable = this.parentKeyToChildTable.get(parentKey);
		if (childTable == null) {
			return;
		}
		final Object[] values = childTable.get(childKey);
		if (values == null) {
			return;
		}
		values[slot] = null;
		for (final Object value : values) {
			if (value != null) {
				return;
			}
		}
		childTable.remove(childKey);
		if (childTable.isEmpty()) {
			this.parentKeyToChildTable.remove(parentKey);
		}
	}

	public void flushChild(final Integer parentKey) {
		this.parentKeyToChildTable.remove(parentKey);
	}

	public void flushAll() {
		this.parentKeyToChildTable.clear();
		this.destroyed = true;
	}
}
