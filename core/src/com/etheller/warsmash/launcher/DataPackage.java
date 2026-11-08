package com.etheller.warsmash.launcher;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.zip.CRC32;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

/**
 * A game data package as the data packer hands it out: a zip of an extracted game data folder with the packer's marker
 * file, which says the tool, the game version and how many files the package holds. Importing unpacks it next to the
 * target folder, checks every file against the zip's checksums and the count against the marker, and only then puts
 * it in place of the target, so a cancelled or failed import leaves the old data as it was.
 */
public final class DataPackage implements Closeable {
	/** The packer's marker file, the last file it writes into a folder it made. */
	public static final String MARKER = "w3-datapacker.txt";
	/** Kept free beside the unpacked files, for the engine's own files and the system. */
	public static final long SPARE_BYTES = 64L << 20;
	private static final int BUFFER = 1 << 16;

	public enum Problem {
		/** Not a zip, or a damaged one. */
		NOT_ZIP,
		/** A zip without the packer's marker. */
		NO_MARKER,
		/** The marker counts other files than the zip holds: a package that was cut short. */
		INCOMPLETE,
		/** A file would land outside the folder. */
		UNSAFE_PATH,
		/** A file uses a compression this reader does not have. */
		UNSUPPORTED,
		NO_SPACE,
		/** A file does not match the zip's checksum. */
		DAMAGED,
		/** The unpacked folder is not game data. */
		NO_GAME_DATA,
		/** A file could not be read or written. */
		IO,
		CANCELLED
	}

	/** Why an import stopped, with the file or the sizes it is about. */
	public static final class ImportException extends IOException {
		private static final long serialVersionUID = 1L;
		public final Problem problem;
		/** The file concerned, or empty. */
		public final String file;

		public ImportException(final Problem problem, final String file, final Throwable cause) {
			super(problem + (file.isEmpty() ? "" : ": " + file), cause);
			this.problem = problem;
			this.file = file;
		}

		public ImportException(final Problem problem, final String file) {
			this(problem, file, null);
		}
	}

	/** Called while unpacking, from the unpacking thread. */
	public interface Progress {
		void update(long bytes, long totalBytes, int files, int totalFiles);
	}

	private final ZipFile zip;
	/** A folder the whole package lies in ("w3-data-classic/"), or empty. */
	private final String root;
	private final List<ZipArchiveEntry> files = new ArrayList<>();
	private final ZipArchiveEntry marker;
	/** Bytes of the unpacked files. */
	public final long totalBytes;
	public final String tool;
	public final String gameVersion;
	public final String profile;
	public final String locale;

	private DataPackage(final ZipFile zip) throws IOException {
		this.zip = zip;
		ZipArchiveEntry found = null;
		final List<ZipArchiveEntry> all = Collections.list(zip.getEntriesInPhysicalOrder());
		for (final ZipArchiveEntry entry : all) {
			final String name = entry.getName().replace('\\', '/');
			if (!entry.isDirectory() && name.toLowerCase(Locale.ROOT).endsWith(MARKER)
					&& (name.indexOf('/') == name.lastIndexOf('/'))
					&& ((found == null) || (name.length() < found.getName().length()))) {
				found = entry;
			}
		}
		if (found == null) {
			throw new ImportException(Problem.NO_MARKER, "");
		}
		this.marker = found;
		final String markerName = found.getName().replace('\\', '/');
		this.root = markerName.substring(0, markerName.length() - MARKER.length());
		if (!this.root.isEmpty() && !this.root.endsWith("/")) {
			throw new ImportException(Problem.NO_MARKER, "");
		}
		long bytes = 0;
		for (final ZipArchiveEntry entry : all) {
			final String name = entry.getName().replace('\\', '/');
			if (entry.isDirectory()) {
				continue;
			}
			if (!name.startsWith(this.root) || !isSafe(name.substring(this.root.length()))) {
				throw new ImportException(Problem.UNSAFE_PATH, name);
			}
			if (!zip.canReadEntryData(entry)) {
				throw new ImportException(Problem.UNSUPPORTED, name);
			}
			this.files.add(entry);
			bytes += Math.max(0, entry.getSize());
		}
		this.totalBytes = bytes;
		final List<String> lines = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(zip.getInputStream(found), StandardCharsets.UTF_8))) {
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				lines.add(line);
			}
		}
		this.tool = field(lines, "tool");
		this.gameVersion = field(lines, "gameVersion");
		this.profile = field(lines, "profile");
		this.locale = field(lines, "locale");
		if (this.tool.isEmpty()) {
			throw new ImportException(Problem.NO_MARKER, "");
		}
		final String entries = field(lines, "entries");
		try {
			if (!entries.isEmpty() && (Integer.parseInt(entries) != this.files.size())) {
				throw new ImportException(Problem.INCOMPLETE, this.files.size() + " / " + entries);
			}
		}
		catch (final NumberFormatException e) {
			throw new ImportException(Problem.NO_MARKER, "");
		}
	}

	/** Reads a package from a seekable channel, such as a file a document picker opened; closed with the package. */
	public static DataPackage open(final SeekableByteChannel channel) throws IOException {
		final ZipFile zip;
		try {
			zip = new ZipFile(channel);
		}
		catch (final IOException e) {
			channel.close();
			throw new ImportException(Problem.NOT_ZIP, "", e);
		}
		return open(zip);
	}

	public static DataPackage open(final File file) throws IOException {
		final ZipFile zip;
		try {
			zip = new ZipFile(file);
		}
		catch (final IOException e) {
			throw new ImportException(Problem.NOT_ZIP, file.getName(), e);
		}
		return open(zip);
	}

	private static DataPackage open(final ZipFile zip) throws IOException {
		try {
			return new DataPackage(zip);
		}
		catch (final IOException | RuntimeException e) {
			zip.close();
			if (e instanceof ImportException) {
				throw (ImportException) e;
			}
			throw new ImportException(Problem.NOT_ZIP, "", e);
		}
	}

	public int fileCount() {
		return this.files.size();
	}

	/** Bytes the import needs free where the target lies. */
	public long neededBytes() {
		return this.totalBytes + SPARE_BYTES;
	}

	private static String field(final List<String> lines, final String name) {
		for (final String line : lines) {
			final int equals = line.indexOf('=');
			if ((equals > 0) && name.equalsIgnoreCase(line.substring(0, equals).trim())) {
				return line.substring(equals + 1).trim();
			}
		}
		return "";
	}

	/** A relative path that stays inside the folder it is unpacked into. */
	private static boolean isSafe(final String name) {
		if (name.isEmpty() || name.startsWith("/") || (name.indexOf(':') >= 0) || (name.indexOf('\0') >= 0)) {
			return false;
		}
		for (final String part : name.split("/")) {
			if (part.equals("..")) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Unpacks the package and puts it in place of the target folder; the old folder is removed afterwards. The marker is
	 * written last, as the packer does, so a folder with it is whole. The caller chooses a target that may be replaced.
	 *
	 * @param cancelled asked between blocks; true stops the import and removes what was unpacked
	 * @return the target folder
	 */
	public File importInto(final File target, final Progress progress, final BooleanSupplier cancelled)
			throws ImportException {
		final File parent = target.getAbsoluteFile().getParentFile();
		parent.mkdirs();
		final File staging = new File(parent, target.getName() + ".importing");
		final File old = new File(parent, target.getName() + ".old");
		deleteTree(staging);
		// Old data left by an import that stopped between the two renames is given back first.
		if (!target.exists() && old.isDirectory()) {
			old.renameTo(target);
		}
		deleteTree(old);
		final long free = parent.getUsableSpace();
		if (free < neededBytes()) {
			throw new ImportException(Problem.NO_SPACE, free + " / " + neededBytes());
		}
		boolean done = false;
		try {
			unpack(staging, progress, cancelled);
			if (DataFolderCheck.detect(staging) == DataFolderCheck.Kind.NONE) {
				throw new ImportException(Problem.NO_GAME_DATA, "");
			}
			if (target.exists() && !target.renameTo(old)) {
				throw new ImportException(Problem.IO, target.getName());
			}
			if (!staging.renameTo(target)) {
				old.renameTo(target);
				throw new ImportException(Problem.IO, target.getName());
			}
			done = true;
		}
		finally {
			if (!done) {
				deleteTree(staging);
			}
		}
		deleteTree(old);
		DataFolderCheck.forget(target);
		return target;
	}

	private void unpack(final File staging, final Progress progress, final BooleanSupplier cancelled)
			throws ImportException {
		final byte[] buffer = new byte[BUFFER];
		final CRC32 crc = new CRC32();
		final List<ZipArchiveEntry> ordered = new ArrayList<>(this.files);
		ordered.remove(this.marker);
		ordered.add(this.marker);
		long bytes = 0;
		int count = 0;
		progress.update(0, this.totalBytes, 0, ordered.size());
		for (final ZipArchiveEntry entry : ordered) {
			final String name = entry.getName().replace('\\', '/').substring(this.root.length());
			final File file = new File(staging, name);
			file.getParentFile().mkdirs();
			crc.reset();
			try (InputStream in = this.zip.getInputStream(entry); OutputStream out = new FileOutputStream(file)) {
				for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
					if (cancelled.getAsBoolean()) {
						throw new ImportException(Problem.CANCELLED, "");
					}
					out.write(buffer, 0, read);
					crc.update(buffer, 0, read);
					bytes += read;
					progress.update(bytes, this.totalBytes, count, ordered.size());
				}
			}
			catch (final ImportException e) {
				throw e;
			}
			catch (final IOException | RuntimeException e) {
				if (staging.getUsableSpace() < BUFFER) {
					throw new ImportException(Problem.NO_SPACE, name, e);
				}
				throw new ImportException(Problem.IO, name, e);
			}
			if ((entry.getCrc() != -1) && (crc.getValue() != entry.getCrc())) {
				throw new ImportException(Problem.DAMAGED, name);
			}
			count++;
			progress.update(bytes, this.totalBytes, count, ordered.size());
		}
	}

	/** Removes a folder and everything in it; what cannot be removed stays. Links are removed, not followed. */
	public static void deleteTree(final File file) {
		final File[] children = file.isDirectory() && !Files.isSymbolicLink(file.toPath()) ? file.listFiles() : null;
		if (children != null) {
			for (final File child : children) {
				deleteTree(child);
			}
		}
		file.delete();
	}

	/** The zip files directly in a folder that hold a data package, by name; empty when there are none. */
	public static List<File> packagesIn(final File folder) {
		final List<File> found = new ArrayList<>();
		final File[] children = folder == null ? null : folder.listFiles();
		if (children == null) {
			return found;
		}
		for (final File child : children) {
			if (child.isFile() && child.getName().toLowerCase(Locale.ROOT).endsWith(".zip")) {
				try (DataPackage dataPackage = open(child)) {
					found.add(child);
				}
				catch (final IOException e) {
					// Not a data package.
				}
			}
		}
		Collections.sort(found);
		return found;
	}

	@Override
	public void close() throws IOException {
		this.zip.close();
	}
}
