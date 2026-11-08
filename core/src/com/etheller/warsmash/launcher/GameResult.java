package com.etheller.warsmash.launcher;

/** How a game started from the launcher ended, as the launcher takes it back. */
public final class GameResult {
	/**
	 * victory, defeat, tie, quit (left the game to the menu), exit (closed the program), error (not started) or crash
	 * (an exception ended the program).
	 */
	public String result;
	public String source;
	public String map;
	public String profile;
	public String loadFailure;
	/** The race the player played, a random one as it was drawn; 0 when the engine did not tell. */
	public int race;
	public long finished;

	public boolean failed() {
		return "error".equals(this.result) || (this.loadFailure != null);
	}

	public boolean crashed() {
		return "crash".equals(this.result);
	}
}
