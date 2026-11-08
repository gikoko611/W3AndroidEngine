package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import com.etheller.warsmash.datasources.DataSource;

/**
 * The length of a sound file, read from its headers and frames without decoding it. Scripts wait by this length, so it
 * is the same on every platform and with or without an audio device. Reads WAV (PCM and compressed with a sample
 * count), MP3 (MPEG audio frames), FLAC and Ogg (Vorbis, Opus).
 */
public final class SoundFileDuration {
	private static final int[][] MPEG1_BITRATES = { { 0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448 },
			{ 0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384 },
			{ 0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320 } };
	private static final int[][] MPEG2_BITRATES = { { 0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256 },
			{ 0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160 },
			{ 0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160 } };
	private static final int[] MPEG1_SAMPLE_RATES = { 44100, 48000, 32000 };
	// A stream that loses frame sync is searched this far for the next frame before the rest is given up.
	private static final int MAX_RESYNC_BYTES = 65536;

	private SoundFileDuration() {
	}

	/**
	 * @return the length in seconds, or 0 when the file is missing or not a sound this can read
	 */
	public static float read(final DataSource dataSource, final String path) {
		if ((dataSource == null) || (path == null) || !dataSource.has(path)) {
			return 0;
		}
		try (InputStream stream = dataSource.getResourceAsStream(path)) {
			return stream == null ? 0 : read(stream);
		}
		catch (final IOException | RuntimeException e) {
			return 0;
		}
	}

	public static float read(final InputStream stream) throws IOException {
		final Reader reader = new Reader(stream);
		final byte[] magic = new byte[4];
		final int got = reader.readUpTo(magic, 4);
		if (got < 4) {
			return 0;
		}
		if (is(magic, "RIFF")) {
			return readWav(reader);
		}
		if (is(magic, "OggS")) {
			return readOgg(reader, magic);
		}
		if ((magic[0] == 'I') && (magic[1] == 'D') && (magic[2] == '3')) {
			final byte[] rest = new byte[6];
			reader.readFully(rest);
			final int flags = rest[1] & 0xFF;
			int size = ((rest[2] & 0x7F) << 21) | ((rest[3] & 0x7F) << 14) | ((rest[4] & 0x7F) << 7) | (rest[5] & 0x7F);
			if ((flags & 0x10) != 0) {
				size += 10;
			}
			reader.skipFully(size);
			if (reader.readUpTo(magic, 4) < 4) {
				return 0;
			}
		}
		if (is(magic, "fLaC")) {
			return readFlac(reader);
		}
		return readMp3(reader, magic);
	}

	private static boolean is(final byte[] magic, final String text) {
		for (int i = 0; i < 4; i++) {
			if (magic[i] != text.charAt(i)) {
				return false;
			}
		}
		return true;
	}

	private static float readWav(final Reader reader) throws IOException {
		reader.skipFully(4);
		final byte[] wave = new byte[4];
		reader.readFully(wave);
		if (!is(wave, "WAVE")) {
			return 0;
		}
		int format = -1;
		int sampleRate = 0;
		long byteRate = 0;
		int blockAlign = 0;
		int samplesPerBlock = 0;
		long factSamples = -1;
		long dataSize = -1;
		final byte[] id = new byte[4];
		while (reader.readUpTo(id, 4) == 4) {
			final long size = reader.readIntLE() & 0xFFFFFFFFL;
			final long padded = size + (size & 1);
			if (is(id, "fmt ")) {
				format = reader.readShortLE();
				reader.readShortLE();
				sampleRate = reader.readIntLE();
				byteRate = reader.readIntLE() & 0xFFFFFFFFL;
				blockAlign = reader.readShortLE();
				reader.readShortLE();
				long used = 16;
				if (size >= 18) {
					final int extraSize = reader.readShortLE();
					used += 2;
					if ((extraSize >= 2) && (size >= 20)) {
						samplesPerBlock = reader.readShortLE();
						used += 2;
					}
				}
				reader.skipFully(padded - used);
			}
			else if (is(id, "fact") && (size >= 4)) {
				factSamples = reader.readIntLE() & 0xFFFFFFFFL;
				reader.skipFully(padded - 4);
			}
			else if (is(id, "data")) {
				dataSize = size;
				if (!reader.skipUpTo(padded)) {
					break;
				}
			}
			else {
				if (!reader.skipUpTo(padded)) {
					break;
				}
			}
		}
		if ((sampleRate <= 0) || (dataSize < 0)) {
			return 0;
		}
		final boolean pcm = (format == 1) || (format == 3) || (format == 0xFFFE);
		if (pcm && (blockAlign > 0)) {
			return (dataSize / blockAlign) / (float) sampleRate;
		}
		if (factSamples >= 0) {
			return factSamples / (float) sampleRate;
		}
		if ((blockAlign > 0) && (samplesPerBlock > 0)) {
			return ((dataSize / blockAlign) * samplesPerBlock) / (float) sampleRate;
		}
		if (byteRate > 0) {
			return dataSize / (float) byteRate;
		}
		return 0;
	}

	private static float readFlac(final Reader reader) throws IOException {
		final byte[] header = new byte[4];
		reader.readFully(header);
		if ((header[0] & 0x7F) != 0) {
			return 0;
		}
		final byte[] info = new byte[18];
		reader.readFully(info);
		final int sampleRate = ((info[10] & 0xFF) << 12) | ((info[11] & 0xFF) << 4) | ((info[12] & 0xF0) >> 4);
		final long totalSamples = ((long) (info[13] & 0x0F) << 32) | ((long) (info[14] & 0xFF) << 24)
				| ((info[15] & 0xFF) << 16) | ((info[16] & 0xFF) << 8) | (info[17] & 0xFF);
		if (sampleRate <= 0) {
			return 0;
		}
		return totalSamples / (float) sampleRate;
	}

	private static float readOgg(final Reader reader, final byte[] magic) throws IOException {
		final byte[] header = new byte[27];
		System.arraycopy(magic, 0, header, 0, 4);
		reader.readFully(header, 4, 23);
		int serial = 0;
		int sampleRate = 0;
		int preSkip = 0;
		long lastGranule = -1;
		boolean first = true;
		while (true) {
			final int pageSerial = intLE(header, 14);
			final long granule = longLE(header, 6);
			final int segments = header[26] & 0xFF;
			final byte[] table = new byte[segments];
			reader.readFully(table);
			int bodySize = 0;
			for (final byte segment : table) {
				bodySize += segment & 0xFF;
			}
			if (first) {
				first = false;
				serial = pageSerial;
				final byte[] body = new byte[bodySize];
				reader.readFully(body);
				if ((bodySize >= 16) && (body[0] == 1) && (body[1] == 'v') && (body[2] == 'o') && (body[3] == 'r')) {
					sampleRate = intLE(body, 12);
				}
				else if ((bodySize >= 12) && (body[0] == 'O') && (body[1] == 'p') && (body[2] == 'u')
						&& (body[3] == 's')) {
					sampleRate = 48000;
					preSkip = (body[10] & 0xFF) | ((body[11] & 0xFF) << 8);
				}
			}
			else {
				reader.skipFully(bodySize);
			}
			if ((pageSerial == serial) && (granule != -1)) {
				lastGranule = granule;
			}
			if (reader.readUpTo(header, 27) < 27) {
				break;
			}
			if ((header[0] != 'O') || (header[1] != 'g') || (header[2] != 'g') || (header[3] != 'S')) {
				break;
			}
		}
		if ((sampleRate <= 0) || (lastGranule <= preSkip)) {
			return 0;
		}
		return (lastGranule - preSkip) / (float) sampleRate;
	}

	private static float readMp3(final Reader reader, final byte[] firstBytes) throws IOException {
		final byte[] header = firstBytes.clone();
		long samples = 0;
		int sampleRate = 0;
		boolean firstFrame = true;
		int searched = 0;
		while (true) {
			final int frameLength = frameLength(header);
			if (frameLength <= 0) {
				if ((header[0] == 'T') && (header[1] == 'A') && (header[2] == 'G')) {
					break;
				}
				// Not a frame here: move on one byte and look again.
				if (++searched > MAX_RESYNC_BYTES) {
					break;
				}
				System.arraycopy(header, 1, header, 0, 3);
				final int next = reader.read();
				if (next < 0) {
					break;
				}
				header[3] = (byte) next;
				continue;
			}
			searched = 0;
			final int frameSampleRate = frameSampleRate(header);
			if (sampleRate == 0) {
				sampleRate = frameSampleRate;
			}
			boolean audio = true;
			if (firstFrame) {
				firstFrame = false;
				final byte[] body = new byte[frameLength - 4];
				final int read = reader.readUpTo(body, body.length);
				// A Xing / Info / VBRI frame at the start only describes the stream; players do not play it.
				audio = !(contains(body, read, "Xing") || contains(body, read, "Info") || contains(body, read, "VBRI"));
				if (read < body.length) {
					if (audio) {
						samples += samplesPerFrame(header);
					}
					break;
				}
			}
			else if (!reader.skipUpTo(frameLength - 4)) {
				samples += samplesPerFrame(header);
				break;
			}
			if (audio) {
				samples += samplesPerFrame(header);
			}
			if (reader.readUpTo(header, 4) < 4) {
				break;
			}
		}
		if (sampleRate <= 0) {
			return 0;
		}
		return samples / (float) sampleRate;
	}

	private static boolean contains(final byte[] body, final int length, final String tag) {
		final int limit = Math.min(length, 64) - 4;
		for (int i = 0; i <= limit; i++) {
			if ((body[i] == tag.charAt(0)) && (body[i + 1] == tag.charAt(1)) && (body[i + 2] == tag.charAt(2))
					&& (body[i + 3] == tag.charAt(3))) {
				return true;
			}
		}
		return false;
	}

	/** @return 3 for MPEG 1, 2 for MPEG 2, 0 for MPEG 2.5, -1 when the bits are not a version */
	private static int version(final byte[] header) {
		final int bits = (header[1] >> 3) & 0x3;
		return bits == 3 ? 3 : bits == 2 ? 2 : bits == 0 ? 0 : -1;
	}

	/** @return 1, 2 or 3, or -1 */
	private static int layer(final byte[] header) {
		final int bits = (header[1] >> 1) & 0x3;
		return bits == 0 ? -1 : 4 - bits;
	}

	private static int frameSampleRate(final byte[] header) {
		final int index = (header[2] >> 2) & 0x3;
		if (index == 3) {
			return -1;
		}
		final int version = version(header);
		final int rate = MPEG1_SAMPLE_RATES[index];
		return version == 3 ? rate : version == 2 ? rate / 2 : rate / 4;
	}

	private static int samplesPerFrame(final byte[] header) {
		final int layer = layer(header);
		if (layer == 1) {
			return 384;
		}
		if ((layer == 3) && (version(header) != 3)) {
			return 576;
		}
		return 1152;
	}

	/** @return the length of the frame starting with this header, or 0 when it is not a frame header */
	static int frameLength(final byte[] header) {
		if (((header[0] & 0xFF) != 0xFF) || ((header[1] & 0xE0) != 0xE0)) {
			return 0;
		}
		final int version = version(header);
		final int layer = layer(header);
		final int bitrateIndex = (header[2] >> 4) & 0xF;
		final int sampleRate = frameSampleRate(header);
		if ((version < 0) || (layer < 0) || (bitrateIndex == 0) || (bitrateIndex == 15) || (sampleRate <= 0)) {
			return 0;
		}
		final int padding = (header[2] >> 1) & 0x1;
		final int bitrate = 1000
				* (version == 3 ? MPEG1_BITRATES[layer - 1][bitrateIndex] : MPEG2_BITRATES[layer - 1][bitrateIndex]);
		if (layer == 1) {
			return (((12 * bitrate) / sampleRate) + padding) * 4;
		}
		final int coefficient = ((layer == 3) && (version != 3)) ? 72 : 144;
		return ((coefficient * bitrate) / sampleRate) + padding;
	}

	private static int intLE(final byte[] data, final int offset) {
		return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8) | ((data[offset + 2] & 0xFF) << 16)
				| ((data[offset + 3] & 0xFF) << 24);
	}

	private static long longLE(final byte[] data, final int offset) {
		return (intLE(data, offset) & 0xFFFFFFFFL) | ((long) intLE(data, offset + 4) << 32);
	}

	private static final class Reader {
		private final InputStream stream;
		private final byte[] scratch = new byte[4];

		private Reader(final InputStream stream) {
			this.stream = stream;
		}

		int read() throws IOException {
			return this.stream.read();
		}

		int readUpTo(final byte[] buffer, final int length) throws IOException {
			int total = 0;
			while (total < length) {
				final int read = this.stream.read(buffer, total, length - total);
				if (read < 0) {
					break;
				}
				total += read;
			}
			return total;
		}

		void readFully(final byte[] buffer) throws IOException {
			readFully(buffer, 0, buffer.length);
		}

		void readFully(final byte[] buffer, final int offset, final int length) throws IOException {
			int total = 0;
			while (total < length) {
				final int read = this.stream.read(buffer, offset + total, length - total);
				if (read < 0) {
					throw new EOFException();
				}
				total += read;
			}
		}

		/** @return false when the stream ended first */
		boolean skipUpTo(final long count) throws IOException {
			long left = count;
			while (left > 0) {
				final long skipped = this.stream.skip(left);
				if (skipped > 0) {
					left -= skipped;
				}
				else if (this.stream.read() < 0) {
					return false;
				}
				else {
					left--;
				}
			}
			return true;
		}

		void skipFully(final long count) throws IOException {
			if (!skipUpTo(count)) {
				throw new EOFException();
			}
		}

		int readShortLE() throws IOException {
			readFully(this.scratch, 0, 2);
			return (this.scratch[0] & 0xFF) | ((this.scratch[1] & 0xFF) << 8);
		}

		int readIntLE() throws IOException {
			readFully(this.scratch, 0, 4);
			return intLE(this.scratch, 0);
		}
	}
}
