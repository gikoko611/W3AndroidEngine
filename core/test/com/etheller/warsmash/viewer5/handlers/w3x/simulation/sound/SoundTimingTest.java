package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Script timing of a sound counted in simulation steps (20 per second), with no audio device: as measured in 1.29, a
 * wait counts from the last start by the SetSoundDuration value, else by the file's length, and stopping the sound
 * does not shorten it.
 */
class SoundTimingTest {
	private static final float EPSILON = 1e-4f;

	private static final class TestSound extends AbstractCSound {
		private final boolean looping;
		private int plays;

		private TestSound(final float fileDuration, final boolean looping) {
			super(fileDuration);
			this.looping = looping;
		}

		@Override
		protected boolean isLooping() {
			return this.looping;
		}

		@Override
		protected void play() {
			this.plays++;
		}

		@Override
		protected void silence() {
		}
	}

	@Test
	void setDurationWinsOverTheFile() {
		final TestSound sound = new TestSound(5.3f, false);
		assertEquals(5.3f, sound.getDuration(), EPSILON);
		sound.setDuration(2.0f);
		assertEquals(2.0f, sound.getDuration(), EPSILON);
		sound.setDuration(0);
		assertEquals(0f, sound.getDuration(), EPSILON);
	}

	@Test
	void playsForItsDurationFromTheStartStep() {
		final TestSound sound = new TestSound(5.3f, false);
		sound.setDuration(2.0f);
		assertFalse(sound.isPlaying(0));
		assertEquals(0f, sound.getRemainingTime(0), EPSILON);
		sound.start(10);
		assertEquals(1, sound.plays);
		assertTrue(sound.isPlaying(10));
		assertTrue(sound.isPlaying(49));
		assertFalse(sound.isPlaying(50));
		assertEquals(1.5f, sound.getRemainingTime(20), EPSILON);
		assertEquals(0f, sound.getRemainingTime(80), EPSILON);
	}

	@Test
	void stopEndsPlayingButNotTheWait() {
		final TestSound sound = new TestSound(0, false);
		sound.setDuration(2.0f);
		sound.start(0);
		sound.stop();
		assertFalse(sound.isPlaying(10));
		assertEquals(1.5f, sound.getRemainingTime(10), EPSILON);
	}

	@Test
	void restartCountsFromTheNewStart() {
		final TestSound sound = new TestSound(3.0f, false);
		sound.start(0);
		sound.start(20);
		assertEquals(3.0f, sound.getRemainingTime(20), EPSILON);
		assertTrue(sound.isPlaying(70));
		assertFalse(sound.isPlaying(80));
	}

	@Test
	void missingFileWithoutDurationIsOverAtOnce() {
		final TestSound sound = new TestSound(0, false);
		sound.start(5);
		assertFalse(sound.isPlaying(5));
		assertEquals(0f, sound.getRemainingTime(5), EPSILON);
	}

	@Test
	void loopPlaysUntilStopped() {
		final TestSound sound = new TestSound(1.0f, true);
		sound.start(0);
		assertTrue(sound.isPlaying(1000));
		sound.stop();
		assertFalse(sound.isPlaying(1001));
	}

	@Test
	void killedSoundDoesNotStartAgain() {
		final TestSound sound = new TestSound(1.0f, false);
		sound.start(0);
		sound.killWhenDone();
		assertFalse(sound.isKilled(10));
		assertTrue(sound.isKilled(20));
		sound.start(30);
		assertEquals(1, sound.plays);
		assertFalse(sound.isPlaying(30));
	}
}
