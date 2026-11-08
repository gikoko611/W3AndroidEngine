package com.etheller.warsmash.parsers.jass.triggers;

import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.scope.trigger.TriggerBooleanExpression;
import com.etheller.interpreter.ast.util.AllocatedHandle;

public class BoolExprNot extends AllocatedHandle implements TriggerBooleanExpression {
	private final TriggerBooleanExpression operand;

	public BoolExprNot(final TriggerBooleanExpression operand) {
		this.operand = operand;
	}

	@Override
	public boolean evaluate(final GlobalScope globalScope, final TriggerExecutionScope triggerScope) {
		return this.operand.evaluate(globalScope, triggerScope);
	}

}
