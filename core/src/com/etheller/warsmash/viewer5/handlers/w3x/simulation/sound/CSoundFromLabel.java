package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import com.etheller.warsmash.viewer5.AudioContext;
import com.etheller.warsmash.viewer5.handlers.w3x.UnitSound;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderUnit;

public class CSoundFromLabel extends AbstractCSound {
	private final UnitSound sound;
	private final boolean looping;
	private final boolean is3d;
	private final boolean stopWhenOutOfRange;
	private final int fadeInRate;
	private final int fadeOutRate;
	private final AudioContext audioContext;
	private final CSoundParameters parameters = new CSoundParameters();
	private int starts;
	private int variant;

	/**
	 * @param sound the label's sound, null when the label is unknown
	 */
	public CSoundFromLabel(final UnitSound sound, final AudioContext audioContext, final boolean looping,
			final boolean is3d, final boolean stopWhenOutOfRange, final int fadeInRate, final int fadeOutRate) {
		super(sound == null ? 0 : sound.getDuration());
		this.sound = sound;
		this.audioContext = audioContext;
		this.looping = looping;
		this.is3d = is3d;
		this.stopWhenOutOfRange = stopWhenOutOfRange;
		this.fadeInRate = fadeInRate;
		this.fadeOutRate = fadeOutRate;
	}

	@Override
	public void setPosition(final float x, final float y, final float z) {
		this.parameters.positioned = true;
		this.parameters.x = x;
		this.parameters.y = y;
		this.parameters.z = z;
	}

	@Override
	public void attachToUnit(final RenderUnit unit) {
		this.parameters.attachedUnit = unit;
	}

	@Override
	public void setDialogueSpeakerNameKey(final String key) {
		this.parameters.dialogueSpeakerNameKey = key;
	}

	@Override
	public String getDialogueSpeakerNameKey() {
		return this.parameters.dialogueSpeakerNameKey;
	}

	@Override
	public void setDialogueTextKey(final String key) {
		this.parameters.dialogueTextKey = key;
	}

	@Override
	public String getDialogueTextKey() {
		return this.parameters.dialogueTextKey;
	}

	@Override
	protected boolean isLooping() {
		return this.looping;
	}

	/**
	 * A label with several files plays one of them each start, and scripts wait by that file's length, so the file is
	 * picked from the game step and the count of starts: the same on every computer, with or without sound.
	 */
	@Override
	protected void starting(final int gameTurnTick) {
		if ((this.sound == null) || (this.sound.getSoundCount() == 0)) {
			return;
		}
		this.variant = variant(gameTurnTick, this.starts++, this.sound.getSoundCount());
		setFileDuration(this.sound.getDuration(this.variant));
	}

	static int variant(final int gameTurnTick, final int starts, final int count) {
		int hash = (gameTurnTick * 0x9E3779B1) + starts;
		hash ^= hash >>> 16;
		hash *= 0x85EBCA6B;
		hash ^= hash >>> 13;
		hash *= 0xC2B2AE35;
		hash ^= hash >>> 16;
		return Math.floorMod(hash, count);
	}

	@Override
	protected void play() {
		if (this.sound == null) {
			return;
		}
		this.parameters.updatePositionFromUnit();
		this.sound.play(this.audioContext, this.parameters.x, this.parameters.y, this.parameters.z, this.variant,
				this.looping ? Boolean.TRUE : null);
	}

	@Override
	protected void silence() {
		if ((this.sound != null) && (this.sound.getLastPlayedSound() != null)) {
			this.sound.getLastPlayedSound().stop();
		}
	}
}
