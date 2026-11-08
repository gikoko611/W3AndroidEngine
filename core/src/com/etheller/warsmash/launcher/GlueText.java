package com.etheller.warsmash.launcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.etheller.warsmash.datasources.DataSource;

/** The glue strings of the game data and their colour codes, as the launcher shows them. */
public final class GlueText {
	private static final Pattern STRING_LINE = Pattern.compile("^\\s*([A-Za-z0-9_]+)\\s+\"(.*)\"\\s*,?\\s*$");

	/** A piece of text in one colour; colour 0 is the text's own colour. */
	public static final class Run {
		public final String text;
		public final int colour;

		Run(final String text, final int colour) {
			this.text = text;
			this.colour = colour;
		}
	}

	private GlueText() {
	}

	/**
	 * The strings of the glue string lists by upper case key, the first definition winning; null when the game
	 * data has no string list.
	 */
	public static Map<String, String> read(final DataSource data) throws IOException {
		if (!data.has(SkinArt.STRING_FILES[0])) {
			return null;
		}
		final Map<String, String> found = new LinkedHashMap<>();
		for (final String path : SkinArt.STRING_FILES) {
			if (!data.has(path)) {
				continue;
			}
			try (InputStream in = data.getResourceAsStream(path);
					BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					final Matcher matcher = STRING_LINE.matcher(line);
					if (matcher.matches()) {
						found.putIfAbsent(matcher.group(1).toUpperCase(Locale.ROOT), matcher.group(2));
					}
				}
			}
		}
		return found;
	}

	/** A glue string's text in its colours: |cAARRGGBB starts a colour, |r ends it. */
	public static List<Run> runs(final String value) {
		final List<Run> runs = new ArrayList<>();
		final StringBuilder text = new StringBuilder();
		int colour = 0;
		int i = 0;
		while (i < value.length()) {
			final char c = value.charAt(i);
			if ((c == '|') && ((i + 1) < value.length())) {
				final char code = Character.toLowerCase(value.charAt(i + 1));
				if ((code == 'c') && ((i + 10) <= value.length())) {
					try {
						final int next = (int) Long.parseLong(value.substring(i + 2, i + 10), 16);
						flush(runs, text, colour);
						colour = next;
						i += 10;
						continue;
					}
					catch (final NumberFormatException e) {
						// Not a colour code; kept as text.
					}
				}
				else if (code == 'r') {
					flush(runs, text, colour);
					colour = 0;
					i += 2;
					continue;
				}
			}
			text.append(c);
			i++;
		}
		flush(runs, text, colour);
		return runs;
	}

	/** A glue string without its colour codes. */
	public static String plain(final String value) {
		final StringBuilder text = new StringBuilder();
		for (final Run run : runs(value)) {
			text.append(run.text);
		}
		return text.toString();
	}

	private static void flush(final List<Run> runs, final StringBuilder text, final int colour) {
		if (text.length() > 0) {
			runs.add(new Run(text.toString(), colour));
			text.setLength(0);
		}
	}
}
