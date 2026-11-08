package com.etheller.warsmash.viewer5.handlers.w3x.ui.menu;

import java.util.Collections;
import java.util.List;

public class CampaignMission {
	private final String header;
	private final String missionName;
	private final String mapFilename;
	private final List<String> movies;

	public CampaignMission(final String header, final String missionName, final String mapFilename) {
		this(header, missionName, mapFilename, Collections.singletonList(mapFilename));
	}

	public CampaignMission(final String header, final String missionName, final String mapFilename,
			final List<String> movies) {
		this.header = header;
		this.missionName = missionName;
		this.mapFilename = mapFilename;
		this.movies = movies;
	}

	public String getHeader() {
		return this.header;
	}

	public String getMissionName() {
		return this.missionName;
	}

	public String getMapFilename() {
		return this.mapFilename;
	}

	/**
	 * @return for a cinematic entry, the movies it plays one after another (the introduction has the logos around
	 *         its movie); for a mission, its map file
	 */
	public List<String> getMovies() {
		return this.movies;
	}
}
