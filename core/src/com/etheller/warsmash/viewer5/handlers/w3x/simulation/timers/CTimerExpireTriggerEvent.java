package com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers;

import com.etheller.interpreter.ast.scope.trigger.RemovableTriggerEvent;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.warsmash.viewer5.handlers.w3x.save.SavedFormerly;

/** A trigger's TriggerRegisterTimerExpireEvent registration. */
public final class CTimerExpireTriggerEvent extends RemovableTriggerEvent {
	@SavedFormerly("val$trigger")
	private final Trigger trigger;
	@SavedFormerly("val$timer")
	private final CTimerJassBase timer;

	public CTimerExpireTriggerEvent(final Trigger trigger, final CTimerJassBase timer) {
		super(trigger);
		this.trigger = trigger;
		this.timer = timer;
	}

	@Override
	public void remove() {
		this.timer.removeEvent(this.trigger);
	}
}
