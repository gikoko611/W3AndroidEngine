package com.etheller.warsmash.viewer5.handlers.w3x.save;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.LiveRenderObject;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderWeatherEffect;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentWeather;

/**
 * Takes the place of a weather effect in a saved game: what is needed to show it again.
 */
public final class SavedWeather implements SimulationRenderComponentWeather {
	private War3ID weatherId;
	private float[] rect;
	private boolean enabled;
	private boolean removed;
	private LiveRenderObject<SimulationRenderComponentWeather> liveObject;

	private SimulationRenderComponentWeather live() {
		return this.liveObject == null ? null : this.liveObject.value;
	}

	static SavedWeather of(final RenderWeatherEffect weather) {
		SavedWeather saved = (SavedWeather) weather.getSavedForm();
		if (saved == null) {
			saved = new SavedWeather();
			saved.weatherId = weather.getWeatherId();
			saved.rect = weather.getRect();
			saved.liveObject = new LiveRenderObject<>(weather);
			weather.setSavedForm(saved);
		}
		saved.enabled = weather.isEnabled();
		saved.removed = weather.isRemoved();
		return saved;
	}

	void show(final War3MapViewer viewer) {
		if (this.removed || (live() != null) || (this.weatherId == null)) {
			return;
		}
		final SimulationRenderComponentWeather weather = viewer.createWeatherEffect(this.weatherId, this.rect[0],
				this.rect[1], this.rect[2], this.rect[3]);
		this.liveObject = new LiveRenderObject<>(weather);
		if (weather instanceof RenderWeatherEffect) {
			((RenderWeatherEffect) weather).setSavedForm(this);
		}
		weather.setEnabled(this.enabled);
	}

	@Override
	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
		if (live() != null) {
			live().setEnabled(enabled);
		}
	}

	@Override
	public void remove() {
		this.removed = true;
		if (live() != null) {
			live().remove();
		}
	}

	@Override
	public boolean isRemoved() {
		return this.removed || ((live() != null) && live().isRemoved());
	}
}
