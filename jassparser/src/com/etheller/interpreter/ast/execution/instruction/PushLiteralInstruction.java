package com.etheller.interpreter.ast.execution.instruction;

import com.etheller.interpreter.ast.execution.JassThread;
import com.etheller.interpreter.ast.value.JassValue;

public class PushLiteralInstruction implements JassInstruction {
	private final JassValue value;
	private final int opCost;

	public PushLiteralInstruction(final JassValue value) {
		this(value, 1);
	}

	public PushLiteralInstruction(final JassValue value, final int opCost) {
		this.value = value;
		this.opCost = opCost;
	}

	@Override
	public void run(final JassThread thread) {
		thread.stackFrame.push(this.value);
	}

	@Override
	public int opCost() {
		return this.opCost;
	}
}
