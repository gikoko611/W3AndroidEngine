package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;
import java.util.zip.CRC32;

import org.junit.jupiter.api.Test;

/**
 * Blocks hashed apart and joined must give the checksum of their bytes hashed in one go, or kept blocks would change
 * the checksum lines.
 */
class SimulationChecksumTest {
	@Test
	void joinedBlocksHashAsOne() {
		final Random random = new Random(7);
		for (final int[] lengths : new int[][] { { 1, 1 }, { 0, 5 }, { 5, 0 }, { 37, 64 }, { 4096, 3 },
				{ 65536, 70000 }, { 1, 2, 3, 400, 5000 } }) {
			int joined = 0;
			final CRC32 whole = new CRC32();
			for (final int length : lengths) {
				final byte[] bytes = new byte[length];
				random.nextBytes(bytes);
				final CRC32 part = new CRC32();
				part.update(bytes);
				whole.update(bytes);
				joined = SimulationChecksum.combine(joined, (int) part.getValue(),
						SimulationChecksum.crcShift(length));
			}
			assertEquals((int) whole.getValue(), joined);
		}
	}
}
