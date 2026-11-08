package com.etheller.warsmash.parsers.jass.triggers;

import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.scope.trigger.TriggerBooleanExpression;
import com.etheller.interpreter.ast.util.AllocatedHandle;

public class BoolExprOr extends AllocatedHandle implements TriggerBooleanExpression {
	private final TriggerBooleanExpression operandA;
	private final TriggerBooleanExpression operandB;

	public BoolExprOr(final TriggerBooleanExpression operandA, final TriggerBooleanExpression operandB) {
		this.operandA = operandA;
		this.operandB = operandB;
	}

	@Override
	public boolean evaluate(final GlobalScope globalScope, final TriggerExecutionScope triggerScope) {
		return this.operandA.evaluate(globalScope, triggerScope) || this.operandB.evaluate(globalScope, triggerScope);
	}

}
