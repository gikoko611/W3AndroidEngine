package com.etheller.warsmash.util;

import java.util.Random;

/**
 * Random.nextFloat(bound) and Random.nextInt(origin, bound) exist on Android only from API 35. These give the same
 * values as the JDK 17 versions, so desktop and Android games stay in step.
 */
public final class RandomBounds {
	private RandomBounds() {
	}

	public static float nextFloat(final Random random, final float bound) {
		if (!((bound > 0) && (bound < Float.POSITIVE_INFINITY))) {
			throw new IllegalArgumentException("bound must be finite and positive");
		}
		final float r = random.nextFloat() * bound;
		return r >= bound ? Math.nextDown(bound) : r;
	}

	public static int nextInt(final Random random, final int origin, final int bound) {
		if (origin >= bound) {
			throw new IllegalArgumentException("bound must be greater than origin");
		}
		int r = random.nextInt();
		final int n = bound - origin;
		final int m = n - 1;
		if ((n & m) == 0) {
			return (r & m) + origin;
		}
		if (n > 0) {
			for (int u = r >>> 1; ((u + m) - (r = u % n)) < 0; u = random.nextInt() >>> 1) {
			}
			return r + origin;
		}
		while ((r < origin) || (r >= bound)) {
			r = random.nextInt();
		}
		return r;
	}
}
