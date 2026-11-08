package com.etheller.warsmash.viewer5.handlers.w3x.simulation.players;

/**
 * What a player command changes outside the simulation's own state: the script dialogs and the pause live with the
 * game's interface, and other players' signals are only shown there.
 */
public interface CPlayerCommandHost {
	CPlayerCommandHost NONE = new CPlayerCommandHost() {
		@Override
		public void clickDialogButton(final int playerIndex, final int dialogHandleId, final int buttonHandleId) {
		}

		@Override
		public void setGamePaused(final int playerIndex, final boolean paused) {
		}

		@Override
		public void minimapSignal(final int playerIndex, final float x, final float y) {
		}
	};

	void clickDialogButton(int playerIndex, int dialogHandleId, int buttonHandleId);

	void setGamePaused(int playerIndex, boolean paused);

	void minimapSignal(int playerIndex, float x, float y);
}
