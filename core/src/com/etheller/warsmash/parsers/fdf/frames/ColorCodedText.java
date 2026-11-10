package com.etheller.warsmash.parsers.fdf.frames;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;

/**
 * Text drawn straight with a font, outside of a frame, with its color codes: "|cAARRGGBB" starts a color, "|r" goes
 * back to the base color and "|n" breaks the line. The layout is kept until the text or the font changes.
 */
public final class ColorCodedText {
	private final List<Piece> pieces = new ArrayList<>();
	private String text;
	private BitmapFont font;
	private float width;
	private float height;

	/** Lays the text out for this font, unless it already is. */
	public void set(final String text, final BitmapFont font, final GlyphLayout glyphLayout) {
		if ((font == this.font) && text.equals(this.text)) {
			return;
		}
		this.text = text;
		this.font = font;
		this.pieces.clear();
		this.width = 0;
		final TextPen pen = new TextPen(font);
		final StringBuilder piece = new StringBuilder();
		Color color = null;
		int line = 0;
		final int length = text.length();
		for (int i = 0; i < length; i++) {
			final char c = text.charAt(i);
			if ((c == '|') && ((i + 1) < length)) {
				final char escaped = text.charAt(i + 1);
				if (((escaped == 'c') || (escaped == 'C')) && ((i + 10) <= length)) {
					final Color parsed = parseColor(text.substring(i + 2, i + 10));
					if (parsed != null) {
						addPiece(piece, color, line, pen, glyphLayout);
						color = parsed;
						i += 9;
						continue;
					}
				}
				else if ((escaped == 'r') || (escaped == 'R')) {
					addPiece(piece, color, line, pen, glyphLayout);
					color = null;
					i++;
					continue;
				}
				else if ((escaped == 'n') || (escaped == 'N')) {
					addPiece(piece, color, line, pen, glyphLayout);
					pen.newLine();
					line++;
					i++;
					continue;
				}
				piece.append(c);
			}
			else if ((c == '\r') || (c == '\n')) {
				if ((c == '\r') && ((i + 1) < length) && (text.charAt(i + 1) == '\n')) {
					i++;
				}
				addPiece(piece, color, line, pen, glyphLayout);
				pen.newLine();
				line++;
			}
			else {
				piece.append(c);
			}
		}
		addPiece(piece, color, line, pen, glyphLayout);
		this.height = font.getCapHeight() + (line * font.getLineHeight());
	}

	private void addPiece(final StringBuilder piece, final Color color, final int line, final TextPen pen,
			final GlyphLayout glyphLayout) {
		if (piece.length() == 0) {
			return;
		}
		final String pieceText = piece.toString();
		piece.setLength(0);
		final float x = pen.place(pieceText);
		glyphLayout.setText(this.font, pieceText);
		this.width = Math.max(this.width, x + glyphLayout.width);
		this.pieces.add(new Piece(pieceText, color, x, line));
	}

	public float getWidth() {
		return this.width;
	}

	public float getHeight() {
		return this.height;
	}

	/**
	 * Draws the text with the top of its first line at y. Uncolored parts take the base color; every part takes its
	 * alpha.
	 */
	public void draw(final Batch batch, final float x, final float y, final Color baseColor) {
		for (final Piece piece : this.pieces) {
			final Color color = piece.color == null ? baseColor : piece.color;
			this.font.setColor(color.r, color.g, color.b, baseColor.a);
			this.font.draw(batch, piece.text, x + piece.x, y - (piece.line * this.font.getLineHeight()));
		}
	}

	/** Draws the whole text in one color, such as its shadow. */
	public void drawInColor(final Batch batch, final float x, final float y, final Color color) {
		this.font.setColor(color);
		for (final Piece piece : this.pieces) {
			this.font.draw(batch, piece.text, x + piece.x, y - (piece.line * this.font.getLineHeight()));
		}
	}

	/** The color of "|cAARRGGBB" from its eight hex digits, or null if they are not hex. */
	public static Color parseColor(final String hex) {
		try {
			final int argb = (int) Long.parseLong(hex, 16);
			// The alpha of a color code is not used: the text is always opaque.
			return new Color((argb << 8) | 0xFF);
		}
		catch (final NumberFormatException exc) {
			return null;
		}
	}

	private static final class Piece {
		private final String text;
		private final Color color;
		private final float x;
		private final int line;

		private Piece(final String text, final Color color, final float x, final int line) {
			this.text = text;
			this.color = color;
			this.x = x;
			this.line = line;
		}
	}
}
