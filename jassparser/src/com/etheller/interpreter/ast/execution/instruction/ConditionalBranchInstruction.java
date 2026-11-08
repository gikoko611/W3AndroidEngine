package com.etheller.interpreter.ast.execution.instruction;

import com.etheller.interpreter.ast.execution.JassThread;
import com.etheller.interpreter.ast.value.JassValue;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;

public class ConditionalBranchInstruction implements JassInstruction {
	private final int newInstructionPointer;
	private final int takenOpCost;

	public ConditionalBranchInstruction(final int newInstructionPointer) {
		this(newInstructionPointer, 1);
	}

	/**
	 * @param takenOpCost operations counted on top of {@link #opCost()} when the branch is taken
	 */
	public ConditionalBranchInstruction(final int newInstructionPointer, final int takenOpCost) {
		this.newInstructionPointer = newInstructionPointer;
		this.takenOpCost = takenOpCost;
	}

	@Override
	public void run(final JassThread thread) {
		final JassValue topStackValue = thread.stackFrame.pop();
		if (topStackValue.visit(BooleanJassValueVisitor.getInstance())) {
			thread.instructionPtr = this.newInstructionPointer;
			thread.opCount += this.takenOpCost;
		}
	}

	@Override
	public int opCost() {
		return 1;
	}
}
