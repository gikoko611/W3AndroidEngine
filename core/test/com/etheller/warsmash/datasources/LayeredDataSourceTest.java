package com.etheller.warsmash.datasources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.util.StringBundle;

/**
 * The graphics packages are laid over the Classic data by path, the in-game interface with the models, and data
 * without the packages is read as it is.
 */
class LayeredDataSourceTest {
	private static final String FOOTMAN = "units\\human\\footman\\footman.mdx";
	private static final String BUTTON = "replaceabletextures\\commandbuttons\\btnfootman.blp";
	private static final String CINEMATIC_BORDER = "ui\\widgets\\escmenu\\human\\human-cinematic-border.blp";

	@Test
	void classicChoicesReadTheDataItself() {
		final DataSource data = installed();
		assertSame(data, LayeredDataSource.wrap(data, GraphicsSet.CLASSIC, "enus"));
	}

	@Test
	void dataWithoutThePackagesStaysClassic() {
		final DataSource data = new Files("units\\human\\footman\\footman.mdx");
		assertSame(data, LayeredDataSource.wrap(data, GraphicsSet.REFORGED, "enus"));
		assertFalse(GraphicsSet.REFORGED.isAvailable(data));
		assertTrue(GraphicsSet.CLASSIC.isAvailable(data));
	}

	@Test
	void reforgedGraphicsReadTheHdPackageFirst() throws Exception {
		final DataSource layered = LayeredDataSource.wrap(installed(), GraphicsSet.REFORGED, "enus");
		assertEquals("_hd.w3mod\\" + FOOTMAN, text(layered, FOOTMAN));
		// Only the DE package has it.
		assertEquals("_de.w3mod\\doodads\\tree.mdx", text(layered, "doodads\\tree.mdx"));
		// Neither package has it.
		assertEquals("units\\orc\\grunt\\grunt.mdx", text(layered, "units\\orc\\grunt\\grunt.mdx"));
		assertEquals(GraphicsSet.REFORGED, LayeredDataSource.graphicsOf(layered));
	}

	@Test
	void deGraphicsReadTheDePackageFirst() throws Exception {
		final DataSource layered = LayeredDataSource.wrap(installed(), GraphicsSet.DE, "enus");
		assertEquals("_de.w3mod\\" + FOOTMAN, text(layered, FOOTMAN));
	}

	@Test
	void interfaceAndIconsFollowTheGraphics() throws Exception {
		final DataSource classic = installed();
		assertEquals("ui\\console\\human\\humanuitile01.blp", text(classic, "UI\\Console\\Human\\HumanUITile01.blp"));
		assertEquals(BUTTON, text(classic, BUTTON));
		final DataSource hd = LayeredDataSource.wrap(installed(), GraphicsSet.REFORGED, "enus");
		assertEquals("_hd.w3mod\\ui\\console\\human\\humanuitile01.blp",
				text(hd, "UI\\Console\\Human\\HumanUITile01.blp"));
		assertEquals("_hd.w3mod\\" + BUTTON, text(hd, BUTTON));
		// The package's cinematic border is the portrait's frame, not the letterbox edge of the same name.
		assertEquals(CINEMATIC_BORDER, text(hd, CINEMATIC_BORDER));
	}

	@Test
	void diffuseTexturesAndLocalesAreFound() throws Exception {
		final DataSource layered = LayeredDataSource.wrap(installed(), GraphicsSet.REFORGED, "enus");
		assertEquals("_hd.w3mod\\terrainart\\lordaeron\\grass_diffuse.dds",
				text(layered, "TerrainArt\\Lordaeron\\Grass.dds"));
		assertEquals("_hd.w3mod\\_locales\\enus.w3mod\\sound\\footman.flac",
				text(layered, "sound\\footman.flac"));
		assertTrue(layered.has("TerrainArt/Lordaeron/Grass.dds"));
		assertEquals("doodads\\tree.mdx", layered.findFirst("units\\none.mdx", "doodads\\tree.mdx"));
		assertEquals(FOOTMAN, layered.findFirst("units\\orc\\grunt\\grunt.mdx", FOOTMAN));
	}

	@Test
	void settingsNameTheGraphicsAndTheLimit() {
		final DataTable ini = new DataTable(StringBundle.EMPTY);
		assertEquals(GraphicsSet.CLASSIC, GraphicsSettings.graphicsSet(ini));
		assertEquals(-1, GraphicsSettings.configuredTextureSize(ini));
		assertEquals(0, GraphicsSettings.textureSize(ini));
		GraphicsSettings.applyDefaultTextureSize(ini, 512);
		assertEquals(512, GraphicsSettings.textureSize(ini));
		GraphicsSettings.applyDefaultTextureSize(ini, 1024);
		assertEquals(512, GraphicsSettings.textureSize(ini));
		GraphicsSettings.apply(ini, GraphicsSet.DE, 0);
		assertEquals(GraphicsSet.DE, GraphicsSettings.graphicsSet(ini));
		assertEquals(0, GraphicsSettings.configuredTextureSize(ini));
		assertEquals(GraphicsSet.REFORGED, GraphicsSet.fromIniName(" reforged "));
		assertEquals(GraphicsSet.CLASSIC, GraphicsSet.fromIniName("Ultra"));
		assertEquals(512, GraphicsSettings.defaultTextureSize(true, false, 12 * 1024));
		assertEquals(512, GraphicsSettings.defaultTextureSize(true, true, 4 * 1024));
		assertEquals(1024, GraphicsSettings.defaultTextureSize(true, true, 8 * 1024));
		assertEquals(0, GraphicsSettings.defaultTextureSize(false, false, 16 * 1024));
	}

	/** Classic files with both packages over them, as an installed game has them. */
	private static DataSource installed() {
		return new Files(FOOTMAN, "units\\orc\\grunt\\grunt.mdx", "doodads\\tree.mdx",
				"ui\\console\\human\\humanuitile01.blp", "terrainart\\lordaeron\\grass.blp", "sound\\footman.flac",
				"_hd.w3mod\\" + FOOTMAN, "_hd.w3mod\\ui\\console\\human\\humanuitile01.blp",
				"_hd.w3mod\\terrainart\\lordaeron\\grass_diffuse.dds",
				"_hd.w3mod\\_locales\\enus.w3mod\\sound\\footman.flac", "_de.w3mod\\" + FOOTMAN,
				"_de.w3mod\\doodads\\tree.mdx", BUTTON, "_hd.w3mod\\" + BUTTON, CINEMATIC_BORDER,
				"_hd.w3mod\\" + CINEMATIC_BORDER);
	}

	private static String text(final DataSource data, final String path) throws Exception {
		try (InputStream in = data.getResourceAsStream(path)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/** Files whose content is their own lower case path; names are found in any case, as in the game data. */
	private static final class Files implements DataSource {
		private final Map<String, String> files = new HashMap<>();

		Files(final String... paths) {
			for (final String path : paths) {
				this.files.put(path.toLowerCase(Locale.US), path.toLowerCase(Locale.US));
			}
		}

		private String find(final String path) {
			return path == null ? null : this.files.get(path.replace('/', '\\').toLowerCase(Locale.US));
		}

		@Override
		public InputStream getResourceAsStream(final String filepath) {
			final String content = find(filepath);
			return content == null ? null : new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
		}

		@Override
		public File getFile(final String filepath) {
			return null;
		}

		@Override
		public File getDirectory(final String filepath) {
			return null;
		}

		@Override
		public ByteBuffer read(final String path) {
			final String content = find(path);
			return content == null ? null : ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8));
		}

		@Override
		public boolean has(final String filepath) {
			return find(filepath) != null;
		}

		@Override
		public Collection<String> getListfile() {
			return this.files.keySet();
		}

		@Override
		public void close() {
		}
	}
}
