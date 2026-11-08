package com.etheller.warsmash.viewer5.handlers.w3x.simulation.util;

import com.etheller.interpreter.ast.util.CHandle;
import com.etheller.warsmash.viewer5.handlers.w3x.save.SavedDefault;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.HandleIdAllocator;

/**
 * A script ubersplat (CreateUbersplat).
 */
public interface SimulationRenderComponentUbersplat extends CHandle {
	State getState();

	@Override
	default int getHandleId() {
		final State state = getState();
		return state == null ? HandleIdAllocator.UNNUMBERED : state.handleId;
	}
	void destroy();

	void reset();

	void finish();

	void show(boolean show);

	void setRender(boolean render);

	void setRenderAlways(boolean renderAlways);

	/**
	 * What a script ubersplat is made of and where it is in its life, enough to make it again.
	 */
	final class State {
		public String name;
		public float x;
		public float y;
		public int red;
		public int green;
		public int blue;
		public int alpha;
		public boolean forcePaused;
		public boolean noBirthTime;
		public boolean shown = true;
		public boolean render = true;
		public boolean renderAlways;
		public boolean finished;
		public boolean destroyed;
		/** Seconds of game time since the splat was made or last reset. */
		public float age;
		/** Age at which FinishUbersplat started its decay. */
		public float finishedAge;
		/** In the ubersplat pool; -1 for a name the splat table does not have (measured on 1.29). */
		@SavedDefault("-2147483648")
		public int handleId = HandleIdAllocator.UNNUMBERED;
		/** Measured on 1.29: an ubersplat also takes a number of the image pool. */
		@SavedDefault("-1")
		public int imageId = -1;

		public State copy() {
			final State copy = new State();
			copy.name = this.name;
			copy.x = this.x;
			copy.y = this.y;
			copy.red = this.red;
			copy.green = this.green;
			copy.blue = this.blue;
			copy.alpha = this.alpha;
			copy.forcePaused = this.forcePaused;
			copy.noBirthTime = this.noBirthTime;
			copy.shown = this.shown;
			copy.render = this.render;
			copy.renderAlways = this.renderAlways;
			copy.finished = this.finished;
			copy.destroyed = this.destroyed;
			copy.age = this.age;
			copy.finishedAge = this.finishedAge;
			copy.handleId = this.handleId;
			copy.imageId = this.imageId;
			return copy;
		}
	}
}
