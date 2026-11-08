package com.etheller.warsmash;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;
import com.etheller.warsmash.datasources.BorrowedDataSource;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.units.Element;
import com.etheller.warsmash.util.GraphicsContext;
import com.etheller.warsmash.util.StringBundle;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.CanvasProvider;
import com.etheller.warsmash.viewer5.FogSettings;
import com.etheller.warsmash.viewer5.PathSolver;
import com.etheller.warsmash.viewer5.Scene;
import com.etheller.warsmash.viewer5.handlers.mdx.Camera;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxComplexInstance;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxHandler;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxModel;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxViewer;
import com.etheller.warsmash.viewer5.handlers.mdx.SequenceLoopMode;
import com.etheller.warsmash.viewer5.handlers.w3x.SequenceUtils;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens.PrimaryTag;

/**
 * The 3D glue scenes of the menus (the main menu backgrounds and the campaign backdrops) played on their own, the way
 * the engine's menu plays them behind its frames: the model's own camera, its fog, birth and then stand. A launcher
 * draws them behind its pages, a page scene and a smaller one beside it. The game data is opened on a worker thread;
 * a scene shows once its model is loaded and drawn.
 */
public class GlueBackdropRenderer implements ApplicationListener, CanvasProvider {
	public static final int LAYERS = 2;
	/** Frames drawn before a scene counts as shown, so that its textures are on screen. */
	private static final int SHOWN_AFTER_FRAMES = 3;

	public interface Listener {
		/** On the rendering thread: the scene of the request is drawn now. */
		void shown(int layer, long request);

		/** On the rendering thread: the scene of the request cannot be drawn (no model, unreadable data). */
		void failed(int layer, long request);
	}

	/** What one layer shows: a skin field naming the model, its fog, and where it draws (screen pixels from the bottom). */
	public static final class Request {
		final long id;
		final String skinField;
		final String fallbackField;
		final FogSettings fog;
		final int menuFogVersion;
		final Rectangle viewport;
		final Rectangle clip;

		/**
		 * @param fog the scene's fog, or null for the menu fog of the given game version
		 * @param viewport the whole view of the scene, which may reach past the clip to crop it
		 */
		public Request(final long id, final String skinField, final String fallbackField, final FogSettings fog,
				final int menuFogVersion, final Rectangle viewport, final Rectangle clip) {
			this.id = id;
			this.skinField = skinField;
			this.fallbackField = fallbackField;
			this.fog = fog;
			this.menuFogVersion = menuFogVersion;
			this.viewport = viewport;
			this.clip = clip;
		}

		public long id() {
			return this.id;
		}

		boolean sameScene(final Request other) {
			return (other != null) && this.skinField.equals(other.skinField)
					&& String.valueOf(this.fallbackField).equals(String.valueOf(other.fallbackField))
					&& (this.fog == other.fog) && (this.menuFogVersion == other.menuFogVersion);
		}
	}

	private static final class Layer {
		Request request;
		Scene scene;
		MdxModel model;
		MdxComplexInstance instance;
		Camera camera;
		boolean playedStand;
		int frames;
		boolean failed;
	}

	private final DataTable warsmashIni;
	private final Listener listener;
	private final Request[] pending = new Request[LAYERS];
	private final boolean[] changed = new boolean[LAYERS];
	private final Layer[] layers = new Layer[LAYERS];
	private final float[] positionTemp = new float[3];
	private final float[] targetTemp = new float[3];
	private final Vector3 position = new Vector3();
	private final Vector3 target = new Vector3();
	private final Vector3 worldUp = new Vector3(0, 0, 1);
	private final DataSource gameData;
	private volatile DataSource dataSource;
	private volatile Element skin;
	private volatile Element menuFog;
	private volatile boolean dataFailed;
	private boolean dataRequested;
	private MdxViewer viewer;
	private int vertexArray;
	/** The GL the vertex array was made with, which leaves deletes alone once its context is lost. */
	private GL30 vertexArrayGl;

	public GlueBackdropRenderer(final DataTable warsmashIni, final Listener listener) {
		this(warsmashIni, listener, null);
	}

	/** @param gameData game data the caller keeps open, read instead of opening it again; null opens its own */
	public GlueBackdropRenderer(final DataTable warsmashIni, final Listener listener, final DataSource gameData) {
		this.warsmashIni = warsmashIni;
		this.listener = listener;
		this.gameData = gameData;
		for (int i = 0; i < LAYERS; i++) {
			this.layers[i] = new Layer();
		}
	}

	/** From any thread: what a layer shows next; null clears it. */
	public void show(final int layer, final Request request) {
		synchronized (this.pending) {
			this.pending[layer] = request;
			this.changed[layer] = true;
		}
		if (Gdx.graphics != null) {
			Gdx.graphics.setContinuousRendering(true);
			Gdx.graphics.requestRendering();
		}
	}

	@Override
	public float getWidth() {
		return Gdx.graphics.getWidth();
	}

	@Override
	public float getHeight() {
		return Gdx.graphics.getHeight();
	}

	@Override
	public void create() {
		final ByteBuffer buffer = ByteBuffer.allocateDirect(4);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		final IntBuffer temp = buffer.asIntBuffer();
		this.vertexArrayGl = Gdx.gl30;
		this.vertexArrayGl.glGenVertexArrays(1, temp);
		this.vertexArray = temp.get(0);
		Gdx.gl30.glClearColor(0, 0, 0, 1);
	}

	/** The game data is opened once, the first time a scene is asked for, away from the rendering thread. */
	private void openData() {
		this.dataRequested = true;
		final Thread thread = new Thread(() -> {
			try {
				WarsmashConstants.loadConstants(this.warsmashIni.get("Emulator"), this.warsmashIni);
				final DataSource data = this.gameData != null ? new BorrowedDataSource(this.gameData)
						: WarsmashGdxMapScreen.parseDataSources(this.warsmashIni);
				this.skin = GameUI.loadSkin(data, WarsmashConstants.GAME_VERSION).getSkin();
				final DataTable miscData = new DataTable(StringBundle.EMPTY);
				try (InputStream stream = data.getResourceAsStream("UI\\MiscData.txt")) {
					miscData.readTXT(stream, true);
				}
				this.menuFog = miscData.get("MenuZFog");
				this.dataSource = data;
			}
			catch (final Exception | LinkageError e) {
				e.printStackTrace();
				this.dataFailed = true;
			}
		}, "GlueBackdropData");
		thread.setDaemon(true);
		thread.start();
	}

	@Override
	public void render() {
		takePending();
		boolean any = false;
		for (final Layer layer : this.layers) {
			any |= layer.request != null;
		}
		if (!any) {
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			Gdx.graphics.setContinuousRendering(false);
			return;
		}
		if (!this.dataRequested) {
			openData();
		}
		if (this.dataFailed) {
			for (int i = 0; i < LAYERS; i++) {
				fail(i);
			}
		}
		if (this.dataSource == null) {
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			return;
		}
		if (this.viewer == null) {
			this.viewer = new MdxViewer(this.dataSource, this);
			this.viewer.addHandler(new MdxHandler());
			for (int i = 0; i < LAYERS; i++) {
				this.layers[i].scene = this.viewer.addSimpleScene();
				if (this.layers[i].instance == null) {
					this.layers[i].scene.camera.viewport(new Rectangle(0, 0, 0, 0));
				}
			}
		}
		for (int i = 0; i < LAYERS; i++) {
			prepare(i);
		}
		Gdx.gl30.glBindVertexArray(this.vertexArray);
		Gdx.gl30.glEnable(GL30.GL_SCISSOR_TEST);
		for (final Layer layer : this.layers) {
			if (layer.instance != null) {
				advance(layer);
				aim(layer);
			}
		}
		this.viewer.updateAndRender();
		Gdx.gl30.glDisable(GL30.GL_SCISSOR_TEST);
		Gdx.gl30.glDisable(GL30.GL_CULL_FACE);
		this.viewer.webGL.useShaderProgram(null);
		for (int i = 0; i < LAYERS; i++) {
			final Layer layer = this.layers[i];
			if ((layer.instance != null) && (layer.frames < SHOWN_AFTER_FRAMES)) {
				layer.frames++;
				if (layer.frames == SHOWN_AFTER_FRAMES) {
					this.listener.shown(i, layer.request.id);
				}
			}
		}
	}

	private void takePending() {
		synchronized (this.pending) {
			for (int i = 0; i < LAYERS; i++) {
				if (!this.changed[i]) {
					continue;
				}
				this.changed[i] = false;
				final Layer layer = this.layers[i];
				final Request next = this.pending[i];
				if ((next != null) && next.sameScene(layer.request) && !layer.failed) {
					// The same scene in another place keeps playing where it is.
					final boolean drawn = layer.frames >= SHOWN_AFTER_FRAMES;
					layer.request = next;
					place(layer);
					if (drawn) {
						this.listener.shown(i, next.id);
					}
					continue;
				}
				clear(layer);
				layer.request = next;
			}
		}
	}

	private void clear(final Layer layer) {
		if (layer.instance != null) {
			layer.instance.detach();
		}
		layer.instance = null;
		layer.model = null;
		layer.camera = null;
		layer.frames = 0;
		layer.failed = false;
		if (layer.scene != null) {
			layer.scene.camera.viewport(new Rectangle(0, 0, 0, 0));
			layer.scene.clip = null;
		}
	}

	private void fail(final int index) {
		final Layer layer = this.layers[index];
		if ((layer.request != null) && !layer.failed) {
			layer.failed = true;
			this.listener.failed(index, layer.request.id);
		}
	}

	private void place(final Layer layer) {
		if (layer.scene != null) {
			layer.scene.camera.viewport(layer.request.viewport);
			layer.scene.clip = layer.request.clip;
		}
	}

	/** Loads the layer's model when it has none yet; as the menu does, birth plays first. */
	private void prepare(final int index) {
		final Layer layer = this.layers[index];
		if ((layer.request == null) || (layer.instance != null) || layer.failed) {
			return;
		}
		try {
			String path = GameUI.trySkinField(this.skin, layer.request.skinField);
			if (((path == null) || path.equals(layer.request.skinField)) && (layer.request.fallbackField != null)) {
				path = GameUI.trySkinField(this.skin, layer.request.fallbackField);
			}
			final MdxModel model = War3MapViewer.loadModelMdx(this.dataSource, this.viewer, path, PathSolver.DEFAULT,
					null);
			if ((model == null) || model.cameras.isEmpty()) {
				fail(index);
				return;
			}
			final MdxComplexInstance instance = (MdxComplexInstance) model.addInstance(0);
			instance.setScene(layer.scene);
			int birth = 0;
			for (int i = 0; i < model.getSequences().size(); i++) {
				if (model.getSequences().get(i).getName().toLowerCase().startsWith("birth")) {
					birth = i;
					break;
				}
			}
			instance.setSequence(birth);
			instance.setSequenceLoopMode(SequenceLoopMode.NEVER_LOOP);
			layer.model = model;
			layer.instance = instance;
			layer.camera = model.cameras.get(0);
			layer.playedStand = false;
			layer.frames = 0;
			final FogSettings fog = layer.request.fog != null ? layer.request.fog
					: this.menuFog != null ? FogSettings.parse(this.menuFog, layer.request.menuFogVersion) : null;
			if (fog != null) {
				layer.scene.fogSettings.style = fog.style;
				layer.scene.fogSettings.color = fog.color;
				layer.scene.fogSettings.density = fog.density;
				layer.scene.fogSettings.start = fog.start;
				layer.scene.fogSettings.end = fog.end;
			}
			place(layer);
		}
		catch (final RuntimeException e) {
			e.printStackTrace();
			fail(index);
		}
	}

	private static void advance(final Layer layer) {
		final MdxComplexInstance instance = layer.instance;
		if (instance.sequenceEnded && (((layer.model.getSequences().get(instance.sequence).getFlags() & 0x1) == 0)
				|| !layer.playedStand)) {
			SequenceUtils.randomSequence(instance, PrimaryTag.STAND, SequenceUtils.EMPTY, true);
			layer.playedStand = true;
		}
	}

	/** The model's camera at the current frame, as the menu screen aims its own. */
	private void aim(final Layer layer) {
		final MdxComplexInstance instance = layer.instance;
		final Camera camera = layer.camera;
		camera.getPositionTranslation(this.positionTemp, instance.sequence, instance.frame, instance.counter);
		camera.getTargetTranslation(this.targetTemp, instance.sequence, instance.frame, instance.counter);
		this.position.set(camera.position).add(this.positionTemp[0], this.positionTemp[1], this.positionTemp[2]);
		this.target.set(camera.targetPosition).add(this.targetTemp[0], this.targetTemp[1], this.targetTemp[2]);
		final com.etheller.warsmash.viewer5.Camera view = layer.scene.camera;
		view.perspective(camera.fieldOfView * 0.6f, view.rect.width / Math.max(1, view.rect.height),
				camera.nearClippingPlane, camera.farClippingPlane);
		view.moveToAndFace(this.position, this.target, this.worldUp);
	}

	/**
	 * On the rendering thread, after the GL context was lost: the scenes are loaded and drawn again from their
	 * requests.
	 */
	public void contextLost() {
		final MdxViewer lost = this.viewer;
		this.viewer = null;
		if (lost != null) {
			GraphicsContext.disposeLost(lost::dispose);
		}
		for (final Layer layer : this.layers) {
			layer.scene = null;
			layer.instance = null;
			layer.model = null;
			layer.camera = null;
			layer.frames = 0;
			layer.failed = false;
		}
		create();
		Gdx.graphics.setContinuousRendering(true);
		Gdx.graphics.requestRendering();
	}

	@Override
	public void resize(final int width, final int height) {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void dispose() {
		if (this.viewer != null) {
			this.viewer.dispose();
			this.viewer = null;
		}
		if (this.vertexArray != 0) {
			final IntBuffer arrays = BufferUtils.newIntBuffer(1);
			arrays.put(0, this.vertexArray);
			this.vertexArrayGl.glDeleteVertexArrays(1, arrays);
			this.vertexArray = 0;
		}
		final DataSource data = this.dataSource;
		this.dataSource = null;
		if (data != null) {
			try {
				data.close();
			}
			catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}
}
