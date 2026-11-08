package com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * Lengths in seconds as data and scripts give them (floats, or doubles in code) turned into simulation steps: a length
 * that is a whole number of steps keeps every step whichever way it was rounded on the way in.
 */
class StepConversionTest {
	@Test
	void wholeMultiplesKeepEveryStep() {
		for (int steps = 0; steps <= 400000; steps++) {
			final String text = BigDecimal.valueOf(steps).multiply(new BigDecimal("0.05")).toPlainString();
			final float asFloat = Float.parseFloat(text);
			final double asDouble = Double.parseDouble(text);
			assertEquals(steps, CTimer.wholeSteps(asFloat), text + " (float)");
			assertEquals(steps, CTimer.wholeSteps(asDouble), text + " (double)");
			assertEquals(steps, CTimer.ceilSteps(asFloat), text + " (float, ceil)");
			assertEquals(steps, CTimer.ceilSteps(asDouble), text + " (double, ceil)");
		}
	}

	@Test
	void floatQuotientsThatRoundBelowAStep() {
		// 0.65f / 0.05f is 12.999999f and 0.4 / 0.05f is 7.9999998, both lost a step when truncated.
		assertEquals(12, (int) (0.65f / 0.05f));
		assertEquals(13, CTimer.wholeSteps(0.65f));
		assertEquals(21, CTimer.wholeSteps(1.05f));
		assertEquals(26, CTimer.wholeSteps(1.3f));
		assertEquals(8, CTimer.wholeSteps(0.4));
		assertEquals(10, CTimer.wholeSteps(0.5));
		// 0.1f / (double) 0.05f is 2.0000000149, which a plain ceiling would make 3.
		assertEquals(2, CTimer.ceilSteps(0.1f));
	}

	@Test
	void partStepsDropOrCount() {
		assertEquals(12, CTimer.wholeSteps(0.63f));
		assertEquals(13, CTimer.wholeSteps(0.67f));
		assertEquals(0, CTimer.wholeSteps(0.04f));
		assertEquals(13, CTimer.ceilSteps(0.63f));
		assertEquals(14, CTimer.ceilSteps(0.67f));
		assertEquals(1, CTimer.ceilSteps(0.04f));
		assertEquals(1, CTimer.ceilSteps(0.001f));
		assertEquals(0, CTimer.ceilSteps(0f));
	}

	@Test
	void exactLengthsKeepTheirDecimals() {
		// The original game measured 0.63 / 0.65 / 0.67 s buffs and timed lives at their exact lengths.
		assertEquals(0.63, CTimer.exactLength(0.63f));
		assertEquals(0.65, CTimer.exactLength(0.65f));
		assertEquals(0.67, CTimer.exactLength(0.67f));
		assertEquals(0.433, CTimer.exactLength(0.433f));
		assertEquals(0.5, CTimer.exactLength(0.5f));
		assertEquals(0.0, CTimer.exactLength(0f));
		assertEquals(0.0, CTimer.exactLength(-1f));
		for (int ms = 0; ms <= 1000000; ms++) {
			final String text = BigDecimal.valueOf(ms).movePointLeft(3).toPlainString();
			assertEquals(Double.parseDouble(text), CTimer.exactLength(Float.parseFloat(text)), text);
		}
	}
}
