package com.etheller.warsmash.movie;

import java.io.File;

/**
 * A movie found in the game data: the picture, and the sound and captions kept beside it when there are any, with the
 * game's font for a player that draws the captions itself.
 */
public final class Movie {
	private final String name;
	private final File video;
	private final File audio;
	private final File captions;
	private final File captionFont;

	public Movie(final String name, final File video, final File audio, final File captions) {
		this(name, video, audio, captions, null);
	}

	public Movie(final String name, final File video, final File audio, final File captions, final File captionFont) {
		this.name = name;
		this.video = video;
		this.audio = audio;
		this.captions = captions;
		this.captionFont = captionFont;
	}

	public String getName() {
		return this.name;
	}

	public File getVideo() {
		return this.video;
	}

	/** @return the sound track, or null for a silent movie */
	public File getAudio() {
		return this.audio;
	}

	/** @return the captions, or null when the movie has none in this language */
	public File getCaptions() {
		return this.captions;
	}

	/** @return the font of the game's interface, or null when the data has none the player can read */
	public File getCaptionFont() {
		return this.captionFont;
	}

	@Override
	public String toString() {
		return this.name + " (" + this.video + ")";
	}
}
