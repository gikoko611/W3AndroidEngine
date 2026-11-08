package com.etheller.warsmash.desktop.launcher;

import java.nio.IntBuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.BufferUtils;
import com.etheller.warsmash.datasources.BorrowedDataSource;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.CanvasProvider;
import com.etheller.warsmash.viewer5.PathSolver;
import com.etheller.warsmash.viewer5.Scene;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxComplexInstance;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxHandler;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxModel;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxViewer;
import com.etheller.warsmash.viewer5.handlers.mdx.SequenceLoopMode;
import com.etheller.warsmash.viewer5.handlers.w3x.SequenceUtils;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;

/**
 * The game's cursor over the launcher, as the engine's menu draws it: the menu skin's cursor model playing its Normal
 * sequence at the mouse, in a scene where the window's height is the 0.6 high frame space of the menus. The system
 * arrow is hidden only while the model is drawn, so a game data without it keeps the arrow.
 */
final class LauncherCursor implements CanvasProvider {
	private static final float FRAME_SPACE_WIDTH = 0.8f;
	private static final float FRAME_SPACE_HEIGHT = 0.6f;

	private final DataSource data;
	private MdxViewer viewer;
	private Scene scene;
	private MdxComplexInstance instance;
	private boolean failed;
	private int vertexArray;
	private Cursor hiddenArrow;
	private boolean arrowHidden;

	/** @param data game data the launcher keeps open */
	LauncherCursor(final DataSource data) {
		this.data = new BorrowedDataSource(data);
	}

	@Override
	public float getWidth() {
		return Gdx.graphics.getWidth();
	}

	@Override
	public float getHeight() {
		return Gdx.graphics.getHeight();
	}

	/** Draws the cursor over what the window shows; mouse coordinates as the input has them (from the top). */
	void render(final int mouseX, final int mouseY) {
		if (this.failed || !load()) {
			return;
		}
		final int width = Math.max(1, Gdx.graphics.getWidth());
		final int height = Math.max(1, Gdx.graphics.getHeight());
		final float unitsPerPixel = Math.max(FRAME_SPACE_HEIGHT / height, FRAME_SPACE_WIDTH / width);
		this.scene.camera.viewport(new Rectangle(0, 0, width, height));
		this.scene.camera.ortho(0, width * unitsPerPixel, 0, height * unitsPerPixel, -1024f, 1024f);
		this.instance.setLocation(mouseX * unitsPerPixel, (height - mouseY) * unitsPerPixel, 0);

		Gdx.gl30.glBindVertexArray(this.vertexArray);
		Gdx.gl30.glEnable(GL30.GL_SCISSOR_TEST);
		// Not updateAndRender: its frame start clears the colour of what the launcher drew under the cursor.
		this.viewer.update();
		this.viewer.render();
		Gdx.gl30.glDisable(GL30.GL_SCISSOR_TEST);
		Gdx.gl30.glDisable(GL30.GL_CULL_FACE);
		this.viewer.webGL.useShaderProgram(null);
		Gdx.gl30.glBindVertexArray(0);
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
		Gdx.gl.glDepthMask(true);
		Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
		Gdx.gl.glViewport(0, 0, width, height);
		if (!this.arrowHidden) {
			hideArrow();
		}
	}

	private boolean load() {
		if (this.instance != null) {
			return true;
		}
		try {
			final String path = GameUI.loadSkin(this.data, WarsmashConstants.GAME_VERSION).getSkin()
					.getField("Cursor");
			final IntBuffer temp = BufferUtils.newIntBuffer(1);
			Gdx.gl30.glGenVertexArrays(1, temp);
			this.vertexArray = temp.get(0);
			this.viewer = new MdxViewer(this.data, this);
			this.viewer.addHandler(new MdxHandler());
			this.scene = this.viewer.addSimpleScene();
			this.scene.alpha = true;
			final MdxModel model = (path == null) || path.isEmpty() ? null
					: War3MapViewer.loadModelMdx(this.data, this.viewer, path, PathSolver.DEFAULT, null);
			if (model == null) {
				this.failed = true;
				return false;
			}
			this.instance = (MdxComplexInstance) model.addInstance(0);
			this.instance.setScene(this.scene);
			this.instance.setSequenceLoopMode(SequenceLoopMode.MODEL_LOOP);
			SequenceUtils.randomSequence(this.instance, "normal");
			return true;
		}
		catch (final RuntimeException e) {
			e.printStackTrace();
			this.failed = true;
			return false;
		}
	}

	private void hideArrow() {
		if (this.hiddenArrow == null) {
			final Pixmap empty = new Pixmap(32, 32, Format.RGBA8888);
			empty.setColor(0, 0, 0, 0);
			empty.fill();
			try {
				this.hiddenArrow = Gdx.graphics.newCursor(empty, 0, 0);
			}
			catch (final RuntimeException e) {
				// Without a native cursor of its own the arrow stays beside the drawn one.
				this.failed = true;
				return;
			}
			finally {
				empty.dispose();
			}
		}
		Gdx.graphics.setCursor(this.hiddenArrow);
		this.arrowHidden = true;
	}

	/** Gives the window its system arrow back, for a screen that draws no cursor of its own. */
	void showArrow() {
		if (this.arrowHidden) {
			Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
			this.arrowHidden = false;
		}
	}

	void dispose() {
		showArrow();
		if (this.hiddenArrow != null) {
			this.hiddenArrow.dispose();
			this.hiddenArrow = null;
		}
		if (this.viewer != null) {
			this.viewer.dispose();
			this.viewer = null;
		}
		if (this.vertexArray != 0) {
			final IntBuffer arrays = BufferUtils.newIntBuffer(1);
			arrays.put(0, this.vertexArray);
			Gdx.gl30.glDeleteVertexArrays(1, arrays);
			this.vertexArray = 0;
		}
		this.instance = null;
		this.scene = null;
	}
}
