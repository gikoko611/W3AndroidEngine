package com.etheller.warsmash.viewer5.handlers.w3x.simulation.replay;

import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/** Writes a replay file as the game goes, in the format {@link CReplay} describes. */
public final class CReplayWriter implements Closeable {
	private final OutputStream output;
	private final Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
	private final DataOutputStream body;
	private boolean closed;

	public CReplayWriter(final OutputStream output, final CReplayHeader header) throws IOException {
		this.output = output;
		final DataOutputStream headerOut = new DataOutputStream(output);
		headerOut.writeInt(CReplay.MAGIC);
		headerOut.writeInt(CReplay.FORMAT_VERSION);
		header.write(headerOut);
		headerOut.flush();
		this.body = new DataOutputStream(new DeflaterOutputStream(output, this.deflater, 4096, true));
	}

	public void command(final int tick, final int playerIndex, final byte[] command) throws IOException {
		this.body.writeByte(CReplay.COMMAND);
		writeVarInt(tick);
		this.body.writeByte(playerIndex);
		writeVarInt(command.length);
		this.body.write(command);
	}

	public void checksum(final int tick, final int checksum) throws IOException {
		this.body.writeByte(CReplay.CHECKSUM);
		writeVarInt(tick);
		this.body.writeInt(checksum);
		this.body.flush();
	}

	public void cacheRead(final int tick, final byte[] bytes) throws IOException {
		this.body.writeByte(CReplay.CACHE);
		writeVarInt(tick);
		if (bytes == null) {
			writeVarInt(0);
		}
		else {
			writeVarInt(bytes.length + 1);
			this.body.write(bytes);
		}
	}

	public void localValue(final int tick, final float value) throws IOException {
		this.body.writeByte(CReplay.LOCAL);
		writeVarInt(tick);
		this.body.writeFloat(value);
	}

	public void end(final int tick) throws IOException {
		this.body.writeByte(CReplay.END);
		writeVarInt(tick);
	}

	private void writeVarInt(int value) throws IOException {
		while ((value & ~0x7F) != 0) {
			this.body.writeByte((value & 0x7F) | 0x80);
			value >>>= 7;
		}
		this.body.writeByte(value);
	}

	@Override
	public void close() throws IOException {
		if (this.closed) {
			return;
		}
		this.closed = true;
		try {
			this.body.close();
		}
		finally {
			// A deflater given to the stream is not ended by it; its native memory would stay until finalization.
			this.deflater.end();
			this.output.close();
		}
	}
}
