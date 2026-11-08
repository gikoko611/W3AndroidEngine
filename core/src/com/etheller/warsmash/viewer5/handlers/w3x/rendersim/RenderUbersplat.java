package com.etheller.warsmash.viewer5.handlers.w3x.rendersim;

import com.etheller.warsmash.units.Element;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.SplatModel;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentUbersplat;

/**
 * A script ubersplat. Its row of Splats\UberSplatData.slk gives a life of birth (start to middle colour), pause
 * (middle colour; PauseTime 0 or forcePaused hold it until FinishUbersplat) and decay (middle to end colour), after
 * which it is gone. Measured on 1.29: the original draws a script ubersplat only after SetUbersplatRenderAlways true
 * or FinishUbersplat, the colour columns do not fade it (only the script alpha does), and a name missing from the table
 * still gives a handle.
 */
public final class RenderUbersplat implements SimulationRenderComponentUbersplat {
	private static int nextKey;

	private final War3MapViewer viewer;
	private final State state;
	private final String modelKey;
	private SplatModel model;
	private final float birthTime;
	private final float pauseTime;
	private final float decayTime;
	private final float[] startColor = new float[3];
	private final float[] middleColor = new float[3];
	private final float[] endColor = new float[3];
	private float startGameTime;
	private Object savedForm;

	public RenderUbersplat(final War3MapViewer viewer, final State state) {
		this.viewer = viewer;
		this.state = state;
		this.modelKey = "ubersplat#" + (nextKey++);
		final Element row = state.name == null ? null : viewer.terrain.uberSplatTable.get(state.name);
		if (row != null) {
			this.birthTime = row.getFieldFloatValue("BirthTime");
			this.pauseTime = row.getFieldFloatValue("PauseTime");
			this.decayTime = row.getFieldFloatValue("Decay");
			readColor(row, "Start", this.startColor);
			readColor(row, "Middle", this.middleColor);
			readColor(row, "End", this.endColor);
			final String texture = row.getField("Dir") + "\\" + row.getField("file") + ".blp";
			this.model = viewer.terrain.addSingleUberSplat(this.modelKey, texture, state.x, state.y,
					row.getFieldFloatValue("Scale"));
		}
		else {
			this.birthTime = 0;
			this.pauseTime = 0;
			this.decayTime = 0;
		}
		this.startGameTime = gameTime() - state.age;
		update();
	}

	private static void readColor(final Element row, final String prefix, final float[] color) {
		color[0] = Math.min(255, row.getFieldFloatValue(prefix + "R")) / 255f;
		color[1] = Math.min(255, row.getFieldFloatValue(prefix + "G")) / 255f;
		color[2] = Math.min(255, row.getFieldFloatValue(prefix + "B")) / 255f;
	}

	private float gameTime() {
		return this.viewer.simulation.getGameTurnTick() * WarsmashConstants.SIMULATION_STEP_TIME;
	}

	/** Brings the drawn colour to the splat's age; false once it is gone. */
	public boolean update() {
		if (this.model == null) {
			return !this.state.destroyed;
		}
		final State state = this.state;
		state.age = gameTime() - this.startGameTime;
		final float birth = state.noBirthTime ? 0 : this.birthTime;
		final float[] color = this.model.color;
		final float decayStart;
		if (state.finished) {
			decayStart = state.finishedAge;
		}
		else if (state.forcePaused || (this.pauseTime <= 0)) {
			decayStart = Float.POSITIVE_INFINITY;
		}
		else {
			decayStart = birth + this.pauseTime;
		}
		if (state.age >= decayStart) {
			if ((state.age - decayStart) >= this.decayTime) {
				removeModel();
				return false;
			}
			mix(this.middleColor, this.endColor, (state.age - decayStart) / this.decayTime, color);
		}
		else if (state.age < birth) {
			mix(this.startColor, this.middleColor, state.age / birth, color);
		}
		else {
			mix(this.middleColor, this.middleColor, 0, color);
		}
		color[0] *= state.red / 255f;
		color[1] *= state.green / 255f;
		color[2] *= state.blue / 255f;
		final boolean drawn = state.shown && state.render && (state.renderAlways || state.finished);
		color[3] = drawn ? Math.max(0, Math.min(255, state.alpha)) / 255f : 0;
		return true;
	}

	private static void mix(final float[] a, final float[] b, final float t, final float[] out) {
		for (int i = 0; i < 3; i++) {
			out[i] = a[i] + ((b[i] - a[i]) * t);
		}
	}

	private void removeModel() {
		if (this.model != null) {
			this.viewer.terrain.removeSingleUberSplat(this.modelKey, this.model);
			this.model = null;
		}
	}

	@Override
	public State getState() {
		return this.state;
	}

	public State captureState() {
		if (this.model != null) {
			this.state.age = gameTime() - this.startGameTime;
		}
		return this.state.copy();
	}

	public boolean isGone() {
		return this.state.destroyed || (this.model == null);
	}

	@Override
	public void destroy() {
		this.state.destroyed = true;
		removeModel();
	}

	@Override
	public void reset() {
		this.startGameTime = gameTime();
		this.state.finished = false;
		update();
	}

	@Override
	public void finish() {
		if (!this.state.finished) {
			float age = gameTime() - this.startGameTime;
			if (!this.state.forcePaused && (this.pauseTime > 0)) {
				age = Math.min(age, (this.state.noBirthTime ? 0 : this.birthTime) + this.pauseTime);
			}
			this.state.finished = true;
			this.state.finishedAge = age;
			update();
		}
	}

	@Override
	public void show(final boolean show) {
		this.state.shown = show;
		update();
	}

	@Override
	public void setRender(final boolean render) {
		this.state.render = render;
		update();
	}

	@Override
	public void setRenderAlways(final boolean renderAlways) {
		this.state.renderAlways = renderAlways;
		update();
	}

	public Object getSavedForm() {
		return this.savedForm;
	}

	public void setSavedForm(final Object savedForm) {
		this.savedForm = savedForm;
	}
}
