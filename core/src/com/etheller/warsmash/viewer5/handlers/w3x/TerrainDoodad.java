package com.etheller.warsmash.viewer5.handlers.w3x;

import com.badlogic.gdx.math.Quaternion;
import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.ArgbImage;
import com.etheller.warsmash.util.RenderMathUtils;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxComplexInstance;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxModel;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.Terrain;

public class TerrainDoodad {
	private static final float[] locationHeap = new float[3];
	public final MdxComplexInstance instance;
	private final GameObject row;

	public TerrainDoodad(final War3MapViewer map, final MdxModel model, final GameObject row,
			final com.etheller.warsmash.parsers.w3x.doo.TerrainDoodad doodad, final ArgbImage pathingTextureImage) {
		final float[] centerOffset = map.terrain.centerOffset;
		final MdxComplexInstance instance = (MdxComplexInstance) model.addInstance(0);

		final int textureWidth = pathingTextureImage.getWidth();
		final int textureHeight = pathingTextureImage.getHeight();
		final int textureWidthTerrainCells = textureWidth / 4;
		final int textureHeightTerrainCells = textureHeight / 4;
		final int minCellX = ((int) doodad.getLocation()[0]);
		final int minCellY = ((int) doodad.getLocation()[1]);
		locationHeap[0] = ((minCellX * 128) + (textureWidthTerrainCells * 64) + centerOffset[0]);
		locationHeap[1] = ((minCellY * 128) + (textureHeightTerrainCells * 64) + centerOffset[1]);
		locationHeap[2] = baseHeight(map.terrain, minCellX, minCellY, textureWidthTerrainCells,
				textureHeightTerrainCells);

		instance.move(locationHeap);
		instance.rotate(new Quaternion().setFromAxisRad(RenderMathUtils.VEC3_UNIT_Z,
				(float) Math.toRadians(row.readSLKTagFloat("fixedRot"))));
		instance.setScene(map.worldScene);

		this.instance = instance;
		this.row = row;
	}

	/**
	 * Cliff doodads are modelled from the bottom of the cliff they replace, like the cliff meshes, so they stand on the
	 * lowest cliff level under them; at height 0 they sink out of sight on maps whose base level is above layer 2.
	 */
	private static float baseHeight(final Terrain terrain, final int minCellX, final int minCellY, final int cellsX,
			final int cellsY) {
		int minLayer = Integer.MAX_VALUE;
		final int maxX = Math.min(minCellX + cellsX, terrain.columns - 1);
		final int maxY = Math.min(minCellY + cellsY, terrain.rows - 1);
		for (int i = Math.max(minCellX, 0); i <= maxX; i++) {
			for (int j = Math.max(minCellY, 0); j <= maxY; j++) {
				minLayer = Math.min(minLayer, terrain.corners[i][j].getLayerHeight());
			}
		}
		if (minLayer == Integer.MAX_VALUE) {
			return 0;
		}
		// The model's edges meet the ground drawn around it, so only the border corners count; the hidden inner ones
		// often keep leftover heights.
		float groundHeight = 0;
		int count = 0;
		for (int i = Math.max(minCellX, 0); i <= maxX; i++) {
			for (int j = Math.max(minCellY, 0); j <= maxY; j++) {
				final boolean border = (i == minCellX) || (i == maxX) || (j == minCellY) || (j == maxY);
				if (border && (terrain.corners[i][j].getLayerHeight() == minLayer)) {
					groundHeight += terrain.corners[i][j].getGroundHeight();
					count++;
				}
			}
		}
		return ((minLayer - 2) + (count == 0 ? 0 : groundHeight / count)) * 128;
	}
}
