package com.etheller.warsmash.android;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound.SoundFileDuration;

/**
 * The channels, sample rate and length of a sound file, read from its headers, so that a sound can be given to the
 * player that can play it before it is decoded. Reads WAV, FLAC, Ogg (Vorbis, Opus) and MP3.
 */
final class SoundFileFormat {
	private static final int MAX_MP3_SYNC_SEARCH = 65536;
	private static final int[] MPEG1_SAMPLE_RATES = { 44100, 48000, 32000 };

	final int channels;
	final int sampleRate;
	final float seconds;

	private SoundFileFormat(final int channels, final int sampleRate, final float seconds) {
		this.channels = channels;
		this.sampleRate = sampleRate;
		this.seconds = seconds;
	}

	/** The size of the sound decoded to 16 bit samples. */
	long decodedBytes() {
		return (long) (this.seconds * this.sampleRate) * this.channels * 2;
	}

	/** @return the format, or null when the file is not a sound this can read */
	static SoundFileFormat read(final File file) {
		final int[] channelsAndRate;
		try (InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
			channelsAndRate = readChannelsAndRate(stream);
		}
		catch (final IOException | RuntimeException e) {
			return null;
		}
		if (channelsAndRate == null) {
			return null;
		}
		float seconds;
		try (InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
			seconds = SoundFileDuration.read(stream);
		}
		catch (final IOException | RuntimeException e) {
			seconds = 0;
		}
		return new SoundFileFormat(channelsAndRate[0], channelsAndRate[1], seconds);
	}

	private static int[] readChannelsAndRate(final InputStream stream) throws IOException {
		final byte[] magic = new byte[4];
		if (readUpTo(stream, magic, 4) < 4) {
			return null;
		}
		if (is(magic, "RIFF")) {
			return readWav(stream);
		}
		if (is(magic, "OggS")) {
			return readOgg(stream);
		}
		if ((magic[0] == 'I') && (magic[1] == 'D') && (magic[2] == '3')) {
			final byte[] rest = new byte[6];
			readFully(stream, rest, 6);
			final int flags = rest[1] & 0xFF;
			int size = ((rest[2] & 0x7F) << 21) | ((rest[3] & 0x7F) << 14) | ((rest[4] & 0x7F) << 7) | (rest[5] & 0x7F);
			if ((flags & 0x10) != 0) {
				size += 10;
			}
			skipFully(stream, size);
			if (readUpTo(stream, magic, 4) < 4) {
				return null;
			}
		}
		if (is(magic, "fLaC")) {
			final byte[] block = new byte[22];
			readFully(stream, block, 22);
			if ((block[0] & 0x7F) != 0) {
				return null;
			}
			final int sampleRate = ((block[14] & 0xFF) << 12) | ((block[15] & 0xFF) << 4) | ((block[16] & 0xF0) >> 4);
			final int channels = ((block[16] >> 1) & 0x7) + 1;
			return new int[] { channels, sampleRate };
		}
		return readMp3(stream, magic);
	}

	private static int[] readWav(final InputStream stream) throws IOException {
		final byte[] header = new byte[8];
		readFully(stream, header, 8);
		if ((header[4] != 'W') || (header[5] != 'A') || (header[6] != 'V') || (header[7] != 'E')) {
			return null;
		}
		final byte[] chunk = new byte[8];
		while (readUpTo(stream, chunk, 8) == 8) {
			final long size = intLE(chunk, 4) & 0xFFFFFFFFL;
			if ((chunk[0] == 'f') && (chunk[1] == 'm') && (chunk[2] == 't') && (chunk[3] == ' ') && (size >= 8)) {
				final byte[] fmt = new byte[8];
				readFully(stream, fmt, 8);
				return new int[] { (fmt[2] & 0xFF) | ((fmt[3] & 0xFF) << 8), intLE(fmt, 4) };
			}
			skipFully(stream, size + (size & 1));
		}
		return null;
	}

	private static int[] readOgg(final InputStream stream) throws IOException {
		final byte[] header = new byte[23];
		readFully(stream, header, 23);
		final int segments = header[22] & 0xFF;
		final byte[] table = new byte[segments];
		readFully(stream, table, segments);
		int bodySize = 0;
		for (final byte segment : table) {
			bodySize += segment & 0xFF;
		}
		final byte[] body = new byte[bodySize];
		readFully(stream, body, bodySize);
		if ((bodySize >= 16) && (body[0] == 1) && (body[1] == 'v') && (body[2] == 'o') && (body[3] == 'r')) {
			return new int[] { body[11] & 0xFF, intLE(body, 12) };
		}
		if ((bodySize >= 10) && (body[0] == 'O') && (body[1] == 'p') && (body[2] == 'u') && (body[3] == 's')) {
			return new int[] { body[9] & 0xFF, 48000 };
		}
		return null;
	}

	private static int[] readMp3(final InputStream stream, final byte[] first) throws IOException {
		final byte[] header = first.clone();
		for (int searched = 0; searched < MAX_MP3_SYNC_SEARCH; searched++) {
			final int version = (header[1] >> 3) & 0x3;
			final int layer = (header[1] >> 1) & 0x3;
			final int bitrateIndex = (header[2] >> 4) & 0xF;
			final int rateIndex = (header[2] >> 2) & 0x3;
			if (((header[0] & 0xFF) == 0xFF) && ((header[1] & 0xE0) == 0xE0) && (version != 1) && (layer != 0)
					&& (bitrateIndex != 0) && (bitrateIndex != 15) && (rateIndex != 3)) {
				final int rate = MPEG1_SAMPLE_RATES[rateIndex];
				final int sampleRate = version == 3 ? rate : version == 2 ? rate / 2 : rate / 4;
				final int channels = ((header[3] >> 6) & 0x3) == 3 ? 1 : 2;
				return new int[] { channels, sampleRate };
			}
			System.arraycopy(header, 1, header, 0, 3);
			final int next = stream.read();
			if (next < 0) {
				return null;
			}
			header[3] = (byte) next;
		}
		return null;
	}

	private static boolean is(final byte[] magic, final String text) {
		for (int i = 0; i < 4; i++) {
			if (magic[i] != text.charAt(i)) {
				return false;
			}
		}
		return true;
	}

	private static int intLE(final byte[] data, final int offset) {
		return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8) | ((data[offset + 2] & 0xFF) << 16)
				| ((data[offset + 3] & 0xFF) << 24);
	}

	private static int readUpTo(final InputStream stream, final byte[] buffer, final int length) throws IOException {
		int total = 0;
		while (total < length) {
			final int read = stream.read(buffer, total, length - total);
			if (read < 0) {
				break;
			}
			total += read;
		}
		return total;
	}

	private static void readFully(final InputStream stream, final byte[] buffer, final int length) throws IOException {
		if (readUpTo(stream, buffer, length) < length) {
			throw new IOException("Unexpected end of sound file");
		}
	}

	private static void skipFully(final InputStream stream, final long count) throws IOException {
		long left = count;
		while (left > 0) {
			final long skipped = stream.skip(left);
			if (skipped > 0) {
				left -= skipped;
			}
			else if (stream.read() < 0) {
				throw new IOException("Unexpected end of sound file");
			}
			else {
				left--;
			}
		}
	}
}
