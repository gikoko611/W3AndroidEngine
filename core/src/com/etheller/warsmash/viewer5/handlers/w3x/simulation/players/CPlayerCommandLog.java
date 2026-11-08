package com.etheller.warsmash.viewer5.handlers.w3x.simulation.players;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The player commands a game applied, in order, each with the step it came after. Applying them again in the same
 * steps of the same game gives the same game.
 */
public final class CPlayerCommandLog {
	private final List<Entry> entries = new ArrayList<>();

	public void add(final int gameTurnTick, final int playerIndex, final byte[] command) {
		this.entries.add(new Entry(gameTurnTick, playerIndex, command));
	}

	public List<Entry> getEntries() {
		return Collections.unmodifiableList(this.entries);
	}

	public int size() {
		return this.entries.size();
	}

	public void clear() {
		this.entries.clear();
	}

	public static final class Entry {
		private final int gameTurnTick;
		private final int playerIndex;
		private final byte[] command;

		public Entry(final int gameTurnTick, final int playerIndex, final byte[] command) {
			this.gameTurnTick = gameTurnTick;
			this.playerIndex = playerIndex;
			this.command = command;
		}

		/** The number of steps the game had made when the command was applied. */
		public int getGameTurnTick() {
			return this.gameTurnTick;
		}

		public int getPlayerIndex() {
			return this.playerIndex;
		}

		public byte[] getCommand() {
			return this.command.clone();
		}

		public void applyTo(final CPlayerUnitOrderListener listener) {
			CPlayerCommandCodec.decode(this.command, listener);
		}
	}
}
