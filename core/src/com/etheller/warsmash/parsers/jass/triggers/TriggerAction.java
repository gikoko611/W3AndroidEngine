package com.etheller.warsmash.parsers.jass.triggers;

import com.etheller.interpreter.ast.function.JassFunction;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.util.AllocatedHandle;
import com.etheller.interpreter.ast.value.CodeJassValue;

public class TriggerAction extends AllocatedHandle {
	private final Trigger trigger;
	private final CodeJassValue actionFunc;
	private final int actionIndex;
	private final JassFunction action;

	public TriggerAction(final Trigger trigger, final CodeJassValue actionFunc, final int actionIndex) {
		this.trigger = trigger;
		this.actionFunc = actionFunc;
		this.actionIndex = actionIndex;
		this.action = trigger.getActionAt(actionIndex);
	}

	public Trigger getTrigger() {
		return this.trigger;
	}

	public CodeJassValue getActionFunc() {
		return this.actionFunc;
	}

	public int getActionIndex() {
		return this.actionIndex;
	}

	public JassFunction getAction() {
		return this.action;
	}
}
