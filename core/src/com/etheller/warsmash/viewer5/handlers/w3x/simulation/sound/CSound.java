package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderUnit;

public interface CSound {
	void start();

	void stop();

	float getPredictedDuration();

	float getRemainingTimeToPlayOnTheDesyncLocalComputer();

	/**
	 * SetSoundDuration: scripts wait by this value, not by the length of the file.
	 */
	default void setDuration(final float seconds) {
	}

	default void setVolume(final int volume) {
	}

	default void setPitch(final float pitch) {
	}

	/** Each start picks a pitch up to this far either side of the set one (RANDOMPITCH labels). */
	default void setPitchVariance(final float variance) {
	}

	default void setChannel(final int channel) {
	}

	default void setPosition(final float x, final float y, final float z) {
	}

	default void setDistances(final float minDistance, final float maxDistance) {
	}

	default void setDistanceCutoff(final float cutoff) {
	}

	default void setConeAngles(final float inside, final float outside, final int outsideVolume) {
	}

	default void setConeOrientation(final float x, final float y, final float z) {
	}

	default void attachToUnit(final RenderUnit unit) {
	}

	default boolean isPlaying() {
		return getRemainingTimeToPlayOnTheDesyncLocalComputer() > 0;
	}

	/** KillSoundWhenDone: once the sound is not playing any more, it is released and cannot be started again. */
	default void killWhenDone() {
	}

	default boolean isKilled() {
		return false;
	}

	default void setDialogueSpeakerNameKey(final String key) {
	}

	default String getDialogueSpeakerNameKey() {
		return "";
	}

	default void setDialogueTextKey(final String key) {
	}

	default String getDialogueTextKey() {
		return "";
	}
}
