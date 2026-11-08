package com.etheller.warsmash.datasources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Game data with graphics packages laid over the Classic files: a path the package has is read from the package, every
 * other path from the Classic data. The interface (the "ui" folder and the command button icons) takes its own
 * packages, so the in-game graphics and the interface can be chosen apart, as the launcher offers them.
 * <p>
 * A package keeps the textures of the new materials as .dds, many with a "_diffuse" suffix (the terrain tiles), and
 * the speech of its models in its own locale folder; both are looked for under the names the Classic data asks for.
 */
public final class LayeredDataSource implements DataSource {
	public static final String HD_PACKAGE = "_hd.w3mod";
	public static final String DE_PACKAGE = "_de.w3mod";
	private static final String[] INTERFACE_FOLDERS = { "ui\\", "replaceabletextures\\commandbuttons\\",
		"replaceabletextures\\commandbuttonsdisabled\\", "replaceabletextures\\passivebuttons\\" };
	private static final String DIFFUSE_SUFFIX = "_diffuse";
	private static final String NOT_LAYERED = "";

	private final DataSource data;
	private final GraphicsSet graphics;
	private final String[] graphicsLayers;
	private final String[] interfaceLayers;
	/** The path in the data that each asked path reads, or NOT_LAYERED for the path itself. */
	private final Map<String, String> resolved = new ConcurrentHashMap<>();

	/**
	 * @param graphics          the in-game graphics; its packages the data lacks are left out
	 * @param reforgedInterface whether the interface comes from the HD package (the Reforged interface)
	 * @param locale            the language folder of the data, such as "enus"
	 */
	private LayeredDataSource(final DataSource data, final GraphicsSet graphics, final boolean reforgedInterface,
			final String locale) {
		this.data = data;
		this.graphics = graphics;
		this.graphicsLayers = layers(data, graphics.getPackages(), locale);
		this.interfaceLayers = reforgedInterface
				? layers(data, GraphicsSet.REFORGED.getPackages(), locale)
				: new String[0];
	}

	/**
	 * The data as the game reads it with these choices: the data itself for Classic graphics and the Classic
	 * interface, or for data without the packages.
	 */
	public static DataSource wrap(final DataSource data, final GraphicsSet graphics, final boolean reforgedInterface,
			final String locale) {
		final GraphicsSet available = graphics.isAvailable(data) ? graphics : GraphicsSet.CLASSIC;
		final boolean reforgedUi = reforgedInterface && GraphicsSet.REFORGED.isAvailable(data);
		if ((available == GraphicsSet.CLASSIC) && !reforgedUi) {
			return data;
		}
		return new LayeredDataSource(data, available, reforgedUi, locale);
	}

	/** The graphics a data source shows: its own when it is layered, else Classic. */
	public static GraphicsSet graphicsOf(final DataSource data) {
		return data instanceof LayeredDataSource ? ((LayeredDataSource) data).graphics : GraphicsSet.CLASSIC;
	}

	/** Whether a data source shows the Reforged interface. */
	public static boolean reforgedInterfaceOf(final DataSource data) {
		return (data instanceof LayeredDataSource) && (((LayeredDataSource) data).interfaceLayers.length > 0);
	}

	private static String[] layers(final DataSource data, final String[] packages, final String locale) {
		final List<String> layers = new ArrayList<>();
		for (final String graphicsPackage : packages) {
			if (data.has(graphicsPackage + "\\" + GraphicsSet.PACKAGE_MARK)) {
				// The package's speech wins over its other files, as a locale layer does over the Classic data.
				if ((locale != null) && !locale.isEmpty()) {
					layers.add(graphicsPackage + "\\_locales\\" + locale + ".w3mod\\");
				}
				layers.add(graphicsPackage + "\\");
			}
		}
		return layers.toArray(new String[0]);
	}

	/** The path in the data that is read for this path. */
	String resolve(final String filepath) {
		if (filepath == null) {
			return null;
		}
		final String found = this.resolved.get(filepath);
		if (found != null) {
			return found == NOT_LAYERED ? filepath : found;
		}
		final String layered = findLayered(filepath);
		this.resolved.put(filepath, layered == null ? NOT_LAYERED : layered);
		return layered == null ? filepath : layered;
	}

	private String findLayered(final String filepath) {
		final String path = filepath.replace('/', '\\');
		final String lower = path.toLowerCase(Locale.US);
		if (lower.startsWith(HD_PACKAGE) || lower.startsWith(DE_PACKAGE)) {
			return null;
		}
		final String[] layers = isInterface(lower) ? this.interfaceLayers : this.graphicsLayers;
		if (layers.length == 0) {
			return null;
		}
		final String diffuse = lower.endsWith(".dds") && !lower.endsWith(DIFFUSE_SUFFIX + ".dds")
				? path.substring(0, path.length() - 4) + DIFFUSE_SUFFIX + ".dds"
				: null;
		for (final String layer : layers) {
			if (this.data.has(layer + path)) {
				return layer + path;
			}
			if ((diffuse != null) && this.data.has(layer + diffuse)) {
				return layer + diffuse;
			}
		}
		return null;
	}

	private static boolean isInterface(final String lowerPath) {
		for (final String folder : INTERFACE_FOLDERS) {
			if (lowerPath.startsWith(folder)) {
				return true;
			}
		}
		return false;
	}

	private boolean isLayered(final String filepath) {
		return resolve(filepath) != filepath;
	}

	@Override
	public InputStream getResourceAsStream(final String filepath) throws IOException {
		return this.data.getResourceAsStream(resolve(filepath));
	}

	@Override
	public File getFile(final String filepath) throws IOException {
		return this.data.getFile(resolve(filepath));
	}

	@Override
	public File getDirectory(final String filepath) throws IOException {
		return this.data.getDirectory(filepath);
	}

	@Override
	public ByteBuffer read(final String path) throws IOException {
		return this.data.read(resolve(path));
	}

	@Override
	public boolean has(final String filepath) {
		return isLayered(filepath) || this.data.has(filepath);
	}

	/** A path a package has wins over the Classic files, whatever its place in the list. */
	@Override
	public String findFirst(final String... filepaths) {
		for (final String filepath : filepaths) {
			if (isLayered(filepath)) {
				return filepath;
			}
		}
		return this.data.findFirst(filepaths);
	}

	@Override
	public Collection<String> getListfile() {
		return this.data.getListfile();
	}

	@Override
	public void close() throws IOException {
		this.data.close();
	}
}
