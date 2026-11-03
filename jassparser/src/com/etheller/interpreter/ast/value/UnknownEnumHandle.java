package com.etheller.interpreter.ast.value;

import com.etheller.interpreter.ast.util.CHandle;

/**
 * An enum handle made by a Convert* native from a value the engine has no constant for. The game keeps enum handles as
 * plain integers, so such a handle is not null and equals another handle of the same value.
 */
public final class UnknownEnumHandle implements CHandle {
	private final int value;

	UnknownEnumHandle(final int value) {
		this.value = value;
	}

	@Override
	public int getHandleId() {
		return this.value;
	}

	@Override
	public String toString() {
		return "enum:" + this.value;
	}
}
