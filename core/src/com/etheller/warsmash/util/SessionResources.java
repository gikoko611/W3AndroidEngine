package com.etheller.warsmash.util;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.utils.Disposable;

/**
 * Textures and sounds are created from many places while a map is on screen and nothing else owns them. The menu makes
 * its own outside of a session, so whatever is tracked here belongs to the map alone.
 */
public final class SessionResources {
	private static List<Disposable> current;

	private SessionResources() {
	}

	public static void begin() {
		current = new ArrayList<>();
	}

	public static <T extends Disposable> T track(final T resource) {
		if ((current != null) && (resource != null)) {
			current.add(resource);
		}
		return resource;
	}

	public static List<Disposable> end() {
		final List<Disposable> resources = current;
		current = null;
		return resources;
	}
}
