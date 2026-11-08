package com.etheller.interpreter.ast.execution.instruction;

import com.etheller.interpreter.ast.execution.JassThread;
import com.etheller.interpreter.ast.value.JassType;
import com.etheller.interpreter.ast.value.JassValue;
import com.etheller.interpreter.ast.value.RealJassValue;
import com.etheller.interpreter.ast.value.visitor.JassTypeGettingValueVisitor;
import com.etheller.interpreter.ast.value.visitor.RealJassValueVisitor;

public class TypeCheckInstruction implements JassInstruction {
	private final JassType type;

	public TypeCheckInstruction(final JassType type) {
		this.type = type;
	}

	@Override
	public void run(final JassThread thread) {
		final JassValue value = thread.stackFrame.peek();
		if (value == null) {
			if (!this.type.isNullable()) {
				throw new RuntimeException("Type " + this.type.getName() + " cannot be assigned to null!");
			}
		}
		else {
			final JassType valueType = value.visit(JassTypeGettingValueVisitor.getInstance());
			if (!this.type.isAssignableFrom(valueType)) {
				throw new RuntimeException("Incompatible types " + valueType.getName() + " != " + this.type.getName());
			}
			if ((this.type == JassType.REAL) && (valueType == JassType.INTEGER)) {
				// The original game converts an integer stored into a real right away, with one more operation.
				thread.stackFrame.pop();
				thread.stackFrame.push(RealJassValue.of(value.visit(RealJassValueVisitor.getInstance())));
				thread.opCount++;
			}
		}
	}
}
