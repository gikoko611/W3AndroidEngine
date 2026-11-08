package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.ability.template;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;

/**
 * Timing of an aura that gets disabled and enabled while it stays on its unit (player-disabled ability, unmet
 * requirements). The aura checks every two seconds counted from when the ability was added, also while disabled.
 * A disabled aura stops handing out buffs but the buffs it already gave stay until its second check after it was
 * disabled. A re-enabled aura first reaches units again at its next check.
 */
public final class AuraCheckClock {
	public static final int WAIT = 0;
	public static final int RUN = 1;
	public static final int RESUME = 2;

	private static final int CHECK_TICKS = CTimer.ceilSteps(2.0f);
	private static final int CHECKS_UNTIL_BUFFS_EXPIRE = 2;

	private int tick = 0;
	private int checksWhileDisabled = -1;
	private boolean waitingForCheck = false;
	private boolean justStarted = false;

	public void start() {
		this.justStarted = true;
		this.tick = 0;
		this.checksWhileDisabled = -1;
		this.waitingForCheck = false;
	}

	/**
	 * @return true while the ability was just added and has not ticked yet (enabled on add, not re-enabled)
	 */
	public boolean isJustStarted() {
		return this.justStarted;
	}

	public void disabled() {
		this.checksWhileDisabled = 0;
		this.waitingForCheck = false;
	}

	public void enabled() {
		this.checksWhileDisabled = -1;
		this.waitingForCheck = true;
	}

	/**
	 * One tick of a disabled aura.
	 *
	 * @return true when the buffs it still holds must be removed now
	 */
	public boolean tickDisabled() {
		if (advance() && (this.checksWhileDisabled >= 0)) {
			this.checksWhileDisabled++;
			if (this.checksWhileDisabled >= CHECKS_UNTIL_BUFFS_EXPIRE) {
				this.checksWhileDisabled = -1;
				return true;
			}
		}
		return false;
	}

	/**
	 * One tick of an enabled aura.
	 *
	 * @return {@link #WAIT} while a re-enabled aura waits for its next check, {@link #RESUME} on that check (scan
	 *         now), otherwise {@link #RUN}
	 */
	public int tickEnabled() {
		final boolean check = advance();
		if (this.waitingForCheck) {
			if (!check) {
				return WAIT;
			}
			this.waitingForCheck = false;
			return RESUME;
		}
		return RUN;
	}

	private boolean advance() {
		this.justStarted = false;
		final boolean check = this.tick == 0;
		this.tick = (this.tick + 1) % CHECK_TICKS;
		return check;
	}
}
