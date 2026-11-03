package com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Campaign state scripts change as missions end: what the campaign menu shows and which map comes next. It is kept in
 * the player's profile and written as it changes.
 */
public final class CCampaignProgress {
	/**
	 * Scripts number campaigns by their place in the campaign list of the game data, and the lists differ: Reign of
	 * Chaos and The Frozen Throne each start at 0 (1.29 keeps their progress apart), the combined list of later
	 * versions numbers them all at once. Each list therefore keeps its own progress.
	 */
	public enum CampaignList {
		COMBINED, REIGN_OF_CHAOS, FROZEN_THRONE
	}

	private static final class Progress {
		private final Set<Long> missions = new HashSet<>();
		private final Set<Integer> campaigns = new HashSet<>();
		private final Set<Integer> openingCinematics = new HashSet<>();
		private final Set<Integer> endingCinematics = new HashSet<>();
		private boolean tutorialCleared;
		private int menuCampaignIndex = -1;
		private int classicMenuCampaignIndex = -1;

		private void write(final DataOutputStream out) throws IOException {
			out.writeInt(this.missions.size());
			for (final long mission : this.missions) {
				out.writeLong(mission);
			}
			writeFlags(out, this.campaigns);
			writeFlags(out, this.openingCinematics);
			writeFlags(out, this.endingCinematics);
			out.writeBoolean(this.tutorialCleared);
			out.writeInt(this.menuCampaignIndex);
			out.writeInt(this.classicMenuCampaignIndex);
		}

		private void read(final DataInputStream in, final boolean hasClassicMenuIndex) throws IOException {
			final int missionCount = in.readInt();
			for (int i = 0; i < missionCount; i++) {
				this.missions.add(in.readLong());
			}
			readFlags(in, this.campaigns);
			readFlags(in, this.openingCinematics);
			readFlags(in, this.endingCinematics);
			this.tutorialCleared = in.readBoolean();
			this.menuCampaignIndex = in.readInt();
			this.classicMenuCampaignIndex = hasClassicMenuIndex ? in.readInt() : -1;
		}
	}

	private static final Map<CampaignList, Progress> PROGRESS = new EnumMap<>(CampaignList.class);
	private static CampaignList campaignList = CampaignList.COMBINED;
	private static boolean campaignSelectScreenForced;
	private static String pendingLevel;
	private static final String STORE_FILE = "Campaigns.w3v";
	private static final int STORE_VERSION = 3;
	private static boolean loaded;

	private CCampaignProgress() {
	}

	/** Set by the menu from the campaign list the game data has, before any progress is read or written. */
	public static void setCampaignList(final CampaignList list) {
		campaignList = list;
	}

	private static Progress current() {
		loadOnce();
		return PROGRESS.computeIfAbsent(campaignList, list -> new Progress());
	}

	private static void loadOnce() {
		if (!loaded) {
			loaded = true;
			CSaveStorage.read(CSaveStorage.profileFile(STORE_FILE), CCampaignProgress::readAll);
		}
	}

	private static void save() {
		CSaveStorage.writeAtomically(CSaveStorage.profileFile(STORE_FILE), CCampaignProgress::writeAll);
	}

	/** Another player profile has its own progress. */
	static void forgetLoaded() {
		PROGRESS.clear();
		loaded = false;
	}

	public static void writeAll(final DataOutputStream out) throws IOException {
		out.writeInt(STORE_VERSION);
		out.writeInt(PROGRESS.size());
		for (final Map.Entry<CampaignList, Progress> entry : PROGRESS.entrySet()) {
			out.writeUTF(entry.getKey().name());
			entry.getValue().write(out);
		}
	}

	public static void readAll(final DataInputStream in) throws IOException {
		final int version = in.readInt();
		if ((version < 1) || (version > STORE_VERSION)) {
			throw new IOException("Unknown campaign progress version");
		}
		PROGRESS.clear();
		if (version < 3) {
			// Older files kept one progress, written with the campaign list this game data still has.
			final Progress progress = new Progress();
			progress.read(in, version >= 2);
			PROGRESS.put(campaignList, progress);
		}
		else {
			final int lists = in.readInt();
			for (int i = 0; i < lists; i++) {
				final String listName = in.readUTF();
				final Progress progress = new Progress();
				progress.read(in, true);
				try {
					PROGRESS.put(CampaignList.valueOf(listName), progress);
				}
				catch (final IllegalArgumentException e) {
					// A list a later version knows about; this one cannot show it.
				}
			}
		}
		loaded = true;
	}

	private static void writeFlags(final DataOutputStream out, final Set<Integer> flags) throws IOException {
		out.writeInt(flags.size());
		for (final int flag : flags) {
			out.writeInt(flag);
		}
	}

	private static void readFlags(final DataInputStream in, final Set<Integer> flags) throws IOException {
		flags.clear();
		final int count = in.readInt();
		for (int i = 0; i < count; i++) {
			flags.add(in.readInt());
		}
	}

	private static long missionKey(final int campaign, final int mission) {
		return (((long) campaign) << 32) | (mission & 0xFFFFFFFFL);
	}

	public static void setMissionAvailable(final int campaign, final int mission, final boolean available) {
		final Set<Long> missions = current().missions;
		if (available) {
			missions.add(missionKey(campaign, mission));
		}
		else {
			missions.remove(missionKey(campaign, mission));
		}
		save();
	}

	public static boolean isMissionAvailable(final int campaign, final int mission) {
		return current().missions.contains(missionKey(campaign, mission));
	}

	public static void setCampaignAvailable(final int campaign, final boolean available) {
		setFlag(current().campaigns, campaign, available);
		save();
	}

	public static boolean isCampaignAvailable(final int campaign) {
		return current().campaigns.contains(campaign);
	}

	public static void setOpeningCinematicAvailable(final int campaign, final boolean available) {
		setFlag(current().openingCinematics, campaign, available);
		save();
	}

	public static void setEndingCinematicAvailable(final int campaign, final boolean available) {
		setFlag(current().endingCinematics, campaign, available);
		save();
	}

	private static void setFlag(final Set<Integer> flags, final int index, final boolean flag) {
		if (flag) {
			flags.add(index);
		}
		else {
			flags.remove(index);
		}
	}

	public static void setTutorialCleared(final boolean cleared) {
		current().tutorialCleared = cleared;
		save();
	}

	public static boolean isTutorialCleared() {
		return current().tutorialCleared;
	}

	public static void setMenuCampaignIndex(final int campaignIndex) {
		current().menuCampaignIndex = campaignIndex;
		save();
	}

	public static int getMenuCampaignIndex() {
		return current().menuCampaignIndex;
	}

	/**
	 * SetCampaignMenuRace: Reign of Chaos missions pick their campaign on the classic campaign screen by race; the
	 * expansion screen keeps its own choice (SetCampaignMenuRaceEx).
	 */
	public static void setClassicMenuCampaignIndex(final int campaignIndex) {
		current().classicMenuCampaignIndex = campaignIndex;
		save();
	}

	public static int getClassicMenuCampaignIndex() {
		return current().classicMenuCampaignIndex;
	}

	public static void forceCampaignSelectScreen() {
		campaignSelectScreenForced = true;
	}

	public static boolean consumeCampaignSelectScreenForced() {
		final boolean forced = campaignSelectScreenForced;
		campaignSelectScreenForced = false;
		return forced;
	}

	private static String pendingComputerSlots;

	public static void setPendingLevel(final String levelFile) {
		pendingLevel = levelFile;
	}

	public static void setPendingComputerSlots(final String computerSlots) {
		pendingComputerSlots = computerSlots;
	}

	public static String consumePendingComputerSlots() {
		final String slots = pendingComputerSlots;
		pendingComputerSlots = null;
		return slots;
	}

	public static String consumePendingLevel() {
		final String level = pendingLevel;
		pendingLevel = null;
		return level;
	}
}
