package com.etheller.warsmash.util;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;

import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.etheller.warsmash.util.SessionResources;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.viewer5.handlers.blp.BlpDecoder;
import com.etheller.warsmash.viewer5.handlers.blp.DdsDecoder;
import com.etheller.warsmash.viewer5.handlers.tga.TgaFile;

/**
 * Uses AWT stuff
 *
 */
public final class ImageUtils {
	private static final int BYTES_PER_PIXEL = 4;
	/**
	 * False on platforms without javax.imageio (Android), where formats that only
	 * the ImageIO plugins decode are skipped instead of failing.
	 */
	public static final boolean IMAGE_IO_AVAILABLE = isClassPresent("javax.imageio.ImageIO");
	public static final String DEFAULT_ICON_PATH = "ReplaceableTextures\\CommandButtons\\BTNTemp.blp";

	public static Texture getAnyExtensionTexture(final DataSource dataSource, final String path) {
		BufferedImage image;
		try {
			final DdsDecoder ddsImage = findDdsInsteadOfBlp(dataSource, path);
			if (ddsImage != null) {
				return getTexture(ddsImage.getWidth(), ddsImage.getHeight(), ddsImage.getRgba());
			}
			final BlpDecoder blpImage = findDecodableBlp(dataSource, path);
			if (blpImage != null) {
				// Raw palette colors in a plain texture match the ImageIO path, which reads
				// them as linear and then uploads with an sRGB format.
				return getTexture(blpImage.getWidth(), blpImage.getHeight(), blpImage.getRgba());
			}
			if (!IMAGE_IO_AVAILABLE && path.toLowerCase().endsWith(".blp") && dataSource.has(path)) {
				System.err.println("No decoder for " + path + " on this platform");
				return null;
			}
			final AnyExtensionImage imageInfo = getAnyExtensionImageFixRGB(dataSource, path, "texture");
			image = imageInfo.getImageData();
			if (image != null) {
				return ImageUtils.getTexture(image, imageInfo.isNeedsSRGBFix());
			}
		}
		catch (final IOException e) {
			return null;
		}
		return null;
	}

	public static AnyExtensionImage getAnyExtensionImageFixRGB(final DataSource dataSource, final String path,
			final String errorType) throws IOException {
		if (path.toLowerCase().endsWith(".blp")) {
			try (InputStream stream = dataSource.getResourceAsStream(path)) {
				if (stream == null) {
					final String tgaPath = path.substring(0, path.length() - 4) + ".tga";
					try (final InputStream tgaStream = dataSource.getResourceAsStream(tgaPath)) {
						if (tgaStream != null) {
							final BufferedImage tgaData = TgaFile.readTGA(tgaPath, tgaStream);
							return new AnyExtensionImage(false, tgaData);
						}
						else {
							final String ddsPath = path.substring(0, path.length() - 4) + ".dds";
							try (final InputStream ddsStream = dataSource.getResourceAsStream(ddsPath)) {
								if (ddsStream != null) {
									final BufferedImage image = ImageIO.read(ddsStream);
									return new AnyExtensionImage(false, image);
								}
								else {
									throw new IllegalStateException("Missing " + errorType + ": " + path);
								}
							}
						}
					}
				}
				else {
					final BufferedImage image = ImageIO.read(stream);
					return new AnyExtensionImage(true, image);
				}
			}
		}
		else {
			throw new IllegalStateException("Missing " + errorType + ": " + path);
		}
	}

	/**
	 * Follows the lookup order of getAnyExtensionImageFixRGB (BLP, then TGA, then
	 * DDS) and decodes the DDS without java.awt when that is what the path resolves
	 * to.
	 */
	private static DdsDecoder findDdsInsteadOfBlp(final DataSource dataSource, final String path)
			throws IOException {
		return findDdsInsteadOfBlp(dataSource, path, 0);
	}

	private static DdsDecoder findDdsInsteadOfBlp(final DataSource dataSource, final String path, final int maxSize)
			throws IOException {
		if (!path.toLowerCase().endsWith(".blp")) {
			return null;
		}
		final String basePath = path.substring(0, path.length() - 4);
		final String ddsPath = basePath + ".dds";
		// In one data source the order above holds; a later source (an override folder) wins with any of the three.
		final String found = dataSource.findFirst(path, basePath + ".tga", ddsPath);
		if ((found != null) && !found.equals(ddsPath)) {
			return null;
		}
		final ByteBuffer ddsData = readFully(dataSource, ddsPath);
		return ddsData == null ? null : DdsDecoder.decode(ddsData, maxSize, false);
	}

	/**
	 * Reads through getResourceAsStream, which unlike read also sees the files inside
	 * a map archive.
	 */
	private static ByteBuffer readFully(final DataSource dataSource, final String path) throws IOException {
		try (InputStream stream = dataSource.getResourceAsStream(path)) {
			return stream == null ? null : ByteBuffer.wrap(IOUtils.toByteArray(stream));
		}
	}

	private static boolean isClassPresent(final String className) {
		try {
			Class.forName(className, false, ImageUtils.class.getClassLoader());
			return true;
		}
		catch (final ClassNotFoundException e) {
			return false;
		}
	}

	private static BlpDecoder findDecodableBlp(final DataSource dataSource, final String path) throws IOException {
		return findDecodableBlp(dataSource, path, 0);
	}

	private static BlpDecoder findDecodableBlp(final DataSource dataSource, final String path, final int maxSize)
			throws IOException {
		if (!path.toLowerCase().endsWith(".blp")) {
			return null;
		}
		final ByteBuffer blpData = readFully(dataSource, path);
		if (blpData == null) {
			return null;
		}
		return BlpDecoder.isBlp(blpData) ? BlpDecoder.decode(blpData, maxSize, false) : null;
	}

	/**
	 * @param rgba width * height RGBA pixels, rows from the top
	 */
	public static Texture getTexture(final int width, final int height, final ByteBuffer rgba) {
		final Pixmap pixmap = new Pixmap(width, height, Format.RGBA8888);
		final ByteBuffer pixels = pixmap.getPixels();
		pixels.position(0);
		pixels.put(rgba.duplicate());
		pixels.position(0);
		// An unmanaged texture has its pixels on the GPU once constructed; only the native copy would stay behind.
		final Texture texture = SessionResources.track(new Texture(pixmap));
		pixmap.dispose();
		texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		return texture;
	}

	/**
	 * Like getAnyExtensionImageFixRGB, but returns RGBA bytes and decodes DDS and BLP without
	 * java.awt.
	 *
	 * @param sRGBCorrect whether to apply the sRGB correction of
	 *                    AnyExtensionImage.getRGBCorrectImageData
	 */
	public static RgbaImage getAnyExtensionRgbaImage(final DataSource dataSource, final String path,
			final String errorType, final boolean sRGBCorrect) throws IOException {
		return getAnyExtensionRgbaImage(dataSource, path, errorType, sRGBCorrect, 0);
	}

	/**
	 * @param maxSize the largest width and height, 0 for no limit: a BLP or DDS larger than that is read from its first
	 *                stored mip level within it
	 */
	public static RgbaImage getAnyExtensionRgbaImage(final DataSource dataSource, final String path,
			final String errorType, final boolean sRGBCorrect, final int maxSize) throws IOException {
		final DdsDecoder ddsImage = findDdsInsteadOfBlp(dataSource, path, maxSize);
		if (ddsImage != null) {
			return new RgbaImage(ddsImage.getWidth(), ddsImage.getHeight(), ddsImage.getRgba(), false);
		}
		final BlpDecoder blpImage = findDecodableBlp(dataSource, path, maxSize);
		if (blpImage != null) {
			// The raw colors in a plain RGBA texture match both ImageIO variants: the
			// sRGB corrected one keeps them, the other is uploaded with an sRGB format.
			return new RgbaImage(blpImage.getWidth(), blpImage.getHeight(), blpImage.getRgba(), false);
		}
		final AnyExtensionImage imageInfo = getAnyExtensionImageFixRGB(dataSource, path, errorType);
		final BufferedImage image = sRGBCorrect ? imageInfo.getRGBCorrectImageData() : imageInfo.getImageData();
		return new RgbaImage(image.getWidth(), image.getHeight(), (ByteBuffer) getTextureBuffer(image),
				imageInfo.isNeedsSRGBFix());
	}

	public static final class RgbaImage {
		private final int width;
		private final int height;
		private final ByteBuffer pixels;
		private final boolean needsSRGBFix;

		public RgbaImage(final int width, final int height, final ByteBuffer pixels, final boolean needsSRGBFix) {
			this.width = width;
			this.height = height;
			this.pixels = pixels;
			this.needsSRGBFix = needsSRGBFix;
		}

		public int getWidth() {
			return this.width;
		}

		public int getHeight() {
			return this.height;
		}

		/**
		 * @return width * height RGBA pixels, rows from the top
		 */
		public ByteBuffer getPixels() {
			return this.pixels;
		}

		public boolean isNeedsSRGBFix() {
			return this.needsSRGBFix;
		}
	}

	public static final class AnyExtensionImage {
		private final boolean needsSRGBFix;
		private final BufferedImage imageData;

		public AnyExtensionImage(final boolean needsSRGBFix, final BufferedImage imageData) {
			this.needsSRGBFix = needsSRGBFix;
			this.imageData = imageData;
		}

		public BufferedImage getImageData() {
			return this.imageData;
		}

		public BufferedImage getRGBCorrectImageData() {
			return this.needsSRGBFix ? forceBufferedImagesRGB(this.imageData) : this.imageData;
		}

		public boolean isNeedsSRGBFix() {
			return this.needsSRGBFix;
		}
	}

	public static Texture getTexture(final BufferedImage image, final boolean sRGBFix) {
		final int[] pixels = new int[image.getWidth() * image.getHeight()];
		image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

		// 4
		// for
		// RGBA,
		// 3
		// for
		// RGB

		final Pixmap pixmap = sRGBFix ? new Pixmap(image.getWidth(), image.getHeight(), Format.RGBA8888) {
			@Override
			public int getGLInternalFormat() {
				return GL30.GL_SRGB8_ALPHA8;
			}
		} : new Pixmap(image.getWidth(), image.getHeight(), Format.RGBA8888);
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				final int pixel = pixels[(y * image.getWidth()) + x];
				pixmap.drawPixel(x, y, (pixel << 8) | (pixel >>> 24));
			}
		}
		// An unmanaged texture has its pixels on the GPU once constructed; only the native copy would stay behind.
		final Texture texture = SessionResources.track(new Texture(pixmap));
		pixmap.dispose();
		texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		return texture;
	}

	public static Texture getTextureNoColorCorrection(final BufferedImage image) {
		final int[] pixels = new int[image.getWidth() * image.getHeight()];
		image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

		// 4
		// for
		// RGBA,
		// 3
		// for
		// RGB

		final Pixmap pixmap = new Pixmap(image.getWidth(), image.getHeight(), Format.RGBA8888);
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				final int pixel = pixels[(y * image.getWidth()) + x];
				pixmap.drawPixel(x, y, (pixel << 8) | (pixel >>> 24));
			}
		}
		// An unmanaged texture has its pixels on the GPU once constructed; only the native copy would stay behind.
		final Texture texture = SessionResources.track(new Texture(pixmap));
		pixmap.dispose();
		texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		return texture;
	}

	/**
	 * Reads a data image such as a pathing map. A BLP gives its stored values, as a TGA does; other formats need
	 * javax.imageio.
	 */
	public static ArgbImage getArgbImage(final InputStream stream, final String path) throws IOException {
		if (stream == null) {
			throw new IllegalStateException("Missing image: " + path);
		}
		final ByteBuffer data = ByteBuffer.wrap(IOUtils.toByteArray(stream));
		if (BlpDecoder.isBlp(data)) {
			final BlpDecoder blp = BlpDecoder.decode(data);
			final ByteBuffer rgba = blp.getRgba();
			final int[] pixels = new int[blp.getWidth() * blp.getHeight()];
			for (int i = 0; i < pixels.length; i++) {
				final int offset = i * 4;
				pixels[i] = ((rgba.get(offset + 3) & 0xFF) << 24) | ((rgba.get(offset) & 0xFF) << 16)
						| ((rgba.get(offset + 1) & 0xFF) << 8) | (rgba.get(offset + 2) & 0xFF);
			}
			return new ArgbImage(blp.getWidth(), blp.getHeight(), pixels);
		}
		if (!IMAGE_IO_AVAILABLE) {
			throw new IllegalStateException("No decoder for " + path + " on this platform");
		}
		final BufferedImage image = ImageIO.read(new ByteArrayInputStream(data.array()));
		final int[] pixels = new int[image.getWidth() * image.getHeight()];
		image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
		return new ArgbImage(image.getWidth(), image.getHeight(), pixels);
	}

	public static Texture getTextureNoColorCorrection(final ArgbImage image) {
		final Pixmap pixmap = new Pixmap(image.getWidth(), image.getHeight(), Format.RGBA8888);
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				final int pixel = image.getRGB(x, y);
				pixmap.drawPixel(x, y, (pixel << 8) | (pixel >>> 24));
			}
		}
		// An unmanaged texture has its pixels on the GPU once constructed; only the native copy would stay behind.
		final Texture texture = SessionResources.track(new Texture(pixmap));
		pixmap.dispose();
		texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		return texture;
	}

	public static Buffer getTextureBuffer(final BufferedImage image) {

		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final int[] pixels = new int[imageWidth * imageHeight];
		image.getRGB(0, 0, imageWidth, imageHeight, pixels, 0, imageWidth);

		final ByteBuffer buffer = ByteBuffer.allocateDirect(imageWidth * imageHeight * BYTES_PER_PIXEL)
				.order(ByteOrder.nativeOrder());
		// 4
		// for
		// RGBA,
		// 3
		// for
		// RGB

		for (int y = 0; y < imageHeight; y++) {
			for (int x = 0; x < imageWidth; x++) {
				final int pixel = pixels[(y * imageWidth) + x];
				buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red component
				buffer.put((byte) ((pixel >> 8) & 0xFF)); // Green component
				buffer.put((byte) (pixel & 0xFF)); // Blue component
				buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha component.
				// Only for RGBA
			}
		}

		buffer.flip();
		return buffer;
	}

	/**
	 * Convert an input buffered image into sRGB color space using component values
	 * directly instead of performing a color space conversion.
	 *
	 * @param in Input image to be converted.
	 * @return Resulting sRGB image.
	 */
	public static BufferedImage forceBufferedImagesRGB(final BufferedImage in) {
		// Resolve input ColorSpace.
		final ColorSpace inCS = in.getColorModel().getColorSpace();
		final ColorSpace sRGBCS = ColorSpace.getInstance(ColorSpace.CS_sRGB);
		if (inCS == sRGBCS) {
			// Already is sRGB.
			return in;
		}
		if (inCS.getNumComponents() != sRGBCS.getNumComponents()) {
			throw new IllegalArgumentException("Input color space has different number of components from sRGB.");
		}

		// Draw input.
		final ColorModel lRGBModel = new ComponentColorModel(inCS, true, false, Transparency.TRANSLUCENT,
				DataBuffer.TYPE_BYTE);
		final ColorModel sRGBModel = new ComponentColorModel(sRGBCS, true, false, Transparency.TRANSLUCENT,
				DataBuffer.TYPE_BYTE);
		final BufferedImage lRGB = new BufferedImage(lRGBModel,
				lRGBModel.createCompatibleWritableRaster(in.getWidth(), in.getHeight()), false, null);
		for (int i = 0; i < in.getWidth(); i++) {
			for (int j = 0; j < in.getHeight(); j++) {
				lRGB.setRGB(i, j, in.getRGB(i, j));
			}
		}

		// Convert to sRGB.
		final BufferedImage sRGB = new BufferedImage(sRGBModel, lRGB.getRaster(), false, null);

		return sRGB;
	}

	private ImageUtils() {
	}
}
