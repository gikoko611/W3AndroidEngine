package com.etheller.warsmash.parsers.fdf.frames;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData;
import com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph;

/**
 * Follows the pen along a line of text drawn in pieces, such as the runs of one color, so that each piece lands where
 * it would in the line drawn whole: with the advances and kerning of the glyphs before it and its own first glyph's
 * offset. Drawing a text puts the ink of its first glyph at the x it is drawn at, so a piece cannot simply start where
 * the ink of the previous one ends.
 */
public final class TextPen {
	private final BitmapFont font;
	private Glyph last;
	/** Where the last glyph's slot starts, relative to a whole line drawn at 0. */
	private float lastSlotX;

	public TextPen(final BitmapFont font) {
		this.font = font;
	}

	public void newLine() {
		this.last = null;
		this.lastSlotX = 0;
	}

	public boolean isLineStarted() {
		return this.last != null;
	}

	/** The x to draw a piece at that starts with this character, after the text the pen has passed. */
	public float nextX(final char c) {
		final Glyph glyph = glyph(c);
		if (this.last == null) {
			return 0;
		}
		final BitmapFontData data = this.font.getData();
		final float slotX = this.lastSlotX + (this.last.xadvance * data.scaleX);
		if (glyph == null) {
			return slotX;
		}
		return (slotX + (this.last.getKerning(c) * data.scaleX) + (glyph.xoffset * data.scaleX))
				- drawnInkOffset(glyph);
	}

	/** The x to draw this piece at; the pen then passes it. */
	public float place(final CharSequence text) {
		final float x = text.length() == 0 ? nextX(' ') : nextX(text.charAt(0));
		pass(text);
		return x;
	}

	public void pass(final CharSequence text) {
		final BitmapFontData data = this.font.getData();
		for (int i = 0; i < text.length(); i++) {
			final char c = text.charAt(i);
			final Glyph glyph = glyph(c);
			if (glyph == null) {
				continue;
			}
			if (this.last == null) {
				this.lastSlotX = glyph.fixedWidth ? 0 : (-glyph.xoffset * data.scaleX) - data.padLeft;
			}
			else {
				this.lastSlotX += (this.last.xadvance + this.last.getKerning(c)) * data.scaleX;
			}
			this.last = glyph;
		}
	}

	/** Where drawing a text that starts with this glyph puts the glyph, from the x it is drawn at. */
	private float drawnInkOffset(final Glyph glyph) {
		final BitmapFontData data = this.font.getData();
		return glyph.fixedWidth ? glyph.xoffset * data.scaleX : -data.padLeft;
	}

	private Glyph glyph(final char c) {
		final BitmapFontData data = this.font.getData();
		final Glyph glyph = data.getGlyph(c);
		return glyph != null ? glyph : data.missingGlyph;
	}
}
