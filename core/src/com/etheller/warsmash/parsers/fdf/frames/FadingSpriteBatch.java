package com.etheller.warsmash.parsers.fdf.frames;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.NumberUtils;

/**
 * A sprite batch whose output can be faded as a whole, so a frame and everything drawn inside it (textures, text,
 * tinted parts) fades together without each frame knowing about it.
 */
public class FadingSpriteBatch extends SpriteBatch {
	private static final int VERTEX_SIZE = 5;
	private static final int COLOR_OFFSET = 2;

	private Color unscaledColor;
	private float alphaScale = 1f;
	private float[] scaledVertices = new float[0];

	public float getAlphaScale() {
		return this.alphaScale;
	}

	public void setAlphaScale(final float alphaScale) {
		this.alphaScale = alphaScale;
		final Color color = unscaled();
		super.setColor(color.r, color.g, color.b, color.a * alphaScale);
	}

	private Color unscaled() {
		// SpriteBatch's constructor may set a color before this class's fields exist.
		if (this.unscaledColor == null) {
			this.unscaledColor = new Color(1, 1, 1, 1);
		}
		return this.unscaledColor;
	}

	@Override
	public void setColor(final Color tint) {
		unscaled().set(tint);
		super.setColor(tint.r, tint.g, tint.b, tint.a * this.alphaScale);
	}

	@Override
	public void setColor(final float r, final float g, final float b, final float a) {
		unscaled().set(r, g, b, a);
		super.setColor(r, g, b, a * this.alphaScale);
	}

	@Override
	public void setPackedColor(final float packedColor) {
		final Color color = unscaled();
		Color.abgr8888ToColor(color, packedColor);
		super.setColor(color.r, color.g, color.b, color.a * this.alphaScale);
	}

	@Override
	public Color getColor() {
		return unscaled();
	}

	@Override
	public float getPackedColor() {
		return unscaled().toFloatBits();
	}

	@Override
	public void draw(final Texture texture, final float[] spriteVertices, final int offset, final int count) {
		if (this.alphaScale >= 1f) {
			super.draw(texture, spriteVertices, offset, count);
			return;
		}
		// Text and sprites carry their own vertex colors, which the batch tint does not reach.
		if (this.scaledVertices.length < count) {
			this.scaledVertices = new float[count];
		}
		System.arraycopy(spriteVertices, offset, this.scaledVertices, 0, count);
		for (int i = COLOR_OFFSET; i < count; i += VERTEX_SIZE) {
			final int abgr = NumberUtils.floatToIntColor(this.scaledVertices[i]);
			final int alpha = (int) (((abgr >>> 24) & 0xff) * this.alphaScale);
			this.scaledVertices[i] = NumberUtils.intToFloatColor((abgr & 0x00ffffff) | (alpha << 24));
		}
		super.draw(texture, this.scaledVertices, 0, count);
	}
}
