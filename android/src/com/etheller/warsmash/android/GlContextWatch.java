package com.etheller.warsmash.android;

import java.io.File;
import java.nio.IntBuffer;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.View;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.android.AndroidGL30;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntSet;

/**
 * libGDX asks Android to keep the GL context while the app is in the background, but Android may still drop it. The
 * libGDX objects that can reload themselves then do; everything else the engine made is gone. The watched listener is
 * told on the rendering thread, before it draws or sizes anything in the new context.
 */
final class GlContextWatch implements ApplicationListener {
	private static final String TAG = "Warsmash";
	/** In a debuggable build, this file in the app's files folder makes every pause drop the context (tests). */
	private static final String DROP_CONTEXT_FILE = "drop-gl-context";

	private final ApplicationListener listener;
	private final Runnable lost;
	private final LossMarker marker = new LossMarker(this);
	private Texture markerTexture;
	private ContextGL30 gl;

	private GlContextWatch(final ApplicationListener listener, final Runnable lost) {
		this.listener = listener;
		this.lost = lost;
	}

	/** @param lost runs on the rendering thread once the context was lost and a new one made */
	static ApplicationListener watch(final ApplicationListener listener, final Runnable lost) {
		return new GlContextWatch(listener, lost);
	}

	/** Test builds can lose the context on purpose, to try what Android does rarely; called after the view exists. */
	static void dropContextOnPauseIfAsked(final Context context, final View view) {
		if (((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0)
				&& new File(context.getFilesDir(), DROP_CONTEXT_FILE).isFile() && (view instanceof GLSurfaceView)) {
			Log.w(TAG, "Test mode: the GL context is dropped at every pause");
			((GLSurfaceView) view).setPreserveEGLContextOnPause(false);
		}
	}

	@Override
	public void create() {
		final Graphics graphics = Gdx.graphics;
		if (graphics.getGL30() != null) {
			this.gl = new ContextGL30();
			graphics.setGL30(this.gl);
		}
		this.markerTexture = new Texture(this.marker);
		this.listener.create();
	}

	/** On the rendering thread, while libGDX reloads its managed textures into a new context, the marker first. */
	private void reloading() {
		if (this.gl != null) {
			this.gl.reloaded = new IntSet();
			this.gl.reloaded.add(this.markerTexture.getTextureObjectHandle());
		}
	}

	private void checkLost() {
		if (this.marker.lost) {
			this.marker.lost = false;
			Log.w(TAG, "The GL context was lost; the screens are made again");
			if (this.gl != null) {
				final ContextGL30 lostGl = this.gl;
				lostGl.lost = true;
				this.gl = new ContextGL30();
				if (lostGl.reloaded != null) {
					this.gl.textures.addAll(lostGl.reloaded);
				}
				Gdx.graphics.setGL30(this.gl);
			}
			this.lost.run();
		}
	}

	@Override
	public void resize(final int width, final int height) {
		checkLost();
		this.listener.resize(width, height);
	}

	@Override
	public void render() {
		checkLost();
		this.listener.render();
	}

	@Override
	public void pause() {
		this.listener.pause();
	}

	@Override
	public void resume() {
		checkLost();
		this.listener.resume();
	}

	@Override
	public void dispose() {
		this.listener.dispose();
		if (this.markerTexture != null) {
			this.markerTexture.dispose();
			this.markerTexture = null;
		}
	}

	/** A managed texture of one pixel: libGDX makes it again in every new context, which is how a loss shows. */
	private static final class LossMarker implements TextureData {
		private final GlContextWatch watch;
		private boolean made;
		private boolean lost;

		LossMarker(final GlContextWatch watch) {
			this.watch = watch;
		}

		@Override
		public TextureDataType getType() {
			return TextureDataType.Custom;
		}

		@Override
		public boolean isPrepared() {
			return true;
		}

		@Override
		public void prepare() {
		}

		@Override
		public Pixmap consumePixmap() {
			throw new GdxRuntimeException("Custom texture data");
		}

		@Override
		public boolean disposePixmap() {
			return false;
		}

		@Override
		public void consumeCustomData(final int target) {
			if (this.made) {
				this.lost = true;
				this.watch.reloading();
			}
			this.made = true;
			Gdx.gl.glTexImage2D(target, 0, GL20.GL_RGBA, 1, 1, 0, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE,
					BufferUtils.newByteBuffer(4));
		}

		@Override
		public int getWidth() {
			return 1;
		}

		@Override
		public int getHeight() {
			return 1;
		}

		@Override
		public Pixmap.Format getFormat() {
			return Pixmap.Format.RGBA8888;
		}

		@Override
		public boolean useMipMaps() {
			return false;
		}

		@Override
		public boolean isManaged() {
			return true;
		}
	}

	/**
	 * The GL of one context. What the engine made with GL calls keeps the GL it was made with; once that context is
	 * lost, deletes through it leave the names alone, as the new context may have given them to other objects. The
	 * libGDX objects that reloaded themselves into the new context delete through the new GL.
	 */
	private static final class ContextGL30 extends AndroidGL30 {
		private volatile boolean lost;
		/**
		 * The texture names this context gave out and still holds. libGDX textures that do not reload themselves are
		 * disposed through the new GL with the names of the lost context, which must not delete what the new context
		 * gave out under the same names.
		 */
		private final IntSet textures = new IntSet();
		/** The texture names libGDX reloaded into a new context through this GL, from the loss on. */
		private IntSet reloaded;

		private boolean quiet() {
			return this.lost;
		}

		private void made(final int texture) {
			(this.reloaded != null ? this.reloaded : this.textures).add(texture);
		}

		@Override
		public int glGenTexture() {
			final int texture = super.glGenTexture();
			made(texture);
			return texture;
		}

		@Override
		public void glGenTextures(final int n, final IntBuffer textures) {
			super.glGenTextures(n, textures);
			for (int i = 0; i < n; i++) {
				made(textures.get(textures.position() + i));
			}
		}

		@Override
		public void glDeleteBuffers(final int n, final IntBuffer buffers) {
			if (!quiet()) {
				super.glDeleteBuffers(n, buffers);
			}
		}

		@Override
		public void glDeleteBuffer(final int buffer) {
			if (!quiet()) {
				super.glDeleteBuffer(buffer);
			}
		}

		@Override
		public void glDeleteFramebuffers(final int n, final IntBuffer framebuffers) {
			if (!quiet()) {
				super.glDeleteFramebuffers(n, framebuffers);
			}
		}

		@Override
		public void glDeleteFramebuffer(final int framebuffer) {
			if (!quiet()) {
				super.glDeleteFramebuffer(framebuffer);
			}
		}

		@Override
		public void glDeleteProgram(final int program) {
			if (!quiet()) {
				super.glDeleteProgram(program);
			}
		}

		@Override
		public void glDeleteRenderbuffers(final int n, final IntBuffer renderbuffers) {
			if (!quiet()) {
				super.glDeleteRenderbuffers(n, renderbuffers);
			}
		}

		@Override
		public void glDeleteRenderbuffer(final int renderbuffer) {
			if (!quiet()) {
				super.glDeleteRenderbuffer(renderbuffer);
			}
		}

		@Override
		public void glDeleteShader(final int shader) {
			if (!quiet()) {
				super.glDeleteShader(shader);
			}
		}

		@Override
		public void glDeleteTextures(final int n, final IntBuffer textures) {
			for (int i = 0; i < n; i++) {
				glDeleteTexture(textures.get(textures.position() + i));
			}
		}

		@Override
		public void glDeleteTexture(final int texture) {
			if (!quiet() && this.textures.remove(texture)) {
				super.glDeleteTexture(texture);
			}
		}

		@Override
		public void glDeleteQueries(final int n, final int[] ids, final int offset) {
			if (!quiet()) {
				super.glDeleteQueries(n, ids, offset);
			}
		}

		@Override
		public void glDeleteQueries(final int n, final IntBuffer ids) {
			if (!quiet()) {
				super.glDeleteQueries(n, ids);
			}
		}

		@Override
		public void glDeleteVertexArrays(final int n, final int[] arrays, final int offset) {
			if (!quiet()) {
				super.glDeleteVertexArrays(n, arrays, offset);
			}
		}

		@Override
		public void glDeleteVertexArrays(final int n, final IntBuffer arrays) {
			if (!quiet()) {
				super.glDeleteVertexArrays(n, arrays);
			}
		}

		@Override
		public void glDeleteSamplers(final int count, final int[] samplers, final int offset) {
			if (!quiet()) {
				super.glDeleteSamplers(count, samplers, offset);
			}
		}

		@Override
		public void glDeleteSamplers(final int count, final IntBuffer samplers) {
			if (!quiet()) {
				super.glDeleteSamplers(count, samplers);
			}
		}

		@Override
		public void glDeleteTransformFeedbacks(final int n, final int[] ids, final int offset) {
			if (!quiet()) {
				super.glDeleteTransformFeedbacks(n, ids, offset);
			}
		}

		@Override
		public void glDeleteTransformFeedbacks(final int n, final IntBuffer ids) {
			if (!quiet()) {
				super.glDeleteTransformFeedbacks(n, ids);
			}
		}
	}
}
