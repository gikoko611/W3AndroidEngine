package com.etheller.warsmash.input.touch;

/**
 * How much of the bottom of the screen the platform's on-screen keyboard covers. The game surface stays full screen
 * under the keyboard (it is neither resized nor panned), so text lines that sit low have to move up by themselves.
 */
public final class OnscreenKeyboard {
	private static volatile int coveredHeightPixels;

	private OnscreenKeyboard() {
	}

	/** Set by the platform when the keyboard opens, closes or changes height. */
	public static void setCoveredHeightPixels(final int pixels) {
		coveredHeightPixels = Math.max(0, pixels);
	}

	public static int getCoveredHeightPixels() {
		return coveredHeightPixels;
	}
}
