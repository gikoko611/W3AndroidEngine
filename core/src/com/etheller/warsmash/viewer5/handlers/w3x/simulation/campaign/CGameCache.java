package com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.badlogic.gdx.files.FileHandle;

/**
 * A game cache handle. Measured on 1.29: every InitGameCache call makes a new handle, handles of one file name share
 * the data, keys ignore case and each value type has its own keys; FlushGameCache empties the file and leaves the
 * handle dead (stores through it are lost).
 *
 * Measured on 1.29, on disk: SaveGameCache writes every cache as it is at that moment, not only the one it names, and
 * returns false for a dead handle; nothing else writes (a store or a flush after the last save is lost at exit);
 * ReloadGameCachesFromDisk puts every cache back to the last save, and a new game starts from it.
 */
public final class CGameCache {
	private static final String STORE_FILE = "GameCaches.w3v";
	private static final int STORE_VERSION = 1;
	private static final Map<String, Data> FILES = new HashMap<>();
	private static boolean loadedFromDisk;
	private static Disk disk = ProfileDisk.INSTANCE;

	private final String fileName;
	private Data data;
	private boolean flushed;

	public CGameCache(final String fileName) {
		loadFromDiskOnce();
		this.fileName = fileName.toLowerCase(Locale.ROOT);
		this.data = FILES.computeIfAbsent(this.fileName, key -> new Data());
	}

	public void flush() {
		FILES.remove(this.fileName);
		this.data = null;
		this.flushed = true;
	}

	public boolean save() {
		return live() && saveAllToDisk();
	}

	public static boolean saveAllToDisk() {
		final byte[] bytes;
		try {
			bytes = toBytes();
		}
		catch (final IOException e) {
			e.printStackTrace();
			return false;
		}
		return disk.write(bytes);
	}

	public static void reloadAllFromDisk() {
		final Map<String, Data> saved = new HashMap<>();
		final byte[] bytes = disk.read();
		if (bytes != null) {
			try {
				readAll(new DataInputStream(new ByteArrayInputStream(bytes)), saved);
			}
			catch (final IOException e) {
				e.printStackTrace();
			}
		}
		FILES.clear();
		FILES.putAll(saved);
		loadedFromDisk = true;
	}

	/**
	 * Where the caches are saved to and read from. A replay records what the game read and plays it back from there,
	 * since the profile's file may have changed in between.
	 */
	public interface Disk {
		/** The saved file's bytes, or null without one. */
		byte[] read();

		boolean write(byte[] bytes);
	}

	public static Disk profileDisk() {
		return ProfileDisk.INSTANCE;
	}

	/** Null puts back the profile's own file. */
	public static void setDisk(final Disk disk) {
		CGameCache.disk = disk == null ? ProfileDisk.INSTANCE : disk;
	}

	/** The caches as the game has them now, or null when none were read from the disk yet; for a replay's start. */
	public static byte[] snapshot() {
		if (!loadedFromDisk) {
			return null;
		}
		try {
			return toBytes();
		}
		catch (final IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/** Puts back the caches of a {@link #snapshot()}. */
	public static void restoreSnapshot(final byte[] snapshot) throws IOException {
		if (snapshot == null) {
			forgetLoaded();
			return;
		}
		restoreAll(new DataInputStream(new ByteArrayInputStream(snapshot)));
	}

	private static byte[] toBytes() throws IOException {
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (DataOutputStream out = new DataOutputStream(bytes)) {
			writeAll(out);
		}
		return bytes.toByteArray();
	}

	private static final class ProfileDisk implements Disk {
		private static final ProfileDisk INSTANCE = new ProfileDisk();

		@Override
		public byte[] read() {
			final FileHandle file = CSaveStorage.profileFile(STORE_FILE);
			if (!file.exists()) {
				return null;
			}
			try {
				return file.readBytes();
			}
			catch (final RuntimeException e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public boolean write(final byte[] bytes) {
			return CSaveStorage.writeAtomically(CSaveStorage.profileFile(STORE_FILE), out -> out.write(bytes));
		}
	}

	private static void loadFromDiskOnce() {
		if (!loadedFromDisk) {
			reloadAllFromDisk();
		}
	}

	/**
	 * Measured on 1.29: after ChangeLevel the next map sees only what was saved; stores after the last save and caches
	 * never saved are gone.
	 */
	public static void dropUnsaved() {
		forgetLoaded();
	}

	/** Another player profile has its own caches. */
	static void forgetLoaded() {
		FILES.clear();
		loadedFromDisk = false;
	}

	public static void writeAll(final DataOutputStream out) throws IOException {
		out.writeInt(STORE_VERSION);
		out.writeInt(FILES.size());
		for (final Map.Entry<String, Data> file : FILES.entrySet()) {
			out.writeUTF(file.getKey());
			file.getValue().write(out);
		}
	}

	private static void readAll(final DataInputStream in, final Map<String, Data> into) throws IOException {
		if (in.readInt() != STORE_VERSION) {
			throw new IOException("Unknown game cache version");
		}
		final int files = in.readInt();
		for (int i = 0; i < files; i++) {
			final String name = in.readUTF();
			final Data data = new Data();
			data.read(in);
			into.put(name, data);
		}
	}

	/** A saved game carries the caches of the moment it was saved. */
	public static void restoreAll(final DataInputStream in) throws IOException {
		final Map<String, Data> saved = new HashMap<>();
		readAll(in, saved);
		FILES.clear();
		FILES.putAll(saved);
		loadedFromDisk = true;
	}

	public void flushMission(final String mission) {
		if (live()) {
			final String key = key(mission);
			this.data.integers.remove(key);
			this.data.reals.remove(key);
			this.data.booleans.remove(key);
			this.data.strings.remove(key);
			this.data.units.remove(key);
		}
	}

	public void storeInteger(final String mission, final String key, final int value) {
		if (live()) {
			put(this.data.integers, mission, key, value);
		}
	}

	public void storeReal(final String mission, final String key, final float value) {
		if (live()) {
			put(this.data.reals, mission, key, value);
		}
	}

	public void storeBoolean(final String mission, final String key, final boolean value) {
		if (live()) {
			put(this.data.booleans, mission, key, value);
		}
	}

	public boolean storeString(final String mission, final String key, final String value) {
		if (live()) {
			put(this.data.strings, mission, key, value);
			return true;
		}
		return false;
	}

	public boolean storeUnit(final String mission, final String key, final CStoredUnit value) {
		if (live()) {
			put(this.data.units, mission, key, value);
			return true;
		}
		return false;
	}

	public Integer getInteger(final String mission, final String key) {
		return live() ? get(this.data.integers, mission, key) : null;
	}

	public Float getReal(final String mission, final String key) {
		return live() ? get(this.data.reals, mission, key) : null;
	}

	public Boolean getBoolean(final String mission, final String key) {
		return live() ? get(this.data.booleans, mission, key) : null;
	}

	public String getString(final String mission, final String key) {
		return live() ? get(this.data.strings, mission, key) : null;
	}

	public CStoredUnit getUnit(final String mission, final String key) {
		return live() ? get(this.data.units, mission, key) : null;
	}

	public void flushInteger(final String mission, final String key) {
		if (live()) {
			remove(this.data.integers, mission, key);
		}
	}

	public void flushReal(final String mission, final String key) {
		if (live()) {
			remove(this.data.reals, mission, key);
		}
	}

	public void flushBoolean(final String mission, final String key) {
		if (live()) {
			remove(this.data.booleans, mission, key);
		}
	}

	public void flushString(final String mission, final String key) {
		if (live()) {
			remove(this.data.strings, mission, key);
		}
	}

	public void flushUnit(final String mission, final String key) {
		if (live()) {
			remove(this.data.units, mission, key);
		}
	}

	/** A reload or a loaded game replaces the data behind every handle that was not flushed. */
	private boolean live() {
		if (this.flushed) {
			return false;
		}
		this.data = FILES.computeIfAbsent(this.fileName, key -> new Data());
		return true;
	}

	private static String key(final String text) {
		return text == null ? "" : text.toLowerCase(Locale.ROOT);
	}

	private static <T> void put(final Map<String, Map<String, T>> values, final String mission, final String key,
			final T value) {
		values.computeIfAbsent(key(mission), ignored -> new HashMap<>()).put(key(key), value);
	}

	private static <T> T get(final Map<String, Map<String, T>> values, final String mission, final String key) {
		final Map<String, T> missionValues = values.get(key(mission));
		return missionValues == null ? null : missionValues.get(key(key));
	}

	private static <T> void remove(final Map<String, Map<String, T>> values, final String mission, final String key) {
		final Map<String, T> missionValues = values.get(key(mission));
		if (missionValues != null) {
			missionValues.remove(key(key));
		}
	}

	private static final class Data {
		private final Map<String, Map<String, Integer>> integers = new HashMap<>();
		private final Map<String, Map<String, Float>> reals = new HashMap<>();
		private final Map<String, Map<String, Boolean>> booleans = new HashMap<>();
		private final Map<String, Map<String, String>> strings = new HashMap<>();
		private final Map<String, Map<String, CStoredUnit>> units = new HashMap<>();

		private void write(final DataOutputStream out) throws IOException {
			writeValues(out, this.integers, DataOutputStream::writeInt);
			writeValues(out, this.reals, DataOutputStream::writeFloat);
			writeValues(out, this.booleans, DataOutputStream::writeBoolean);
			writeValues(out, this.strings, (stream, text) -> stream.writeUTF(text));
			writeValues(out, this.units, (stream, unit) -> unit.write(stream));
		}

		private void read(final DataInputStream in) throws IOException {
			readValues(in, this.integers, DataInputStream::readInt);
			readValues(in, this.reals, DataInputStream::readFloat);
			readValues(in, this.booleans, DataInputStream::readBoolean);
			readValues(in, this.strings, stream -> stream.readUTF());
			readValues(in, this.units, CStoredUnit::read);
		}
	}

	private interface ValueWriter<T> {
		void write(DataOutputStream out, T value) throws IOException;
	}

	private interface ValueReader<T> {
		T read(DataInputStream in) throws IOException;
	}

	private static <T> void writeValues(final DataOutputStream out, final Map<String, Map<String, T>> values,
			final ValueWriter<T> writer) throws IOException {
		out.writeInt(values.size());
		for (final Map.Entry<String, Map<String, T>> mission : values.entrySet()) {
			out.writeUTF(mission.getKey());
			out.writeInt(mission.getValue().size());
			for (final Map.Entry<String, T> entry : mission.getValue().entrySet()) {
				out.writeUTF(entry.getKey());
				writer.write(out, entry.getValue());
			}
		}
	}

	private static <T> void readValues(final DataInputStream in, final Map<String, Map<String, T>> values,
			final ValueReader<T> reader) throws IOException {
		final int missions = in.readInt();
		for (int i = 0; i < missions; i++) {
			final Map<String, T> mission = new HashMap<>();
			values.put(in.readUTF(), mission);
			final int entries = in.readInt();
			for (int j = 0; j < entries; j++) {
				final String key = in.readUTF();
				mission.put(key, reader.read(in));
			}
		}
	}
}
