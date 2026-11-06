package com.etheller.warsmash.datasources;

import java.util.Locale;

/**
 * Finds an asset that a data source does not have under the path it was asked for.
 * <p>
 * Reforged campaign maps, the 3.0 skin tables and even some Classic models name files that only the HD packages
 * have (units such as the Chaplain, effects, cinematic doodads, bridge textures); the game shows those in Classic
 * graphics too. The packages are layers of the Classic data (war3.w3mod\_de.w3mod, war3.w3mod\_hd.w3mod in CASC, the
 * same folders in a prepared data folder), so a path below them resolves through the usual prefixes. Scripts also
 * write paths with doubled separators or with the absolute path of the editor's import folder.
 */
public final class AssetPathFallback {
	// The DE package holds lighter versions of the same models.
	private static final String[] GRAPHICS_PACKAGES = { "_de.w3mod", "_hd.w3mod" };
	private static final String IMPORT_FOLDER = "war3mapimported\\";

	private AssetPathFallback() {
	}

	/**
	 * Returns a path that the data source has for the asset, or null. Call it only for a path the data source does not
	 * have, so that a Classic file always wins over the packages.
	 */
	public static String find(final DataSource dataSource, final String path) {
		if ((path == null) || path.isEmpty()) {
			return null;
		}
		final String normalized = normalize(path);
		if (!normalized.equals(path) && dataSource.has(normalized)) {
			return normalized;
		}
		for (final String graphicsPackage : GRAPHICS_PACKAGES) {
			final String candidate = graphicsPackage + "\\" + normalized;
			if (dataSource.has(candidate)) {
				return candidate;
			}
		}
		return null;
	}

	/**
	 * Backslash separated path without doubled separators, and relative to the map for an absolute path into the
	 * editor's import folder.
	 */
	public static String normalize(final String path) {
		final StringBuilder builder = new StringBuilder(path.length());
		for (int i = 0; i < path.length(); i++) {
			char c = path.charAt(i);
			if (c == '/') {
				c = '\\';
			}
			if ((c == '\\') && ((builder.length() == 0) || (builder.charAt(builder.length() - 1) == '\\'))) {
				continue;
			}
			builder.append(c);
		}
		String normalized = builder.toString();
		if ((normalized.length() > 2) && (normalized.charAt(1) == ':')) {
			final int importFolder = normalized.toLowerCase(Locale.US).lastIndexOf(IMPORT_FOLDER);
			if (importFolder != -1) {
				normalized = normalized.substring(importFolder);
			}
		}
		return normalized;
	}
}
