package com.etheller.warsmash.datasources;

import java.util.Locale;

/**
 * The look of units, buildings, doodads, effects and terrain in a game: the Classic files, or one of the graphics
 * packages of 2.0 and later data laid over them. The packages are folders of the Classic data (war3.w3mod\_hd.w3mod
 * and war3.w3mod\_de.w3mod in an installed game) that hold the same paths with other files, and their skin fields
 * ("file:hd", "file:de") name other models. The interface has a choice of its own, see {@link LayeredDataSource}.
 */
public enum GraphicsSet {
	CLASSIC("Classic", new String[0], new String[] { "sd" }),
	/** Reforged HD: the full HD package, with the DE package below it for what HD lacks. */
	REFORGED("Reforged", new String[] { LayeredDataSource.HD_PACKAGE, LayeredDataSource.DE_PACKAGE },
			new String[] { "hd", "de" }),
	/** The lighter DE package, with the HD package below it for what DE lacks. */
	DE("DE", new String[] { LayeredDataSource.DE_PACKAGE, LayeredDataSource.HD_PACKAGE }, new String[] { "de", "hd" });

	/** A model every graphics package has; data without it lack the package. */
	static final String PACKAGE_MARK = "units\\human\\footman\\footman.mdx";

	private final String iniName;
	private final String[] packages;
	private final String[] qualities;

	GraphicsSet(final String iniName, final String[] packages, final String[] qualities) {
		this.iniName = iniName;
		this.packages = packages;
		this.qualities = qualities;
	}

	/** The name in the [Graphics] section of the engine configuration. */
	public String getIniName() {
		return this.iniName;
	}

	/** The package folders laid over the Classic data, the topmost first. */
	public String[] getPackages() {
		return this.packages.clone();
	}

	/** The skin field suffixes this set reads, the preferred first. */
	public String[] getQualities() {
		return this.qualities.clone();
	}

	/** Whether the data has this set's own package; Classic is always there. */
	public boolean isAvailable(final DataSource data) {
		return (this.packages.length == 0) || data.has(getMarkPath());
	}

	/** A file of this set's own package that shows the data has the package; null for Classic. */
	public String getMarkPath() {
		return this.packages.length == 0 ? null : this.packages[0] + "\\" + PACKAGE_MARK;
	}

	/** The set an engine configuration names; Classic for none or an unknown one. */
	public static GraphicsSet fromIniName(final String name) {
		if (name != null) {
			final String trimmed = name.trim().toLowerCase(Locale.ROOT);
			for (final GraphicsSet set : values()) {
				if (set.iniName.toLowerCase(Locale.ROOT).equals(trimmed)) {
					return set;
				}
			}
		}
		return CLASSIC;
	}
}
