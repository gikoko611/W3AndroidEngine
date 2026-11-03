package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.TimeUtils;
import com.etheller.warsmash.viewer5.AudioBufferSource;
import com.etheller.warsmash.viewer5.AudioContext;
import com.etheller.warsmash.viewer5.AudioPanner;
import com.etheller.warsmash.viewer5.gl.Extensions;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderUnit;

public class CSoundFilename implements CSound {
	private final Sound sound;
	private final boolean looping;
	private final boolean stopWhenOutOfRange;
	private final int fadeInRate;
	private final int fadeOutRate;
	private final AudioContext audioContext;
	private final CSoundParameters parameters = new CSoundParameters();
	private final String eaxSetting;
	private long lastStartTimestamp;
	private float duration = -1;
	private long lastSoundId = -1;
	private boolean started;
	private boolean killWhenDone;

	public CSoundFilename(final Sound sound, final AudioContext audioContext, final boolean looping,
			final boolean stopWhenOutOfRange, final int fadeInRate, final int fadeOutRate, final String eaxSetting) {
		this.sound = sound;
		this.audioContext = audioContext;
		this.looping = looping;
		this.stopWhenOutOfRange = stopWhenOutOfRange;
		this.fadeInRate = fadeInRate;
		this.fadeOutRate = fadeOutRate;
		this.eaxSetting = eaxSetting;
	}

	@Override
	public void setPosition(final float x, final float y, final float z) {
		this.parameters.positioned = true;
		this.parameters.x = x;
		this.parameters.y = y;
		this.parameters.z = z;
	}

	@Override
	public void setVolume(final int volume) {
		this.parameters.volume = volume;
	}

	@Override
	public void setPitch(final float pitch) {
		this.parameters.pitch = pitch;
	}

	@Override
	public void setPitchVariance(final float variance) {
		this.parameters.pitchVariance = variance;
	}

	@Override
	public void setChannel(final int channel) {
		this.parameters.channel = channel;
	}

	@Override
	public void setDistances(final float minDistance, final float maxDistance) {
		this.parameters.minDistance = minDistance;
		this.parameters.maxDistance = maxDistance;
	}

	@Override
	public void setDistanceCutoff(final float cutoff) {
		this.parameters.distanceCutoff = cutoff;
	}

	@Override
	public void setConeAngles(final float inside, final float outside, final int outsideVolume) {
		this.parameters.coneInside = inside;
		this.parameters.coneOutside = outside;
		this.parameters.coneOutsideVolume = outsideVolume;
	}

	@Override
	public void setConeOrientation(final float x, final float y, final float z) {
		this.parameters.coneX = x;
		this.parameters.coneY = y;
		this.parameters.coneZ = z;
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
	public void killWhenDone() {
		this.killWhenDone = true;
	}

	@Override
	public boolean isKilled() {
		return this.killWhenDone && !isPlaying();
	}

	@Override
	public void start() {
		if ((this.audioContext == null) || isKilled()) {
			return;
		}
		final AudioPanner panner = this.audioContext.createPanner(this.stopWhenOutOfRange);
		final AudioBufferSource source = this.audioContext.createBufferSource();

		this.parameters.updatePositionFromUnit();

		// A 3D sound nobody placed plays at the listener rather than at the map origin.
		if (this.parameters.positioned) {
			panner.setPosition(this.parameters.x, this.parameters.y, this.parameters.z);
		}
		else {
			panner.setPosition(this.audioContext.listener.getX(), this.audioContext.listener.getY(),
					this.audioContext.listener.getZ());
		}
		panner.setDistances(Math.min(this.parameters.distanceCutoff, this.parameters.maxDistance),
				this.parameters.minDistance);
		panner.connect(this.audioContext.destination);

		// Source.
		source.buffer = this.sound;
		source.connect(panner);

		// Make a sound.
		this.lastSoundId = source.start(0, this.parameters.scaledVolume(1.0f, this.audioContext),
				this.parameters.startPitch(), this.looping);
		this.started = this.lastSoundId != -1;
		if (this.started && this.looping) {
			CSoundMixer.loopStarted(this.sound, this.lastSoundId, this.parameters.unmixedVolume(1.0f, this.audioContext),
					this.parameters.channel);
		}

		this.lastStartTimestamp = TimeUtils.millis();
	}

	@Override
	public void stop() {
		this.started = false;
		if (this.sound != null) {
			// Without an id this would also stop other sounds created from the same file.
			if (this.lastSoundId != -1) {
				this.sound.stop(this.lastSoundId);
			}
			else {
				this.sound.stop();
			}
		}
	}

	/**
	 * A stacked sound plays as one loop for all its areas; CStackedSounds sets its loudness and side from the nearest
	 * area every frame, so it is started unplaced.
	 */
	long startStacked() {
		if ((this.sound == null) || (this.audioContext == null)) {
			return -1;
		}
		return Extensions.audio.play(this.sound, 0f, this.parameters.startPitch(), 0, 0, 0, false, 1, 1, true);
	}

	void setStackedLevel(final long id, final float gain, final float pan) {
		final float volume = this.parameters.scaledVolume(1.0f, null) * gain;
		this.sound.setPan(id, pan, volume);
	}

	void stopStacked(final long id) {
		this.sound.stop(id);
	}

	CSoundParameters getParameters() {
		return this.parameters;
	}

	@Override
	public float getPredictedDuration() {
		if (this.duration >= 0) {
			return this.duration;
		}
		return Extensions.audio.getDuration(this.sound);
	}

	@Override
	public void setDuration(final float seconds) {
		this.duration = seconds;
	}

	@Override
	public boolean isPlaying() {
		return this.started && (this.looping || (getRemainingTimeToPlayOnTheDesyncLocalComputer() > 0));
	}

	@Override
	public float getRemainingTimeToPlayOnTheDesyncLocalComputer() {
		final long currentTime = TimeUtils.millis();
		final long deltaTime = currentTime - this.lastStartTimestamp;
		final float deltaTimeSeconds = deltaTime / 1000f;
		final float predictedDuration = getPredictedDuration();
		if (deltaTimeSeconds > predictedDuration) {
			return 0;
		}
		return predictedDuration - deltaTimeSeconds;
	}

}
