package com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.etheller.interpreter.ast.debug.JassException;
import com.etheller.interpreter.ast.execution.JassThread;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.util.CAllocatedHandle;
import com.etheller.interpreter.ast.util.CExtensibleHandle;
import com.etheller.interpreter.ast.value.CodeJassValue;
import com.etheller.interpreter.ast.value.StructJassValue;
import com.etheller.warsmash.parsers.jass.scope.CommonTriggerExecutionScope;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;

public class CTimerJass extends CTimerJassBase implements CExtensibleHandle, CAllocatedHandle {
	private int handleId;
	private boolean handleReleased;
	private boolean handleIdKept;
	private int referenceCount;
	private StructJassValue structJassValue;
	private CodeJassValue handlerFunc;
	private final List<Trigger> eventTriggers = new ArrayList<>();

	public CTimerJass(final int handleId) {
		this.handleId = handleId;
	}

	@Override
	public StructJassValue getStructValue() {
		return this.structJassValue;
	}

	@Override
	public void setStructValue(final StructJassValue value) {
		this.structJassValue = value;
	}

	@Override
	public void setHandlerFunc(final CodeJassValue handlerFunc) {
		this.handlerFunc = handlerFunc;
	}

	@Override
	public void onFire(final CSimulation simulation) {
		final CommonTriggerExecutionScope handlerScope = CommonTriggerExecutionScope.expiringTimer(null, this);
		// Snapshotting these values at the top... This is leaky and later I should make
		// a better solution, but I was having a problem with bj_stockUpdateTimer where
		// it would
		// modify its own state while firing, and I put this in as an ideological
		// safeguard so that
		// the handler func cannot append more triggers to ourself or change our
		// behavior when we
		// are only halfway done firing.
		final CodeJassValue handlerFunc = this.handlerFunc;
		final List<Trigger> eventTriggers = this.eventTriggers.isEmpty() ? Collections.emptyList()
				: new ArrayList<>(this.eventTriggers);
		final GlobalScope globalScope = simulation.getGlobalScope();
		// 1.29: the expire-event triggers run before the timer's own callback.
		for (final Trigger trigger : eventTriggers) {
			final CommonTriggerExecutionScope executionScope = CommonTriggerExecutionScope.expiringTimer(trigger, this);
			globalScope.queueTrigger(null, null, trigger, executionScope, executionScope);
		}
		try {
			if (handlerFunc != null) {
				final JassThread timerThread = globalScope.createThread(handlerFunc, handlerScope);
				timerThread.setSleepEndsThread(true);
				globalScope.runThreadUntilSleep(timerThread);
			}
		}
		catch (final Exception e) {
			throw new JassException(globalScope, "Exception during jass timer fire", e);
		}
	}

	@Override
	public void addEvent(final Trigger trigger) {
		this.eventTriggers.add(trigger);
	}

	@Override
	public void removeEvent(final Trigger trigger) {
		this.eventTriggers.remove(trigger);
	}

	@Override
	public int getHandleId() {
		return this.handleId;
	}

	@Override
	public void setHandleId(final int handleId) {
		this.handleId = handleId;
	}

	@Override
	public boolean releaseHandle() {
		if (this.handleReleased) {
			return false;
		}
		this.handleReleased = true;
		return true;
	}

	@Override
	public void keepHandleId() {
		this.handleIdKept = true;
	}

	@Override
	public boolean isHandleIdKept() {
		return this.handleIdKept;
	}

	@Override
	public int changeReferenceCount(final int delta) {
		this.referenceCount += delta;
		return this.referenceCount;
	}

	@Override
	public int getReferenceCount() {
		return this.referenceCount;
	}

}
