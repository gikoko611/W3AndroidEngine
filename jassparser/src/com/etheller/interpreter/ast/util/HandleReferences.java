package com.etheller.interpreter.ast.util;

import com.etheller.interpreter.ast.value.HandleJassValue;

/**
 * Counts the script variables (globals, locals, array slots) and hashtable entries that point at a handle. 1.29 gives
 * a destroyed handle's id again only once nothing points at it any more (measured).
 */
public final class HandleReferences {
	private HandleReferences() {
	}

	/** A variable or slot that held oldValue now holds newValue. */
	public static void replace(final Object oldValue, final Object newValue) {
		if (oldValue != newValue) {
			change(newValue, 1);
			change(oldValue, -1);
		}
	}

	/** value is a JassValue or the handle itself. */
	public static void change(final Object value, final int delta) {
		final Object handle = value instanceof HandleJassValue ? ((HandleJassValue) value).getJavaValue() : value;
		if (handle instanceof CAllocatedHandle) {
			final CAllocatedHandle allocated = (CAllocatedHandle) handle;
			// A save from before the counting holds references never counted; they must not take the count below 0.
			if ((delta > 0) || (allocated.getReferenceCount() > 0)) {
				allocated.changeReferenceCount(delta);
			}
		}
	}
}
