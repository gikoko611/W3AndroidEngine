package com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Campaign state scripts change as missions end: what the campaign menu shows and which map comes next. It is kept in
 * the player's profile and written as it changes.
 */
public final class CCampaignProgress {
	private static final Set<Long> MISSIONS = new HashSet<>();
	private static final Set<Integer> CAMPAIGNS = new HashSet<>();
	private static final Set<Integer> OPENING_CINEMATICS = new HashSet<>();
	private static final Set<Integer> ENDING_CINEMATICS = new HashSet<>();
	private static boolean tutorialCleared;
	private static int menuCampaignIndex = -1;
	private static boolean campaignSelectScreenForced;
	private static String pendingLevel;
	private static final String STORE_FILE = "Campaigns.w3v";
	private static final int STORE_VERSION = 1;
	private static boolean loaded;

	private CCampaignProgress() {
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
		MISSIONS.clear();
		CAMPAIGNS.clear();
		OPENING_CINEMATICS.clear();
		ENDING_CINEMATICS.clear();
		tutorialCleared = false;
		menuCampaignIndex = -1;
		loaded = false;
	}

	public static void writeAll(final DataOutputStream out) throws IOException {
		out.writeInt(STORE_VERSION);
		out.writeInt(MISSIONS.size());
		for (final long mission : MISSIONS) {
			out.writeLong(mission);
		}
		writeFlags(out, CAMPAIGNS);
		writeFlags(out, OPENING_CINEMATICS);
		writeFlags(out, ENDING_CINEMATICS);
		out.writeBoolean(tutorialCleared);
		out.writeInt(menuCampaignIndex);
	}

	public static void readAll(final DataInputStream in) throws IOException {
		if (in.readInt() != STORE_VERSION) {
			throw new IOException("Unknown campaign progress version");
		}
		MISSIONS.clear();
		final int missions = in.readInt();
		for (int i = 0; i < missions; i++) {
			MISSIONS.add(in.readLong());
		}
		readFlags(in, CAMPAIGNS);
		readFlags(in, OPENING_CINEMATICS);
		readFlags(in, ENDING_CINEMATICS);
		tutorialCleared = in.readBoolean();
		menuCampaignIndex = in.readInt();
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

	public static void setMissionAvailable(final int campaign, final int mission, final boolean available) {
		loadOnce();
		final long key = (((long) campaign) << 32) | (mission & 0xFFFFFFFFL);
		if (available) {
			MISSIONS.add(key);
		}
		else {
			MISSIONS.remove(key);
		}
		save();
	}

	public static boolean isMissionAvailable(final int campaign, final int mission) {
		loadOnce();
		return MISSIONS.contains((((long) campaign) << 32) | (mission & 0xFFFFFFFFL));
	}

	public static void setCampaignAvailable(final int campaign, final boolean available) {
		loadOnce();
		setFlag(CAMPAIGNS, campaign, available);
		save();
	}

	public static boolean isCampaignAvailable(final int campaign) {
		loadOnce();
		return CAMPAIGNS.contains(campaign);
	}

	public static void setOpeningCinematicAvailable(final int campaign, final boolean available) {
		loadOnce();
		setFlag(OPENING_CINEMATICS, campaign, available);
		save();
	}

	public static void setEndingCinematicAvailable(final int campaign, final boolean available) {
		loadOnce();
		setFlag(ENDING_CINEMATICS, campaign, available);
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
		loadOnce();
		tutorialCleared = cleared;
		save();
	}

	public static boolean isTutorialCleared() {
		loadOnce();
		return tutorialCleared;
	}

	public static void setMenuCampaignIndex(final int campaignIndex) {
		loadOnce();
		menuCampaignIndex = campaignIndex;
		save();
	}

	public static int getMenuCampaignIndex() {
		loadOnce();
		return menuCampaignIndex;
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
