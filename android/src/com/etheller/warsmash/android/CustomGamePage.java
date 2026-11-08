package com.etheller.warsmash.android;

import static com.etheller.warsmash.launcher.CustomGame.COLOR_NAMES;
import static com.etheller.warsmash.launcher.CustomGame.OPTION_SHOWN;
import static com.etheller.warsmash.launcher.CustomGame.PLAYER_COLORS;
import static com.etheller.warsmash.launcher.CustomGame.RACES;
import static com.etheller.warsmash.launcher.CustomGame.SLOT_HIDDEN;
import static com.etheller.warsmash.launcher.CustomGame.SLOT_TYPES;
import static com.etheller.warsmash.launcher.CustomGame.SLOT_YOU;
import static com.etheller.warsmash.launcher.LauncherKeys.KEY_LAUNCH_RACE;
import static com.etheller.warsmash.launcher.LauncherKeys.KEY_MAP;
import static com.etheller.warsmash.launcher.LauncherKeys.KEY_MAP_FOLDER;
import static com.etheller.warsmash.launcher.LauncherKeys.KEY_OPTIONS;

import java.util.ArrayList;
import java.util.List;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.etheller.warsmash.launcher.CampaignChoice;
import com.etheller.warsmash.launcher.CustomGame;
import com.etheller.warsmash.launcher.LauncherBoard;
import com.etheller.warsmash.launcher.SkinArt;

/** Custom Game: the maps of the game data, the chosen map's slots and its advanced options. */
final class CustomGamePage extends LauncherViews {
	private List<GameCatalog.MapEntry> maps;
	private GameCatalog.MapDetails mapDetails;
	/** The custom game's choices for the shown map. */
	private final CustomGame lobby = new CustomGame();

	private FrameLayout customRight;
	private LauncherKit.GlueButton customStart;
	private View customAdvanced;

	CustomGamePage(final LauncherActivity activity) {
		super(activity);
	}

	private String chosenMap() {
		return CampaignChoice.chosenMap(this.activity.preferences.getString(KEY_MAP, null), this.maps);
	}

	private boolean[] advancedOptions() {
		return CustomGame.advancedOptions(this.activity.preferences.getString(KEY_OPTIONS, null));
	}

	void showCustom() {
		if (this.maps == null) {
			final int generation = loadingPage(LauncherActivity.RAIL_CUSTOM, LauncherActivity.PAGE_CUSTOM);
			this.activity.catalog.maps(found -> {
				if (generation != this.activity.generation) {
					return;
				}
				if (found == null) {
					unreadable();
					return;
				}
				this.maps = found;
				showCustom();
			});
			return;
		}
		final LauncherBoard b = this.activity.board;
		final boolean t = b.tablet;
		this.lobby.advanced = advancedOptions();
		final FrameLayout view = this.activity.homePage.shell(LauncherActivity.RAIL_CUSTOM);
		final FrameLayout content = content(view, 0);
		final float listW = t ? 620 : 420;
		final float rowH = t ? 112 : 96;
		final float frame = b.pageInset;
		final FrameLayout listPanel = framed(content, 0, 0, listW, b.contentH);
		final float rowW = listW - (2 * frame);
		final LinearLayout rows = scrollList(listPanel, frame, frame, rowW, b.contentH - (2 * frame));
		final String chosen = chosenMap();
		final String folder = shownMapFolder(chosen);
		final String prefix = folder.isEmpty() ? "" : folder + "/";
		// As in the original list: the way up, the folders, then the maps of the folder shown.
		if (!folder.isEmpty()) {
			final int up = folder.lastIndexOf('/');
			rows.addView(folderRow("..", rowW, rowH, up < 0 ? "" : folder.substring(0, up)),
					new LinearLayout.LayoutParams(MATCH, px(rowH)));
		}
		for (final String child : CampaignChoice.childFolders(this.maps, folder)) {
			rows.addView(folderRow(child.substring(prefix.length()), rowW, rowH, child),
					new LinearLayout.LayoutParams(MATCH, px(rowH)));
		}
		final List<View> mapRows = new ArrayList<>();
		View chosenRow = null;
		for (final GameCatalog.MapEntry entry : this.maps) {
			if (!CampaignChoice.mapFolder(entry.path).equalsIgnoreCase(folder)) {
				continue;
			}
			final View row = mapRow(entry, rowW, rowH);
			onTap(row, false, v -> {
				for (final View other : mapRows) {
					markRow(other, other == v);
				}
				this.activity.preferences.edit().putString(KEY_MAP, entry.path).apply();
				loadMapDetails(entry.path);
			});
			markRow(row, entry.path.equals(chosen));
			if (entry.path.equals(chosen)) {
				chosenRow = row;
			}
			mapRows.add(row);
			rows.addView(row, new LinearLayout.LayoutParams(MATCH, px(rowH)));
		}
		if (chosenRow != null) {
			reveal(chosenRow);
		}
		else if (this.maps.isEmpty()) {
			place(listPanel, text(getString(R.string.game_no_maps), b.body, this.activity.art.text2()), frame, frame,
					rowW, -1);
		}
		final float rightX = t ? 656 : 430;
		this.customRight = new FrameLayout(this.activity);
		place(content, this.customRight, rightX, 0, b.contentW - rightX, b.contentH);
		action(view, 0, glueLabel("CANCEL", "Cancel"), v -> this.activity.homePage.showHome());
		this.customAdvanced = t ? null
				: action(view, 1, getString(R.string.game_advanced), v -> showAdvancedSheet());
		if (this.customAdvanced != null) {
			this.customAdvanced.setEnabled(false);
		}
		this.customStart = action(view, 2, glueLabel("KEY_START_GAME", "Start Game"), v -> startCustom());
		this.customStart.setEnabled(false);
		this.activity.setPage(view, LauncherActivity.PAGE_CUSTOM);
		if (chosen != null) {
			loadMapDetails(chosen);
		}
	}

	/** The folder the list shows: the one opened last while it still has maps, else the chosen map's. */
	private String shownMapFolder(final String chosen) {
		return CampaignChoice.shownMapFolder(this.activity.preferences.getString(KEY_MAP_FOLDER, null), chosen,
				this.maps);
	}

	/** A folder of the list (icon-folder and its name); tapping it shows its maps. */
	private View folderRow(final String name, final float w, final float rowH, final String opens) {
		final boolean t = this.activity.board.tablet;
		final LinearLayout row = new LinearLayout(this.activity);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setPadding(px(6), 0, px(8), 0);
		final float line = Math.min(rowH * 0.86f, 84);
		final float icon = line * 0.82f;
		row.addView(image(SkinArt.ICON_FOLDER, ImageView.ScaleType.FIT_CENTER),
				new LinearLayout.LayoutParams(px(icon), px(icon)));
		final String shown = CampaignChoice.folderName(name);
		final TextView title = text(shown, line * 0.56f, this.activity.art.text());
		final LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0,
				ViewGroup.LayoutParams.WRAP_CONTENT, 1);
		titleParams.leftMargin = px(18);
		row.addView(title, titleParams);
		row.setContentDescription(shown);
		onTap(row, false, v -> {
			this.activity.preferences.edit().putString(KEY_MAP_FOLDER, opens).apply();
			showCustom();
		});
		return row;
	}

	/** A map of the list: its kind of map, the number of players and the name, cut to the list's width. */
	private View mapRow(final GameCatalog.MapEntry entry, final float w, final float rowH) {
		final boolean t = this.activity.board.tablet;
		final LinearLayout row = new LinearLayout(this.activity);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setPadding(px(6), 0, px(8), 0);
		final float line = Math.min(rowH * 0.86f, 84);
		final float icon = line * 0.82f;
		// The number of players stands in the map icon, as the original list draws it.
		final FrameLayout mark = new FrameLayout(this.activity);
		mark.addView(image(entry.melee ? SkinArt.ICON_MELEE : SkinArt.ICON_UMS, ImageView.ScaleType.FIT_CENTER),
				new FrameLayout.LayoutParams(MATCH, MATCH));
		final TextView players = text(Integer.toString(entry.players), icon * 0.5f, this.activity.art.gold());
		players.setGravity(Gravity.CENTER);
		mark.addView(players, new FrameLayout.LayoutParams(MATCH, MATCH));
		row.addView(mark, new LinearLayout.LayoutParams(px(icon), px(icon)));
		final CharSequence name = LauncherKit.glueText(entry.label);
		final TextView title = text(name, line * 0.56f, this.activity.art.text());
		final LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0,
				ViewGroup.LayoutParams.WRAP_CONTENT, 1);
		titleParams.leftMargin = px(18);
		row.addView(title, titleParams);
		row.setContentDescription(name);
		return row;
	}

	private void loadMapDetails(final String path) {
		this.mapDetails = null;
		this.customStart.setEnabled(false);
		if (this.customAdvanced != null) {
			this.customAdvanced.setEnabled(false);
		}
		this.customRight.removeAllViews();
		place(this.customRight, text(getString(R.string.game_loading_list), this.activity.board.body,
				this.activity.art.text2()), 0, 0,
				-1, -1);
		final int generation = this.activity.generation;
		this.activity.catalog.mapDetails(path, details -> {
			if ((generation != this.activity.generation) || !path.equals(chosenMap())) {
				return;
			}
			this.customRight.removeAllViews();
			if (details == null) {
				final TextView failed = text(getString(R.string.game_unreadable), this.activity.board.body, ERROR);
				place(this.customRight, failed, 0, 0, -1, -1);
				return;
			}
			this.mapDetails = details;
			this.lobby.chooseSlots(details);
			fillMapDetails(details);
			this.customStart.setEnabled(true);
			if (this.customAdvanced != null) {
				this.customAdvanced.setEnabled(true);
			}
		});
	}

	private void fillMapDetails(final GameCatalog.MapDetails details) {
		final LauncherBoard b = this.activity.board;
		final boolean t = b.tablet;
		final FrameLayout right = this.customRight;
		final float frame = b.pageInset;
		final float mapW = t ? 1008 : 738;
		final float mapH = t ? 620 : 120;
		final FrameLayout map = framed(right, 0, 0, mapW, mapH);
		final float picture = t ? 400 : mapH - (2 * frame);
		final ImageView minimap = new ImageView(this.activity);
		minimap.setScaleType(ImageView.ScaleType.FIT_XY);
		minimap.setBackground(minimapFrame(t ? 8 : 4));
		final int rim = px(t ? 8 : 4);
		minimap.setPadding(rim, rim, rim, rim);
		minimap.setImageBitmap(details.minimap != null ? GameCatalog.bitmap(details.minimap)
				: this.activity.art.bitmap(SkinArt.MINIMAP_UNKNOWN));
		minimap.setContentDescription(LauncherKit.glueText(details.name).toString());
		place(map, minimap, frame, frame, picture, picture);
		final float factsX = frame + picture + (t ? 36 : 14);
		final float factsW = mapW - factsX - frame;
		final CharSequence suggested = glueLabel("COLON_SUGGESTED_PLAYERS", "Suggested Players:");
		final CharSequence size = glueLabel("COLON_MAP_SIZE", "Map Size:");
		final CharSequence tileset = glueLabel("COLON_TILESET", "Tileset:");
		if (t) {
			place(map, text(mapTitle(details), 42, this.activity.art.text()), factsX, frame, factsW, -1);
			final CharSequence[][] facts = { { suggested, LauncherKit.glueText(details.suggested) },
				{ size, details.size }, { tileset, details.tileset } };
			final float labelW = 270;
			for (int i = 0; i < facts.length; i++) {
				final float y = frame + 76 + (i * 46);
				place(map, text(facts[i][0], 30, this.activity.art.gold()), factsX, y, labelW, -1);
				place(map, text(facts[i][1], 30, this.activity.art.text()), factsX + labelW + 10, y,
						factsW - labelW - 10, -1);
			}
			final float descriptionY = frame + 76 + (3 * 46) + 20;
			final TextView description = lines(LauncherKit.glueText(details.description), 27, this.activity.art.text2(),
					6);
			place(map, description, factsX, descriptionY, factsW, mapH - frame - descriptionY);
			addAdvanced(right, 1044, 0, 500, 620);
		}
		else {
			place(map, text(mapTitle(details), 28, this.activity.art.text()), factsX, frame - 2, factsW, -1);
			final String line = LauncherKit.glueText(details.suggested) + " · " + details.size + " · " + details.tileset;
			place(map, text(line, 20, this.activity.art.text()), factsX, frame + 44, factsW, -1);
		}
		final float slotsY = t ? 656 : 130;
		final float slotsW = t ? 1544 : 738;
		final float slotsH = b.contentH - slotsY;
		final FrameLayout slots = framed(right, 0, slotsY, slotsW, slotsH);
		final float rowW = slotsW - (2 * frame);
		final float headerH = t ? 56 : 0;
		if (t) {
			final float[] columns = slotColumns(rowW);
			final String[] headers = { glue("PLAYER", "Player"), glue("COLON_RACE", "Race"), glue("TEAM", "Team"),
				glue("COLOR", "Color") };
			for (int i = 0; i < headers.length; i++) {
				place(slots, text(LauncherKit.glueText(headers[i]).toString().replace(":", ""), 28,
						this.activity.art.gold()),
						frame + columns[2 * i] + 8, frame - 4, columns[(2 * i) + 1], -1);
			}
		}
		final LinearLayout rows = scrollList(slots, frame, frame + headerH, rowW, slotsH - (2 * frame) - headerH);
		for (int i = 0; i < details.slots.size(); i++) {
			if (this.lobby.slotTypes[i] != SLOT_HIDDEN) {
				rows.addView(slotRow(details, i, rowW), new LinearLayout.LayoutParams(MATCH, px(slotRowH())));
			}
		}
	}

	/** The map's name after its number of players, as the original list shows it. */
	private CharSequence mapTitle(final GameCatalog.MapDetails details) {
		final android.text.SpannableStringBuilder title = new android.text.SpannableStringBuilder();
		final int players = CampaignChoice.players(details);
		if (players > 0) {
			title.append(Integer.toString(players),
					new android.text.style.ForegroundColorSpan(this.activity.art.gold()),
					android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE).append(' ');
		}
		return title.append(LauncherKit.glueText(details.name));
	}

	private float slotRowH() {
		return this.activity.board.tablet ? 154 : 96;
	}

	/** Where the slot columns start and how wide they are: player, race, team and colour, as 40 : 24 : 15 : 18. */
	private float[] slotColumns(final float w) {
		final boolean t = this.activity.board.tablet;
		final float gap = t ? 16 : 6;
		final float player = w * (t ? 0.40f : 0.38f);
		final float race = w * (t ? 0.24f : 0.28f);
		final float team = w * 0.15f;
		final float color = w - player - race - team - (3 * gap);
		return new float[] { 0, player, player + gap, race, player + race + (2 * gap), team,
			player + race + team + (3 * gap), color };
	}

	/** A player slot: who plays it, the race, the team and the colour; fixed settings cannot change. */
	private View slotRow(final GameCatalog.MapDetails details, final int index, final float w) {
		final boolean t = this.activity.board.tablet;
		final GameCatalog.Slot slot = details.slots.get(index);
		final float rowH = slotRowH();
		final float menuH = t ? 130 : 88;
		final float textSize = t ? 38 : 22;
		final float[] columns = slotColumns(w);
		final FrameLayout row = new FrameLayout(this.activity);
		final float menuY = t ? 0 : (rowH - menuH) / 2;
		final boolean editable = !CustomGame.fixedSlots(details);
		if (this.lobby.slotTypes[index] == SLOT_YOU) {
			// The player's own slot shows the profile's name; it cannot be changed.
			place(row, dropdown(new String[] { GameCatalog.currentProfile(this.activity) }, 0, true, menuH, textSize,
					null),
					columns[0], menuY, columns[1], menuH);
		}
		else {
			place(row, dropdown(glueList(SLOT_TYPES, 1), this.lobby.slotTypes[index], editable, menuH, textSize,
					choice -> this.lobby.slotTypes[index] = choice), columns[0], menuY, columns[1], menuH);
		}
		place(row, dropdown(glueList(RACES, 0), this.lobby.slotRaces[index], editable, menuH, textSize,
				choice -> this.lobby.slotRaces[index] = choice), columns[2], menuY, columns[3], menuH);
		final String[] teams = teamNames(details);
		final int team = this.lobby.slotTeams[index] >= 0 ? this.lobby.slotTeams[index]
				: CustomGame.defaultTeam(details, index);
		place(row, dropdown(teams, Math.min(team, teams.length - 1), editable && !details.fixedSettings, menuH,
				textSize, choice -> this.lobby.slotTeams[index] = choice), columns[4], menuY, columns[5], menuH);
		final String[] colors = glueList(COLOR_NAMES, 0);
		final int color = this.lobby.colorOf(details, index);
		place(row, dropdown(colors, color, editable, menuH, textSize, choice -> this.lobby.slotColors[index] = choice,
				PLAYER_COLORS), columns[6], menuY, columns[7], menuH);
		return row;
	}

	/** The teams a slot can join: the map's forces when it sets them, else one team per slot of the lobby. */
	private String[] teamNames(final GameCatalog.MapDetails details) {
		if (details.customForces && !details.forces.isEmpty()) {
			final String[] names = new String[details.forces.size()];
			for (int i = 0; i < names.length; i++) {
				names[i] = LauncherKit.glueText(details.forces.get(i)).toString();
			}
			return names;
		}
		final String[] names = new String[details.slots.size()];
		for (int i = 0; i < names.length; i++) {
			names[i] = Integer.toString(i + 1);
		}
		return names;
	}

	private CharSequence[] optionLabels() {
		return new CharSequence[] { glueLabel("LOCK_TEAMS", "Lock Teams"),
			glueLabel("FULL_SHARED_UNIT_CONTROL", getString(R.string.game_shared_control)),
			glueLabel("RANDOM_RACES", "Random Races"), glueLabel("RANDOM_HERO", "Random Hero") };
	}

	/**
	 * The tablet's Advanced Options beside the map: the title, a check box per option and the Observers menu, which
	 * a single player game keeps at none as the original does.
	 */
	private void addAdvanced(final FrameLayout parent, final float x, final float y, final float w, final float h) {
		final float frame = this.activity.board.pageInset;
		final FrameLayout panel = framed(parent, x, y, w, h);
		place(panel, text(glueLabel("ADVANCED_OPTIONS", "Advanced Options"), 34, this.activity.art.gold()), frame,
				frame - 4,
				w - (2 * frame), -1);
		final CharSequence[] labels = optionLabels();
		final float step = 82;
		// Check boxes of 56 px every 82 px, as the design has them; the row is the larger touch target.
		final float top = frame + 37;
		for (int i = 0; i < labels.length; i++) {
			final int option = OPTION_SHOWN[i];
			place(panel, checkLine(labels[i], this.lobby.advanced[option], 94, 27, on -> setOption(option, on)), frame,
					top + (i * step), w - (2 * frame), 94);
		}
		final float observersY = top + 19 + (labels.length * step) + 6;
		place(panel, text(glueLabel("COLON_OBSERVERS", "Observers:"), 27, this.activity.art.gold()), frame, observersY,
				w - (2 * frame), -1);
		place(panel, dropdown(new String[] { glue("NONE", "None") }, 0, true, 80, 33, null), frame,
				observersY + 40, w - (2 * frame), 80);
	}

	private void setOption(final int option, final boolean on) {
		this.activity.preferences.edit().putString(KEY_OPTIONS, this.lobby.setOption(option, on)).apply();
	}

	/** The phone's Advanced Options: a sheet over the page, two columns of full height rows. */
	private void showAdvancedSheet() {
		final FrameLayout box = this.activity.dialogs.dialogBox(1120, 600);
		if (box == null) {
			return;
		}
		final float edge = 32;
		place(box, text(glueLabel("ADVANCED_OPTIONS", "Advanced Options"), 30, this.activity.art.gold()), edge + 16,
				edge + 8,
				1120 - (2 * edge) - 32, -1);
		final CharSequence[] labels = optionLabels();
		final float columnW = (1120 - (2 * edge) - 48) / 2;
		for (int i = 0; i < labels.length; i++) {
			final int option = i;
			place(box, checkLine(labels[i], this.lobby.advanced[OPTION_SHOWN[i]], 96, 24,
					on -> setOption(OPTION_SHOWN[option], on)),
					edge + 16 + ((i % 2) * (columnW + 16)), edge + 70 + ((i / 2) * 96), columnW, 96);
		}
		place(box, button(glueLabel("OK", getString(R.string.game_ok)), 96, 30,
				v -> this.activity.dialogs.closeDialog()), 1120 - edge - 320,
				600 - edge - 96, 320, 96);
	}

	private void startCustom() {
		final GameCatalog.MapDetails details = this.mapDetails;
		if (details == null) {
			return;
		}
		this.activity.preferences.edit().putInt(KEY_LAUNCH_RACE, this.lobby.playerRace()).apply();
		this.activity.launchGame(this.lobby.request(details), LauncherActivity.PAGE_CUSTOM);
	}
}
