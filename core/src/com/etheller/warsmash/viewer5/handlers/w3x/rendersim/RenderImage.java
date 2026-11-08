package com.etheller.warsmash.viewer5.handlers.w3x.rendersim;

import com.etheller.warsmash.viewer5.handlers.w3x.SplatModel;
import com.etheller.warsmash.viewer5.handlers.w3x.SplatModel.SplatMover;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CImage;

/**
 * Draws a script image on the ground. The splat is laid again when the image moves or changes its type or water
 * setting. The type sets the draw order: ubersplat images lowest, then occlusion masks, indicators and selections on
 * top; images of one type in the order they were made. An image at a constant height is drawn flat at that height
 * instead of on the ground.
 */
public final class RenderImage {
	private static final float LOWEST_LAYER = 2.0f;
	private static final float LAYER_STEP = 0.5f;
	private static int nextKey;

	private final War3MapViewer viewer;
	private final CImage image;
	private final String modelKey;
	private SplatModel model;
	private SplatMover mover;
	private float laidX = Float.NaN;
	private float laidY = Float.NaN;
	private boolean laidAboveWater;
	private int laidType;
	private boolean laidConstantHeight;
	private float laidHeight;

	public RenderImage(final War3MapViewer viewer, final CImage image) {
		this.viewer = viewer;
		this.image = image;
		this.modelKey = "image#" + (nextKey++);
	}

	/** False once the image is destroyed and no longer drawn. */
	public boolean update() {
		if (this.image.isDestroyed()) {
			remove();
			return false;
		}
		final float x = this.image.getMinX();
		final float y = this.image.getMinY();
		if ((this.model == null) || (x != this.laidX) || (y != this.laidY)
				|| (this.image.isAboveWater() != this.laidAboveWater) || (this.image.getImageType() != this.laidType)
				|| (this.image.isConstantHeight() != this.laidConstantHeight)
				|| (this.laidConstantHeight && (this.image.getHeight() != this.laidHeight))) {
			remove();
			this.laidX = x;
			this.laidY = y;
			this.laidAboveWater = this.image.isAboveWater();
			this.laidType = this.image.getImageType();
			this.mover = this.viewer.terrain.addImageSplat(this.modelKey, this.image.getFile(), x, y,
					x + this.image.getSizeX(), y + this.image.getSizeY(), this.laidAboveWater, layer(this.laidType));
			this.model = this.viewer.terrain.getSplatModel(this.modelKey);
			this.laidConstantHeight = this.image.isConstantHeight();
			this.laidHeight = this.image.getHeight();
			if ((this.mover != null) && this.laidConstantHeight) {
				this.mover.setHeightAbsolute(true, this.laidHeight);
			}
		}
		final int color = this.image.getColor();
		final float[] drawn = this.model.color;
		drawn[0] = ((color >>> 24) & 0xFF) / 255f;
		drawn[1] = ((color >>> 16) & 0xFF) / 255f;
		drawn[2] = ((color >>> 8) & 0xFF) / 255f;
		drawn[3] = this.image.isDrawn() ? (color & 0xFF) / 255f : 0;
		return true;
	}

	/** Selection (1) on top, ubersplat (4) lowest. */
	private static float layer(final int imageType) {
		final int fromBottom = CImage.TYPE_UBERSPLAT - Math.max(CImage.TYPE_SELECTION,
				Math.min(CImage.TYPE_UBERSPLAT, imageType));
		return LOWEST_LAYER + (fromBottom * LAYER_STEP);
	}

	private void remove() {
		if (this.model != null) {
			this.viewer.terrain.removeSingleUberSplat(this.modelKey, this.model);
			this.model = null;
			this.mover = null;
		}
	}
}
