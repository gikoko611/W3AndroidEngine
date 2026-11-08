package com.etheller.warsmash.parsers.jass.triggers;

import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.scope.trigger.TriggerBooleanExpression;
import com.etheller.interpreter.ast.util.AllocatedHandle;
import com.etheller.interpreter.ast.value.BooleanJassValue;
import com.etheller.interpreter.ast.value.CodeJassValue;
import com.etheller.interpreter.ast.value.JassValue;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;

/** What Condition and Filter make: one handle for each function of a script (measured on 1.29). */
public class BoolExprCondition extends AllocatedHandle implements TriggerBooleanExpression {
	private final CodeJassValue takesNothingReturnsBooleanFunction;
	private final GlobalScope scope;

	public BoolExprCondition(final CodeJassValue returnsBooleanFunction, final GlobalScope scope) {
		this.takesNothingReturnsBooleanFunction = returnsBooleanFunction;
		this.scope = scope;
	}

	public CodeJassValue getFunction() {
		return this.takesNothingReturnsBooleanFunction;
	}

	public GlobalScope getScope() {
		return this.scope;
	}

	@Override
	public boolean evaluate(final GlobalScope globalScope, final TriggerExecutionScope triggerScope) {
		final JassValue jassReturnValue = this.takesNothingReturnsBooleanFunction.callAndExecuteCapturingReturnValue(
				globalScope, triggerScope, "BoolExprCondition", BooleanJassValue.FALSE);
		final Boolean booleanReturnValue = jassReturnValue.visit(BooleanJassValueVisitor.getInstance());
		return booleanReturnValue.booleanValue();
	}

}
