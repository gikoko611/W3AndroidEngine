package com.etheller.warsmash.parsers.jass;

import com.etheller.interpreter.ast.function.JassFunction;
import com.etheller.interpreter.ast.value.CodeJassValue;
import com.etheller.interpreter.ast.value.HandleJassType;
import com.etheller.interpreter.ast.value.HandleJassTypeConstructor;
import com.etheller.interpreter.ast.value.HandleJassValue;
import com.etheller.interpreter.ast.value.RealJassValue;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.CodeJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.RealJassValueVisitor;
import com.etheller.warsmash.parsers.jass.Jass2.CommonEnvironment;
import com.etheller.warsmash.parsers.jass.scope.CommonTriggerExecutionScope;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimerJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimerJassBase;

/**
 * Timer natives.
 */
final class TimerNatives extends CommonNatives {
	TimerNatives(final CommonEnvironment environment) {
		super(environment);
	}

	@Override
	void register() {
		final HandleJassType timerType = type("timer");

		// ============================================================================
		// Timer API
		//
		final JassFunction createTimerFxn = (arguments, globalScope, triggerScope) -> {
			return new HandleJassValue(timerType,
					new CTimerJass(this.simulation.getHandleIdAllocator().createHandleId()));
		};
		jassProgramVisitor.getJassNativeManager().createNative("CreateTimer", createTimerFxn);
		timerType.setConstructorNative(new HandleJassTypeConstructor("CreateTimer"));
		jassProgramVisitor.getJassNativeManager().createNative("DestroyTimer",
				(arguments, globalScope, triggerScope) -> {
					final CTimer timer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					if (timer != null) {
						timer.kill(this.simulation);
						release(timer);
					}
					return null;
				});
		timerType.setDestructorNative(new HandleJassTypeConstructor("DestroyTimer"));
		jassProgramVisitor.getJassNativeManager().createNative("TimerStart",
				(arguments, globalScope, triggerScope) -> {
					final CTimerJassBase timer = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final Double timeout = arguments.get(1).visit(RealJassValueVisitor.getInstance());
					final boolean periodic = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
					final CodeJassValue handlerFunc = nullable(arguments, 3, CodeJassValueVisitor.getInstance());
					if ((timer != null) && timer.isRunning()) {
						timer.pause(this.simulation);
					}
					if ((timer != null)) {
						timer.setTimeoutTime(timeout.floatValue());
						timer.setRepeats(periodic);
						timer.setHandlerFunc(handlerFunc);
						timer.start(this.simulation);
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("TimerGetElapsed",
				(arguments, globalScope, triggerScope) -> {
					final CTimer timer = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					if (timer == null) {
						return RealJassValue.ZERO;
					}
					return RealJassValue.of(timer.getElapsed(this.simulation));
				});
		jassProgramVisitor.getJassNativeManager().createNative("TimerGetRemaining",
				(arguments, globalScope, triggerScope) -> {
					final CTimer timer = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					if (timer == null) {
						return RealJassValue.ZERO;
					}
					return RealJassValue.of(timer.getRemaining(this.simulation));
				});
		jassProgramVisitor.getJassNativeManager().createNative("TimerGetTimeout",
				(arguments, globalScope, triggerScope) -> {
					final CTimer timer = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					if (timer == null) {
						return RealJassValue.ZERO;
					}
					return RealJassValue.of(timer.getTimeoutTime());
				});
		jassProgramVisitor.getJassNativeManager().createNative("PauseTimer",
				(arguments, globalScope, triggerScope) -> {
					final CTimer timer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					timer.pause(this.simulation);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("ResumeTimer",
				(arguments, globalScope, triggerScope) -> {
					final CTimer timer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					timer.resume(this.simulation);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetExpiredTimer",
				(arguments, globalScope, triggerScope) -> {
					if (triggerScope instanceof CommonTriggerExecutionScope) {
						return new HandleJassValue(timerType,
								((CommonTriggerExecutionScope) triggerScope).getExpiringTimer());
					}
					else {
						return new HandleJassValue(timerType, null);
					}
				});
	}
}
