package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.badlogic.gdx.audio.Sound;
import com.etheller.warsmash.testing.TestObjects;
import com.etheller.warsmash.viewer5.handlers.w3x.UnitSound;

/**
 * A sound label with several files plays one of them each start, picked the same way on every computer, and scripts
 * wait by the length of the file picked.
 */
class LabelSoundVariantTest {
	private static final float[] LENGTHS = { 1.0f, 2.5f, 4.0f };

	@SuppressWarnings("unchecked")
	private static UnitSound label() {
		final UnitSound sound = new UnitSound(null, 1, 1, 0, 0, 0, 0, false);
		for (final float length : LENGTHS) {
			((List<String>) TestObjects.get(sound, "soundPaths")).add("Sound\\Test" + length + ".wav");
			((List<Sound>) TestObjects.get(sound, "sounds")).add(null);
			((List<Float>) TestObjects.get(sound, "durations")).add(length);
		}
		return sound;
	}

	private static List<Float> durationsOfStarts(final int[] ticks) {
		final CSoundFromLabel sound = new CSoundFromLabel(label(), null, false, false, false, 0, 0);
		final List<Float> durations = new ArrayList<>();
		for (final int tick : ticks) {
			sound.start(tick);
			durations.add(sound.getDuration());
		}
		return durations;
	}

	@Test
	void sameStartsPickTheSameFiles() {
		final int[] ticks = { 3, 40, 41, 41, 200, 1234, 1234, 5000 };
		assertEquals(durationsOfStarts(ticks), durationsOfStarts(ticks));
	}

	@Test
	void durationIsThatOfTheFilePlayed() {
		final Set<Float> seen = new HashSet<>();
		final int[] ticks = new int[60];
		for (int i = 0; i < ticks.length; i++) {
			ticks[i] = i * 7;
		}
		for (final float duration : durationsOfStarts(ticks)) {
			seen.add(duration);
		}
		assertEquals(LENGTHS.length, seen.size(), "every file is played over many starts: " + seen);
	}

	@Test
	void beforeAnyStartTheFirstFileCounts() {
		final CSoundFromLabel sound = new CSoundFromLabel(label(), null, false, false, false, 0, 0);
		assertEquals(LENGTHS[0], sound.getDuration(), 1e-6f);
	}

	@Test
	void variantIsInRange() {
		for (int tick = -50; tick < 500; tick += 13) {
			for (int count = 1; count < 6; count++) {
				final int variant = CSoundFromLabel.variant(tick, tick & 3, count);
				assertTrue((variant >= 0) && (variant < count));
			}
		}
	}
}
