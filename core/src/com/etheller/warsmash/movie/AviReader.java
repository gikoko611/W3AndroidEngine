package com.etheller.warsmash.movie;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.function.IntPredicate;

/**
 * Reads the first video stream of an AVI file frame by frame, as it is stored, for a platform decoder. The movies keep
 * one compressed frame in each chunk of the movi list, so the chunks are read in order and no index is needed.
 */
public final class AviReader implements Closeable {
	private static final int RIFF = fourcc("RIFF");
	private static final int LIST = fourcc("LIST");
	private static final int AVI = fourcc("AVI ");
	private static final int HDRL = fourcc("hdrl");
	private static final int STRL = fourcc("strl");
	private static final int STRH = fourcc("strh");
	private static final int STRF = fourcc("strf");
	private static final int MOVI = fourcc("movi");
	private static final int VIDS = fourcc("vids");

	private final FileChannel channel;
	private final ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
	private int codec;
	private int width;
	private int height;
	private int rate;
	private int scale;
	private int frameCount;
	private int videoStream = -1;
	private long moviStart;
	private long moviEnd;
	private long position;
	private long frame;
	/** Where the data of the frame chunk found last starts. */
	private long chunkData;
	/** The ends of the rec lists the reader is inside, innermost last. */
	private final long[] listEnds = new long[8];
	private int listDepth;

	public AviReader(final File file) throws IOException {
		this.channel = new RandomAccessFile(file, "r").getChannel();
		try {
			readHeaders();
		}
		catch (final IOException | RuntimeException e) {
			this.channel.close();
			throw e;
		}
	}

	private void readHeaders() throws IOException {
		if ((readInt(0) != RIFF) || (readInt(8) != AVI)) {
			throw new IOException("Not an AVI file");
		}
		final long end = Math.min(this.channel.size(), 8 + (readInt(4) & 0xFFFFFFFFL));
		long offset = 12;
		while ((offset + 8) <= end) {
			final int id = readInt(offset);
			final long size = readInt(offset + 4) & 0xFFFFFFFFL;
			if ((id == LIST) && (readInt(offset + 8) == HDRL)) {
				readStreamLists(offset + 12, offset + 8 + size);
			}
			else if ((id == LIST) && (readInt(offset + 8) == MOVI)) {
				this.moviStart = offset + 12;
				this.moviEnd = Math.min(end, offset + 8 + size);
				break;
			}
			offset += 8 + size + (size & 1);
		}
		if ((this.videoStream < 0) || (this.moviStart == 0)) {
			throw new IOException("No video stream");
		}
		if ((this.rate <= 0) || (this.scale <= 0)) {
			this.rate = 24;
			this.scale = 1;
		}
		this.position = this.moviStart;
	}

	private void readStreamLists(long offset, final long end) throws IOException {
		int stream = 0;
		while ((offset + 8) <= end) {
			final int id = readInt(offset);
			final long size = readInt(offset + 4) & 0xFFFFFFFFL;
			if ((id == LIST) && (readInt(offset + 8) == STRL)) {
				if ((this.videoStream < 0) && readStream(offset + 12, offset + 8 + size)) {
					this.videoStream = stream;
				}
				stream++;
			}
			offset += 8 + size + (size & 1);
		}
	}

	private boolean readStream(long offset, final long end) throws IOException {
		boolean video = false;
		while ((offset + 8) <= end) {
			final int id = readInt(offset);
			final long size = readInt(offset + 4) & 0xFFFFFFFFL;
			if ((id == STRH) && (size >= 36)) {
				video = readInt(offset + 8) == VIDS;
				if (video) {
					this.scale = readInt(offset + 8 + 20);
					this.rate = readInt(offset + 8 + 24);
					this.frameCount = readInt(offset + 8 + 32);
				}
			}
			else if ((id == STRF) && video && (size >= 20)) {
				this.width = readInt(offset + 8 + 4);
				this.height = Math.abs(readInt(offset + 8 + 8));
				this.codec = readInt(offset + 8 + 16);
			}
			offset += 8 + size + (size & 1);
		}
		return video;
	}

	/** @return the compression four-character code of the video, VP90 for the game's movies */
	public String getCodec() {
		final char[] name = new char[4];
		for (int i = 0; i < 4; i++) {
			name[i] = (char) ((this.codec >>> (i * 8)) & 0xFF);
		}
		return new String(name);
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	/** @return the frames the header promises, 0 when it does not say */
	public int getFrameCount() {
		return this.frameCount;
	}

	public long getDurationMicros() {
		return frameTimeMicros(this.frameCount);
	}

	public long frameTimeMicros(final long frame) {
		return (frame * this.scale * 1000000L) / this.rate;
	}

	/**
	 * Reads the next frame of the video into the buffer, from its position up to its limit.
	 *
	 * @return the frame's number, from 0, or -1 at the end of the movie; an empty frame (the picture stays) leaves
	 *         the buffer empty
	 * @throws IOException when the frame does not fit in the buffer, or the file cannot be read
	 */
	public long readFrame(final ByteBuffer into) throws IOException {
		final long size = nextFrameChunk();
		if (size < 0) {
			return -1;
		}
		if (size > into.remaining()) {
			throw new IOException("A frame of " + size + " bytes does not fit in " + into.remaining());
		}
		final int limit = into.limit();
		into.limit(into.position() + (int) size);
		long at = this.chunkData;
		while (into.hasRemaining()) {
			final int read = this.channel.read(into, at);
			if (read < 0) {
				throw new IOException("The movie ends inside a frame");
			}
			at += read;
		}
		into.limit(limit);
		return this.frame++;
	}

	/**
	 * Moves back to the start, then on to the last key frame at or before the frame, where decoding can start; to the
	 * first frame when there is none.
	 *
	 * @param keyFrame tells a key frame by the first byte of its data
	 * @return the number of the frame read next
	 */
	public long seekKeyFrame(final long frame, final IntPredicate keyFrame) throws IOException {
		this.position = this.moviStart;
		this.frame = 0;
		this.listDepth = 0;
		long keyPosition = this.position;
		long keyFrameNumber = 0;
		int keyListDepth = 0;
		final long[] keyListEnds = new long[this.listEnds.length];
		while (this.frame <= frame) {
			final long position = this.position;
			final int listDepth = this.listDepth;
			final long[] listEnds = this.listEnds.clone();
			final long size = nextFrameChunk();
			if (size < 0) {
				break;
			}
			if ((size > 0) && keyFrame.test(readByte(this.chunkData))) {
				keyPosition = position;
				keyFrameNumber = this.frame;
				keyListDepth = listDepth;
				System.arraycopy(listEnds, 0, keyListEnds, 0, listEnds.length);
			}
			this.frame++;
		}
		this.position = keyPosition;
		this.frame = keyFrameNumber;
		this.listDepth = keyListDepth;
		System.arraycopy(keyListEnds, 0, this.listEnds, 0, keyListEnds.length);
		return this.frame;
	}

	/** Moves past the next frame chunk of the video: its size, with its data at chunkData, or -1 at the end. */
	private long nextFrameChunk() throws IOException {
		final int wanted = videoChunkId(this.videoStream);
		while (true) {
			while ((this.listDepth > 0) && (this.position >= this.listEnds[this.listDepth - 1])) {
				this.listDepth--;
			}
			if ((this.position + 8) > this.moviEnd) {
				return -1;
			}
			final int id = readInt(this.position);
			final long size = readInt(this.position + 4) & 0xFFFFFFFFL;
			if (id == LIST) {
				if (this.listDepth < this.listEnds.length) {
					this.listEnds[this.listDepth++] = this.position + 8 + size;
				}
				this.position += 12;
				continue;
			}
			final long data = this.position + 8;
			this.position = data + size + (size & 1);
			if ((id & 0xFFFF) != (wanted & 0xFFFF)) {
				continue;
			}
			final int kind = id >>> 16;
			if ((kind != (fourcc("00dc") >>> 16)) && (kind != (fourcc("00db") >>> 16))) {
				continue;
			}
			this.chunkData = data;
			return size;
		}
	}

	private static int videoChunkId(final int stream) {
		return ('0' + (stream / 10)) | (('0' + (stream % 10)) << 8) | (fourcc("00dc") & 0xFFFF0000);
	}

	private int readByte(final long offset) throws IOException {
		this.header.clear().limit(1);
		while (this.header.hasRemaining()) {
			if (this.channel.read(this.header, offset) < 0) {
				throw new IOException("The movie ends early");
			}
		}
		return this.header.get(0) & 0xFF;
	}

	private int readInt(final long offset) throws IOException {
		this.header.clear().limit(4);
		long at = offset;
		while (this.header.hasRemaining()) {
			if (this.channel.read(this.header, at) < 0) {
				throw new IOException("The movie ends early");
			}
			at = offset + this.header.position();
		}
		return this.header.getInt(0);
	}

	private static int fourcc(final String name) {
		return name.charAt(0) | (name.charAt(1) << 8) | (name.charAt(2) << 16) | (name.charAt(3) << 24);
	}

	@Override
	public void close() throws IOException {
		this.channel.close();
	}
}
