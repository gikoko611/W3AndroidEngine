package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.etheller.warsmash.parsers.w3x.w3e.War3MapW3e;
import com.etheller.warsmash.util.ParseUtils;
import com.etheller.warsmash.util.War3ID;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;

/**
 * The game's terrain answers come from the map file and the cliff data alone: cliff levels, water and ramps of the
 * cell a point is in, the corner grid, and the ground height scripts read.
 */
class CTerrainDataTest {
	private static final int COLUMNS = 4;
	private static final int ROWS = 3;
	private static final float ORIGIN_X = -256;
	private static final float ORIGIN_Y = -128;

	private interface CornerValue {
		int get(int column, int row);
	}

	private static int layer(final int column, final int row) {
		return (column + (2 * row)) % 5;
	}

	private static boolean water(final int column, final int row) {
		return (column == 1) && (row == 1);
	}

	private static War3MapW3e map() throws IOException {
		return map(COLUMNS, ROWS, CTerrainDataTest::layer, (column, row) -> water(column, row) ? 1 : 0,
				(column, row) -> 0);
	}

	private static War3MapW3e map(final int columns, final int rows, final CornerValue layer, final CornerValue water,
			final CornerValue ramp) throws IOException {
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (LittleEndianDataOutputStream out = new LittleEndianDataOutputStream(bytes)) {
			ParseUtils.writeWar3ID(out, War3ID.fromString("W3E!"));
			out.writeInt(11);
			out.write('L');
			out.writeInt(0);
			out.writeInt(0);
			out.writeInt(0);
			out.writeInt(columns);
			out.writeInt(rows);
			out.writeFloat(ORIGIN_X);
			out.writeFloat(ORIGIN_Y);
			for (int row = 0; row < rows; row++) {
				for (int column = 0; column < columns; column++) {
					out.writeShort(8192);
					out.writeShort(8192);
					out.write(((water.get(column, row) != 0) ? 0b01000000 : 0)
							| ((ramp.get(column, row) != 0) ? 0b00010000 : 0));
					out.write(0);
					out.write(layer.get(column, row));
				}
			}
		}
		return new War3MapW3e(new LittleEndianDataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
	}

	private static float x(final float column) {
		return ORIGIN_X + (column * 128);
	}

	private static float y(final float row) {
		return ORIGIN_Y + (row * 128);
	}

	@Test
	void cellsAnswerWithTheirBottomLeftCorner() throws IOException {
		final CTerrainData terrain = new CTerrainData(map(), 'L');
		for (int row = 0; row < (ROWS - 1); row++) {
			for (int column = 0; column < (COLUMNS - 1); column++) {
				final float x = ORIGIN_X + (column * 128) + 100;
				final float y = ORIGIN_Y + (row * 128) + 5;
				assertEquals(layer(column, row), terrain.getLayerHeight(x, y));
				assertEquals(water(column, row), terrain.isWater(x, y));
			}
		}
	}

	@Test
	void outsideTheMapIsAboveEveryCliffAndDry() throws IOException {
		final CTerrainData terrain = new CTerrainData(map(), 'L');
		assertEquals(CTerrainData.OUTSIDE_LAYER_HEIGHT, terrain.getLayerHeight(ORIGIN_X - 300, ORIGIN_Y));
		assertEquals(CTerrainData.OUTSIDE_LAYER_HEIGHT, terrain.getLayerHeight(ORIGIN_X + ((COLUMNS - 1) * 128),
				ORIGIN_Y));
		assertFalse(terrain.isWater(ORIGIN_X + 128, ORIGIN_Y + (ROWS * 128)));
		assertEquals(0f, terrain.getLocationZ(ORIGIN_X - 300, ORIGIN_Y));
	}

	@Test
	void gridAndTerrainSpace() throws IOException {
		final CTerrainData terrain = new CTerrainData(map(), 'L');
		assertEquals('l', terrain.getTileset());
		assertArrayEquals(new int[] { COLUMNS, ROWS }, terrain.getCornerCount());
		assertEquals(1.5f, terrain.getTerrainSpaceX(ORIGIN_X + 192));
		assertEquals(-1f, terrain.getTerrainSpaceY(ORIGIN_Y - 128));
		assertArrayEquals(new int[] { 0, 0, 2, 2 }, terrain.getTerrainModBufferSize(ORIGIN_X + 64, ORIGIN_Y + 64, 64));
		assertArrayEquals(new int[] { 0, 0, 2, 3 },
				terrain.getTerrainModBufferSize(ORIGIN_X - 500, ORIGIN_Y - 500, 5000, 5000));
	}

	/** Six by six corners: rows 0-2 on level 0, rows 3-5 on level 1, ramp flags on column 3 across the cliff. */
	private static War3MapW3e rampMap() throws IOException {
		return map(6, 6, (column, row) -> row >= 3 ? 1 : 0, (column, row) -> 0,
				(column, row) -> ((column == 3) && ((row == 2) || (row == 3))) ? 1 : 0);
	}

	@Test
	void rampsAreWhereTheCliffTypeHasARampModel() throws IOException {
		final Set<String> asked = new HashSet<>();
		final CTerrainData terrain = new CTerrainData(rampMap(), 'L', new CCliffLayout.CliffTypes() {
			@Override
			public String getRampModelDir(final int cliffType) {
				return "Ramp";
			}

			@Override
			public boolean has(final String path) {
				asked.add(path);
				return true;
			}
		}, 0);
		assertTrue(asked.contains("Doodads\\Terrain\\Ramp\\RampBHLA0.mdx"));
		assertTrue(terrain.isRamp(x(2.5f), y(2.5f)));
		assertTrue(terrain.isRamp(x(2.5f), y(1.5f)));
		assertTrue(terrain.isRamp(x(3.5f), y(2.5f)));
		assertFalse(terrain.isRamp(x(0.5f), y(2.5f)));
		assertFalse(terrain.isRamp(x(2.5f), y(0.5f)));

		final CTerrainData noModels = new CTerrainData(rampMap(), 'L');
		assertFalse(noModels.isRamp(x(2.5f), y(2.5f)));
	}

	@Test
	void hiddenCellsLeaveTheRampLayout() throws IOException {
		final CTerrainData terrain = new CTerrainData(rampMap(), 'L', new CCliffLayout.CliffTypes() {
			@Override
			public String getRampModelDir(final int cliffType) {
				return "Ramp";
			}

			@Override
			public boolean has(final String path) {
				return true;
			}
		}, 0);
		terrain.hideCell(2, 2);
		terrain.hideCell(3, 2);
		assertTrue(terrain.isRamp(x(2.5f), y(2.5f)));
		terrain.flushHiddenCells();
		assertFalse(terrain.isRamp(x(2.5f), y(2.5f)));
		assertFalse(terrain.isRamp(x(3.5f), y(2.5f)));
	}

	@Test
	void locationZInterpolatesTheCornersAndFollowsChanges() throws IOException {
		// Ground height 0 at every corner: a corner stands at (level - 2) * 128.
		final CTerrainData terrain = new CTerrainData(map(), 'L');
		assertEquals((layer(0, 0) - 2) * 128f, terrain.getLocationZ(x(0), y(0)));
		final float halfway = (((layer(0, 0) + layer(1, 0) + layer(0, 1) + layer(1, 1)) / 4f) - 2) * 128f;
		assertEquals(halfway, terrain.getLocationZ(x(0.5f), y(0.5f)), 0.001f);

		terrain.setDeformation(0, 0, 1, 1, new float[] { 64 });
		assertEquals(((layer(0, 0) - 2) * 128f) + 64, terrain.getLocationZ(x(0), y(0)), 0.001f);
		terrain.setDeformation(0, 0, 1, 1, new float[] { 0 });
		terrain.adjustGround(x(0.2f), y(0.2f), 512);
		assertEquals(((layer(0, 0) - 2) * 128f) + 128, terrain.getLocationZ(x(0), y(0)), 0.001f);
		terrain.adjustGround(new int[] { 0, 0, 0, 0 }, new float[] { -512 });
		assertEquals((layer(0, 0) - 2) * 128f, terrain.getLocationZ(x(0), y(0)), 0.001f);
	}

	@Test
	void locationZOverWaterIsTheWaterSurfaceWhenHigher() throws IOException {
		final CTerrainData terrain = new CTerrainData(map(), 'L', CCliffLayout.CliffTypes.NONE, 10);
		// Water height 0 at every corner, raised by the tileset's 10 levels.
		assertEquals(10 * 128f, terrain.getLocationZ(x(1), y(1)), 0.001f);
		assertEquals((layer(0, 0) - 2) * 128f, terrain.getLocationZ(x(0), y(0)), 0.001f);
	}
}
