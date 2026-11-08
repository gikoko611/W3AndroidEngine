package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import com.etheller.warsmash.util.Log;

public class CMIDISound extends AbstractCSound {
	private final String soundLabel;
	private final int fadeInRate;
	private final int fadeOutRate;

	public CMIDISound(final String soundLabel, final int fadeInRate, final int fadeOutRate) {
		super(0);
		this.soundLabel = soundLabel;
		this.fadeInRate = fadeInRate;
		this.fadeOutRate = fadeOutRate;
	}

	@Override
	protected boolean isLooping() {
		return false;
	}

	@Override
	protected void play() {
		Log.warn(
				"Not starting MIDI sound because we don't have a LibGDX API to play those: " + this.soundLabel);
	}

	@Override
	protected void silence() {
	}
}
