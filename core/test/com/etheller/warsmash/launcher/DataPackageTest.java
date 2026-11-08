package com.etheller.warsmash.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.etheller.warsmash.viewer5.handlers.w3x.ui.mapsetup.MapListContainer;

/** Importing a data package zip, and the test maps the custom game lists leave out. */
class DataPackageTest {
	@TempDir
	File temp;

	private static Map<String, String> gameFiles(final String root, final String entries) {
		final Map<String, String> files = new LinkedHashMap<>();
		files.put(root + "ui/WorldEditGameStrings.txt", "[WorldEditStrings]\n");
		files.put(root + "units/UnitData.slk", "ID;PWXL;N;E\n");
		files.put(root + DataPackage.MARKER, "tool=Reforged Extractor toolkit 0.2.0\ngameVersion=3.0.1\n"
				+ "profile=full-classic\nlocale=enUS\nentries=" + entries + "\n");
		return files;
	}

	private File zip(final String name, final Map<String, String> files) throws IOException {
		final File zip = new File(this.temp, name);
		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
			for (final Map.Entry<String, String> file : files.entrySet()) {
				out.putNextEntry(new ZipEntry(file.getKey()));
				out.write(file.getValue().getBytes(StandardCharsets.UTF_8));
				out.closeEntry();
			}
		}
		return zip;
	}

	@Test
	void importsAndReplacesTheOldFolder() throws IOException {
		final File target = new File(this.temp, "data");
		target.mkdirs();
		Files.write(new File(target, "stale.txt").toPath(), new byte[] { 1 });
		final long[] last = new long[4];
		try (DataPackage dataPackage = DataPackage.open(zip("p.zip", gameFiles("", "3")))) {
			assertEquals("3.0.1", dataPackage.gameVersion);
			assertEquals("enUS", dataPackage.locale);
			assertEquals(3, dataPackage.fileCount());
			dataPackage.importInto(target, (bytes, total, files, count) -> {
				last[0] = bytes;
				last[1] = total;
				last[2] = files;
				last[3] = count;
			}, () -> false);
		}
		assertEquals(last[1], last[0]);
		assertEquals(3, last[2]);
		assertEquals(3, last[3]);
		assertEquals(DataFolderCheck.Kind.FOLDER, DataFolderCheck.detect(target));
		assertFalse(new File(target, "stale.txt").exists());
		assertFalse(new File(this.temp, "data.importing").exists());
		assertFalse(new File(this.temp, "data.old").exists());
		assertEquals("Reforged Extractor toolkit 0.2.0", DataFolderCheck.packerTool(target));
	}

	@Test
	void takesAPackageZippedWithItsFolder() throws IOException {
		final File target = new File(this.temp, "data");
		try (DataPackage dataPackage = DataPackage.open(zip("p.zip", gameFiles("w3-data-classic/", "3")))) {
			dataPackage.importInto(target, (bytes, total, files, count) -> {
			}, () -> false);
		}
		assertTrue(new File(target, "units/UnitData.slk").isFile());
	}

	@Test
	void refusesZipsThatAreNotWholePackages() throws IOException {
		final Map<String, String> noMarker = gameFiles("", "3");
		noMarker.remove(DataPackage.MARKER);
		assertEquals(DataPackage.Problem.NO_MARKER, problem(zip("a.zip", noMarker)));
		assertEquals(DataPackage.Problem.INCOMPLETE, problem(zip("b.zip", gameFiles("", "4"))));
		final Map<String, String> escaping = gameFiles("", "4");
		escaping.put("../outside.txt", "x");
		assertEquals(DataPackage.Problem.UNSAFE_PATH, problem(zip("c.zip", escaping)));
		final File notZip = new File(this.temp, "d.zip");
		Files.write(notZip.toPath(), "not a zip".getBytes(StandardCharsets.UTF_8));
		assertEquals(DataPackage.Problem.NOT_ZIP, problem(notZip));
		final Map<String, String> noData = new LinkedHashMap<>();
		noData.put(DataPackage.MARKER, "tool=Reforged Extractor toolkit\nentries=1\n");
		try (DataPackage dataPackage = DataPackage.open(zip("e.zip", noData))) {
			final DataPackage.ImportException e = assertThrows(DataPackage.ImportException.class,
					() -> dataPackage.importInto(new File(this.temp, "data"), (bytes, total, files, count) -> {
					}, () -> false));
			assertEquals(DataPackage.Problem.NO_GAME_DATA, e.problem);
		}
		assertFalse(new File(this.temp, "data").exists());
	}

	private static DataPackage.Problem problem(final File zip) {
		try (DataPackage dataPackage = DataPackage.open(zip)) {
			return null;
		}
		catch (final DataPackage.ImportException e) {
			return e.problem;
		}
		catch (final IOException e) {
			return DataPackage.Problem.IO;
		}
	}

	@Test
	void cancellingKeepsTheOldData() throws IOException {
		final File target = new File(this.temp, "data");
		target.mkdirs();
		Files.write(new File(target, "kept.txt").toPath(), new byte[] { 1 });
		try (DataPackage dataPackage = DataPackage.open(zip("p.zip", gameFiles("", "3")))) {
			final DataPackage.ImportException e = assertThrows(DataPackage.ImportException.class,
					() -> dataPackage.importInto(target, (bytes, total, files, count) -> {
					}, () -> true));
			assertEquals(DataPackage.Problem.CANCELLED, e.problem);
		}
		assertTrue(new File(target, "kept.txt").isFile());
		assertFalse(new File(this.temp, "data.importing").exists());
	}

	@Test
	void findsPackagesInAFolder() throws IOException {
		zip("w3-data-classic.zip", gameFiles("", "3"));
		zip("other.zip", new LinkedHashMap<>());
		assertEquals(1, DataPackage.packagesIn(this.temp).size());
	}

	@Test
	void readabilityCountsFilesAndKeepsTheFirstNames() throws IOException {
		final File folder = new File(this.temp, "data");
		new File(folder, "units").mkdirs();
		Files.write(new File(folder, "units/UnitData.slk").toPath(), new byte[] { 1 });
		final DataFolderCheck.Readability readability = DataFolderCheck.readability(folder, 100, 3);
		assertEquals(0, readability.unreadable);
		assertEquals(2, readability.files);
		assertFalse(readability.partial);
		assertTrue(DataFolderCheck.readability(folder, 1, 3).partial);
		assertEquals(1, DataFolderCheck.readability(new File(this.temp, "missing"), 100, 3).unreadable);
	}

	@Test
	void testMapsAreNotListed() {
		assertTrue(MapListContainer.isListedMap("Maps\\FrozenThrone\\(2)EchoIsles.w3x"));
		assertTrue(MapListContainer.isListedMap("(2)EchoIsles.w3x"));
		assertFalse(MapListContainer.isListedMap("Maps\\WC3Probes\\chain.w3x"));
		assertFalse(MapListContainer.isListedMap("maps/probemaps/test.w3x"));
		assertFalse(MapListContainer.isListedMap("ProbeMaps\\test.w3x"));
		assertTrue(MapListContainer.isTestMap("ProbeMaps\\test.w3x"));
		assertFalse(MapListContainer.isTestMap("Maps\\Campaign\\probemaps.w3x"));
	}
}
