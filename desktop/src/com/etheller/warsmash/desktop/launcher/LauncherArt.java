package com.etheller.warsmash.desktop.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.launcher.CampaignChoice;
import com.etheller.warsmash.launcher.LauncherImages;
import com.etheller.warsmash.launcher.LauncherLog;
import com.etheller.warsmash.launcher.SkinArt;
import com.etheller.warsmash.util.ArgbImage;
import com.etheller.warsmash.util.DataSourceFileHandle;

/**
 * The launcher's art on the desktop: the same files of the game data the Android launcher prepares into its cache,
 * read straight from the data source into textures when first drawn.
 */
final class LauncherArt {
	private static final String CHARACTERS = FreeTypeFontGenerator.DEFAULT_CHARS
			+ "\u00b7\u2026\u2013\u2014\u203a\u2039\u00a9\u00ae\u00b0\u00d7\u2022\u2019\u201c\u201d\u00e9\u00f6\u00fc";

	/** A frame cut into its nine parts; the middle may be absent. */
	static final class Frame {
		final TextureRegion[] parts = new TextureRegion[9];
		final boolean centre;

		Frame(final Texture texture, final int slice, final boolean centre) {
			this.centre = centre;
			final int[] xs = { 0, slice, texture.getWidth() - slice, texture.getWidth() };
			final int[] ys = { 0, slice, texture.getHeight() - slice, texture.getHeight() };
			for (int row = 0; row < 3; row++) {
				for (int column = 0; column < 3; column++) {
					final TextureRegion part = new TextureRegion(texture, xs[column], ys[row],
							xs[column + 1] - xs[column], ys[row + 1] - ys[row]);
					part.flip(false, true);
					this.parts[(row * 3) + column] = part;
				}
			}
		}
	}

	final boolean reforged;
	private final DataSource data;
	private final Map<String, SkinArt.Item> items = new HashMap<>();
	private final Map<String, TextureRegion> images = new HashMap<>();
	private final Map<String, Frame> frames = new HashMap<>();
	private final Map<Integer, BitmapFont> fonts = new HashMap<>();
	private final Map<String, Texture> owned = new HashMap<>();
	private FreeTypeFontGenerator generator;
	private boolean generatorTried;
	private Map<String, String> missionPictures;
	/** While set, the pictures made are made without colour and darker. */
	private boolean muting;

	LauncherArt(final DataSource data, final boolean reforged) {
		this.data = data;
		this.reforged = reforged;
		for (final SkinArt.Item item : SkinArt.items(reforged)) {
			this.items.put(item.name, item);
		}
	}

	DataSource data() {
		return this.data;
	}

	boolean has(final String name) {
		return image(name) != null;
	}

	/** A picture of the skin by its launcher name; null when the game data lacks it. */
	TextureRegion image(final String name) {
		if (this.images.containsKey(name)) {
			return this.images.get(name);
		}
		TextureRegion region = null;
		final SkinArt.Item item = this.items.get(name);
		if (item != null) {
			try {
				final Texture texture = load(item);
				if (texture != null) {
					region = new TextureRegion(texture);
					region.flip(false, true);
				}
			}
			catch (final IOException | RuntimeException e) {
				LauncherLog.w("Unable to read " + item.path, e);
			}
		}
		this.images.put(name, region);
		return region;
	}

	/** A picture of the skin without its colours and darkened, for what cannot be chosen yet; null when missing. */
	TextureRegion muted(final String name) {
		final String key = "muted:" + name;
		if (this.images.containsKey(key)) {
			return this.images.get(key);
		}
		TextureRegion region = null;
		final SkinArt.Item item = this.items.get(name);
		if (item != null) {
			this.muting = true;
			try {
				final Texture texture = load(item);
				if (texture != null) {
					region = new TextureRegion(texture);
					region.flip(false, true);
				}
			}
			catch (final IOException | RuntimeException e) {
				LauncherLog.w("Unable to read " + item.path, e);
			}
			finally {
				this.muting = false;
			}
		}
		this.images.put(key, region);
		return region;
	}

	/** A framing picture cut into nine parts: edge files and Reforged frames with their slice. */
	Frame frame(final String name) {
		if (this.frames.containsKey(name)) {
			return this.frames.get(name);
		}
		Frame frame = null;
		final SkinArt.Item item = this.items.get(name);
		final TextureRegion image = image(name);
		if ((item != null) && (image != null)) {
			final Texture texture = image.getTexture();
			if (item.kind == SkinArt.Kind.EDGE) {
				frame = new Frame(texture, texture.getWidth() / 3, true);
			}
			else {
				final int slice = item.slice > 0 ? item.slice : 32;
				frame = new Frame(texture,
						Math.min(slice, Math.min(texture.getWidth(), texture.getHeight()) / 3), item.centre);
			}
		}
		this.frames.put(name, frame);
		return frame;
	}

	private Texture load(final SkinArt.Item item) throws IOException {
		switch (item.kind) {
		case PLAIN:
			return texture(LauncherImages.decode(this.data, item.path));
		case EDGE: {
			final ArgbImage edge = LauncherImages.decode(this.data, item.path);
			return edge == null ? null : texture(LauncherImages.edgeNine(edge));
		}
		case HOLDER: {
			final ArgbImage border = LauncherImages.decode(this.data, item.path);
			return border == null ? null : texture(LauncherImages.holder(border));
		}
		case TILES:
			return texture(LauncherImages.tiles(this.data, item.path));
		case WEB:
			return encoded(item.path);
		default:
			return null;
		}
	}

	/** A PNG or JPEG of the game data. */
	private Texture encoded(final String path) throws IOException {
		if (!this.data.has(path)) {
			return null;
		}
		final byte[] bytes;
		try (InputStream in = this.data.getResourceAsStream(path)) {
			if (in == null) {
				return null;
			}
			bytes = LauncherImages.bytes(in);
		}
		final Pixmap pixmap = new Pixmap(bytes, 0, bytes.length);
		try {
			if (this.muting) {
				mute(pixmap);
			}
			return own(path, new Texture(pixmap, false));
		}
		finally {
			pixmap.dispose();
		}
	}

	private static void mute(final Pixmap pixmap) {
		pixmap.setBlending(Pixmap.Blending.None);
		for (int y = 0; y < pixmap.getHeight(); y++) {
			for (int x = 0; x < pixmap.getWidth(); x++) {
				final int rgba = pixmap.getPixel(x, y);
				final int grey = Math.round(((((rgba >>> 24) & 0xFF) * 0.299f) + (((rgba >> 16) & 0xFF) * 0.587f)
						+ (((rgba >> 8) & 0xFF) * 0.114f)) * 0.8f);
				pixmap.drawPixel(x, y, (grey << 24) | (grey << 16) | (grey << 8) | (rgba & 0xFF));
			}
		}
	}

	private Texture own(final String key, final Texture texture) {
		texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		final Texture old = this.owned.put(key + "#" + System.identityHashCode(texture), texture);
		if (old != null) {
			old.dispose();
		}
		return texture;
	}

	Texture texture(final ArgbImage image) {
		if (image == null) {
			return null;
		}
		final Pixmap pixmap = new Pixmap(image.getWidth(), image.getHeight(), Pixmap.Format.RGBA8888);
		final int[] pixels = LauncherImages.pixels(image);
		final ByteBuffer rgba = pixmap.getPixels();
		rgba.position(0);
		for (final int argb : pixels) {
			rgba.put((byte) (argb >> 16)).put((byte) (argb >> 8)).put((byte) argb).put((byte) (argb >>> 24));
		}
		rgba.position(0);
		try {
			if (this.muting) {
				mute(pixmap);
			}
			return own("image", new Texture(pixmap, false));
		}
		finally {
			pixmap.dispose();
		}
	}

	/** A picture made from pixels, owned until the art goes. */
	TextureRegion region(final ArgbImage image) {
		final Texture texture = texture(image);
		if (texture == null) {
			return null;
		}
		final TextureRegion region = new TextureRegion(texture);
		region.flip(false, true);
		return region;
	}

	/** A Reforged picture by its data path, or a classic one without extension. */
	TextureRegion picture(final String path) {
		final String key = "picture:" + path;
		if (this.images.containsKey(key)) {
			return this.images.get(key);
		}
		TextureRegion region = null;
		try {
			final Texture texture = path.toLowerCase(Locale.ROOT).endsWith(".jpg")
					|| path.toLowerCase(Locale.ROOT).endsWith(".png") ? encoded(path)
							: texture(LauncherImages.decode(this.data, path));
			if (texture != null) {
				region = new TextureRegion(texture);
				region.flip(false, true);
			}
		}
		catch (final IOException | RuntimeException e) {
			LauncherLog.w("Unable to read " + path, e);
		}
		this.images.put(key, region);
		return region;
	}

	TextureRegion portrait(final int index) {
		return image(SkinArt.portraitFile(index));
	}

	/** The Reforged mission pictures of the game data, keyed by their path below the mission folder. */
	Map<String, String> missionPictures() {
		if (this.missionPictures == null) {
			this.missionPictures = new LinkedHashMap<>();
			final Collection<String> listed = this.data.getListfile();
			if (listed != null) {
				for (final String listedPath : listed) {
					final String path = listedPath.replace('/', '\\');
					final String lower = path.toLowerCase(Locale.ROOT);
					if (CampaignChoice.missionPicture(lower)) {
						this.missionPictures.put(lower.substring(SkinArt.MISSION_FOLDER.length()), path);
					}
				}
			}
		}
		return this.missionPictures;
	}

	/** The game's font at a size in screen pixels, made once per size. */
	BitmapFont font(final int pixels) {
		final int size = Math.max(6, Math.min(400, pixels));
		BitmapFont font = this.fonts.get(size);
		if (font == null) {
			final FreeTypeFontGenerator generator = generator();
			if (generator == null) {
				font = new BitmapFont(true);
			}
			else {
				final FreeTypeFontParameter parameter = new FreeTypeFontParameter();
				parameter.size = size;
				parameter.flip = true;
				parameter.characters = CHARACTERS;
				parameter.minFilter = Texture.TextureFilter.Linear;
				parameter.magFilter = Texture.TextureFilter.Linear;
				font = generator.generateFont(parameter);
				// A sign the font lacks is left out rather than drawn as a box.
				font.getData().missingGlyph = null;
			}
			font.setUseIntegerPositions(false);
			this.fonts.put(size, font);
		}
		return font;
	}

	private FreeTypeFontGenerator generator() {
		if (!this.generatorTried) {
			this.generatorTried = true;
			final SkinArt.Item item = this.items.get(SkinArt.FONT);
			try {
				if ((item != null) && this.data.has(item.path)) {
					this.generator = new FreeTypeFontGenerator(new DataSourceFileHandle(this.data, item.path));
				}
			}
			catch (final RuntimeException e) {
				LauncherLog.w("Unable to read the font", e);
			}
		}
		return this.generator;
	}

	/** Fonts are made for a scale; a new scale makes them again. */
	void dropFonts() {
		for (final BitmapFont font : this.fonts.values()) {
			font.dispose();
		}
		this.fonts.clear();
	}

	void dispose() {
		dropFonts();
		if (this.generator != null) {
			this.generator.dispose();
		}
		for (final Texture texture : this.owned.values()) {
			texture.dispose();
		}
		this.owned.clear();
		this.images.clear();
		this.frames.clear();
	}
}
