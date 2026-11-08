package com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers;

import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;

/**
 * The end of a timed effect at its exact game time. Checked once a step; when the end falls inside the coming step,
 * it is left to a timer at that time, so script timers in that step still see the effect before it and no longer
 * after it (the original game ends a 0.63 s buff after 0.63 s, not at a step boundary).
 */
public final class CExactExpiry {
	private static final double TIME_EPSILON = 1e-9;

	public interface Action {
		void expire(CSimulation simulation);
	}

	private double expireTime = Double.POSITIVE_INFINITY;
	private CTimer pendingTimer;

	/** Ends after the given length from now. */
	public void start(final CSimulation simulation, final double length) {
		startRemaining(simulation, CTimer.exactLength(length));
	}

	/** Ends after exactly the given time from now. */
	public void startRemaining(final CSimulation simulation, final double remaining) {
		cancel(simulation);
		this.expireTime = CTimer.currentTime(simulation) + remaining;
	}

	public void never(final CSimulation simulation) {
		cancel(simulation);
		this.expireTime = Double.POSITIVE_INFINITY;
	}

	public void postpone(final CSimulation simulation, final double time) {
		cancel(simulation);
		this.expireTime += time;
	}

	public double getRemaining(final CSimulation simulation) {
		return Math.max(0, this.expireTime - CTimer.currentTime(simulation));
	}

	/**
	 * The step's check: true when the end is already reached, so the caller ends the effect now; an end inside the
	 * coming step is handed to the action at its time.
	 */
	public boolean check(final CSimulation simulation, final Action action) {
		final double now = CTimer.currentTime(simulation);
		if (now >= (this.expireTime - TIME_EPSILON)) {
			cancel(simulation);
			return true;
		}
		if ((this.pendingTimer == null)
				&& ((now + WarsmashConstants.SIMULATION_STEP_TIME) > (this.expireTime + TIME_EPSILON))) {
			final CTimer timer = new CTimer() {
				@Override
				protected boolean isExactTiming() {
					return true;
				}

				@Override
				public void onFire(final CSimulation simulation) {
					if (CExactExpiry.this.pendingTimer == this) {
						CExactExpiry.this.pendingTimer = null;
						action.expire(simulation);
					}
				}
			};
			this.pendingTimer = timer;
			timer.startAt(simulation, this.expireTime);
		}
		return false;
	}

	public void cancel(final CSimulation simulation) {
		if (this.pendingTimer != null) {
			final CTimer timer = this.pendingTimer;
			this.pendingTimer = null;
			timer.kill(simulation);
		}
	}
}
