package com.etheller.warsmash.launcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CCampaignProgress;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.menu.CampaignCatalog;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.menu.CampaignMission;

/**
 * The launcher's campaign choices: which campaign and mission it offers, and the scene and pictures shown with them.
 */
public final class CampaignChoice {
	private CampaignChoice() {
	}

	/**
	 * The campaign the progress last chose; else the last one a mission opened or got into; else the first open one.
	 */
	public static CampaignCatalog.Entry playedCampaign(final CampaignCatalog catalog,
			final CCampaignProgress.Snapshot progress) {
		final CampaignCatalog.Entry chosen = catalog.getEntry(catalog.menuCampaignNumber(progress));
		if ((chosen != null) && CampaignCatalog.isOpen(chosen, progress)) {
			return chosen;
		}
		CampaignCatalog.Entry played = null;
		CampaignCatalog.Entry first = null;
		for (final CampaignCatalog.Entry entry : catalog.getEntries()) {
			if (!CampaignCatalog.isOpen(entry, progress)) {
				continue;
			}
			if (first == null) {
				first = entry;
			}
			if (started(entry, progress)) {
				played = entry;
			}
		}
		return played != null ? played : first;
	}

	/** A campaign a script opened, or one whose second mission is open. */
	public static boolean started(final CampaignCatalog.Entry entry, final CCampaignProgress.Snapshot progress) {
		return CampaignCatalog.isOpen(entry, progress)
				&& (!entry.getData().isDefaultOpen() || CampaignCatalog.isMissionOpen(entry, 1, progress));
	}

	/** The furthest open mission of a campaign, -1 for none. */
	public static int furthestMission(final CampaignCatalog.Entry entry, final CCampaignProgress.Snapshot progress) {
		final List<CampaignMission> missions = entry.getData().getMissions();
		int furthest = -1;
		for (int i = 0; i < missions.size(); i++) {
			if (CampaignCatalog.isMissionOpen(entry, i, progress)) {
				furthest = i;
			}
		}
		return furthest;
	}

	/** The campaign the page shows chosen: the one chosen last while it is open, else the progress's, else the first. */
	public static CampaignCatalog.Entry chosenCampaign(final CampaignCatalog catalog,
			final CCampaignProgress.Snapshot progress, final Integer stored) {
		final int menu = catalog.menuCampaignNumber(progress);
		for (final int number : new int[] { stored != null ? stored : menu, menu }) {
			final CampaignCatalog.Entry entry = catalog.getEntry(number);
			if ((entry != null) && CampaignCatalog.isOpen(entry, progress)) {
				return entry;
			}
		}
		for (final CampaignCatalog.Entry entry : catalog.getEntries()) {
			if (CampaignCatalog.isOpen(entry, progress)) {
				return entry;
			}
		}
		return null;
	}

	/** The scene behind a campaign's pages: the recorded model its Background names. */
	public static String campaignScene(final String section) {
		switch (section.toLowerCase(Locale.ROOT)) {
		case "human":
			return "roc_human";
		case "undead":
			return "roc_undead";
		case "orc":
			return "roc_orc";
		case "nightelf":
			return "roc_nightelf";
		case "nightelfex":
			return "tft_nightelf";
		case "humanex":
			return "tft_human";
		case "undeadex":
			return "tft_undead";
		case "orcex":
			return "tft_orc";
		default:
			return "roc_prologue";
		}
	}

	/** Where each classic scene's model stands, as a part of the recording's width (measured from its frames). */
	public static float modelX(final String scene) {
		switch (scene) {
		case "roc_prologue":
			return 0.32f;
		case "roc_human":
			return 0.33f;
		case "roc_undead":
			return 0.40f;
		case "roc_orc":
			return 0.31f;
		case "roc_nightelf":
		case "tft_nightelf":
			return 0.34f;
		case "tft_human":
			return 0.26f;
		case "tft_undead":
			return 0.23f;
		case "tft_orc":
			return 0.15f;
		default:
			return 0.5f;
		}
	}

	/**
	 * Where to crop a recorded scene so that its model stands in the middle of a narrower frame: the model's x (as a
	 * part of the recording's width) moved as far as the recording allows.
	 */
	public static float modelFocus(final float modelX, final float frameAspect, final float sceneAspect) {
		final float visible = frameAspect / sceneAspect;
		if (visible >= 1) {
			return 0.5f;
		}
		return Math.max(0, Math.min(1, (modelX - (visible / 2)) / (1 - visible)));
	}

	/** A Reforged mission picture of the game data: a jpg in a campaign's own folder (lower case path). */
	public static boolean missionPicture(final String lowerPath) {
		// The credits and crests lie right in the campaign folder.
		return lowerPath.startsWith(SkinArt.MISSION_FOLDER) && lowerPath.endsWith(".jpg")
				&& (lowerPath.indexOf('\\', SkinArt.MISSION_FOLDER.length()) >= 0);
	}

	/**
	 * The Reforged picture of each mission, by the words of its name: the campaign's folder is the one most names
	 * point to, then each mission takes the best match in it. The pictures are keyed by their path below the mission
	 * folder; the values come back as given.
	 */
	public static <T> List<T> missionPictures(final List<String> missionNames, final Map<String, T> pictures) {
		final List<T> found = new ArrayList<>();
		for (int i = 0; i < missionNames.size(); i++) {
			found.add(null);
		}
		if (pictures.isEmpty()) {
			return found;
		}
		final List<List<String>> names = new ArrayList<>();
		for (final String mission : missionNames) {
			names.add(words(mission));
		}
		final Map<String, Integer> votes = new HashMap<>();
		for (final List<String> name : names) {
			final String best = bestPicture(name, pictures.keySet(), null);
			if (best != null) {
				votes.merge(best.substring(0, best.lastIndexOf('\\') + 1), 1, Integer::sum);
			}
		}
		String folder = null;
		for (final Map.Entry<String, Integer> vote : votes.entrySet()) {
			if ((folder == null) || (vote.getValue() > votes.get(folder))) {
				folder = vote.getKey();
			}
		}
		if (folder == null) {
			return found;
		}
		for (int i = 0; i < found.size(); i++) {
			final String best = bestPicture(names.get(i), pictures.keySet(), folder);
			found.set(i, best == null ? null : pictures.get(best));
		}
		return found;
	}

	/** The Reforged picture of a mission by its name alone, or null; keyed as {@link #missionPictures}. */
	public static <T> T missionPicture(final String missionName, final Map<String, T> pictures) {
		final String best = bestPicture(words(missionName), pictures.keySet(), null);
		return best == null ? null : pictures.get(best);
	}

	private static String bestPicture(final List<String> name, final Collection<String> paths, final String folder) {
		String best = null;
		int bestScore = 0;
		for (final String path : paths) {
			if ((folder != null) && !(path.startsWith(folder) && (path.indexOf('\\', folder.length()) < 0))) {
				continue;
			}
			final List<String> file = words(path.substring(path.lastIndexOf('\\') + 1));
			int score = 0;
			for (final String word : name) {
				if (file.contains(word)) {
					score++;
				}
			}
			if (score > bestScore) {
				best = path;
				bestScore = score;
			}
		}
		return best;
	}

	private static List<String> words(final String text) {
		final List<String> words = new ArrayList<>();
		for (final String word : text.toLowerCase(Locale.ROOT).replace(".jpg", "").split("[^a-z]+")) {
			if ((word.length() >= 3) && !"the".equals(word) && !"and".equals(word)) {
				words.add(word);
			}
		}
		return words;
	}

	/** The folder of a map of the list, below Maps, with forward slashes; "" for the top. */
	public static String mapFolder(final String path) {
		String folder = path.replace('\\', '/');
		final int slash = folder.lastIndexOf('/');
		folder = slash < 0 ? "" : folder.substring(0, slash);
		if (folder.regionMatches(true, 0, "maps", 0, 4) && ((folder.length() == 4) || (folder.charAt(4) == '/'))) {
			folder = folder.substring(Math.min(folder.length(), 5));
		}
		return folder;
	}

	/** The folder the list shows: the one opened last while it still has maps, else the chosen map's. */
	public static String shownMapFolder(final String kept, final String chosen, final List<GameCatalog.MapEntry> maps) {
		final String wanted = kept != null ? kept : chosen != null ? mapFolder(chosen) : "";
		if (wanted.isEmpty()) {
			return "";
		}
		for (final GameCatalog.MapEntry entry : maps) {
			final String folder = mapFolder(entry.path);
			if (folder.equalsIgnoreCase(wanted) || folder.regionMatches(true, 0, wanted + "/", 0, wanted.length() + 1)) {
				return wanted;
			}
		}
		return "";
	}

	/** The game's own folders as the original list names them; a lower-cased one of the data gets a capital. */
	public static String folderName(final String name) {
		switch (name) {
		case "frozenthrone":
			return "FrozenThrone";
		case "scenario":
			return "Scenario";
		case "download":
			return "Download";
		default:
			return "..".equals(name) || !name.equals(name.toLowerCase(Locale.ROOT)) ? name
					: Character.toUpperCase(name.charAt(0)) + name.substring(1);
		}
	}

	/** The folders right below a folder of the map list, by their path, in name order. */
	public static List<String> childFolders(final List<GameCatalog.MapEntry> maps, final String folder) {
		final String prefix = folder.isEmpty() ? "" : folder + "/";
		final TreeMap<String, String> folders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (final GameCatalog.MapEntry entry : maps) {
			final String mapFolder = mapFolder(entry.path);
			if ((mapFolder.length() > prefix.length()) && mapFolder.regionMatches(true, 0, prefix, 0, prefix.length())) {
				final int end = mapFolder.indexOf('/', prefix.length());
				final String child = end < 0 ? mapFolder : mapFolder.substring(0, end);
				folders.put(child, child);
			}
		}
		return new ArrayList<>(folders.values());
	}

	/** How many players a map has: its user and computer slots. */
	public static int players(final GameCatalog.MapDetails details) {
		int players = 0;
		for (final GameCatalog.Slot slot : details.slots) {
			if ((slot.type == 1) || (slot.type == 2)) {
				players++;
			}
		}
		return players;
	}

	/** The chosen map while the list has it, else the first. */
	public static String chosenMap(final String stored, final List<GameCatalog.MapEntry> maps) {
		for (final GameCatalog.MapEntry entry : maps) {
			if (entry.path.equals(stored)) {
				return stored;
			}
		}
		return maps.isEmpty() ? null : maps.get(0).path;
	}

	/** A game time as the load page shows it. */
	public static String elapsed(final float seconds) {
		final int total = Math.max(0, (int) seconds);
		return String.format(Locale.ROOT, "%02d:%02d:%02d", total / 3600, (total / 60) % 60, total % 60);
	}

	/** A campaign header without its " Campaign" ending, as the profile page lists it. */
	public static String shortCampaign(final CharSequence header) {
		final String text = header.toString();
		return text.endsWith(" Campaign") ? text.substring(0, text.length() - " Campaign".length()) : text;
	}
}
