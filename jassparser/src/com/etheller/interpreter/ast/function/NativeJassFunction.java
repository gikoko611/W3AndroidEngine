package com.etheller.interpreter.ast.function;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.etheller.interpreter.ast.debug.JassException;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.util.JassSettings;
import com.etheller.interpreter.ast.value.HandleJassValue;
import com.etheller.interpreter.ast.value.JassType;
import com.etheller.interpreter.ast.value.JassValue;
import com.etheller.interpreter.ast.value.UnknownEnumHandle;
import com.etheller.interpreter.ast.value.visitor.JassTypeGettingValueVisitor;

public class NativeJassFunction {
	private final List<JassParameter> parameters;
	private final JassType returnType;
	private final String name;
	private final JassFunction implementation;
	private final AtomicBoolean reportedNullFailure = new AtomicBoolean();

	public NativeJassFunction(final List<JassParameter> parameters, final JassType returnType, final String name,
			final JassFunction impl) {
		this.parameters = parameters;
		this.returnType = returnType;
		this.name = name;
		this.implementation = impl;
	}

	public List<JassParameter> getParameters() {
		return this.parameters;
	}

	public JassType getReturnType() {
		return this.returnType;
	}

	public String getName() {
		return this.name;
	}

	public JassFunction getImplementation() {
		return this.implementation;
	}

	public final JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
			final TriggerExecutionScope triggerScope) {
		if (arguments.size() != this.parameters.size()) {
			throw new JassException(globalScope, "Invalid number of arguments passed to function: " + arguments.size()
					+ " != " + this.parameters.size(), null);
		}
		for (int i = 0; i < this.parameters.size(); i++) {
			final JassParameter parameter = this.parameters.get(i);
			final JassValue argument = arguments.get(i);
			if (!parameter.matchesType(argument)) {
				if ((parameter == null) || (argument == null)) {
					System.err.println(
							"We called some Jass function with incorrect argument types, and the types were null!!!");
					System.err.println("This is a temporary hack for tests and showcase programming solutions");
					return null;
				}
				System.err.println(
						parameter.getType() + " != " + argument.visit(JassTypeGettingValueVisitor.getInstance()));
				throw new JassException(globalScope,
						"Invalid type " + argument.visit(JassTypeGettingValueVisitor.getInstance()).getName()
								+ " for specified argument " + parameter.getType().getName(),
						null);
			}
		}
		if (!checkNativeExists()) {
			return this.returnType.getNullValue();
		}
		try {
			return this.implementation.call(arguments, globalScope, triggerScope);
		}
		catch (final NullPointerException e) {
			if (!JassSettings.CONTINUE_EXECUTING_ON_ERROR) {
				throw e;
			}
			// The original's natives ignore a null handle; one that this engine does not check must not end the
			// whole script thread (a cinematic would stop halfway).
			if (this.reportedNullFailure.compareAndSet(false, true)) {
				System.err.println("Native " + this.name + " failed on a null value; ignored like the original:");
				e.printStackTrace();
			}
			return this.returnType.getNullValue();
		}
		catch (final ClassCastException e) {
			// Measured (1.29): a native given an enum value the game has no constant for reads
			// it as 0 or false and changes nothing.
			if (!hasUnknownEnum(arguments)) {
				throw e;
			}
			return this.returnType.getNullValue();
		}
	}

	private static boolean hasUnknownEnum(final List<JassValue> arguments) {
		for (final JassValue argument : arguments) {
			if ((argument instanceof HandleJassValue)
					&& (((HandleJassValue) argument).getJavaValue() instanceof UnknownEnumHandle)) {
				return true;
			}
		}
		return false;
	}

	private boolean checkNativeExists() {
		if (this.implementation == null) {
			System.err.println(
					"Call to native function that was declared but had no native implementation: " + this.name);
			return false;
//			throw new UnsupportedOperationException(
//					"Call to native function that was declared but had no native implementation: " + this.name);
		}
		return true;
	}
}
