package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.math.Rectangle;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.parsers.w3x.w3e.Corner;
import com.etheller.warsmash.parsers.w3x.w3e.War3MapW3e;
import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.units.Element;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WorldEditStrings;

/**
 * The map's terrain as the game rules see it, read from the map file and the terrain data tables: cliff levels,
 * ramps, water, ground heights and the corner grid. The drawn terrain keeps its own copy, laid out by the same
 * {@link CCliffLayout}, so the game runs the same with or without it.
 */
public final class CTerrainData {
	/** Cliff level reported outside the map, above any real one. */
	public static final int OUTSIDE_LAYER_HEIGHT = 999;

	private final char tileset;
	private final int columns;
	private final int rows;
	private final float centerOffsetX;
	private final float centerOffsetY;
	/** Indexed [column][row], as the drawn terrain's. */
	private final CTerrainCorner[][] corners;
	private final CCliffLayout.CliffTypes cliffTypes;
	private final float waterHeightOffset;
	private final float[] waterHeights;
	/** Ground height of each corner with its cliff level, ramp and deformation, in terrain units (128 world units). */
	private final float[] groundCornerHeights;
	/** Script terrain deformations per corner, in terrain units. */
	private float[] deformation;

	/** A terrain with no ramp models and water at the map's own height, for tests. */
	public CTerrainData(final War3MapW3e w3eFile, final char tileset) {
		this(w3eFile, tileset, CCliffLayout.CliffTypes.NONE, 0);
	}

	/** The tileset is the map info's, which the data files are chosen by. */
	public CTerrainData(final War3MapW3e w3eFile, final char tileset, final CCliffLayout.CliffTypes cliffTypes,
			final float waterHeightOffset) {
		this.tileset = Character.toLowerCase(tileset);
		this.cliffTypes = cliffTypes;
		this.waterHeightOffset = waterHeightOffset;
		final int[] mapSize = w3eFile.getMapSize();
		this.columns = mapSize[0];
		this.rows = mapSize[1];
		final float[] centerOffset = w3eFile.getCenterOffset();
		this.centerOffsetX = centerOffset[0];
		this.centerOffsetY = centerOffset[1];
		final Corner[][] mapCorners = w3eFile.getCorners();
		this.corners = new CTerrainCorner[this.columns][this.rows];
		this.waterHeights = new float[this.columns * this.rows];
		for (int row = 0; row < this.rows; row++) {
			for (int column = 0; column < this.columns; column++) {
				this.corners[column][row] = new CTerrainCorner(mapCorners[row][column]);
				this.waterHeights[(row * this.columns) + column] = mapCorners[row][column].getWaterHeight();
			}
		}
		CCliffLayout.markCliffs(this.corners, this.columns, this.rows);
		layOutCliffs(new Rectangle(0, 0, this.columns - 1, this.rows - 1));
		this.groundCornerHeights = new float[this.columns * this.rows];
		for (int row = 0; row < this.rows; row++) {
			for (int column = 0; column < this.columns; column++) {
				this.groundCornerHeights[(row * this.columns) + column] = this.corners[column][row]
						.computeFinalGroundHeight();
			}
		}
		// As the drawn terrain: the last row and column keep their plain heights, ramps only lift the others.
		updateGroundHeights(0, 0, this.columns - 1, this.rows - 1);
	}

	/**
	 * Reads the cliff types (CliffTypes.slk) and the water level (Water.slk) the map's terrain uses. Cliff types the
	 * data lacks take the map's first known one, so the later ones keep their place.
	 */
	public static CTerrainData load(final War3MapW3e w3eFile, final char tileset, final DataSource dataSource,
			final WorldEditStrings worldEditStrings) throws IOException {
		final DataTable cliffTable = new DataTable(worldEditStrings);
		try (InputStream stream = dataSource.getResourceAsStream("TerrainArt\\CliffTypes.slk")) {
			cliffTable.readSLK(stream);
		}
		final DataTable waterTable = new DataTable(worldEditStrings);
		try (InputStream stream = dataSource.getResourceAsStream("TerrainArt\\Water.slk")) {
			waterTable.readSLK(stream);
		}
		Element fallbackCliffInfo = null;
		for (final War3ID cliffTile : w3eFile.getCliffTiles()) {
			if (fallbackCliffInfo == null) {
				fallbackCliffInfo = cliffTable.get(cliffTile.asStringValue());
			}
		}
		final List<String> rampModelDirs = new ArrayList<>();
		for (final War3ID cliffTile : w3eFile.getCliffTiles()) {
			Element cliffInfo = cliffTable.get(cliffTile.asStringValue());
			if (cliffInfo == null) {
				cliffInfo = fallbackCliffInfo;
				if (cliffInfo == null) {
					continue;
				}
			}
			rampModelDirs.add(cliffInfo.getField("rampModelDir"));
		}
		final Element waterInfo = waterTable.get(w3eFile.getTileset() + "Sha");
		final float waterHeightOffset = waterInfo != null ? waterInfo.getFieldFloatValue("height") : 0;
		return new CTerrainData(w3eFile, tileset, new CCliffLayout.CliffTypes() {
			@Override
			public String getRampModelDir(final int cliffType) {
				return rampModelDirs.get(cliffType);
			}

			@Override
			public boolean has(final String path) {
				return dataSource.has(path);
			}
		}, waterHeightOffset);
	}

	private void layOutCliffs(final Rectangle area) {
		try {
			CCliffLayout.update(this.corners, this.columns, this.rows, area, this.cliffTypes,
					CCliffLayout.Listener.NONE);
		}
		catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void updateGroundHeights(final int x, final int y, final int width, final int height) {
		for (int j = y; j < (y + height); j++) {
			for (int i = x; i < (x + width); i++) {
				final float deformed = this.deformation == null ? 0 : this.deformation[(j * this.columns) + i];
				final float rampHeight = CCliffLayout.rampHeight(this.corners, this.columns, this.rows, i, j);
				this.groundCornerHeights[(j * this.columns) + i] = this.corners[i][j].computeFinalGroundHeight()
						+ rampHeight + deformed;
			}
		}
	}

	public char getTileset() {
		return this.tileset;
	}

	public int[] getCornerCount() {
		return new int[] { this.columns, this.rows };
	}

	public float getTerrainSpaceX(final float x) {
		return (x - this.centerOffsetX) / 128.0f;
	}

	public float getTerrainSpaceY(final float y) {
		return (y - this.centerOffsetY) / 128.0f;
	}

	private int cellX(final float x) {
		return (int) ((x - this.centerOffsetX) / 128.0f);
	}

	private int cellY(final float y) {
		return (int) ((y - this.centerOffsetY) / 128.0f);
	}

	/** The corner at the bottom left of the cell holding the point, or null outside the map. */
	private CTerrainCorner cornerAt(final float x, final float y) {
		final int cellX = cellX(x);
		final int cellY = cellY(y);
		if ((cellX >= 0) && (cellX < (this.columns - 1)) && (cellY >= 0) && (cellY < (this.rows - 1))) {
			return this.corners[cellX][cellY];
		}
		return null;
	}

	public int getLayerHeight(final float x, final float y) {
		final CTerrainCorner corner = cornerAt(x, y);
		return corner == null ? OUTSIDE_LAYER_HEIGHT : corner.getLayerHeight();
	}

	public boolean isWater(final float x, final float y) {
		final CTerrainCorner corner = cornerAt(x, y);
		return (corner != null) && (corner.getWater() != 0);
	}

	/** Whether the point is on a ramp (a walkable slope between cliff levels), where sight is not cut by them. */
	public boolean isRamp(final float x, final float y) {
		final CTerrainCorner corner = cornerAt(x, y);
		return (corner != null) && corner.romp;
	}

	/**
	 * Takes a cell out of the cliff layout, as terrain doodads (bridges, their own ramps) do where their pathing
	 * texture lies; {@link #flushHiddenCells()} lays the cliffs out again.
	 */
	public void hideCell(final int column, final int row) {
		this.corners[column][row].hideCliff = true;
	}

	public void flushHiddenCells() {
		layOutCliffs(new Rectangle(0, 0, this.columns - 1, this.rows - 1));
	}

	/**
	 * The height scripts read (GetLocationZ): the original game interpolates a tile bilinearly from its four corners,
	 * and over water it reads the water surface.
	 */
	public float getLocationZ(final float x, final float y) {
		final float userCellSpaceX = (x - this.centerOffsetX) / 128.0f;
		final float userCellSpaceY = (y - this.centerOffsetY) / 128.0f;
		final int cellX = (int) userCellSpaceX;
		final int cellY = (int) userCellSpaceY;
		if ((cellX < 0) || (cellX >= (this.columns - 1)) || (cellY < 0) || (cellY >= (this.rows - 1))) {
			return 0;
		}
		final float bottomLeft = this.groundCornerHeights[(cellY * this.columns) + cellX];
		final float bottomRight = this.groundCornerHeights[(cellY * this.columns) + cellX + 1];
		final float topLeft = this.groundCornerHeights[((cellY + 1) * this.columns) + cellX];
		final float topRight = this.groundCornerHeights[((cellY + 1) * this.columns) + cellX + 1];
		final float sqX = userCellSpaceX - cellX;
		final float sqY = userCellSpaceY - cellY;
		final float bottom = bottomLeft + ((bottomRight - bottomLeft) * sqX);
		final float top = topLeft + ((topRight - topLeft) * sqX);
		final float ground = (bottom + ((top - bottom) * sqY)) * 128.0f;
		if (this.corners[cellX][cellY].getWater() != 0) {
			return Math.max(ground, getWaterHeight(userCellSpaceX, userCellSpaceY, cellX, cellY));
		}
		return ground;
	}

	private float getWaterHeight(final float userCellSpaceX, final float userCellSpaceY, final int cellX,
			final int cellY) {
		final float bottomLeft = this.waterHeights[(cellY * this.columns) + cellX];
		final float bottomRight = this.waterHeights[(cellY * this.columns) + cellX + 1];
		final float topLeft = this.waterHeights[((cellY + 1) * this.columns) + cellX];
		final float topRight = this.waterHeights[((cellY + 1) * this.columns) + cellX + 1];
		final float sqX = userCellSpaceX - cellX;
		final float sqY = userCellSpaceY - cellY;
		final float height;
		if ((sqX + sqY) < 1) {
			height = bottomLeft + ((bottomRight - bottomLeft) * sqX) + ((topLeft - bottomLeft) * sqY);
		}
		else {
			height = topRight + ((bottomRight - topRight) * (1 - sqY)) + ((topLeft - topRight) * (1 - sqX));
		}
		return (height + this.waterHeightOffset) * 128.0f;
	}

	/** Sets the script deformation offsets (world units) of the corners in a rectangle. */
	public void setDeformation(final int column, final int row, final int width, final int height,
			final float[] offsets) {
		if (this.deformation == null) {
			this.deformation = new float[this.columns * this.rows];
		}
		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++) {
				this.deformation[((row + j) * this.columns) + column + i] = offsets[(j * width) + i] / 128f;
			}
		}
		updateGroundHeights(column, row, width, height);
	}

	/** Raises the corner at the bottom left of the cell holding the point by the given world units, for good. */
	public void adjustGround(final float x, final float y, final float amount) {
		final int column = cellX(x);
		final int row = cellY(y);
		final CTerrainCorner corner = this.corners[column][row];
		corner.setGroundHeight(corner.getGroundHeight() + (amount / 512));
		updateGroundHeights(column, row, 1, 1);
	}

	/** Raises the corners of a rectangle {x0, y0, x1, y1} for good, each by its world units in the buffer. */
	public void adjustGround(final int[] rect, final float[] modBuffer) {
		final int x = rect[0];
		final int y = rect[1];
		final int width = (rect[2] - rect[0]) + 1;
		final int height = (rect[3] - rect[1]) + 1;
		for (int j = y; j < (y + height); j++) {
			for (int i = x; i < (x + width); i++) {
				final CTerrainCorner corner = this.corners[i][j];
				corner.setGroundHeight(corner.getGroundHeight() + (modBuffer[(i - x) + ((j - y) * width)] / 512));
			}
		}
		updateGroundHeights(x, y, width, height);
	}

	/**
	 * Corner rectangle {x0, y0, x1, y1} that terrain height changes over the area touch. The x bounds are limited by
	 * the row count and the y bounds by the column count, as the drawn terrain does; on square maps it is the same.
	 */
	public int[] getTerrainModBufferSize(final float x, final float y, final float width, final float height) {
		return new int[] { Math.min(this.rows - 1, Math.max(0, cellX(x))),
				Math.min(this.columns - 1, Math.max(0, cellY(y))),
				Math.min(this.rows - 1, Math.max(0, cellX(x + width) + 1)),
				Math.min(this.columns - 1, Math.max(0, cellY(y + height) + 1)) };
	}

	public int[] getTerrainModBufferSize(final float centerX, final float centerY, final float radius) {
		return getTerrainModBufferSize(centerX - radius, centerY - radius, radius * 2, radius * 2);
	}
}
