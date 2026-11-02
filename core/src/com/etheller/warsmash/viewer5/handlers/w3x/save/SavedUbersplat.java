package com.etheller.warsmash.viewer5.handlers.w3x.save;

import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.LiveRenderObject;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderUbersplat;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentUbersplat;

/**
 * Takes the place of a script ubersplat in a saved game: what is needed to show it again where it was in its life.
 */
public final class SavedUbersplat implements SimulationRenderComponentUbersplat {
	private State state;
	private LiveRenderObject<RenderUbersplat> liveObject;

	private RenderUbersplat live() {
		return this.liveObject == null ? null : this.liveObject.value;
	}

	static SavedUbersplat of(final RenderUbersplat splat) {
		SavedUbersplat saved = (SavedUbersplat) splat.getSavedForm();
		if (saved == null) {
			saved = new SavedUbersplat();
			saved.liveObject = new LiveRenderObject<>(splat);
			splat.setSavedForm(saved);
		}
		saved.state = splat.captureState();
		return saved;
	}

	void show(final War3MapViewer viewer) {
		if ((live() != null) || (this.state == null) || this.state.destroyed) {
			return;
		}
		final RenderUbersplat splat = viewer.createUbersplat(this.state.copy());
		splat.setSavedForm(this);
		this.liveObject = new LiveRenderObject<>(splat);
	}

	@Override
	public void destroy() {
		this.state.destroyed = true;
		if (live() != null) {
			live().destroy();
		}
	}

	@Override
	public void reset() {
		if (live() != null) {
			live().reset();
		}
	}

	@Override
	public void finish() {
		if (live() != null) {
			live().finish();
		}
	}

	@Override
	public void show(final boolean show) {
		this.state.shown = show;
		if (live() != null) {
			live().show(show);
		}
	}

	@Override
	public void setRender(final boolean render) {
		this.state.render = render;
		if (live() != null) {
			live().setRender(render);
		}
	}

	@Override
	public void setRenderAlways(final boolean renderAlways) {
		this.state.renderAlways = renderAlways;
		if (live() != null) {
			live().setRenderAlways(renderAlways);
		}
	}
}
