package com.etheller.warsmash.viewer5.handlers.blp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Decodes a block compressed DDS image (DXT1, DXT3, DXT5, ATI2) into RGBA bytes, without depending on java.awt or
 * javax.imageio: the largest stored mip level within a size limit, and the smaller stored levels below it when asked.
 */
public final class DdsDecoder {
	private static final int MAGIC = 0x20534444; // "DDS "
	private static final int HEADER_SIZE = 128;
	private static final int DDSD_MIPMAPCOUNT = 0x20000;
	private static final int DDPF_FOURCC = 0x4;
	static final int FOURCC_DXT1 = fourCC("DXT1");
	static final int FOURCC_DXT3 = fourCC("DXT3");
	static final int FOURCC_DXT5 = fourCC("DXT5");
	private static final int FOURCC_ATI2 = fourCC("ATI2");

	private final MipLevels levels;

	private DdsDecoder(final MipLevels levels) {
		this.levels = levels;
	}

	public int getWidth() {
		return this.levels.getWidth(0);
	}

	public int getHeight() {
		return this.levels.getHeight(0);
	}

	/**
	 * @return direct buffer of width * height RGBA pixels, rows from the top
	 */
	public ByteBuffer getRgba() {
		return this.levels.getRgba(0);
	}

	/** The decoded levels, the largest first. */
	public MipLevels getLevels() {
		return this.levels;
	}

	public static boolean isDds(final ByteBuffer file) {
		final ByteBuffer data = file.duplicate().order(ByteOrder.LITTLE_ENDIAN);
		return (data.remaining() >= HEADER_SIZE) && (data.getInt(0) == MAGIC);
	}

	/** The top level only, at its full size. */
	public static DdsDecoder decode(final ByteBuffer file) throws IOException {
		return decode(file, 0, false);
	}

	/**
	 * @param maxSize the largest width and height to decode, 0 for no limit: the first stored level within it becomes
	 *                the top level; a file without such a level keeps its smallest one
	 * @param mips    whether the smaller stored levels are decoded too
	 */
	public static DdsDecoder decode(final ByteBuffer file, final int maxSize, final boolean mips) throws IOException {
		final ByteBuffer data = file.duplicate().order(ByteOrder.LITTLE_ENDIAN);
		if ((data.remaining() < HEADER_SIZE) || (data.getInt(0) != MAGIC)) {
			throw new IOException("Not a DDS file");
		}
		final int flags = data.getInt(8);
		final int height = data.getInt(12);
		final int width = data.getInt(16);
		final int storedLevels = (flags & DDSD_MIPMAPCOUNT) != 0 ? Math.max(1, data.getInt(28)) : 1;
		final int pixelFormatFlags = data.getInt(80);
		final int fourCC = data.getInt(84);
		if ((pixelFormatFlags & DDPF_FOURCC) == 0) {
			throw new IOException("Unsupported uncompressed DDS");
		}
		if ((width <= 0) || (height <= 0) || (((long) width * height) > (1 << 26))) {
			throw new IOException("Bad DDS dimensions " + width + "x" + height);
		}
		if ((fourCC != FOURCC_DXT1) && (fourCC != FOURCC_DXT3) && (fourCC != FOURCC_DXT5) && (fourCC != FOURCC_ATI2)) {
			throw new IOException("Unsupported DDS format: " + Integer.toHexString(fourCC));
		}
		final int blockSize = fourCC == FOURCC_DXT1 ? 8 : 16;
		// A stored chain may stop early, or claim levels the file is too short to hold; only whole levels count.
		final int maxLevels = Math.min(storedLevels, MipLevels.levelCount(width, height));
		final long[] offsets = new long[maxLevels + 1];
		offsets[0] = HEADER_SIZE;
		for (int level = 0; level < maxLevels; level++) {
			offsets[level + 1] = offsets[level]
					+ levelBytes(MipLevels.size(width, level), MipLevels.size(height, level), blockSize);
		}
		int first = MipLevels.firstWithin(width, height, maxSize, maxLevels);
		while ((first > 0) && (offsets[first + 1] > data.limit())) {
			first--;
		}
		long offset = offsets[first];
		final MipLevels levels = new MipLevels();
		for (int level = first; level < maxLevels; level++) {
			final int levelWidth = MipLevels.size(width, level);
			final int levelHeight = MipLevels.size(height, level);
			final long size = levelBytes(levelWidth, levelHeight, blockSize);
			if ((level > first) && ((offset + size) > data.limit())) {
				break;
			}
			final byte[] src = new byte[(int) Math.max(0, Math.min(size, data.limit() - offset))];
			if (src.length > 0) {
				data.position((int) offset);
				data.get(src);
			}
			levels.add(levelWidth, levelHeight, decodeBlocks(src, levelWidth, levelHeight, fourCC));
			offset += size;
			if (!mips) {
				break;
			}
		}
		return new DdsDecoder(levels);
	}

	private static long levelBytes(final int width, final int height, final int blockSize) {
		return (long) Math.max(1, (width + 3) / 4) * Math.max(1, (height + 3) / 4) * blockSize;
	}

	/**
	 * @param data the blocks of one image, left to right and top to bottom; missing
	 *             trailing blocks decode as zeros
	 * @return width * height RGBA pixels, rows from the top
	 */
	static byte[] decodeBlocks(final byte[] data, final int width, final int height, final int fourCC)
			throws IOException {
		final int blocksWide = Math.max(1, (width + 3) / 4);
		final int blocksHigh = Math.max(1, (height + 3) / 4);
		final int blockSize = fourCC == FOURCC_DXT1 ? 8 : 16;
		final byte[] src = data.length >= (blocksWide * blocksHigh * blockSize) ? data
				: Arrays.copyOf(data, blocksWide * blocksHigh * blockSize);
		final byte[] pixels = new byte[width * height * 4];
		final byte[] block = new byte[16 * 4];
		final byte[] channelA = new byte[16];
		final byte[] channelB = new byte[16];
		final int[] palette = new int[16];
		final int[] values = new int[8];
		int offset = 0;
		for (int blockY = 0; blockY < blocksHigh; blockY++) {
			for (int blockX = 0; blockX < blocksWide; blockX++) {
				if (fourCC == FOURCC_DXT1) {
					decodeColorBlock(src, offset, block, palette, true);
					offset += 8;
				}
				else if (fourCC == FOURCC_DXT3) {
					decodeColorBlock(src, offset + 8, block, palette, false);
					for (int i = 0; i < 16; i++) {
						final int alpha = ((src[offset + (i >> 1)] & 0xFF) >>> ((i & 1) * 4)) & 0xF;
						block[(i * 4) + 3] = (byte) ((alpha << 4) | alpha);
					}
					offset += 16;
				}
				else if (fourCC == FOURCC_DXT5) {
					decodeInterpolatedBlock(src, offset, channelA, values);
					decodeColorBlock(src, offset + 8, block, palette, false);
					for (int i = 0; i < 16; i++) {
						block[(i * 4) + 3] = channelA[i];
					}
					offset += 16;
				}
				else if (fourCC == FOURCC_ATI2) {
					// Unlike BC5, the legacy ATI2 code stores the green block first.
					decodeInterpolatedBlock(src, offset, channelB, values);
					decodeInterpolatedBlock(src, offset + 8, channelA, values);
					for (int i = 0; i < 16; i++) {
						block[i * 4] = channelA[i];
						block[(i * 4) + 1] = channelB[i];
						block[(i * 4) + 2] = 0;
						block[(i * 4) + 3] = (byte) 0xFF;
					}
					offset += 16;
				}
				else {
					throw new IOException("Unsupported DDS format: " + Integer.toHexString(fourCC));
				}
				writeBlock(pixels, block, blockX * 4, blockY * 4, width, height);
			}
		}
		return pixels;
	}

	private static void decodeColorBlock(final byte[] src, final int offset, final byte[] out, final int[] palette,
			final boolean dxt1) {
		final int color0 = (src[offset] & 0xFF) | ((src[offset + 1] & 0xFF) << 8);
		final int color1 = (src[offset + 2] & 0xFF) | ((src[offset + 3] & 0xFF) << 8);
		final int indices = (src[offset + 4] & 0xFF) | ((src[offset + 5] & 0xFF) << 8)
				| ((src[offset + 6] & 0xFF) << 16) | ((src[offset + 7] & 0xFF) << 24);
		expand565(color0, palette, 0);
		expand565(color1, palette, 4);
		final boolean fourColors = !dxt1 || (color0 > color1);
		for (int channel = 0; channel < 3; channel++) {
			final int c0 = palette[channel];
			final int c1 = palette[4 + channel];
			if (fourColors) {
				palette[8 + channel] = ((2 * c0) + c1) / 3;
				palette[12 + channel] = (c0 + (2 * c1)) / 3;
			}
			else {
				palette[8 + channel] = (c0 + c1) / 2;
				palette[12 + channel] = 0;
			}
		}
		palette[3] = 255;
		palette[7] = 255;
		palette[11] = 255;
		palette[15] = fourColors ? 255 : 0;
		for (int i = 0; i < 16; i++) {
			final int index = ((indices >>> (2 * i)) & 0x3) * 4;
			out[i * 4] = (byte) palette[index];
			out[(i * 4) + 1] = (byte) palette[index + 1];
			out[(i * 4) + 2] = (byte) palette[index + 2];
			out[(i * 4) + 3] = (byte) palette[index + 3];
		}
	}

	private static void decodeInterpolatedBlock(final byte[] src, final int offset, final byte[] out,
			final int[] values) {
		final int value0 = src[offset] & 0xFF;
		final int value1 = src[offset + 1] & 0xFF;
		long indices = 0;
		for (int i = 0; i < 6; i++) {
			indices |= (long) (src[offset + 2 + i] & 0xFF) << (8 * i);
		}
		values[0] = value0;
		values[1] = value1;
		if (value0 > value1) {
			for (int i = 1; i < 7; i++) {
				values[i + 1] = (((7 - i) * value0) + (i * value1)) / 7;
			}
		}
		else {
			for (int i = 1; i < 5; i++) {
				values[i + 1] = (((5 - i) * value0) + (i * value1)) / 5;
			}
			values[6] = 0;
			values[7] = 255;
		}
		for (int i = 0; i < 16; i++) {
			out[i] = (byte) values[(int) ((indices >>> (3 * i)) & 0x7)];
		}
	}

	private static void expand565(final int color, final int[] out, final int offset) {
		final int red = (color >>> 11) & 0x1F;
		final int green = (color >>> 5) & 0x3F;
		final int blue = color & 0x1F;
		out[offset] = (red << 3) | (red >>> 2);
		out[offset + 1] = (green << 2) | (green >>> 4);
		out[offset + 2] = (blue << 3) | (blue >>> 2);
	}

	private static void writeBlock(final byte[] pixels, final byte[] block, final int x0, final int y0,
			final int width, final int height) {
		final int columns = Math.min(4, width - x0) * 4;
		for (int y = 0; y < 4; y++) {
			final int pixelY = y0 + y;
			if (pixelY >= height) {
				break;
			}
			System.arraycopy(block, y * 16, pixels, ((pixelY * width) + x0) * 4, columns);
		}
	}

	private static int fourCC(final String code) {
		return code.charAt(0) | (code.charAt(1) << 8) | (code.charAt(2) << 16) | (code.charAt(3) << 24);
	}
}
