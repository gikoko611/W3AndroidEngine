package com.etheller.warsmash.movie;

import java.nio.ByteBuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.etheller.warsmash.util.Log;

/**
 * The picture of a movie on the GPU: its three YUV planes as they were decoded, turned into colours by a shader as
 * they are drawn, so no thread converts or scales a picture.
 */
public final class MovieTexture {
	private static final String VERTEX_SHADER = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"
			+ "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
			+ "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
			+ "uniform mat4 u_projTrans;\n" //
			+ "varying vec4 v_color;\n" //
			+ "varying vec2 v_texCoords;\n" //
			+ "void main() {\n" //
			+ "  v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
			+ "  v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
			+ "  gl_Position = u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
			+ "}\n";
	private static final String FRAGMENT_SHADER = "#ifdef GL_ES\n" //
			+ "precision mediump float;\n" //
			+ "#endif\n" //
			+ "varying vec4 v_color;\n" //
			+ "varying vec2 v_texCoords;\n" //
			+ "uniform sampler2D u_texture;\n" //
			+ "uniform sampler2D u_textureU;\n" //
			+ "uniform sampler2D u_textureV;\n" //
			+ "uniform vec3 u_range;\n" // luma offset, luma scale, chroma scale
			+ "uniform vec4 u_matrix;\n" // red from V, green from U, green from V, blue from U
			+ "void main() {\n" //
			+ "  float y = (texture2D(u_texture, v_texCoords).r - u_range.x) * u_range.y;\n" //
			+ "  float u = (texture2D(u_textureU, v_texCoords).r - 128.0 / 255.0) * u_range.z;\n" //
			+ "  float v = (texture2D(u_textureV, v_texCoords).r - 128.0 / 255.0) * u_range.z;\n" //
			+ "  gl_FragColor = vec4(y + u_matrix.x * v, y - u_matrix.y * u - u_matrix.z * v, y + u_matrix.w * u, v_color.a);\n" //
			+ "}\n";

	private ShaderProgram shader;
	private boolean shaderFailed;
	private PlaneTexture planeY;
	private PlaneTexture planeU;
	private PlaneTexture planeV;
	private int width;
	private int height;
	private final float[] range = new float[3];
	private final float[] matrix = new float[4];

	/** Puts a picture on the GPU; on the render thread. */
	public void upload(final MoviePicture picture) {
		if (!compiled()) {
			return;
		}
		final int width = picture.getWidth();
		final int height = picture.getHeight();
		if ((this.planeY == null) || (this.planeY.getWidth() != width) || (this.planeY.getHeight() != height)) {
			disposePlanes();
			this.planeY = new PlaneTexture(width, height);
			this.planeU = new PlaneTexture(picture.getChromaWidth(), picture.getChromaHeight());
			this.planeV = new PlaneTexture(picture.getChromaWidth(), picture.getChromaHeight());
		}
		Gdx.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1);
		this.planeY.upload(picture.getY());
		this.planeU.upload(picture.getU());
		this.planeV.upload(picture.getV());
		Gdx.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 4);
		this.width = width;
		this.height = height;
		setColours(picture.isFullRange(), picture.isBt709());
	}

	private boolean compiled() {
		if ((this.shader == null) && !this.shaderFailed) {
			this.shader = new ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER);
			if (!this.shader.isCompiled()) {
				Log.warn("The movie shader does not compile: " + this.shader.getLog());
				this.shader.dispose();
				this.shader = null;
				this.shaderFailed = true;
			}
		}
		return this.shader != null;
	}

	/** Whether a picture was put on the GPU since the texture was made or {@link #forget forgotten}. */
	public boolean hasPicture() {
		return (this.width > 0) && (this.height > 0) && (this.shader != null);
	}

	/** Shows nothing until the next picture, keeping the textures for it. */
	public void forget() {
		this.width = 0;
		this.height = 0;
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	/** The YUV to RGB rules of the picture's colour standard. */
	private void setColours(final boolean fullRange, final boolean bt709) {
		final float kr = bt709 ? 0.2126f : 0.299f;
		final float kb = bt709 ? 0.0722f : 0.114f;
		final float kg = 1 - kr - kb;
		this.range[0] = fullRange ? 0 : 16f / 255f;
		this.range[1] = fullRange ? 1 : 255f / 219f;
		this.range[2] = fullRange ? 1 : 255f / 224f;
		this.matrix[0] = 2 * (1 - kr);
		this.matrix[1] = (2 * kb * (1 - kb)) / kg;
		this.matrix[2] = (2 * kr * (1 - kr)) / kg;
		this.matrix[3] = 2 * (1 - kb);
	}

	/**
	 * Draws a part of the picture with a batch that is drawing; the batch's shader is the same afterwards. The parts
	 * are of the picture's size, v from its top row.
	 */
	public void draw(final SpriteBatch batch, final float x, final float y, final float w, final float h,
			final float u, final float v, final float u2, final float v2) {
		if (!hasPicture()) {
			return;
		}
		final ShaderProgram previous = batch.getShader();
		batch.setShader(this.shader);
		this.planeU.bind(1);
		this.planeV.bind(2);
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
		this.shader.setUniformi("u_textureU", 1);
		this.shader.setUniformi("u_textureV", 2);
		this.shader.setUniform3fv("u_range", this.range, 0, 3);
		this.shader.setUniform4fv("u_matrix", this.matrix, 0, 4);
		batch.draw(this.planeY, x, y, w, h, u, v, u2, v2);
		// The quad is drawn with this shader and these planes when the batch changes shader again.
		batch.setShader(previous);
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
	}

	private void disposePlanes() {
		for (final PlaneTexture plane : new PlaneTexture[] { this.planeY, this.planeU, this.planeV }) {
			if (plane != null) {
				plane.dispose();
			}
		}
		this.planeY = null;
		this.planeU = null;
		this.planeV = null;
		forget();
	}

	public void dispose() {
		disposePlanes();
		if (this.shader != null) {
			this.shader.dispose();
			this.shader = null;
		}
		this.shaderFailed = false;
	}

	/** One plane of the picture: a texture of single bytes, rewritten for every picture. */
	private static final class PlaneTexture extends Texture {
		PlaneTexture(final int width, final int height) {
			super(new PlaneData(width, height));
			setFilter(TextureFilter.Linear, TextureFilter.Linear);
			setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
		}

		void upload(final ByteBuffer plane) {
			bind();
			Gdx.gl.glTexSubImage2D(GL20.GL_TEXTURE_2D, 0, 0, 0, getWidth(), getHeight(), PlaneData.format(),
					GL20.GL_UNSIGNED_BYTE, plane);
		}
	}

	private static final class PlaneData implements TextureData {
		private final int width;
		private final int height;

		PlaneData(final int width, final int height) {
			this.width = width;
			this.height = height;
		}

		/** One red channel where the context has it, luminance on OpenGL ES 2. */
		static int format() {
			return Gdx.gl30 != null ? GL30.GL_RED : GL20.GL_LUMINANCE;
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
			throw new GdxRuntimeException("A movie plane has no pixmap");
		}

		@Override
		public boolean disposePixmap() {
			return false;
		}

		@Override
		public void consumeCustomData(final int target) {
			Gdx.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1);
			Gdx.gl.glTexImage2D(target, 0, Gdx.gl30 != null ? GL30.GL_R8 : GL20.GL_LUMINANCE, this.width,
					this.height, 0, format(), GL20.GL_UNSIGNED_BYTE, null);
			Gdx.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 4);
		}

		@Override
		public int getWidth() {
			return this.width;
		}

		@Override
		public int getHeight() {
			return this.height;
		}

		@Override
		public Pixmap.Format getFormat() {
			return Pixmap.Format.Alpha;
		}

		@Override
		public boolean useMipMaps() {
			return false;
		}

		@Override
		public boolean isManaged() {
			return false;
		}
	}
}
