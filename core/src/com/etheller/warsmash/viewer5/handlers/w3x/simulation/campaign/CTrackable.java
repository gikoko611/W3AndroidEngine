package com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign;

import java.util.ArrayList;
import java.util.List;

import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.trigger.RemovableTriggerEvent;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.util.AllocatedHandle;
import com.etheller.warsmash.parsers.jass.scope.CommonTriggerExecutionScope;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.JassGameEventsWar3;

/**
 * A model a script puts on the map that the player can point at (track) and click (hit). It is never destroyed and
 * cannot be moved; it takes the next handle id.
 */
public final class CTrackable extends AllocatedHandle {
	private final String modelPath;
	private final float x;
	private final float y;
	private final float facing;
	private final List<Trigger> hitTriggers = new ArrayList<>();
	private final List<Trigger> trackTriggers = new ArrayList<>();

	public CTrackable(final String modelPath, final float x, final float y, final float facing) {
		this.modelPath = modelPath == null ? "" : modelPath;
		this.x = x;
		this.y = y;
		this.facing = facing;
	}

	public String getModelPath() {
		return this.modelPath;
	}

	public float getX() {
		return this.x;
	}

	public float getY() {
		return this.y;
	}

	/** In degrees, as the script gave it. */
	public float getFacing() {
		return this.facing;
	}

	public RemovableTriggerEvent addHitEvent(final Trigger trigger) {
		return addEvent(this.hitTriggers, trigger);
	}

	public RemovableTriggerEvent addTrackEvent(final Trigger trigger) {
		return addEvent(this.trackTriggers, trigger);
	}

	private static RemovableTriggerEvent addEvent(final List<Trigger> triggers, final Trigger trigger) {
		triggers.add(trigger);
		return new EventRegistration(trigger, triggers);
	}

	private static final class EventRegistration extends RemovableTriggerEvent {
		private final Trigger trigger;
		private final List<Trigger> triggers;

		private EventRegistration(final Trigger trigger, final List<Trigger> triggers) {
			super(trigger);
			this.trigger = trigger;
			this.triggers = triggers;
		}

		@Override
		public void remove() {
			this.triggers.remove(this.trigger);
		}
	}

	/** A player clicked it, through that player's commands. */
	public void hit(final GlobalScope globalScope) {
		fire(globalScope, this.hitTriggers, JassGameEventsWar3.EVENT_GAME_TRACKABLE_HIT);
	}

	/** A player's pointer came onto it, through that player's commands. */
	public void track(final GlobalScope globalScope) {
		fire(globalScope, this.trackTriggers, JassGameEventsWar3.EVENT_GAME_TRACKABLE_TRACK);
	}

	private void fire(final GlobalScope globalScope, final List<Trigger> triggers, final JassGameEventsWar3 event) {
		for (final Trigger trigger : new ArrayList<>(triggers)) {
			final CommonTriggerExecutionScope scope = CommonTriggerExecutionScope.trackableScope(event, trigger, this);
			globalScope.queueTrigger(null, null, trigger, scope, scope);
		}
	}

	public boolean hasEvents() {
		return !this.hitTriggers.isEmpty() || !this.trackTriggers.isEmpty();
	}
}
