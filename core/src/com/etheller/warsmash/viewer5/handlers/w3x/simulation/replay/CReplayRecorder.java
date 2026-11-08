package com.etheller.warsmash.viewer5.handlers.w3x.simulation.replay;

import java.io.IOException;
import java.util.List;
import java.util.function.ToIntFunction;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CGameCache;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerCommandLog;

/**
 * Records a game into a replay: after every step the commands the game's command log got since the last step, every
 * {@link CReplayHeader#getChecksumInterval()} steps the game's checksum, the game cache files the game reads and
 * the local values its scripts read.
 * A replay that cannot be written any more stops recording; the game goes on.
 */
public final class CReplayRecorder {
	private final CSimulation game;
	private final CReplayWriter writer;
	private final int checksumInterval;
	private final ToIntFunction<CSimulation> checksum;
	private int writtenCommands;
	private int lastRecordedStep;
	private boolean endPending;
	private boolean stopped;
	private boolean ended;

	public CReplayRecorder(final CSimulation game, final CReplayWriter writer, final int checksumInterval,
			final ToIntFunction<CSimulation> checksum) {
		this.game = game;
		this.writer = writer;
		this.checksumInterval = checksumInterval;
		this.checksum = checksum;
		this.writtenCommands = game.getPlayerCommandLog().size();
		this.lastRecordedStep = game.getGameTurnTick();
	}

	/** Called after each step, before the commands that wait for the end of the step are applied. */
	public void afterStep() {
		if (this.stopped) {
			return;
		}
		try {
			writeNewCommands();
			final int tick = this.game.getGameTurnTick();
			if ((tick % this.checksumInterval) == 0) {
				this.writer.checksum(tick, this.checksum.applyAsInt(this.game));
			}
			this.lastRecordedStep = tick;
		}
		catch (final IOException e) {
			fail(e);
			return;
		}
		if (this.endPending) {
			finish();
		}
	}

	/**
	 * The game ended at its current step (EndGame, a quit): the replay ends there. Ended by a script in the middle of
	 * a step, it ends after the step, which a playback makes too.
	 */
	public void gameEnded() {
		if (this.stopped) {
			return;
		}
		if (this.game.getGameTurnTick() == this.lastRecordedStep) {
			finish();
		}
		else {
			this.endPending = true;
		}
	}

	/** The game cache files as the game read them from the disk; a playback reads these instead. */
	public CGameCache.Disk recordingDisk(final CGameCache.Disk disk) {
		return new CGameCache.Disk() {
			@Override
			public byte[] read() {
				final byte[] bytes = disk.read();
				if (!CReplayRecorder.this.stopped) {
					try {
						CReplayRecorder.this.writer.cacheRead(CReplayRecorder.this.game.getGameTurnTick(), bytes);
					}
					catch (final IOException e) {
						fail(e);
					}
				}
				return bytes;
			}

			@Override
			public boolean write(final byte[] bytes) {
				return disk.write(bytes);
			}
		};
	}

	/**
	 * A value a script read that only this player's program has, such as the camera: a playback gives the script the
	 * recorded one instead.
	 */
	public float localValue(final float value) {
		if (!this.stopped) {
			try {
				this.writer.localValue(this.game.getGameTurnTick(), value);
			}
			catch (final IOException e) {
				fail(e);
			}
		}
		return value;
	}

	public boolean isStopped() {
		return this.stopped;
	}

	/** Whether the replay was written to the game's end. */
	public boolean isEnded() {
		return this.ended;
	}

	/** Ends the replay at the game's current step and closes the file. */
	public void finish() {
		if (this.stopped) {
			return;
		}
		try {
			writeNewCommands();
			this.writer.end(this.game.getGameTurnTick());
			this.writer.close();
			this.stopped = true;
			this.ended = true;
		}
		catch (final IOException e) {
			fail(e);
		}
	}

	private void writeNewCommands() throws IOException {
		final CPlayerCommandLog log = this.game.getPlayerCommandLog();
		final List<CPlayerCommandLog.Entry> entries = log.getEntries();
		for (int i = this.writtenCommands; i < entries.size(); i++) {
			final CPlayerCommandLog.Entry entry = entries.get(i);
			this.writer.command(entry.getGameTurnTick(), entry.getPlayerIndex(), entry.getCommand());
		}
		this.writtenCommands = entries.size();
	}

	private void fail(final IOException e) {
		System.err.println("Replay recording stopped: " + e);
		this.stopped = true;
		try {
			this.writer.close();
		}
		catch (final IOException closeFailure) {
			// The recording is given up either way.
		}
	}
}
