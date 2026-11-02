package com.etheller.warsmash.viewer5.handlers.w3x.simulation.util;

/**
 * A script ubersplat (CreateUbersplat).
 */
public interface SimulationRenderComponentUbersplat {
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
			return copy;
		}
	}
}
