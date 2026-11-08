package com.etheller.warsmash.parsers.jass.triggers;

import com.etheller.interpreter.ast.scope.trigger.RemovableTriggerEvent;
import com.etheller.interpreter.ast.util.AllocatedHandle;

/** What a script gets for a registered trigger event: the event is kept by its trigger, the id by this. */
public class TriggerEventHandle extends AllocatedHandle {
	private final RemovableTriggerEvent event;

	public TriggerEventHandle(final RemovableTriggerEvent event) {
		this.event = event;
	}

	public RemovableTriggerEvent getEvent() {
		return this.event;
	}
}
