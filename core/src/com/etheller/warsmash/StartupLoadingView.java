package com.etheller.warsmash;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.util.ImageUtils;

/**
 * Drawn while the menu is being built when no map is started right after it, so startup is not a black screen without
 * progress. Until the game data is open only a plain bar is available; after that the load bar textures of the game
 * itself are used. There is no backdrop: the original shows no picture before its menu or a loading screen.
 */
final class StartupLoadingView {
	private static final String BAR = "UI\\Glues\\Loading\\LoadBar\\Loading-Bar";
	private static final String MODEL_TEXTURES = "Textures\\Loading-Bar";
	// Layout of UI\Glues\Loading\LoadBar\LoadBar.mdx, the bar of the map loading screen: the border spans 0.458 of the
	// 0.8 wide 4:3 screen at 8:1, and every other layer covers its own part of it, given as left, right, bottom and top
	// insets in fractions of the border.
	private static final float BAR_WIDTH_OF_4_3 = 0.5726f;
	private static final float BAR_ASPECT = 8.008f;
	private static final float BAR_BOTTOM_OF_HEIGHT = 0.0548f;
	private static final float[] CHANNEL = { 0.0593f, 0.0642f, 0.2692f, 0.2710f };
	private static final float[] GLASS = { 0.0699f, 0.0685f, 0.5175f, 0.3409f };

	private final SpriteBatch batch = new SpriteBatch();
	private final ShapeRenderer shapes = new ShapeRenderer();
	private Texture barBackground;
	private Texture barFill;
	private Texture barGlass;
	private Texture barBorder;

	void loadGameTextures(final DataSource dataSource) {
		this.barBackground = loadEither(dataSource, MODEL_TEXTURES + "Background.blp", BAR + "Background.blp");
		this.barFill = load(dataSource, BAR + "Fill.blp");
		this.barGlass = loadEither(dataSource, MODEL_TEXTURES + "Glass.blp", BAR + "Glass.blp");
		this.barBorder = load(dataSource, BAR + "Border.blp");
	}

	private static Texture loadEither(final DataSource dataSource, final String path, final String fallback) {
		final Texture texture = load(dataSource, path);
		return texture != null ? texture : load(dataSource, fallback);
	}

	private static Texture load(final DataSource dataSource, final String path) {
		try {
			return ImageUtils.getAnyExtensionTexture(dataSource, path);
		}
		catch (final RuntimeException e) {
			return null;
		}
	}

	/**
	 * @param showBar false while a map's own loading screen is about to be shown: the screen stays black until then, as
	 *                the original goes to black before its loading screen
	 */
	void render(final float progress, final boolean showBar) {
		final int width = Gdx.graphics.getWidth();
		final int height = Gdx.graphics.getHeight();
		Gdx.gl.glViewport(0, 0, width, height);
		Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		this.batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
		this.shapes.setProjectionMatrix(this.batch.getProjectionMatrix());
		if (!showBar) {
			return;
		}

		final float fourThirdsWidth = Math.min(width, (height * 4f) / 3f);
		final float barWidth = fourThirdsWidth * BAR_WIDTH_OF_4_3;
		final float barHeight = barWidth / BAR_ASPECT;
		final float barX = (width - barWidth) / 2;
		final float barY = height * BAR_BOTTOM_OF_HEIGHT;
		final float clamped = Math.max(0, Math.min(1, progress));

		this.batch.begin();
		this.batch.setColor(Color.WHITE);
		final boolean textured = (this.barBackground != null) && (this.barFill != null);
		if (textured) {
			drawPart(this.barBackground, barX, barY, barWidth, barHeight, CHANNEL, 1);
			drawPart(this.barFill, barX, barY, barWidth, barHeight, CHANNEL, clamped);
			if (this.barGlass != null) {
				drawPart(this.barGlass, barX, barY, barWidth, barHeight, GLASS, 1);
			}
			if (this.barBorder != null) {
				this.batch.draw(this.barBorder, barX, barY, barWidth, barHeight);
			}
		}
		this.batch.end();

		if (!textured) {
			Gdx.gl.glEnable(GL20.GL_BLEND);
			this.shapes.begin(ShapeType.Filled);
			this.shapes.setColor(0.05f, 0.06f, 0.09f, 1);
			this.shapes.rect(barX, barY, barWidth, barHeight);
			this.shapes.setColor(0.22f, 0.45f, 0.78f, 1);
			final float inset = Math.max(2, barHeight * 0.12f);
			this.shapes.rect(barX + inset, barY + inset, (barWidth - (2 * inset)) * clamped, barHeight - (2 * inset));
			this.shapes.end();
			this.shapes.begin(ShapeType.Line);
			this.shapes.setColor(0.79f, 0.64f, 0.29f, 1);
			this.shapes.rect(barX, barY, barWidth, barHeight);
			this.shapes.end();
		}
	}

	private void drawPart(final Texture texture, final float barX, final float barY, final float barWidth,
			final float barHeight, final float[] insets, final float shown) {
		final float x = barX + (barWidth * insets[0]);
		final float y = barY + (barHeight * insets[2]);
		final float partWidth = barWidth * (1 - insets[0] - insets[1]);
		final float partHeight = barHeight * (1 - insets[2] - insets[3]);
		this.batch.draw(texture, x, y, partWidth * shown, partHeight, 0, 0, Math.round(texture.getWidth() * shown),
				texture.getHeight(), false, false);
	}

	void dispose() {
		this.batch.dispose();
		this.shapes.dispose();
		final Texture[] owned = { this.barBackground, this.barFill, this.barGlass, this.barBorder };
		for (final Texture texture : owned) {
			if (texture != null) {
				texture.dispose();
			}
		}
	}
}
