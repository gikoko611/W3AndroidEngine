package com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit;

import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.scope.variableevent.CLimitOp;
import com.etheller.warsmash.parsers.jass.scope.CommonTriggerExecutionScope;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.state.CUnitState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.JassGameEventsWar3;

/**
 * TriggerRegisterUnitStateEvent, as measured on 1.29: the event fires when a change brings life or mana to the limit
 * from the other side, never at registration and not again while the value stays past the limit. LESS_THAN behaves
 * as LESS_THAN_OR_EQUAL and GREATER_THAN as GREATER_THAN_OR_EQUAL; the greater ones also fire when the value comes
 * down onto the limit exactly. EQUAL, NOT_EQUAL, an unknown limitop and the maximum life and mana states never fire.
 */
public final class CUnitStateEvent extends CWidgetEvent {
	private final CUnitState state;
	private final CLimitOp limitOp;
	private final double limitValue;
	private double lastValue;

	public CUnitStateEvent(final GlobalScope globalScope, final CUnit unit, final Trigger trigger,
			final CUnitState state, final CLimitOp limitOp, final double limitValue) {
		super(globalScope, unit, trigger, JassGameEventsWar3.EVENT_UNIT_STATE_LIMIT, null);
		this.state = state;
		this.limitOp = limitOp;
		this.limitValue = limitValue;
		this.lastValue = unit.getUnitState(null, state);
	}

	/**
	 * @param written a script wrote the state, which counts even when the value is the same
	 */
	public void check(final CUnit unit, final boolean written) {
		final double value = unit.getUnitState(null, this.state);
		final double old = this.lastValue;
		if (!written && (value == old)) {
			return;
		}
		this.lastValue = value;
		if (reached(old, value)) {
			fire(unit, CommonTriggerExecutionScope.unitStateLimitScope(getTrigger(), unit, this.state));
		}
	}

	private boolean reached(final double old, final double value) {
		if ((this.limitOp == null) || ((this.state != CUnitState.LIFE) && (this.state != CUnitState.MANA))) {
			return false;
		}
		switch (this.limitOp) {
		case LESS_THAN:
		case LESS_THAN_OR_EQUAL:
			return (old >= this.limitValue) && (value <= this.limitValue);
		case GREATER_THAN_OR_EQUAL:
		case GREATER_THAN:
			return (value >= this.limitValue) && ((old < this.limitValue) || (value == this.limitValue));
		default:
			return false;
		}
	}
}
