package com.etheller.interpreter.ast.execution.instruction;

import com.etheller.interpreter.ast.execution.JassThread;
import com.etheller.interpreter.ast.scope.GlobalScopeAssignable;
import com.etheller.interpreter.ast.util.HandleReferences;
import com.etheller.interpreter.ast.value.JassValue;

public class GlobalAssignmentInstruction implements JassInstruction {
	private final int globalId;

	public GlobalAssignmentInstruction(final int globalId) {
		this.globalId = globalId;
	}

	@Override
	public void run(final JassThread thread) {
		final GlobalScopeAssignable global = thread.globalScope.getAssignableGlobalById(this.globalId);
		final JassValue value = thread.stackFrame.pop();
		HandleReferences.replace(global.getValue(), value);
		global.setValue(value);
	}

	@Override
	public int opCost() {
		return 1;
	}
}
