package com.etheller.warsmash.android;

import static com.etheller.warsmash.launcher.LauncherKeys.CAMPAIGN_MAPS;
import static com.etheller.warsmash.launcher.LauncherKeys.DIFFICULTIES;
import static com.etheller.warsmash.launcher.LauncherKeys.KEY_CAMPAIGN;
import static com.etheller.warsmash.launcher.LauncherKeys.KEY_DIFFICULTY;
import static com.etheller.warsmash.launcher.LauncherKeys.KEY_MISSION;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.etheller.warsmash.launcher.CampaignChoice;
import com.etheller.warsmash.launcher.LaunchRequest;
import com.etheller.warsmash.launcher.LauncherBoard;
import com.etheller.warsmash.launcher.SkinArt;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CCampaignProgress;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.menu.CampaignCatalog;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.menu.CampaignMenuData;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.menu.CampaignMission;

/** The Campaign page with its difficulty box, and the Missions of the campaign chosen. */
final class CampaignPage extends LauncherViews {
	CampaignPage(final LauncherActivity activity) {
		super(activity);
	}

	private boolean campaignsReady(final int kind) {
		if (this.activity.campaigns != null) {
			return true;
		}
		final int generation = loadingPage(LauncherActivity.RAIL_CAMPAIGN, kind);
		this.activity.catalog.campaigns(found -> {
			if (generation != this.activity.generation) {
				return;
			}
			if (found == null) {
				unreadable();
				return;
			}
			this.activity.campaigns = found;
			this.activity.showPage(kind);
		});
		return false;
	}

	/** The campaign last chosen, else the one the profile's last mission chose, else the first open one. */
	private CampaignCatalog.Entry chosenCampaign() {
		return CampaignChoice.chosenCampaign(this.activity.campaigns.catalog, this.activity.campaigns.progress,
				this.activity.preferences.contains(KEY_CAMPAIGN) ? this.activity.preferences.getInt(KEY_CAMPAIGN, 0)
				: null);
	}

	/** The campaign's scene, its model at 22 % from the left, darkening to the right where the list is. */
	private void sceneBehind(final FrameLayout view, final CampaignCatalog.Entry entry) {
		final String scene = entry != null ? CampaignChoice.campaignScene(entry.getSection()) : "roc_prologue";
		final float focus = 0.22f;
		final Bitmap still = this.activity.art.poster(scene);
		final Drawable drawable = still != null ? new LauncherKit.CoverDrawable(still, focus, 0)
				: this.activity.homePage.sceneStill();
		final LauncherBoard b = this.activity.board;
		view.addView(this.activity.sceneBackdrop(drawable, scene, focus, LauncherActivity.LAYER_PAGE,
				entry != null ? entry.getData().getBackground() : "TutorialBackdrop", null,
				entry != null ? entry.getData().getBackgroundFogSettings() : null, 0, 0, 0, b.width, b.height, -1), 0,
				new FrameLayout.LayoutParams(MATCH, MATCH));
		final View shade = new View(this.activity);
		shade.setBackground(gradient(false, new int[] { Color.TRANSPARENT, Color.TRANSPARENT,
			Color.argb(0xC7, 0, 0, 0) }, new float[] { 0, 0.4f, 1 }));
		view.addView(shade, 1, new FrameLayout.LayoutParams(MATCH, MATCH));
	}

	private float campaignListX() {
		return this.activity.board.campaignListX();
	}

	private float campaignListW() {
		return this.activity.board.tablet ? 924 : 600;
	}

	/** On the phone the campaign pages reach the bottom of the screen. */
	private float campaignExtra() {
		return this.activity.board.tablet ? 0 : 108;
	}

	private float campaignRowH() {
		return this.activity.board.tablet ? 136 : 96;
	}

	/** A row of the campaign screens: the arrow button, the small gold header and the name. */
	private View campaignRow(final String header, final String name, final boolean chosen,
			final View.OnClickListener listener) {
		final float rowH = campaignRowH();
		final float w = campaignListW();
		final FrameLayout row = new FrameLayout(this.activity);
		// The design's row: the button takes 86 % of the row, the header and name follow at fixed fractions of it;
		// 15 % of the text size stands for the space the page's text boxes keep above the letters.
		final float arrow = rowH * 0.86f;
		place(row, image(SkinArt.CAMPAIGN_ARROW, ImageView.ScaleType.FIT_CENTER), 0, (rowH - arrow) / 2, arrow,
				arrow);
		final float textX = arrow + (rowH * 0.14f);
		final float headerSize = rowH * 0.27f;
		final float nameSize = rowH * 0.42f;
		final CharSequence headerText = glueLabel(header, header);
		final CharSequence nameText = glueLabel(name, name);
		place(row, text(headerText, headerSize, chosen ? this.activity.art.text() : this.activity.art.gold()), textX,
				(rowH * 0.04f) + (headerSize * 0.15f), w - textX - 8, -1);
		final TextView title = text(nameText, nameSize, chosen ? this.activity.art.gold() : this.activity.art.text());
		if (chosen) {
			title.setShadowLayer(nameSize * this.activity.scale * 0.35f, 0, 0, this.activity.art.gold());
		}
		place(row, title, textX, (rowH * 0.36f) + (nameSize * 0.15f), w - textX - 8, -1);
		onTap(row, false, listener);
		row.setContentDescription(headerText + " " + nameText);
		return row;
	}

	void showCampaign() {
		if (!campaignsReady(LauncherActivity.PAGE_CAMPAIGN)) {
			return;
		}
		final LauncherBoard b = this.activity.board;
		final CampaignCatalog catalog = this.activity.campaigns.catalog;
		final CCampaignProgress.Snapshot progress = this.activity.campaigns.progress;
		final CampaignCatalog.Entry chosen = chosenCampaign();
		final FrameLayout view = this.activity.homePage.shell(LauncherActivity.RAIL_CAMPAIGN, false);
		sceneBehind(view, chosen);
		final FrameLayout content = content(view, campaignExtra());
		final LinearLayout rows = scrollList(content, campaignListX(), 0, campaignListW(), b.contentH + campaignExtra());
		View chosenRow = null;
		boolean gap = true;
		int number = 0;
		for (final Object data : catalog.getDatas()) {
			if (data == null) {
				// A blank entry of the list is a gap between groups of campaigns.
				if (!gap) {
					rows.addView(new View(this.activity), new LinearLayout.LayoutParams(1, px(campaignRowH() / 2)));
					gap = true;
				}
				continue;
			}
			final CampaignCatalog.Entry entry = catalog.getEntry(number++);
			if (!CampaignCatalog.isOpen(entry, progress)) {
				continue;
			}
			final View row = campaignRow(entry.getData().getHeader(), entry.getData().getName(), entry == chosen,
					v -> {
						if (this.activity.preferences.getInt(KEY_CAMPAIGN, -1) != entry.getNumber()) {
							this.activity.preferences.edit().putInt(KEY_CAMPAIGN, entry.getNumber()).remove(KEY_MISSION)
									.apply();
						}
						showMissions();
					});
			rows.addView(row, new LinearLayout.LayoutParams(MATCH, px(campaignRowH())));
			gap = false;
			if (entry == chosen) {
				chosenRow = row;
			}
		}
		if (chosenRow == null) {
			place(content, text(getString(R.string.game_no_campaigns), b.body, this.activity.art.text2()),
					campaignListX(), 0,
					campaignListW(), -1);
		}
		else {
			reveal(chosenRow);
		}
		addDifficulty(content);
		this.activity.setPage(view, LauncherActivity.PAGE_CAMPAIGN);
	}

	/**
	 * The difficulty box at the bottom left: the label and its menu in one line, the description under them; with
	 * the Reforged maps in the data, the campaign version's menu in a box of its own above it.
	 */
	private void addDifficulty(final FrameLayout content) {
		final boolean t = this.activity.board.tablet;
		final boolean versions = (this.activity.campaigns != null) && this.activity.campaigns.catalog.hasReforgedMaps();
		final float edge = t ? 38 : 14;
		final float pad = t ? 24 : 12;
		final float inner = edge + pad;
		final float menuH = t ? 98 : 96;
		final float menuW = t ? 400 : 230;
		final float labelSize = t ? 36 : 24;
		final float w = t ? 820 : 508;
		final float h = t ? 340 : 233;
		final float y = t ? 1092 : 349;
		final FrameLayout box = new FrameLayout(this.activity);
		box.setBackground(editFrame(edge));
		place(content, box, 0, y, w, h);
		final TextView label = text(glueLabel("SELECT_DIFFICULTY_LEVEL", "Difficulty Level:"), labelSize,
				this.activity.art.gold());
		label.setGravity(Gravity.CENTER_VERTICAL);
		place(box, label, inner, inner, w - (2 * inner) - menuW - 12, menuH);
		final String current = this.activity.preferences.getString(KEY_DIFFICULTY, DIFFICULTIES[0][0]);
		int selected = 0;
		for (int i = 0; i < DIFFICULTIES.length; i++) {
			if (DIFFICULTIES[i][0].equals(current)) {
				selected = i;
			}
		}
		place(box, dropdown(glueList(DIFFICULTIES, 1), selected, true, menuH, t ? 34 : 26,
				choice -> this.activity.preferences.edit().putString(KEY_DIFFICULTY, DIFFICULTIES[choice][0]).apply()),
				w - inner - menuW, inner, menuW, menuH);
		place(box, lines(glueLabel("DIFFICULTY_TEXT", ""), t ? 26 : 19, this.activity.art.text(), 3), inner,
				inner + menuH + (t ? 16 : 10), w - (2 * inner), -1);
		if (!versions) {
			return;
		}
		// The version box stands above the difficulty box, which keeps its place in the design.
		final float versionH = (2 * inner) + menuH;
		final FrameLayout versionBox = new FrameLayout(this.activity);
		versionBox.setBackground(editFrame(edge));
		place(content, versionBox, 0, y - versionH - (t ? 24 : 8), w, versionH);
		final TextView versionLabel = text(glueLabel("CAMPAIGN_VERSION", getString(R.string.launcher_campaign_maps)),
				labelSize, this.activity.art.gold());
		versionLabel.setGravity(Gravity.CENTER_VERTICAL);
		// "Campaign Version:" is longer than the difficulty's label; its menu is narrower.
		final float versionMenuW = t ? 320 : menuW;
		place(versionBox, versionLabel, inner, inner, w - (2 * inner) - versionMenuW - 12, menuH);
		final String profile = this.activity.profiles.current();
		final int version = LaunchContract.CAMPAIGN_MAPS_CLASSIC.equals(this.activity.profiles.campaignMaps(profile))
				? 1 : 0;
		place(versionBox, dropdown(glueList(CAMPAIGN_MAPS, 1), version, true, menuH, t ? 34 : 26,
				choice -> this.activity.profiles.setCampaignMaps(profile, CAMPAIGN_MAPS[choice][0])),
				w - inner - versionMenuW,
				inner, versionMenuW, menuH);
	}

	void showMissions() {
		if (!campaignsReady(LauncherActivity.PAGE_MISSIONS)) {
			return;
		}
		final CampaignCatalog.Entry entry = chosenCampaign();
		if (entry == null) {
			showCampaign();
			return;
		}
		final LauncherBoard b = this.activity.board;
		final boolean t = b.tablet;
		final CCampaignProgress.Snapshot progress = this.activity.campaigns.progress;
		final List<CampaignMission> missions = entry.getData().getMissions();
		// The mission last played, else the furthest one open: after a victory the one it opened.
		int chosen = this.activity.preferences.getInt(KEY_MISSION, -1);
		if ((chosen < 0) || (chosen >= missions.size()) || !CampaignCatalog.isMissionOpen(entry, chosen, progress)) {
			chosen = 0;
			for (int i = 0; i < missions.size(); i++) {
				if (CampaignCatalog.isMissionOpen(entry, i, progress)) {
					chosen = i;
				}
			}
		}
		final FrameLayout view = this.activity.homePage.shell(LauncherActivity.RAIL_CAMPAIGN, false);
		sceneBehind(view, entry);
		final FrameLayout content = content(view, campaignExtra());
		final LinearLayout rows = scrollList(content, campaignListX(), 0, campaignListW(), b.contentH + campaignExtra());
		// Reforged rows: the mission's own loading picture in the campaign button frame.
		final String[] pictures = this.activity.art.reforged() && this.activity.art.has(SkinArt.MISSION_BORDER)
				? missionPictures(missions)
				: null;
		if (pictures != null) {
			rows.setPadding(0, px(t ? 16 : 8), 0, 0);
		}
		View chosenRow = null;
		// The cinematics the campaign has, as the original list shows them: the introduction and the opening once
		// open, before the missions, the ending after them once won.
		final CampaignMenuData data = entry.getData();
		cinematicRow(rows, data.getIntroCinematic(), pictures != null);
		if (progress.isOpeningCinematicAvailable(entry.getNumber())) {
			cinematicRow(rows, data.getOpenCinematic(), pictures != null);
		}
		for (int i = 0; i < missions.size(); i++) {
			if (!CampaignCatalog.isMissionOpen(entry, i, progress)) {
				continue;
			}
			final int mission = i;
			final View.OnClickListener launch = v -> launchMission(entry, mission, LauncherActivity.PAGE_MISSIONS);
			final View row;
			final float rowH;
			if (pictures != null) {
				final boolean won = i < (missions.size() - 1) ? CampaignCatalog.isMissionOpen(entry, i + 1, progress)
						: progress.isEndingCinematicAvailable(entry.getNumber());
				row = missionRow(missions.get(i).getHeader(), missions.get(i).getMissionName(), i == chosen, won,
						pictures[i], launch);
				rowH = missionRowH();
			}
			else {
				row = campaignRow(missions.get(i).getHeader(), missions.get(i).getMissionName(), i == chosen, launch);
				rowH = campaignRowH();
			}
			rows.addView(row, new LinearLayout.LayoutParams(MATCH, px(rowH)));
			if (i == chosen) {
				chosenRow = row;
			}
		}
		if (progress.isEndingCinematicAvailable(entry.getNumber())) {
			cinematicRow(rows, data.getEndCinematic(), pictures != null);
		}
		if (chosenRow != null) {
			reveal(chosenRow);
		}
		// The campaign's header and name centred under the scene, left of the list.
		final float nameSize = t ? 72 : 40;
		final float headerSize = t ? 40 : 24;
		final float nameY = t ? b.contentH - 20 - (nameSize * 1.2f) : 380;
		final float nameW = campaignListX() - (t ? 60 : 24);
		final TextView header = text(glueLabel(entry.getData().getHeader(), ""), headerSize, this.activity.art.gold());
		header.setGravity(Gravity.CENTER_HORIZONTAL);
		place(content, header, 0, t ? b.contentH - 110 - (headerSize * 1.2f) : nameY - headerSize - 8, nameW, -1);
		final TextView name = text(glueLabel(entry.getData().getName(), ""), nameSize, this.activity.art.text());
		name.setGravity(Gravity.CENTER_HORIZONTAL);
		place(content, name, 0, nameY, nameW, -1);
		action(view, 0, glueLabel("BACK", getString(R.string.launcher_back)), v -> showCampaign());
		this.activity.setPage(view, LauncherActivity.PAGE_MISSIONS);
	}

	/** A row that plays a campaign cinematic's movies, when the game data has them. */
	private void cinematicRow(final LinearLayout rows, final CampaignMission cinematic, final boolean pictures) {
		if ((cinematic == null) || !this.activity.campaigns.hasMovie(cinematic)) {
			return;
		}
		final View.OnClickListener play = v -> playMovies(cinematic.getMovies());
		final View row = pictures
				? missionRow(cinematic.getHeader(), cinematic.getMissionName(), false, false, null, play)
				: campaignRow(cinematic.getHeader(), cinematic.getMissionName(), false, play);
		rows.addView(row, new LinearLayout.LayoutParams(MATCH, px(pictures ? missionRowH() : campaignRowH())));
	}

	private void playMovies(final List<String> names) {
		final int width = this.activity.getResources().getDisplayMetrics().widthPixels;
		this.activity.catalog.movies(names, width, movies -> {
			if ((movies == null) || movies.isEmpty() || this.activity.isFinishing()) {
				Log.w(LauncherActivity.TAG, "No movie to play for " + names);
				return;
			}
			this.activity.startActivity(MovieActivity.intent(this.activity, movies));
			this.activity.overridePendingTransition(0, 0);
		});
	}

	private float missionRowH() {
		return this.activity.board.tablet ? 146 : 102;
	}

	/**
	 * A Reforged mission row: the mission's picture in the campaign button frame (the hover frame when chosen), the
	 * check mark once won, then the header and the name.
	 */
	private View missionRow(final String header, final String name, final boolean chosen, final boolean won,
			final String picture, final View.OnClickListener listener) {
		final boolean t = this.activity.board.tablet;
		final float rowH = missionRowH();
		final float gap = t ? 10 : 6;
		final float frame = rowH - gap - 8;
		final float x = t ? 16 : 8;
		final float y = 4;
		final FrameLayout row = new FrameLayout(this.activity);
		final ImageView art = new ImageView(this.activity);
		art.setScaleType(ImageView.ScaleType.FIT_XY);
		final Bitmap full = picture != null ? this.activity.art.bitmap(picture) : null;
		if (full != null) {
			// The pictures are wide; the frame shows a square of them a little left of the middle.
			final int side = Math.min(full.getWidth(), full.getHeight());
			final int left = Math.round((full.getWidth() - side) * 0.18f);
			art.setImageBitmap(Bitmap.createBitmap(full, left, 0, side, side));
		}
		else {
			art.setBackgroundColor(Color.rgb(0x11, 0x11, 0x11));
		}
		place(row, art, x + (frame * 0.07f), y + (frame * 0.07f), frame * 0.86f, frame * 0.86f);
		if (full == null) {
			place(row, image(SkinArt.CAMPAIGN_ARROW, ImageView.ScaleType.FIT_CENTER), x + (frame * 0.28f),
					y + (frame * 0.28f), frame * 0.44f, frame * 0.44f);
		}
		place(row, image(chosen ? SkinArt.MISSION_BORDER_HOVER : SkinArt.MISSION_BORDER,
				ImageView.ScaleType.FIT_XY), x, y, frame, frame);
		if (won) {
			place(row, image(SkinArt.MISSION_CHECK, ImageView.ScaleType.FIT_CENTER), x + (frame * 0.62f),
					frame * 0.62f, frame * 0.42f, frame * 0.42f);
		}
		final float textX = x + frame + (t ? 24 : 14);
		final float w = campaignListW() - textX - 8;
		final float rh = rowH - gap;
		final CharSequence headerText = glueLabel(header, header);
		final CharSequence nameText = glueLabel(name, name);
		place(row, text(headerText, rh * 0.2f, chosen ? this.activity.art.text() : this.activity.art.gold()), textX,
				y + (rh * 0.16f),
				w, -1);
		final TextView title = text(nameText, rh * 0.3f, chosen ? this.activity.art.gold() : this.activity.art.text());
		if (chosen) {
			title.setShadowLayer(rh * 0.3f * this.activity.scale * 0.4f, 0, 0, this.activity.art.gold());
		}
		place(row, title, textX, y + (rh * 0.42f), w, -1);
		onTap(row, false, listener);
		row.setContentDescription(headerText + " " + nameText);
		return row;
	}

	/**
	 * The cached picture of each mission, found by the words of its name in the picture's file name, within the
	 * campaign folder most of the missions match; null for a mission without one (the cinematics).
	 */
	private String[] missionPictures(final List<CampaignMission> missions) {
		final List<String> names = new ArrayList<>();
		for (final CampaignMission mission : missions) {
			names.add(glueLabel(mission.getMissionName(), mission.getMissionName()).toString());
		}
		return CampaignChoice.missionPictures(names, this.activity.art.missionPictures()).toArray(new String[0]);
	}

	void launchMission(final CampaignCatalog.Entry entry, final int mission, final int returnPage) {
		this.activity.preferences.edit().putInt(KEY_CAMPAIGN, entry.getNumber()).putInt(KEY_MISSION, mission).apply();
		this.activity.launchGame(LaunchRequest.mission(this.activity.campaigns.catalog, entry, mission,
				this.activity.profiles.campaignMaps(this.activity.profiles.current()),
				this.activity.preferences.getString(KEY_DIFFICULTY, DIFFICULTIES[0][0])), returnPage);
	}
}
