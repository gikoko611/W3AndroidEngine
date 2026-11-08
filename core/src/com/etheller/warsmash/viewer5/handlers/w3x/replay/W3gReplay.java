package com.etheller.warsmash.viewer5.handlers.w3x.replay;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * What a Warcraft III replay (.w3g) tells before its actions: the game's version and length, the map and the players.
 * This engine cannot play one, but lists it like the original lists it.
 */
public final class W3gReplay {
	public static final String EXTENSION = ".w3g";
	private static final byte[] MAGIC = "Warcraft III recorded game\u001a\0".getBytes(StandardCharsets.ISO_8859_1);
	/** From this version on (Reforged) the blocks have 4-byte sizes. */
	private static final int REFORGED_BLOCKS = 10032;
	/** Enough of the game's data for the records before the first action. */
	private static final int START_DATA = 1 << 16;
	private static final int RECORD_PLAYER = 0x16;
	private static final int RECORD_GAME_START = 0x19;
	private static final int SLOT_USED = 2;

	public static final class Player {
		public final int id;
		public final String name;
		public final boolean computer;
		public final int team;
		public final int color;
		/** As JASS race preferences: 1 human, 2 orc, 4 night elf, 8 undead, 32 random. */
		public final int race;
		public final boolean observer;

		Player(final int id, final String name, final boolean computer, final int team, final int color,
				final int race, final boolean observer) {
			this.id = id;
			this.name = name;
			this.computer = computer;
			this.team = team;
			this.color = color;
			this.race = race;
			this.observer = observer;
		}
	}

	/** "1.29.6060": the patch and the build. */
	public final String version;
	public final long lengthMillis;
	public final boolean multiplayer;
	public final String gameName;
	public final String mapPath;
	public final List<Player> players;

	private W3gReplay(final String version, final long lengthMillis, final boolean multiplayer,
			final String gameName, final String mapPath, final List<Player> players) {
		this.version = version;
		this.lengthMillis = lengthMillis;
		this.multiplayer = multiplayer;
		this.gameName = gameName;
		this.mapPath = mapPath;
		this.players = players;
	}

	public static W3gReplay read(final InputStream input) throws IOException {
		final DataInputStream in = new DataInputStream(input);
		final byte[] magic = new byte[MAGIC.length];
		in.readFully(magic);
		if (!Arrays.equals(magic, MAGIC)) {
			throw new IOException("not a Warcraft III replay");
		}
		final ByteBuffer head = ByteBuffer.wrap(readBytes(in, 20)).order(ByteOrder.LITTLE_ENDIAN);
		final int headerSize = head.getInt();
		head.getInt();
		final int headerVersion = head.getInt();
		head.getInt();
		final int blocks = head.getInt();
		final int read = MAGIC.length + 20;
		final ByteBuffer subHeader = ByteBuffer.wrap(readBytes(in, headerSize - read))
				.order(ByteOrder.LITTLE_ENDIAN);
		final int version;
		final int build;
		final int flags;
		if (headerVersion == 0) {
			subHeader.getShort();
			version = subHeader.getShort() & 0xFFFF;
		}
		else {
			subHeader.getInt();
			version = subHeader.getInt();
		}
		build = subHeader.getShort() & 0xFFFF;
		flags = subHeader.getShort() & 0xFFFF;
		final long length = subHeader.getInt() & 0xFFFFFFFFL;
		final byte[] data = startData(in, blocks, version >= REFORGED_BLOCKS);
		final String patch = "1." + (version >= 10000 ? version - 10000 : version) + "." + build;
		return parse(data, patch, length, (flags & 0x8000) != 0);
	}

	private static byte[] startData(final DataInputStream in, final int blocks, final boolean wideBlocks)
			throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final Inflater inflater = new Inflater();
		try {
			for (int i = 0; (i < blocks) && (out.size() < START_DATA); i++) {
				final ByteBuffer blockHead = ByteBuffer.wrap(readBytes(in, wideBlocks ? 12 : 8))
						.order(ByteOrder.LITTLE_ENDIAN);
				final int compressed = wideBlocks ? blockHead.getInt() : blockHead.getShort() & 0xFFFF;
				final int size = wideBlocks ? blockHead.getInt() : blockHead.getShort() & 0xFFFF;
				final byte[] block = readBytes(in, compressed);
				inflater.reset();
				inflater.setInput(block);
				final byte[] plain = new byte[size];
				int done = 0;
				while ((done < size) && !inflater.finished()) {
					final int n = inflater.inflate(plain, done, size - done);
					if ((n == 0) && (inflater.needsInput() || inflater.needsDictionary())) {
						break;
					}
					done += n;
				}
				out.write(plain, 0, done);
			}
		}
		catch (final DataFormatException e) {
			throw new IOException("damaged replay", e);
		}
		finally {
			inflater.end();
		}
		return out.toByteArray();
	}

	private static byte[] readBytes(final DataInputStream in, final int count) throws IOException {
		if (count < 0) {
			throw new IOException("damaged replay");
		}
		final byte[] bytes = new byte[count];
		in.readFully(bytes);
		return bytes;
	}

	private static W3gReplay parse(final byte[] bytes, final String version, final long length,
			final boolean multiplayer) throws IOException {
		try {
			final ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
			data.position(4);
			final List<Integer> ids = new ArrayList<>();
			final List<String> names = new ArrayList<>();
			readPlayer(data, ids, names);
			final String gameName = string(data);
			data.get();
			final byte[] settings = decodeSettings(encoded(data));
			final ByteBuffer settingsData = ByteBuffer.wrap(settings).order(ByteOrder.LITTLE_ENDIAN);
			settingsData.position(13);
			final String mapPath = string(settingsData);
			data.position(data.position() + 12);
			while ((data.get(data.position()) & 0xFF) == RECORD_PLAYER) {
				readPlayer(data, ids, names);
				data.position(data.position() + 4);
			}
			// Reforged puts its player profiles here, each a record of a known length.
			while (((data.get(data.position()) & 0xFF) == 0x38) || ((data.get(data.position()) & 0xFF) == 0x39)) {
				data.position(data.position() + 2);
				final int size = data.getInt();
				data.position(data.position() + size);
			}
			if ((data.get() & 0xFF) != RECORD_GAME_START) {
				throw new IOException("no game start record");
			}
			data.getShort();
			final int slots = data.get() & 0xFF;
			final List<Player> players = new ArrayList<>();
			for (int i = 0; i < slots; i++) {
				final int id = data.get() & 0xFF;
				data.get();
				final int status = data.get() & 0xFF;
				final boolean computer = data.get() != 0;
				final int team = data.get() & 0xFF;
				final int color = data.get() & 0xFF;
				final int race = data.get() & 0xFF;
				data.get();
				data.get();
				if (status != SLOT_USED) {
					continue;
				}
				String name = null;
				for (int j = 0; j < ids.size(); j++) {
					if (ids.get(j) == id) {
						name = names.get(j);
					}
				}
				// Observers sit in the team after the last one a map can have.
				final boolean observer = (team == 12) || (team == 24);
				players.add(new Player(id, computer ? null : name, computer, team, color, race & 0x3F, observer));
			}
			return new W3gReplay(version, length, multiplayer, gameName, mapPath, players);
		}
		catch (final RuntimeException e) {
			throw new IOException("damaged replay", e);
		}
	}

	private static void readPlayer(final ByteBuffer data, final List<Integer> ids, final List<String> names) {
		data.get();
		final int id = data.get() & 0xFF;
		final String name = string(data);
		final int extra = data.get() & 0xFF;
		data.position(data.position() + extra);
		ids.add(id);
		names.add(name);
	}

	private static String string(final ByteBuffer data) {
		final int start = data.position();
		int end = start;
		while (data.get(end) != 0) {
			end++;
		}
		data.position(end + 1);
		return new String(data.array(), start, end - start, StandardCharsets.UTF_8);
	}

	private static byte[] encoded(final ByteBuffer data) {
		final int start = data.position();
		int end = start;
		while (data.get(end) != 0) {
			end++;
		}
		data.position(end + 1);
		return Arrays.copyOfRange(data.array(), start, end);
	}

	/** Every 8th byte holds the low bits of the next 7: a clear bit means the byte was made odd by one. */
	private static byte[] decodeSettings(final byte[] encoded) {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (int i = 0; i < encoded.length; i += 8) {
			final int mask = encoded[i] & 0xFF;
			for (int j = 1; (j < 8) && ((i + j) < encoded.length); j++) {
				final int value = encoded[i + j] & 0xFF;
				out.write((mask & (1 << j)) != 0 ? value : value - 1);
			}
		}
		return out.toByteArray();
	}
}
