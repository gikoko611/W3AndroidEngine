package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Lengths read from made-up files of each format, as the header or frame count gives them.
 */
class SoundFileDurationTest {
	private static final float EPSILON = 1e-4f;

	private static float read(final byte[] data) throws IOException {
		return SoundFileDuration.read(new ByteArrayInputStream(data));
	}

	private static ByteBuffer le(final int size) {
		return ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
	}

	private static void chunk(final ByteArrayOutputStream out, final String id, final byte[] body) {
		final ByteBuffer header = le(8);
		header.put(id.getBytes(StandardCharsets.US_ASCII)).putInt(body.length);
		out.writeBytes(header.array());
		out.writeBytes(body);
		if ((body.length & 1) != 0) {
			out.write(0);
		}
	}

	private static byte[] wav(final int format, final int channels, final int sampleRate, final int blockAlign,
			final int bits, final byte[] fmtExtra, final Integer factSamples, final int dataSize) {
		final ByteArrayOutputStream chunks = new ByteArrayOutputStream();
		final ByteBuffer fmt = le(16 + fmtExtra.length);
		fmt.putShort((short) format).putShort((short) channels).putInt(sampleRate)
				.putInt((sampleRate * blockAlign) / Math.max(1, channels)).putShort((short) blockAlign)
				.putShort((short) bits).put(fmtExtra);
		chunk(chunks, "fmt ", fmt.array());
		chunk(chunks, "LIST", "INFOISFT\5\0\0\0abcd\0".getBytes(StandardCharsets.US_ASCII));
		if (factSamples != null) {
			chunk(chunks, "fact", le(4).putInt(factSamples).array());
		}
		chunk(chunks, "data", new byte[dataSize]);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final ByteBuffer riff = le(12);
		riff.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(chunks.size() + 4)
				.put("WAVE".getBytes(StandardCharsets.US_ASCII));
		out.writeBytes(riff.array());
		out.writeBytes(chunks.toByteArray());
		return out.toByteArray();
	}

	@Test
	void pcmWav() throws IOException {
		// 22050 Hz, 16 bit mono: 44100 bytes are one second.
		assertEquals(1.0f, read(wav(1, 1, 22050, 2, 16, new byte[0], null, 44100)), EPSILON);
		// 44100 Hz, 16 bit stereo, with an odd chunk before the data.
		assertEquals(0.5f, read(wav(1, 2, 44100, 4, 16, new byte[0], null, 88200)), EPSILON);
	}

	@Test
	void adpcmWavUsesItsSampleCount() throws IOException {
		final byte[] extra = le(4).putShort((short) 2).putShort((short) 1017).array();
		assertEquals(0.5f, read(wav(0x11, 1, 22050, 512, 4, extra, 11025, 5632)), EPSILON);
		// Without a fact chunk: whole blocks times samples per block.
		assertEquals((11 * 1017) / 22050f, read(wav(0x11, 1, 22050, 512, 4, extra, null, 5632)), EPSILON);
	}

	private static byte[] mp3Frame(final int b1, final int b2, final int b3, final int length, final String tag) {
		final byte[] frame = new byte[length];
		frame[0] = (byte) 0xFF;
		frame[1] = (byte) b1;
		frame[2] = (byte) b2;
		frame[3] = (byte) b3;
		if (tag != null) {
			final byte[] text = tag.getBytes(StandardCharsets.US_ASCII);
			System.arraycopy(text, 0, frame, 36, text.length);
		}
		return frame;
	}

	@Test
	void mpeg1Layer3() throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		// ID3v2 tag of 100 bytes.
		out.writeBytes(new byte[] { 'I', 'D', '3', 3, 0, 0, 0, 0, 0, 100 });
		out.writeBytes(new byte[100]);
		// 128 kbit/s, 44100 Hz: 417 bytes, 418 with padding. The first frame is a Xing header, not audio.
		out.writeBytes(mp3Frame(0xFB, 0x90, 0x00, 417, "Xing"));
		for (int i = 0; i < 100; i++) {
			final boolean padded = (i % 3) == 0;
			out.writeBytes(mp3Frame(0xFB, padded ? 0x92 : 0x90, 0x00, padded ? 418 : 417, null));
		}
		final byte[] id3v1 = new byte[128];
		id3v1[0] = 'T';
		id3v1[1] = 'A';
		id3v1[2] = 'G';
		out.writeBytes(id3v1);
		assertEquals((100 * 1152) / 44100f, read(out.toByteArray()), EPSILON);
	}

	@Test
	void mpeg2Layer3SkipsJunkBetweenFrames() throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		// 64 kbit/s, 22050 Hz: 72 * 64000 / 22050 = 208 bytes, 576 samples.
		for (int i = 0; i < 50; i++) {
			out.writeBytes(mp3Frame(0xF3, 0x80, 0x00, 208, null));
			if (i == 20) {
				out.writeBytes(new byte[] { 1, 2, 3 });
			}
		}
		assertEquals((50 * 576) / 22050f, read(out.toByteArray()), EPSILON);
	}

	@Test
	void flac() throws IOException {
		final ByteBuffer data = ByteBuffer.allocate(4 + 4 + 34);
		data.put("fLaC".getBytes(StandardCharsets.US_ASCII));
		data.put((byte) 0x80).put((byte) 0).put((byte) 0).put((byte) 34);
		data.putShort((short) 4096).putShort((short) 4096).put(new byte[6]);
		// 44100 Hz (20 bits), 2 channels, 16 bits, 88200 samples (36 bits).
		final long packed = (44100L << 44) | (1L << 41) | (15L << 36) | 88200L;
		data.putLong(packed);
		assertEquals(2.0f, read(data.array()), EPSILON);
	}

	private static byte[] oggPage(final long granule, final byte[] body) {
		final ByteBuffer page = le(27 + 1 + body.length);
		page.put("OggS".getBytes(StandardCharsets.US_ASCII)).put((byte) 0).put((byte) 0).putLong(granule).putInt(7)
				.putInt(0).putInt(0).put((byte) 1).put((byte) body.length).put(body);
		return page.array();
	}

	@Test
	void oggVorbis() throws IOException {
		final ByteBuffer id = le(30);
		id.put((byte) 1).put("vorbis".getBytes(StandardCharsets.US_ASCII)).putInt(0).put((byte) 2).putInt(48000);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.writeBytes(oggPage(0, id.array()));
		out.writeBytes(oggPage(48000, new byte[100]));
		out.writeBytes(oggPage(96000, new byte[50]));
		assertEquals(2.0f, read(out.toByteArray()), EPSILON);
	}

	@Test
	void notASound() throws IOException {
		assertEquals(0f, read(new byte[0]), EPSILON);
		assertEquals(0f, read("plain text, not a sound".getBytes(StandardCharsets.US_ASCII)), EPSILON);
		assertEquals(0f, SoundFileDuration.read(null, "Sound\\none.wav"), EPSILON);
	}
}
