package com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.etheller.warsmash.testing.TestObjects;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;

/**
 * The timer part of a simulation step on a simulation without a map: timers started during a step join the queue at
 * the start of the next one, then every timer due by the step fires in order of its tick, in start order within a
 * tick.
 */
class TimerQueueTest {
	private CSimulation simulation;
	private final List<String> fired = new ArrayList<>();
	private final List<Integer> firedTicks = new ArrayList<>();

	private final class TestTimer extends CTimer {
		private final String name;
		private final Consumer<CSimulation> callback;

		TestTimer(final String name, final float timeout, final Consumer<CSimulation> callback) {
			this.name = name;
			this.callback = callback;
			setTimeoutTime(timeout);
		}

		TestTimer(final String name, final float timeout) {
			this(name, timeout, simulation -> {
			});
		}

		@Override
		public void onFire(final CSimulation simulation) {
			TimerQueueTest.this.fired.add(this.name);
			TimerQueueTest.this.firedTicks.add(simulation.getGameTurnTick());
			this.callback.accept(simulation);
		}
	}

	@BeforeEach
	void setUp() {
		this.simulation = TestObjects.allocate(CSimulation.class);
		TestObjects.set(this.simulation, "activeTimers", new LinkedList<CTimer>());
		TestObjects.set(this.simulation, "addedTimers", new ArrayList<CTimer>());
		TestObjects.set(this.simulation, "timerFiringExactTime", Double.NaN);
	}

	private void step() {
		TestObjects.set(this.simulation, "gameTurnTick", this.simulation.getGameTurnTick() + 1);
		TestObjects.call(this.simulation, "fireDueTimers", new Class<?>[0]);
	}

	private void steps(final int count) {
		for (int i = 0; i < count; i++) {
			step();
		}
	}

	private static int ticks(final float seconds) {
		return (int) (seconds / WarsmashConstants.SIMULATION_STEP_TIME);
	}

	@Test
	void firesByTickThenByStartOrder() {
		final TestTimer a = new TestTimer("a", 0.5f);
		final TestTimer b = new TestTimer("b", 0.2f);
		final TestTimer c = new TestTimer("c", 0.5f);
		final TestTimer d = new TestTimer("d", 0.1f);
		for (final TestTimer timer : new TestTimer[] { a, b, c, d }) {
			timer.start(this.simulation);
		}
		steps(20);
		assertEquals(Arrays.asList("d", "b", "a", "c"), this.fired);
		assertEquals(Arrays.asList(ticks(0.1f), ticks(0.2f), ticks(0.5f), ticks(0.5f)), this.firedTicks);
	}

	@Test
	void restartMovesTheTimerBackInTheQueue() {
		final TestTimer a = new TestTimer("a", 0.3f);
		final TestTimer b = new TestTimer("b", 0.3f);
		a.start(this.simulation);
		b.start(this.simulation);
		steps(2);
		a.start(this.simulation);
		steps(20);
		assertEquals(Arrays.asList("b", "a"), this.fired);
		assertEquals(Arrays.asList(ticks(0.3f), 2 + ticks(0.3f)), this.firedTicks);
	}

	@Test
	void timerStartedInACallbackWaitsForTheNextStep() {
		final TestTimer later = new TestTimer("later", 0f);
		final TestTimer first = new TestTimer("first", 0.1f, simulation -> later.start(simulation));
		first.start(this.simulation);
		steps(10);
		assertEquals(Arrays.asList("first", "later"), this.fired);
		assertEquals(Arrays.asList(ticks(0.1f), ticks(0.1f) + 1), this.firedTicks);
	}

	@Test
	void timerPausedByAnEarlierCallbackDoesNotFireThatStep() {
		final TestTimer second = new TestTimer("second", 0.2f);
		final TestTimer first = new TestTimer("first", 0.2f, simulation -> second.pause(simulation));
		first.start(this.simulation);
		second.start(this.simulation);
		steps(20);
		assertEquals(Arrays.asList("first"), this.fired);
		assertFalse(second.isRunning());
	}

	@Test
	void periodicTimerKeepsItsPeriod() {
		final TestTimer periodic = new TestTimer("periodic", 0.25f);
		periodic.setRepeats(true);
		periodic.start(this.simulation);
		final int period = ticks(0.25f);
		steps((3 * period) + 1);
		assertEquals(Arrays.asList(period, 2 * period, 3 * period), this.firedTicks);
	}

	@Test
	void decimalLengthsKeepTheirLastStep() {
		// As floats these are a little under a whole number of steps: 0.65 is 12.9999995 of them.
		final TestTimer a = new TestTimer("a", 0.65f);
		final TestTimer b = new TestTimer("b", 1.05f);
		final TestTimer c = new TestTimer("c", 2.1f);
		final TestTimer d = new TestTimer("d", 0.07f);
		for (final TestTimer timer : new TestTimer[] { a, b, c, d }) {
			timer.start(this.simulation);
		}
		steps(60);
		assertEquals(Arrays.asList("d", "a", "b", "c"), this.fired);
		assertEquals(Arrays.asList(1, 13, 21, 42), this.firedTicks);
		assertEquals(13, CTimer.wholeSteps(0.65f));
		assertEquals(0, CTimer.wholeSteps(0.04f));
		assertEquals(2000, CTimer.wholeSteps(100f));
	}

	@Test
	void killedPeriodicTimerStops() {
		final TestTimer periodic = new TestTimer("periodic", 0.1f);
		periodic.setRepeats(true);
		periodic.start(this.simulation);
		final int period = ticks(0.1f);
		steps((2 * period) + 1);
		periodic.kill(this.simulation);
		steps(5 * period);
		assertEquals(2, this.fired.size());
		assertFalse(periodic.isRunning());
	}
}
