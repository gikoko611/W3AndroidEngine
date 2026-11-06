package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * RegisterStackedSound: region ambience such as lakes, streams and waves. All areas a sound is registered for share
 * one loop, heard from the area nearest to the camera target and only within the sound's distance cutoff.
 * Calibration (1.29 plays no sound in measurement runs): distances are taken from the camera target, not the eye, otherwise
 * the cutoff of 900 of the water loops would never be reached at the usual camera distance of 1650.
 */
public final class CStackedSounds {
	private final Map<CSoundFilename, Stack> stacks = new IdentityHashMap<>();

	public void register(final CSound sound, final boolean byPosition, final float width, final float height) {
		if (!(sound instanceof CSoundFilename)) {
			return;
		}
		final CSoundFilename filename = (CSoundFilename) sound;
		Stack stack = this.stacks.get(filename);
		if (stack == null) {
			stack = new Stack(filename);
			this.stacks.put(filename, stack);
		}
		stack.areas.add(area(filename, byPosition, width, height));
	}

	public void unregister(final CSound sound, final boolean byPosition, final float width, final float height) {
		final Stack stack = this.stacks.get(sound);
		if (stack == null) {
			return;
		}
		final Area removed = area(stack.sound, byPosition, width, height);
		final Iterator<Area> iterator = stack.areas.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().sameAs(removed)) {
				iterator.remove();
				break;
			}
		}
		if (stack.areas.isEmpty()) {
			stack.stop();
			this.stacks.remove(sound);
		}
	}

	private static Area area(final CSoundFilename sound, final boolean byPosition, final float width,
			final float height) {
		final CSoundParameters parameters = sound.getParameters();
		if (!byPosition) {
			return new Area(false, 0, 0, 0, 0);
		}
		return new Area(true, parameters.x, parameters.y, width / 2, height / 2);
	}

	/**
	 * @param rightX the horizontal direction to the right of the screen, for the side the sound comes from
	 */
	public void update(final float targetX, final float targetY, final float rightX, final float rightY) {
		final float rightLength = (float) Math.sqrt((rightX * rightX) + (rightY * rightY));
		for (final Stack stack : this.stacks.values()) {
			float nearest = Float.MAX_VALUE;
			float nearestDX = 0;
			float nearestDY = 0;
			for (final Area area : stack.areas) {
				if (!area.positioned) {
					nearest = 0;
					nearestDX = 0;
					nearestDY = 0;
					break;
				}
				final float dx = Math.max(Math.abs(targetX - area.x) - area.halfWidth, 0)
						* Math.signum(area.x - targetX);
				final float dy = Math.max(Math.abs(targetY - area.y) - area.halfHeight, 0)
						* Math.signum(area.y - targetY);
				final float distance = (float) Math.sqrt((dx * dx) + (dy * dy));
				if (distance < nearest) {
					nearest = distance;
					nearestDX = dx;
					nearestDY = dy;
				}
			}
			final CSoundParameters parameters = stack.sound.getParameters();
			if (nearest > parameters.distanceCutoff) {
				stack.stop();
				continue;
			}
			if (stack.id == -1) {
				stack.id = stack.sound.startStacked();				if (stack.id == -1) {
					continue;
				}
			}
			final float reference = Math.max(parameters.minDistance, 1f);
			final float clamped = Math.min(Math.max(nearest, reference), Math.max(parameters.maxDistance, reference));
			final float gain = reference / clamped;
			float pan = 0;
			if ((nearest > 0) && (rightLength > 0)) {
				final float side = ((nearestDX * rightX) + (nearestDY * rightY)) / rightLength;
				pan = Math.max(-1f, Math.min(1f, side / Math.max(nearest, reference)));
			}
			stack.sound.setStackedLevel(stack.id, gain, pan);
		}
	}

	public void stopAll() {
		for (final Stack stack : this.stacks.values()) {
			stack.stop();
		}
		this.stacks.clear();
	}

	private static final class Stack {
		private final CSoundFilename sound;
		private final List<Area> areas = new ArrayList<>();
		private long id = -1;

		private Stack(final CSoundFilename sound) {
			this.sound = sound;
		}

		private void stop() {
			if (this.id != -1) {
				this.sound.stopStacked(this.id);
				this.id = -1;
			}
		}
	}

	private static final class Area {
		private final boolean positioned;
		private final float x;
		private final float y;
		private final float halfWidth;
		private final float halfHeight;

		private Area(final boolean positioned, final float x, final float y, final float halfWidth,
				final float halfHeight) {
			this.positioned = positioned;
			this.x = x;
			this.y = y;
			this.halfWidth = halfWidth;
			this.halfHeight = halfHeight;
		}

		private boolean sameAs(final Area other) {
			return (this.positioned == other.positioned) && (this.x == other.x) && (this.y == other.y)
					&& (this.halfWidth == other.halfWidth) && (this.halfHeight == other.halfHeight);
		}
	}
}
