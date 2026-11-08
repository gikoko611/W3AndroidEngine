package com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign;

import com.etheller.interpreter.ast.util.CHandle;

/**
 * A script image: a texture laid flat on the ground. Measured on 1.29: images are numbered in a pool of their own,
 * which the units' own ground images share, and an image of an unknown type (only 1 to 4 are known) or of a file that
 * is not there still gives a handle, with the id -1.
 */
public final class CImage implements CHandle {
	public static final int INVALID_ID = -1;
	public static final int TYPE_SELECTION = 1;
	public static final int TYPE_INDICATOR = 2;
	public static final int TYPE_OCCLUSION_MASK = 3;
	public static final int TYPE_UBERSPLAT = 4;

	private final String file;
	private final float sizeX;
	private final float sizeY;
	private float x;
	private float y;
	private float z;
	private final float originX;
	private final float originY;
	private int imageType;
	private int handleId = INVALID_ID;
	private boolean shown = true;
	private boolean render = true;
	private boolean renderAlways;
	private boolean constantHeight;
	private float height;
	private boolean aboveWater;
	private boolean useWaterAlpha;
	private int color = -1;
	private boolean destroyed;

	public CImage(final String file, final float sizeX, final float sizeY, final float x, final float y,
			final float z, final float originX, final float originY, final int imageType) {
		this.file = file == null ? "" : file;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.x = x;
		this.y = y;
		this.z = z;
		this.originX = originX;
		this.originY = originY;
		this.imageType = imageType;
	}

	public static boolean isKnownType(final int imageType) {
		return (imageType >= TYPE_SELECTION) && (imageType <= TYPE_UBERSPLAT);
	}

	@Override
	public int getHandleId() {
		return this.handleId;
	}

	public void setHandleId(final int handleId) {
		this.handleId = handleId;
	}

	public boolean isValid() {
		return this.handleId != INVALID_ID;
	}

	public String getFile() {
		return this.file;
	}

	/** The corner with the smallest coordinates: the image's position less its origin. */
	public float getMinX() {
		return this.x - this.originX;
	}

	public float getMinY() {
		return this.y - this.originY;
	}

	public float getSizeX() {
		return this.sizeX;
	}

	public float getSizeY() {
		return this.sizeY;
	}

	public float getZ() {
		return this.z;
	}

	public void setPosition(final float x, final float y, final float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public int getImageType() {
		return this.imageType;
	}

	public void setImageType(final int imageType) {
		this.imageType = imageType;
	}

	public boolean isShown() {
		return this.shown;
	}

	public void setShown(final boolean shown) {
		this.shown = shown;
	}

	public boolean isRender() {
		return this.render;
	}

	public void setRender(final boolean render) {
		this.render = render;
	}

	public boolean isRenderAlways() {
		return this.renderAlways;
	}

	public void setRenderAlways(final boolean renderAlways) {
		this.renderAlways = renderAlways;
	}

	/** Drawn at all: as with script ubersplats, only once the script asked for it to be drawn always. */
	public boolean isDrawn() {
		return !this.destroyed && this.shown && this.render && this.renderAlways;
	}

	public boolean isConstantHeight() {
		return this.constantHeight;
	}

	public float getHeight() {
		return this.height;
	}

	public void setConstantHeight(final boolean constantHeight, final float height) {
		this.constantHeight = constantHeight;
		this.height = height;
	}

	public boolean isAboveWater() {
		return this.aboveWater;
	}

	public boolean isUseWaterAlpha() {
		return this.useWaterAlpha;
	}

	public void setAboveWater(final boolean aboveWater, final boolean useWaterAlpha) {
		this.aboveWater = aboveWater;
		this.useWaterAlpha = useWaterAlpha;
	}

	/** Red, green, blue and alpha from the high byte down. */
	public int getColor() {
		return this.color;
	}

	public void setColor(final int color) {
		this.color = color;
	}

	public boolean isDestroyed() {
		return this.destroyed;
	}

	public void destroy() {
		this.destroyed = true;
	}
}
