package com.etheller.warsmash.viewer5.handlers.w3x.replay;

/** The recording or the playback of a game's replay, ended with the game. */
public interface ReplaySession {
	/**
	 * A value a script reads that only this player's program has, such as the camera: a recording keeps it, a
	 * playback gives the recorded one.
	 */
	float localValue(float value);

	/** The game ended at its current step (EndGame, a quit); it makes no more steps. */
	void gameEnded();

	void close();
}
