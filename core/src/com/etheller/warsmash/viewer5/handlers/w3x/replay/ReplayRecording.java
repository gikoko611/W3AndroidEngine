package com.etheller.warsmash.viewer5.handlers.w3x.replay;

import java.io.BufferedOutputStream;
import java.io.IOException;

import com.badlogic.gdx.files.FileHandle;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.SimulationChecksum;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CGameCache;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.replay.CReplayHeader;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.replay.CReplayRecorder;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.replay.CReplayWriter;

/**
 * A game being recorded: written as it goes, moved to its place when the game ends, unless the map's script asked
 * for no replay.
 */
public final class ReplayRecording implements ReplaySession {
	private final CReplayRecorder recorder;
	private final FileHandle written;
	private final FileHandle target;
	private boolean closed;
	private boolean notSaved;

	ReplayRecording(final CSimulation simulation, final CReplayHeader header, final FileHandle written,
			final FileHandle target) throws IOException {
		this.written = written;
		this.target = target;
		written.parent().mkdirs();
		final CReplayWriter writer = new CReplayWriter(new BufferedOutputStream(written.write(false)), header);
		this.recorder = new CReplayRecorder(simulation, writer, header.getChecksumInterval(),
				SimulationChecksum::compute);
		CGameCache.setDisk(this.recorder.recordingDisk(CGameCache.profileDisk()));
	}

	@Override
	public float localValue(final float value) {
		return this.recorder.localValue(value);
	}

	/**
	 * DoNotSaveReplay: the game leaves no replay and the score screen offers none. A file a test asked to record
	 * into is still written.
	 */
	public void doNotSave() {
		this.notSaved = true;
	}

	public boolean isSaved() {
		return !this.notSaved || this.written.equals(this.target);
	}

	/** After each step, before the commands that wait for the step's end are applied. */
	public void afterStep() {
		this.recorder.afterStep();
	}

	@Override
	public void gameEnded() {
		this.recorder.gameEnded();
	}

	@Override
	public void close() {
		if (this.closed) {
			return;
		}
		this.closed = true;
		CGameCache.setDisk(null);
		this.recorder.finish();
		final boolean complete = this.recorder.isEnded();
		if (this.written.equals(this.target)) {
			return;
		}
		if (!isSaved()) {
			this.written.delete();
		}
		else if (complete) {
			this.written.moveTo(this.target);
		}
	}

	/**
	 * Save Replay on the score screen: the game is over, its replay ends here and a copy is kept under the given name.
	 *
	 * @return the saved file, or null when the replay could not be written
	 */
	public FileHandle saveAs(final String name) {
		close();
		if (!isSaved() || !this.target.exists()) {
			return null;
		}
		final FileHandle copy = GameReplays.folder().child(name + GameReplays.EXTENSION);
		this.target.copyTo(copy);
		return copy;
	}
}
