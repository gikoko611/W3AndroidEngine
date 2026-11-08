package com.etheller.interpreter.ast.execution.instruction;

import com.etheller.interpreter.ast.execution.JassThread;
import com.etheller.interpreter.ast.value.JassValue;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;

public class InvertedConditionalBranchInstruction implements JassInstruction {
	private final int newInstructionPointer;
	private final int opCost;
	private final int takenOpCost;

	public InvertedConditionalBranchInstruction(final int newInstructionPointer) {
		this(newInstructionPointer, 1, 1);
	}

	/**
	 * @param takenOpCost operations counted on top of opCost when the branch is taken
	 */
	public InvertedConditionalBranchInstruction(final int newInstructionPointer, final int opCost,
			final int takenOpCost) {
		this.newInstructionPointer = newInstructionPointer;
		this.opCost = opCost;
		this.takenOpCost = takenOpCost;
	}

	@Override
	public void run(final JassThread thread) {
		final JassValue topStackValue = thread.stackFrame.pop();
		if (!topStackValue.visit(BooleanJassValueVisitor.getInstance())) {
			thread.instructionPtr = this.newInstructionPointer;
			thread.opCount += this.takenOpCost;
		}
	}

	@Override
	public int opCost() {
		return this.opCost;
	}
}
