package com.etheller.warsmash.movie;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The captions of a movie, from its SubRip (.srt) file. */
public final class MovieCaptions {
	private static final Pattern TIMES = Pattern.compile(
			"(\\d+):(\\d+):(\\d+)[,.](\\d+)\\s*-->\\s*(\\d+):(\\d+):(\\d+)[,.](\\d+)");
	private static final Pattern TAGS = Pattern.compile("<[^>]*>|\\{\\\\[^}]*\\}");

	private final List<Long> starts = new ArrayList<>();
	private final List<Long> ends = new ArrayList<>();
	private final List<String> texts = new ArrayList<>();

	private MovieCaptions() {
	}

	/** @return the captions, none when the file is missing or unreadable */
	public static MovieCaptions read(final File file) {
		final MovieCaptions captions = new MovieCaptions();
		if (file == null) {
			return captions;
		}
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
			long start = -1;
			long end = -1;
			final StringBuilder text = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.replace("﻿", "").trim();
				final Matcher times = TIMES.matcher(line);
				if (times.find()) {
					captions.add(start, end, text);
					start = millis(times, 1);
					end = millis(times, 5);
				}
				else if (line.isEmpty()) {
					captions.add(start, end, text);
					start = -1;
				}
				else if (start >= 0) {
					if (text.length() > 0) {
						text.append('\n');
					}
					text.append(TAGS.matcher(line).replaceAll(""));
				}
			}
			captions.add(start, end, text);
		}
		catch (final IOException | RuntimeException e) {
			System.err.println("Unable to read the captions " + file + ": " + e);
		}
		return captions;
	}

	private void add(final long start, final long end, final StringBuilder text) {
		if ((start >= 0) && (text.length() > 0)) {
			this.starts.add(start);
			this.ends.add(end);
			this.texts.add(text.toString());
		}
		text.setLength(0);
	}

	private static long millis(final Matcher matcher, final int group) {
		String fraction = matcher.group(group + 3);
		while (fraction.length() < 3) {
			fraction += "0";
		}
		return (Long.parseLong(matcher.group(group)) * 3600000L) + (Long.parseLong(matcher.group(group + 1)) * 60000L)
				+ (Long.parseLong(matcher.group(group + 2)) * 1000L) + Long.parseLong(fraction.substring(0, 3));
	}

	public int size() {
		return this.texts.size();
	}

	public List<String> getTexts() {
		return Collections.unmodifiableList(this.texts);
	}

	/** @return the caption on screen at this time of the movie, or null */
	public String at(final long millis) {
		for (int i = 0; i < this.texts.size(); i++) {
			if ((millis >= this.starts.get(i)) && (millis < this.ends.get(i))) {
				return this.texts.get(i);
			}
		}
		return null;
	}
}
