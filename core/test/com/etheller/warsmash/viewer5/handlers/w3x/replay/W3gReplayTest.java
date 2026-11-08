package com.etheller.warsmash.viewer5.handlers.w3x.replay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.Deflater;

import org.junit.jupiter.api.Test;

public class W3gReplayTest {
	@Test
	public void readsTheGameBeforeItsActions() throws IOException {
		final W3gReplay replay = W3gReplay.read(new ByteArrayInputStream(replay(29, 6060, 45125)));
		assertEquals("1.29.6060", replay.version);
		assertEquals(45125, replay.lengthMillis);
		assertFalse(replay.multiplayer);
		assertEquals("Local Game", replay.gameName);
		assertEquals("Maps\\FrozenThrone\\(2)EchoIsles.w3x", replay.mapPath);
		assertEquals(3, replay.players.size());
		assertEquals("Host", replay.players.get(0).name);
		assertEquals(2, replay.players.get(0).race);
		assertEquals(0, replay.players.get(0).color);
		assertTrue(replay.players.get(1).computer);
		assertNull(replay.players.get(1).name);
		assertEquals(8, replay.players.get(1).race);
		assertTrue(replay.players.get(2).observer);
	}

	@Test
	public void readsReforgedBlocks() throws IOException {
		final W3gReplay replay = W3gReplay.read(new ByteArrayInputStream(replay(10032, 6105, 1000)));
		assertEquals("1.32.6105", replay.version);
		assertEquals("Host", replay.players.get(0).name);
	}

	@Test
	public void refusesOtherFiles() {
		assertThrows(IOException.class,
				() -> W3gReplay.read(new ByteArrayInputStream("not a replay at all, not at all".getBytes())));
		final byte[] cut = replay(29, 6060, 1000);
		assertThrows(IOException.class, () -> W3gReplay.read(new ByteArrayInputStream(Arrays.copyOf(cut, 90))));
	}

	private static byte[] replay(final int version, final int build, final int length) {
		final byte[] data = startData();
		final Deflater deflater = new Deflater();
		deflater.setInput(data);
		deflater.finish();
		final byte[] compressed = new byte[data.length + 64];
		final int compressedSize = deflater.deflate(compressed);
		deflater.end();
		final boolean wide = version >= 10032;
		final ByteBuffer out = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN);
		out.put("Warcraft III recorded game\u001a\0".getBytes(StandardCharsets.ISO_8859_1));
		out.putInt(68).putInt(compressedSize).putInt(1).putInt(data.length).putInt(1);
		out.put("PX3W".getBytes(StandardCharsets.ISO_8859_1)).putInt(version).putShort((short) build)
				.putShort((short) 0).putInt(length).putInt(0);
		if (wide) {
			out.putInt(compressedSize).putInt(data.length).putInt(0);
		}
		else {
			out.putShort((short) compressedSize).putShort((short) data.length).putInt(0);
		}
		out.put(compressed, 0, compressedSize);
		return Arrays.copyOf(out.array(), out.position());
	}

	private static byte[] startData() {
		final ByteArrayOutputStream data = new ByteArrayOutputStream();
		write(data, 0, 0, 0, 0);
		player(data, 0, 1, "Host");
		string(data, "Local Game");
		data.write(0);
		final ByteArrayOutputStream settings = new ByteArrayOutputStream();
		for (int i = 0; i < 13; i++) {
			settings.write(i + 2);
		}
		string(settings, "Maps\\FrozenThrone\\(2)EchoIsles.w3x");
		string(settings, "Creator");
		final byte[] encoded = encode(settings.toByteArray());
		data.write(encoded, 0, encoded.length);
		data.write(0);
		write(data, 3, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0);
		data.write(0x16);
		player(data, 0, 2, "Watcher");
		write(data, 0, 0, 0, 0);
		final int[][] slots = { { 1, 2, 0, 0, 0, 2 }, { 0, 2, 1, 1, 1, 8 }, { 2, 2, 0, 12, 2, 32 },
			{ 0, 0, 0, 0, 3, 0 } };
		data.write(0x19);
		final int size = 1 + (slots.length * 9) + 6;
		write(data, size & 0xFF, size >> 8, slots.length);
		for (final int[] slot : slots) {
			write(data, slot[0], 100, slot[1], slot[2], slot[3], slot[4], slot[5], 1, 100);
		}
		write(data, 1, 2, 3, 4, 0, 2);
		write(data, 0x1F, 2, 0, 100, 0);
		return data.toByteArray();
	}

	private static void player(final ByteArrayOutputStream data, final int record, final int id, final String name) {
		write(data, record, id);
		string(data, name);
		write(data, 1, 0);
	}

	private static void string(final ByteArrayOutputStream data, final String text) {
		final byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
		data.write(bytes, 0, bytes.length);
		data.write(0);
	}

	private static void write(final ByteArrayOutputStream data, final int... bytes) {
		for (final int b : bytes) {
			data.write(b);
		}
	}

	/** Every byte made odd, so that none is zero, with a mask byte before each 7 telling which were even. */
	private static byte[] encode(final byte[] plain) {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (int i = 0; i < plain.length; i += 7) {
			int mask = 1;
			final int end = Math.min(plain.length, i + 7);
			for (int j = i; j < end; j++) {
				if ((plain[j] & 1) != 0) {
					mask |= 1 << ((j - i) + 1);
				}
			}
			out.write(mask);
			for (int j = i; j < end; j++) {
				out.write((plain[j] & 1) != 0 ? plain[j] : plain[j] + 1);
			}
		}
		return out.toByteArray();
	}
}
