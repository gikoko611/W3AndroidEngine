package com.etheller.interpreter.ast.execution.instruction;

import com.etheller.interpreter.ast.execution.JassThread;
import com.etheller.interpreter.ast.value.JassValue;

public class LocalAssignmentInstruction implements JassInstruction {
	private final int localId;

	public LocalAssignmentInstruction(final int localId) {
		this.localId = localId;
	}

	@Override
	public void run(final JassThread thread) {
		final JassValue value = thread.stackFrame.pop();
		thread.stackFrame.assign(this.localId, value);
	}

	@Override
	public int opCost() {
		return 1;
	}
}
