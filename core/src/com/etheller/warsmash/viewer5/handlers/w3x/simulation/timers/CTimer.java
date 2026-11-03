package com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers;

import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;

public abstract class CTimer {
	private static final float MIN_SAME_TICK_TIMEOUT = 0.001f;
	private static double firingExactTime = Double.NaN;

	private int engineFireTick;
	private double scheduleTime;
	private float timeoutTime;
	private float segmentTime;
	private boolean running = false;
	private boolean repeats;
	private boolean paused;
	private float pausedElapsed;
	private float pausedRemaining;
	private int resumedFiresLeft;
	private int generation;

	public void setTimeoutTime(final float timeoutTime) {
		this.timeoutTime = timeoutTime;
	}

	public void resetTimeoutTime(final float timeoutTime) {
		this.timeoutTime = timeoutTime;
		this.segmentTime = timeoutTime;
		final int ticks = (int) (timeoutTime / WarsmashConstants.SIMULATION_STEP_TIME);
		this.engineFireTick = (int) Math.round(this.scheduleTime / WarsmashConstants.SIMULATION_STEP_TIME) + ticks;
	}

	public boolean isRepeats() {
		return this.repeats;
	}

	public boolean isRunning() {
		return this.running;
	}

	public float getTimeoutTime() {
		return this.timeoutTime;
	}

	/**
	 * Script timers expire at their exact time, several times within one simulation step if the timeout is shorter
	 * than the step; engine timers keep whole steps.
	 */
	protected boolean isExactTiming() {
		return false;
	}

	public boolean canFireInCurrentTick() {
		return isExactTiming() && (this.segmentTime >= MIN_SAME_TICK_TIMEOUT);
	}

	private static double now(final CSimulation simulation) {
		// Inside an exact timer's callback the game clock reads that timer's expiry time, not the step's (1.29: a
		// timer started there counts from it).
		if (!Double.isNaN(firingExactTime)) {
			return firingExactTime;
		}
		return simulation.getGameTurnTick() * (double) WarsmashConstants.SIMULATION_STEP_TIME;
	}

	public void start(final CSimulation simulation) {
		this.resumedFiresLeft = 0;
		schedule(simulation, now(simulation), this.timeoutTime);
	}

	public void startRepeatingTimerWithDelay(final CSimulation simulation, final float delay) {
		this.repeats = true;
		this.resumedFiresLeft = 0;
		schedule(simulation, now(simulation), delay);
	}

	private void schedule(final CSimulation simulation, final double startTime, final float length) {
		this.generation++;
		this.running = true;
		this.paused = false;
		this.scheduleTime = startTime;
		this.segmentTime = length;
		final int currentTick = simulation.getGameTurnTick();
		if (isExactTiming()) {
			this.engineFireTick = Math.max(currentTick,
					(int) Math.ceil(((startTime + length) / WarsmashConstants.SIMULATION_STEP_TIME) - 1e-3));
		}
		else {
			this.engineFireTick = currentTick + (int) (length / WarsmashConstants.SIMULATION_STEP_TIME);
		}
		simulation.registerTimer(this);
	}

	public void pause(final CSimulation simulation) {
		if (!this.paused) {
			this.pausedElapsed = getElapsed(simulation);
			this.pausedRemaining = getRemaining(simulation);
		}
		this.paused = true;
		this.running = false;
		this.resumedFiresLeft = 0;
		this.generation++;
		simulation.unregisterTimer(this);
	}

	public void kill(final CSimulation simulation) {
		this.pause(simulation);
		this.repeats = false;
	}

	public void resume(final CSimulation simulation) {
		if (!this.paused) {
			start(simulation);
			return;
		}
		// 1.29: a resumed periodic timer fires after what was left, once more after a full period, then stops and
		// reads as paused again.
		this.resumedFiresLeft = this.repeats ? 2 : 1;
		schedule(simulation, now(simulation), this.pausedRemaining);
	}

	public float getElapsed(final CSimulation simulation) {
		if (this.paused) {
			return this.pausedElapsed;
		}
		final double elapsed = now(simulation) - this.scheduleTime;
		return (float) Math.max(0, Math.min(elapsed, this.segmentTime));
	}

	public float getRemaining(final CSimulation simulation) {
		if (this.paused) {
			return this.pausedRemaining;
		}
		return this.segmentTime - getElapsed(simulation);
	}

	public void setRepeats(final boolean repeats) {
		this.repeats = repeats;
	}

	public int getEngineFireTick() {
		return this.engineFireTick;
	}

	public abstract void onFire(final CSimulation simulation);

	public void fire(final CSimulation simulation) {
		// its implied that we will have "unregisterTimer" happen automatically
		// before this is called
		this.running = false;
		// the value of this.repeats is snapshotted before firing, because it is allowed
		// to change
		// while firing (we might recycle this timer object)
		final boolean repeats = this.repeats;
		final int generation = this.generation;
		final double firedAt = this.scheduleTime + this.segmentTime;
		final double outerTime = firingExactTime;
		if (isExactTiming()) {
			firingExactTime = Math.min(firedAt,
					simulation.getGameTurnTick() * (double) WarsmashConstants.SIMULATION_STEP_TIME);
		}
		try {
			onFire(simulation);
		}
		finally {
			firingExactTime = outerTime;
		}
		if (generation != this.generation) {
			return;
		}
		if (this.resumedFiresLeft > 0) {
			this.resumedFiresLeft--;
			if (this.resumedFiresLeft == 0) {
				this.paused = true;
				return;
			}
		}
		if (repeats) {
			if (isExactTiming() && (this.timeoutTime >= MIN_SAME_TICK_TIMEOUT)) {
				schedule(simulation, firedAt, this.timeoutTime);
			}
			else {
				schedule(simulation, now(simulation), this.timeoutTime);
			}
		}
	}
}
