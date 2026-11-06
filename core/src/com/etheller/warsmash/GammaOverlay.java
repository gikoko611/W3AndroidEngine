package com.etheller.warsmash;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.GameOptions;

/**
 * Gamma as a blend over the finished frame. A device gamma ramp does not exist on Android and rendering through an
 * extra framebuffer would cost every frame on a phone, so this approximates the curve: brightening is a screen blend,
 * which lifts the darks and keeps white; darkening is a multiply.
 */
final class GammaOverlay {
	private static final float MAX_BRIGHTEN = 0.35f;
	private static final float MAX_DARKEN = 0.5f;
	private static final Matrix4 PROJECTION = new Matrix4().setToOrtho2D(0, 0, 1, 1);
	private static Texture white;
	// A finished game activity takes its GL context along, and with it the texture; the next one makes its own.
	private static Object whiteContext;

	private GammaOverlay() {
	}

	static void render(final SpriteBatch batch) {
		final int gamma = GameOptions.get().getGamma();
		if (gamma == GameOptions.NEUTRAL_GAMMA) {
			return;
		}
		if ((white == null) || (whiteContext != Gdx.app)) {
			whiteContext = Gdx.app;
			final Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
			pixmap.setColor(Color.WHITE);
			pixmap.fill();
			white = new Texture(pixmap);
			pixmap.dispose();
		}
		final float amount = (gamma - GameOptions.NEUTRAL_GAMMA) / (float) GameOptions.NEUTRAL_GAMMA;
		final float shade;
		if (amount > 0) {
			shade = amount * MAX_BRIGHTEN;
			batch.setBlendFunction(GL20.GL_ONE_MINUS_DST_COLOR, GL20.GL_ONE);
		}
		else {
			shade = 1 + (amount * MAX_DARKEN);
			batch.setBlendFunction(GL20.GL_ZERO, GL20.GL_SRC_COLOR);
		}
		Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
		batch.setProjectionMatrix(PROJECTION);
		batch.setColor(shade, shade, shade, 1);
		batch.begin();
		batch.enableBlending();
		batch.draw(white, 0, 0, 1, 1);
		batch.end();
		batch.setColor(1, 1, 1, 1);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
	}
}
