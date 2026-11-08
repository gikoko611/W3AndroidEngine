package com.etheller.interpreter.ast.execution.instruction;

import com.etheller.interpreter.ast.execution.JassStackFrame;
import com.etheller.interpreter.ast.execution.JassThread;
import com.etheller.interpreter.ast.value.JassValue;

public class ReturnInstruction implements JassInstruction {
	public static final ReturnInstruction INSTANCE = new ReturnInstruction();
	/**
	 * A return with a value: the original game first moves the value to its return register.
	 */
	public static final ReturnInstruction RETURN_VALUE = new ReturnInstruction(2);

	private final int opCost;

	public ReturnInstruction() {
		this(1);
	}

	public ReturnInstruction(final int opCost) {
		this.opCost = opCost;
	}

	@Override
	public void run(final JassThread thread) {
		final JassStackFrame finishingStackFrame = thread.stackFrame;
		final JassValue valueToReturn = finishingStackFrame.pop();
		finishingStackFrame.keepLocalHandleIds();
		thread.stackFrame = finishingStackFrame.stackBase;
		thread.instructionPtr = finishingStackFrame.returnAddressInstructionPtr;
		if (thread.stackFrame != null) {
			thread.stackFrame.push(valueToReturn);
		}
	}

	@Override
	public int opCost() {
		return this.opCost;
	}
}
