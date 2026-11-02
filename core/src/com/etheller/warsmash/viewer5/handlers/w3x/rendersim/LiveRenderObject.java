package com.etheller.warsmash.viewer5.handlers.w3x.rendersim;

/**
 * The drawn object behind a saved stand-in. A saved game leaves this out, so a loaded stand-in starts with none.
 */
public final class LiveRenderObject<T> {
	public final T value;

	public LiveRenderObject(final T value) {
		this.value = value;
	}
}
