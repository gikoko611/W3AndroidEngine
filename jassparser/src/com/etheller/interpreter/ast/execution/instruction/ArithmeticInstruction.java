package com.etheller.interpreter.ast.execution.instruction;

import com.etheller.interpreter.ast.execution.JassThread;
import com.etheller.interpreter.ast.expression.ArithmeticSign;
import com.etheller.interpreter.ast.value.JassValue;
import com.etheller.interpreter.ast.value.visitor.ArithmeticJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ArithmeticLeftHandNullJassValueVisitor;

public class ArithmeticInstruction implements JassInstruction {
	private final ArithmeticSign arithmeticSign;
	private final int opCost;

	public ArithmeticInstruction(final ArithmeticSign arithmeticSign) {
		this.arithmeticSign = arithmeticSign;
		// The original game's and / or take one operation less than the other binary operators.
		this.opCost = (arithmeticSign.getShortCircuitValue() != null) ? 1 : 3;
	}

	@Override
	public void run(final JassThread thread) {
		final JassValue rightValue = thread.stackFrame.pop();
		final JassValue leftValue = thread.stackFrame.pop();
		JassValue result;
		try {
			if (leftValue == null) {
				if (rightValue == null) {
					result = this.arithmeticSign.apply((String) null, (String) null);
				}
				else {
					result = rightValue
							.visit(ArithmeticLeftHandNullJassValueVisitor.INSTANCE.reset(this.arithmeticSign));
				}
			}
			else {
				result = leftValue.visit(ArithmeticJassValueVisitor.INSTANCE.reset(rightValue, this.arithmeticSign));
			}
		}
		catch (final ArithmeticException exception) {
			// The original game silently stops a thread that divides by zero; the game and other threads go on.
			System.err.println("JASS thread stopped: division by zero");
			thread.halt();
			return;
		}
		thread.stackFrame.push(result);
	}

	@Override
	public int opCost() {
		return this.opCost;
	}
}
