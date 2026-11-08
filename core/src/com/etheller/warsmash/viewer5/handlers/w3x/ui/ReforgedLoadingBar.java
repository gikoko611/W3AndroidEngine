package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.etheller.warsmash.datasources.DataSource;

/**
 * The loading bar of the Reforged web interface's loading screens (LoadingBar, 1100 x 70 in a 1080 pixel high design
 * space): a nine-part frame image with its fill and end cap.
 */
public final class ReforgedLoadingBar {
	static final String FRAME = ReforgedLoadingScreen.FOLDER + "loading-bar-4k.png";
	private static final String FILL = ReforgedLoadingScreen.FOLDER + "loadingscreen-fill-flat.png";
	private static final String ENDCAP = ReforgedLoadingScreen.FOLDER + "loadingscreen-bar-endcap_4k.png";
	public static final float DESIGN_HEIGHT = 1080;
	public static final float WIDTH = 1100;
	public static final float HEIGHT = 70;

	private final Texture frame;
	private final Texture fill;
	private final Texture endcap;

	private ReforgedLoadingBar(final Texture frame, final Texture fill, final Texture endcap) {
		this.frame = frame;
		this.fill = fill;
		this.endcap = endcap;
	}

	/** @return null when the game data has no Reforged web interface */
	public static ReforgedLoadingBar load(final DataSource dataSource) {
		if (!dataSource.has(FRAME)) {
			return null;
		}
		final Texture frame = ReforgedLoadingScreen.load(dataSource, FRAME, 0);
		final Texture fill = ReforgedLoadingScreen.load(dataSource, FILL, 0);
		if ((frame == null) || (fill == null)) {
			dispose(frame, fill);
			return null;
		}
		fill.setWrap(TextureWrap.Repeat, TextureWrap.ClampToEdge);
		return new ReforgedLoadingBar(frame, fill, ReforgedLoadingScreen.load(dataSource, ENDCAP, 0));
	}

	/**
	 * @param barX  left of the bar in screen pixels
	 * @param barY  bottom of the bar in screen pixels
	 * @param s     screen pixels for one pixel of the page
	 * @param ratio how much of the bar is filled, 0 to 1
	 */
	public void draw(final SpriteBatch batch, final float barX, final float barY, final float s, final float ratio) {
		final float barW = WIDTH * s;
		final float barH = HEIGHT * s;
		ReforgedLoadingScreen.drawFrame(batch, this.frame, barX, barY, barW, barH, new float[] { 40, 80, 82, 80 },
				new float[] { 18 * s, 42 * s, 34 * s, 42 * s });

		// The fill container is 1044 wide, 24 in from the left and 4 lower than the bar, and clips what it holds.
		final float clipX = barX + (24 * s);
		final float clipY = barY - (4 * s);
		final float clipW = 1044 * s;
		final float clipH = barH;
		final float fillX = clipX + (15 * s);
		final float fillW = Math.min(1075 * s * ratio, (clipX + clipW) - fillX);
		final float fillH = 41 * s;
		final float fillTop = (clipY + clipH) - (10 * s);
		if (fillW > 0) {
			final float tileW = 633 * s;
			final float tileH = (tileW * this.fill.getHeight()) / this.fill.getWidth();
			batch.draw(this.fill, fillX, fillTop - fillH, fillW, fillH, 0, 0, fillW / tileW, fillH / tileH);
		}
		if ((this.endcap != null) && (ratio > 0)) {
			// The end cap lights up the bar under it (color-dodge on the page).
			final float capX = (fillX + Math.max(fillW, 0)) - (14 * s);
			final float capW = 22 * s;
			final float capH = 82 * s;
			final float capTop = fillTop + (20 * s);
			final float visibleTop = Math.min(capTop, clipY + clipH);
			final float visibleBottom = Math.max(capTop - capH, clipY);
			final float visibleRight = Math.min(capX + capW, clipX + clipW);
			if ((visibleTop > visibleBottom) && (visibleRight > capX)) {
				batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
				batch.draw(this.endcap, capX, visibleBottom, visibleRight - capX, visibleTop - visibleBottom, 0,
						(capTop - visibleTop) / capH, (visibleRight - capX) / capW, (capTop - visibleBottom) / capH);
				batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
			}
		}
	}

	public void dispose() {
		dispose(this.frame, this.fill, this.endcap);
	}

	private static void dispose(final Texture... textures) {
		for (final Texture texture : textures) {
			if (texture != null) {
				texture.dispose();
			}
		}
	}
}
