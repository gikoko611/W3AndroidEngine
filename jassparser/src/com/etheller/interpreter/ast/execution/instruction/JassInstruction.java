package com.etheller.interpreter.ast.execution.instruction;

import com.etheller.interpreter.ast.execution.JassThread;

public interface JassInstruction {
	void run(JassThread thread);

	/**
	 * How many operations of the original game's script engine this instruction stands for; a thread that goes over
	 * the operation limit is stopped.
	 */
	default int opCost() {
		return 0;
	}
}
