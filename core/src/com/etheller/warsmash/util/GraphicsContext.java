package com.etheller.warsmash.util;

/**
 * Android may drop the GL context while the app is in the background, and every texture, buffer, shader and
 * framebuffer goes with it. Objects made before that hold names that the new context may already have given to
 * objects made since, so what was made with GL calls keeps the GL it was made with, and the platform makes that GL
 * leave deletes alone once its context is lost.
 */
public final class GraphicsContext {
	private static int generation;

	private GraphicsContext() {
	}

	/** Changes each time the context is lost; GL objects kept across screens are made again when it changed. */
	public static int generation() {
		return generation;
	}

	/** On the rendering thread, once a lost context was replaced: runs the disposal of what the old one held. */
	public static void disposeLost(final Runnable disposal) {
		generation++;
		try {
			disposal.run();
		}
		catch (final RuntimeException e) {
			Log.error(e);
		}
	}
}
