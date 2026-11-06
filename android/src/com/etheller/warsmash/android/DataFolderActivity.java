package com.etheller.warsmash.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Lets the player choose the folder with the game data before the engine starts, and writes the engine configuration
 * for it. Once a saved folder holds valid data the window is skipped and the launcher opens at once; the "Game data"
 * app shortcut (or the {@link #EXTRA_OPEN} extra) opens it again.
 */
public class DataFolderActivity extends Activity {
	static final String EXTRA_OPEN = "openLauncher";
	private static final String TAG = "Warsmash";
	private static final String PREFERENCES = "launcher";
	private static final String KEY_DATA_FOLDER = "dataFolder";
	private static final String INI_TEMPLATE = "warsmash-template.ini";
	private static final int REQUEST_FOLDER = 1;

	private static final int GOLD = Color.rgb(0xE8, 0xC7, 0x6A);
	private static final int BRONZE = Color.rgb(0x73, 0x59, 0x0A);
	private static final int BRONZE_INNER = Color.rgb(0x46, 0x41, 0x2D);
	private static final int TEXT = Color.rgb(0xEC, 0xE3, 0xCF);
	private static final int TEXT_DIM = Color.rgb(0xA9, 0xA1, 0x90);
	private static final int DISABLED = Color.rgb(0x5C, 0x5A, 0x52);
	private static final int GREEN = Color.rgb(0x8F, 0xD4, 0x9E);
	private static final int RED = Color.rgb(0xE2, 0x9A, 0x9A);

	private enum Status {
		WAITING, CHECKING, FOUND, MISSING, NEEDS_ACCESS, GONE, WRITE_FAILED
	}

	private enum Look {
		NEXT, NORMAL, DISABLED, IN_USE
	}

	/**
	 * Gate layout in design pixels: the tablet board is 2960 x 1848, the phone board 1600 x 720; the panel keeps these
	 * proportions on any screen.
	 */
	private static final class Board {
		final float width, height;
		final float panelW, panelH, inner;
		final float logo, logoY, title, hintY, hint;
		final float labelY, fieldY, fieldH, path;
		final float halfY, buttonH, half, gap, buttonText;
		final float statusY, statusH, icon, statusTitle, statusDetail, action;
		final float footY, foot, bottomY;

		Board(final boolean tablet) {
			if (tablet) {
				this.width = 2960;
				this.height = 1848;
				this.panelW = 1840;
				this.panelH = 1120;
				this.inner = 96;
				this.logo = 120;
				this.logoY = 64;
				this.title = 60;
				this.hintY = 142;
				this.hint = 28;
				this.labelY = 212;
				this.fieldY = 256;
				this.fieldH = 104;
				this.path = 34;
				this.halfY = 392;
				this.buttonH = 112;
				this.half = 808;
				this.gap = 32;
				this.buttonText = 36;
				this.statusY = 544;
				this.statusH = 232;
				this.icon = 88;
				this.statusTitle = 40;
				this.statusDetail = 28;
				this.action = 560;
				this.footY = 804;
				this.foot = 26;
				this.bottomY = 944;
			}
			else {
				this.width = 1600;
				this.height = 720;
				this.panelW = 1568;
				this.panelH = 688;
				this.inner = 40;
				this.logo = 64;
				this.logoY = 28;
				this.title = 40;
				this.hintY = 76;
				this.hint = 19;
				this.labelY = 110;
				this.fieldY = 140;
				this.fieldH = 80;
				this.path = 24;
				this.halfY = 236;
				this.buttonH = 96;
				this.half = 732;
				this.gap = 24;
				this.buttonText = 28;
				this.statusY = 348;
				this.statusH = 156;
				this.icon = 60;
				this.statusTitle = 28;
				this.statusDetail = 19;
				this.action = 420;
				this.footY = 516;
				this.foot = 18;
				this.bottomY = 568;
			}
		}
	}

	private File dataFolder;
	private File chosenFolder;
	private boolean writeFailed;
	private float scale;
	private TextView folderField;
	private Button chooseButton;
	private LinearLayout statusBox;
	private TextView statusIcon;
	private TextView statusText;
	private TextView statusDetail;
	private Button accessButton;
	private Button appFolderButton;
	private Button continueButton;
	private TextView footLine;
	private DataFolderCheck.Kind kind = DataFolderCheck.Kind.NONE;
	private int checks;
	private boolean appHasData;
	private String zip;
	private String description;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (AndroidLauncher.showRunningGame(this)) {
			finish();
			return;
		}
		final File appData = appDataFolder();
		final String saved = getSharedPreferences(PREFERENCES, MODE_PRIVATE).getString(KEY_DATA_FOLDER, null);
		this.dataFolder = saved != null ? new File(saved) : appData;
		if ((saved == null) && (DataFolderCheck.detect(appData) == DataFolderCheck.Kind.NONE)) {
			this.dataFolder = null;
		}
		this.chosenFolder = this.dataFolder;
		this.appHasData = DataFolderCheck.detect(appData) != DataFolderCheck.Kind.NONE;
		final boolean open = getIntent().getBooleanExtra(EXTRA_OPEN, false);
		if (!open && (this.dataFolder != null) && (saved != null)) {
			this.kind = DataFolderCheck.detect(this.dataFolder);
			if ((this.kind != DataFolderCheck.Kind.NONE) && start()) {
				return;
			}
		}
		setContentView(buildLayout());
		refresh();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (this.folderField != null) {
			refresh();
		}
	}

	private int px(final float design) {
		return Math.round(design * this.scale);
	}

	private int line(final float design) {
		return Math.max(1, px(design));
	}

	private static GradientDrawable box(final int fill, final int stroke, final int strokeWidth) {
		final GradientDrawable drawable = new GradientDrawable();
		drawable.setColor(fill);
		drawable.setStroke(strokeWidth, stroke);
		return drawable;
	}

	private TextView text(final String value, final float size, final int color, final Typeface face) {
		final TextView view = new TextView(this);
		view.setText(value);
		view.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * this.scale);
		view.setTextColor(color);
		view.setTypeface(face);
		view.setIncludeFontPadding(false);
		return view;
	}

	private Button button(final float size, final View.OnClickListener listener) {
		final Button button = new Button(this);
		button.setAllCaps(false);
		button.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * this.scale);
		button.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
		button.setLetterSpacing(0.04f);
		button.setStateListAnimator(null);
		button.setPadding(0, 0, 0, 0);
		button.setOnClickListener(listener);
		return button;
	}

	/** One blue button is always the next step; the others keep their place and only change colour. */
	private void look(final Button button, final Look look) {
		button.setEnabled((look == Look.NEXT) || (look == Look.NORMAL));
		switch (look) {
		case NEXT:
			final GradientDrawable blue = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
					new int[] { Color.rgb(0x14, 0x23, 0x9A), Color.rgb(0x00, 0x00, 0x39) });
			blue.setStroke(line(3), Color.rgb(0x2B, 0x3F, 0xBF));
			button.setBackground(blue);
			button.setTextColor(GOLD);
			break;
		case NORMAL:
			button.setBackground(box(Color.rgb(0x05, 0x06, 0x0C), BRONZE, line(3)));
			button.setTextColor(GOLD);
			break;
		case IN_USE:
			button.setBackground(box(Color.rgb(0x10, 0x24, 0x16), Color.rgb(0x2F, 0x5A, 0x3A), line(3)));
			button.setTextColor(GREEN);
			break;
		default:
			button.setBackground(box(Color.rgb(0x0A, 0x0B, 0x10), Color.rgb(0x2C, 0x2B, 0x27), line(3)));
			button.setTextColor(DISABLED);
			break;
		}
	}

	private void place(final FrameLayout parent, final View view, final float x, final float y, final float w,
			final float h) {
		final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				w < 0 ? ViewGroup.LayoutParams.WRAP_CONTENT : px(w), h < 0 ? ViewGroup.LayoutParams.WRAP_CONTENT : px(h));
		params.leftMargin = px(x);
		params.topMargin = px(y);
		parent.addView(view, params);
	}

	private View buildLayout() {
		final DisplayMetrics metrics = getResources().getDisplayMetrics();
		final float screenW = Math.max(metrics.widthPixels, metrics.heightPixels);
		final float screenH = Math.min(metrics.widthPixels, metrics.heightPixels);
		// Phones are much wider than they are high; tablets come close to 16:10.
		final Board board = new Board((screenW / screenH) < 1.9f);
		this.scale = Math.min(screenW / board.width, screenH / board.height);

		final FrameLayout root = new FrameLayout(this);
		final GradientDrawable background = new GradientDrawable();
		background.setGradientType(GradientDrawable.RADIAL_GRADIENT);
		background.setColors(new int[] { Color.rgb(0x12, 0x1A, 0x3A), Color.rgb(0x07, 0x0A, 0x18),
			Color.rgb(0x03, 0x04, 0x09) });
		background.setGradientRadius(screenW * 0.75f);
		background.setGradientCenter(0.5f, 0.3f);
		root.setBackground(background);

		final FrameLayout panel = new FrameLayout(this);
		final LayerDrawable frame = new LayerDrawable(new Drawable[] {
			box(Color.argb(0xEB, 0x03, 0x04, 0x0A), BRONZE, line(4)), box(Color.TRANSPARENT, BRONZE_INNER, line(2)) });
		frame.setLayerInset(1, px(10), px(10), px(10), px(10));
		panel.setBackground(frame);
		root.addView(panel, new FrameLayout.LayoutParams(px(board.panelW), px(board.panelH), Gravity.CENTER));

		final float x = board.inner;
		final float w = board.panelW - (2 * board.inner);
		final ImageView logo = new ImageView(this);
		logo.setImageResource(R.mipmap.ic_launcher);
		place(panel, logo, x, board.logoY, board.logo, board.logo);
		final float textX = x + board.logo + board.gap;
		final TextView title = text(getString(R.string.data_folder_title), board.title, GOLD,
				Typeface.create(Typeface.SERIF, Typeface.NORMAL));
		title.setLetterSpacing(0.06f);
		place(panel, title, textX, board.logoY, w - board.logo - board.gap, -1);
		final TextView hint = text(getString(R.string.data_folder_hint), board.hint, TEXT_DIM, Typeface.SANS_SERIF);
		hint.setSingleLine(true);
		hint.setEllipsize(TextUtils.TruncateAt.END);
		place(panel, hint, textX, board.hintY, w - board.logo - board.gap, -1);

		final TextView label = text(getString(R.string.data_folder_label).toUpperCase(Locale.ROOT), board.hint,
				TEXT_DIM, Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
		label.setLetterSpacing(0.12f);
		place(panel, label, x, board.labelY, w, -1);
		this.folderField = text("", board.path, TEXT, Typeface.MONOSPACE);
		this.folderField.setSingleLine(true);
		this.folderField.setEllipsize(TextUtils.TruncateAt.START);
		this.folderField.setGravity(Gravity.CENTER_VERTICAL);
		this.folderField.setPadding(px(board.gap), 0, px(board.gap), 0);
		this.folderField.setBackground(box(Color.rgb(0x0D, 0x10, 0x16), Color.rgb(0x2E, 0x36, 0x44), line(2)));
		this.folderField.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_folder, 0, 0, 0);
		this.folderField.setCompoundDrawablePadding(px(board.gap / 2));
		place(panel, this.folderField, x, board.fieldY, w, board.fieldH);

		this.chooseButton = button(board.buttonText, view -> chooseFolder());
		place(panel, this.chooseButton, x, board.halfY, board.half, board.buttonH);
		// Android 11+ folder pickers do not show Android/data, so the app folder cannot be picked again.
		this.appFolderButton = button(board.buttonText, view -> {
			this.dataFolder = appDataFolder();
			this.chosenFolder = this.dataFolder;
			this.writeFailed = false;
			refresh();
		});
		place(panel, this.appFolderButton, x + board.half + board.gap, board.halfY, board.half, board.buttonH);

		this.statusBox = new LinearLayout(this);
		this.statusBox.setOrientation(LinearLayout.HORIZONTAL);
		this.statusBox.setGravity(Gravity.CENTER_VERTICAL);
		this.statusBox.setPadding(px(board.gap), 0, px(board.gap), 0);
		this.statusIcon = text("", board.icon * 0.7f, TEXT_DIM, Typeface.DEFAULT_BOLD);
		this.statusIcon.setGravity(Gravity.CENTER);
		this.statusBox.addView(this.statusIcon, new LinearLayout.LayoutParams(px(board.icon), px(board.icon)));
		final LinearLayout lines = new LinearLayout(this);
		lines.setOrientation(LinearLayout.VERTICAL);
		lines.setPadding(px(board.gap), 0, px(board.gap), 0);
		this.statusText = text("", board.statusTitle, TEXT, Typeface.create(Typeface.SERIF, Typeface.BOLD));
		lines.addView(this.statusText);
		this.statusDetail = text("", board.statusDetail, TEXT_DIM, Typeface.SANS_SERIF);
		this.statusDetail.setMaxLines(2);
		this.statusDetail.setEllipsize(TextUtils.TruncateAt.END);
		final LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		detailParams.topMargin = px(board.statusDetail / 2);
		lines.addView(this.statusDetail, detailParams);
		this.statusBox.addView(lines, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
		this.accessButton = button(board.buttonText, view -> requestAllFilesAccess());
		this.accessButton.setText(R.string.data_folder_allow);
		this.statusBox.addView(this.accessButton, new LinearLayout.LayoutParams(px(board.action), px(board.buttonH)));
		place(panel, this.statusBox, x, board.statusY, w, board.statusH);

		this.footLine = text("", board.foot, TEXT_DIM, Typeface.SANS_SERIF);
		this.footLine.setSingleLine(true);
		this.footLine.setEllipsize(TextUtils.TruncateAt.END);
		place(panel, this.footLine, x, board.footY, w, -1);

		final TextView engine = text(getString(R.string.data_folder_engine, versionName()), board.foot, TEXT_DIM,
				Typeface.SANS_SERIF);
		engine.setGravity(Gravity.CENTER_VERTICAL);
		place(panel, engine, x, board.bottomY, w - board.action - board.gap, board.buttonH);
		this.continueButton = button(board.buttonText * 1.12f, view -> {
			if (this.writeFailed || (this.kind != DataFolderCheck.Kind.NONE)) {
				this.writeFailed = false;
				start();
			}
		});
		this.continueButton.setLetterSpacing(0.12f);
		place(panel, this.continueButton, (x + w) - board.action, board.bottomY, board.action, board.buttonH);
		return root;
	}

	private String versionName() {
		try {
			return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		}
		catch (final PackageManager.NameNotFoundException e) {
			return "";
		}
	}

	/** Searches the folder off the main thread; a large folder would otherwise freeze the window. */
	private void refresh() {
		final int generation = ++this.checks;
		final File folder = this.dataFolder;
		// Without access the files are hidden, and searching thousands of their folders only stalls the window.
		final boolean search = (folder != null) && folder.isDirectory()
				&& (!needsAllFilesAccess() || isInAppFolder(folder));
		if (search) {
			show(Status.CHECKING);
		}
		new Thread(() -> {
			File found = folder;
			if (search) {
				final File located = DataFolderCheck.locate(folder);
				if (located != null) {
					found = located;
				}
			}
			final DataFolderCheck.Kind foundKind = found == null ? DataFolderCheck.Kind.NONE
					: DataFolderCheck.detect(found);
			final boolean appHasData = DataFolderCheck.detect(appDataFolder()) != DataFolderCheck.Kind.NONE;
			final String zip = foundKind == DataFolderCheck.Kind.NONE ? DataFolderCheck.zipName(found) : null;
			String description = null;
			if (foundKind != DataFolderCheck.Kind.NONE) {
				description = DataFolderCheck.describe(foundKind, found) + " · "
						+ DataFolderCheck.checkedFile(foundKind);
				final String locale = DataFolderCheck.locale(foundKind, found);
				if (!locale.isEmpty()) {
					description += " · " + locale;
				}
			}
			final File result = found;
			final String details = description;
			runOnUiThread(() -> {
				if ((generation != this.checks) || isFinishing()) {
					return;
				}
				this.dataFolder = result;
				this.kind = foundKind;
				this.appHasData = appHasData;
				this.zip = zip;
				this.description = details;
				show(null);
			});
		}, "DataFolderCheck").start();
	}

	private void show(final Status checking) {
		final Status status;
		if (checking != null) {
			status = checking;
		}
		else if (this.dataFolder == null) {
			status = Status.WAITING;
		}
		else if (this.writeFailed) {
			status = Status.WRITE_FAILED;
		}
		else if (this.kind != DataFolderCheck.Kind.NONE) {
			status = Status.FOUND;
		}
		else if (needsAllFilesAccess() && !isInAppFolder(this.dataFolder)) {
			status = Status.NEEDS_ACCESS;
		}
		else if (!this.dataFolder.exists()) {
			status = Status.GONE;
		}
		else {
			status = Status.MISSING;
		}
		showFolder();
		final boolean appInUse = (status == Status.FOUND) && isInAppFolder(this.dataFolder);
		this.chooseButton.setText(this.dataFolder == null ? R.string.data_folder_choose : R.string.data_folder_change);
		look(this.chooseButton, (status == Status.WAITING) || (status == Status.MISSING) || (status == Status.GONE)
				? Look.NEXT
				: Look.NORMAL);
		this.appFolderButton.setText(appInUse ? R.string.data_folder_app_in_use
				: this.appHasData ? R.string.data_folder_use_app : R.string.data_folder_use_app_empty);
		look(this.appFolderButton, appInUse ? Look.IN_USE : this.appHasData ? Look.NORMAL : Look.DISABLED);
		this.accessButton.setVisibility(status == Status.NEEDS_ACCESS ? View.VISIBLE : View.GONE);
		look(this.accessButton, Look.NEXT);
		this.continueButton.setText(status == Status.WRITE_FAILED ? R.string.data_folder_retry
				: R.string.data_folder_continue);
		look(this.continueButton,
				(status == Status.FOUND) || (status == Status.WRITE_FAILED) ? Look.NEXT : Look.DISABLED);

		final int failFill = Color.rgb(0x2A, 0x14, 0x14);
		final int failStroke = Color.rgb(0x6A, 0x2E, 0x2E);
		switch (status) {
		case CHECKING:
			showStatus("↻", TEXT_DIM, Color.rgb(0x0B, 0x12, 0x30), Color.rgb(0x2B, 0x3F, 0xBF),
					getString(R.string.data_folder_checking), getString(R.string.data_folder_checking_detail));
			break;
		case FOUND:
			showStatus("✓", GREEN, Color.rgb(0x10, 0x22, 0x16), Color.rgb(0x2F, 0x5A, 0x3A),
					getString(this.kind == DataFolderCheck.Kind.CASC ? R.string.data_folder_casc
							: R.string.data_folder_extracted),
					this.description);
			break;
		case NEEDS_ACCESS:
			showStatus("!", GOLD, Color.rgb(0x2A, 0x20, 0x12), Color.rgb(0x6A, 0x54, 0x26),
					getString(R.string.data_folder_needs_access), getString(R.string.data_folder_needs_access_detail));
			break;
		case MISSING:
			showStatus("✗", RED, failFill, failStroke, getString(R.string.data_folder_missing),
					this.zip != null ? getString(R.string.data_folder_zip, this.zip)
							: getString(R.string.data_folder_missing_detail));
			break;
		case GONE:
			showStatus("✗", RED, failFill, failStroke, getString(R.string.data_folder_gone),
					getString(R.string.data_folder_gone_detail));
			break;
		case WRITE_FAILED:
			showStatus("✗", RED, failFill, failStroke, getString(R.string.data_folder_write_failed),
					getString(R.string.data_folder_write_failed_detail));
			break;
		default:
			showStatus("○", TEXT_DIM, Color.rgb(0x0B, 0x12, 0x30), Color.rgb(0x2B, 0x3F, 0xBF),
					getString(R.string.data_folder_waiting), getString(R.string.data_folder_waiting_detail));
			break;
		}
		this.footLine.setText(status == Status.FOUND ? R.string.data_folder_foot_ready
				: R.string.data_folder_foot_waiting);
	}

	private void showStatus(final String icon, final int color, final int fill, final int stroke, final String title,
			final String detail) {
		this.statusBox.setBackground(box(fill, stroke, line(2)));
		this.statusIcon.setText(icon);
		this.statusIcon.setTextColor(color);
		this.statusText.setText(title);
		this.statusText.setTextColor(color);
		this.statusDetail.setText(detail);
	}

	/** The chosen folder as the system names it, and in green the subfolder the data was found in. */
	private void showFolder() {
		if (this.dataFolder == null) {
			this.folderField.setText(R.string.data_folder_none);
			this.folderField.setTextColor(TEXT_DIM);
			return;
		}
		this.folderField.setTextColor(TEXT);
		final File chosen = this.chosenFolder != null ? this.chosenFolder : this.dataFolder;
		final String chosenPath = chosen.getAbsolutePath();
		final String foundPath = this.dataFolder.getAbsolutePath();
		if (!foundPath.startsWith(chosenPath + File.separator)) {
			this.folderField.setText(displayPath(this.dataFolder));
			return;
		}
		final SpannableStringBuilder shown = new SpannableStringBuilder(displayPath(chosen));
		final int start = shown.length();
		shown.append(" › ").append(foundPath.substring(chosenPath.length() + 1).replace("/", " › "));
		shown.setSpan(new ForegroundColorSpan(GREEN), start, shown.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		this.folderField.setText(shown);
	}

	/** Shows a folder as the system names it: "Internal storage / WC3" rather than the raw path. */
	private String displayPath(final File folder) {
		final String path = folder.getAbsolutePath();
		final String appFolder = getExternalFilesDir(null).getAbsolutePath();
		if (path.startsWith(appFolder)) {
			return "App folder" + path.substring(appFolder.length()).replace("/", " / ");
		}
		final String primary = Environment.getExternalStorageDirectory().getAbsolutePath();
		if (path.startsWith(primary)) {
			final String rest = path.substring(primary.length()).replace("/", " / ");
			return "Internal storage" + rest;
		}
		final String[] volume = path.split("/", 4);
		if ((volume.length == 4) && volume[1].equals("storage")) {
			return "SD card" + ("/" + volume[3]).replace("/", " / ");
		}
		return path;
	}

	private File appDataFolder() {
		return new File(getExternalFilesDir(null), "data");
	}

	/** The app's own folder is readable without "All files access". */
	private boolean isInAppFolder(final File folder) {
		final String appFolder = getExternalFilesDir(null).getAbsolutePath();
		final String path = folder.getAbsolutePath();
		return path.equals(appFolder) || path.startsWith(appFolder + File.separator);
	}

	private static boolean needsAllFilesAccess() {
		return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) && !Environment.isExternalStorageManager();
	}

	private void requestAllFilesAccess() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			final Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
					Uri.parse("package:" + getPackageName()));
			startActivity(intent);
		}
	}

	private void chooseFolder() {
		final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		startActivityForResult(intent, REQUEST_FOLDER);
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if ((requestCode != REQUEST_FOLDER) || (resultCode != RESULT_OK) || (data == null) || (data.getData() == null)) {
			return;
		}
		final File folder = folderOfTree(data.getData());
		if (folder != null) {
			this.dataFolder = folder;
			this.chosenFolder = folder;
			this.writeFailed = false;
			refresh();
		}
	}

	/**
	 * The engine reads its data through plain files, so a picked tree is turned back into the path it stands for:
	 * "primary:Games/WC3" is internal storage, "1234-ABCD:WC3" a removable card.
	 */
	static File folderOfTree(final Uri treeUri) {
		final String documentId = DocumentsContract.getTreeDocumentId(treeUri);
		final int colon = documentId.indexOf(':');
		if (colon < 0) {
			return null;
		}
		final String volume = documentId.substring(0, colon);
		final String relative = documentId.substring(colon + 1);
		final File base = "primary".equalsIgnoreCase(volume) ? Environment.getExternalStorageDirectory()
				: new File("/storage/" + volume);
		return relative.isEmpty() ? base : new File(base, relative);
	}

	private boolean start() {
		final File appFolder = getExternalFilesDir(null);
		try {
			EngineResources.ensureCurrent(this);
			// The engine folder is listed as a data source only if it exists; scripts and maps are placed there later.
			new File(appFolder, "engine").mkdirs();
			writeIni(new File(appFolder, AndroidLauncher.INI_FILE_NAME), DataFolderCheck.dataSourcesSection(this.kind,
					this.dataFolder, EngineResources.folder(this), new File(appFolder, "engine")));
		}
		catch (final IOException e) {
			Log.e(TAG, "Unable to write the engine configuration", e);
			this.writeFailed = true;
			if (this.folderField != null) {
				refresh();
			}
			return false;
		}
		final SharedPreferences.Editor editor = getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit();
		editor.putString(KEY_DATA_FOLDER, this.dataFolder.getAbsolutePath());
		editor.apply();

		final Bundle extras = getIntent().getExtras();
		// A map asked for by name starts at once; otherwise the launcher opens.
		final boolean map = (extras != null) && extras.containsKey(AndroidLauncher.EXTRA_LOAD_FILE);
		final Intent intent = new Intent(this, map ? AndroidLauncher.class : LauncherActivity.class);
		if (extras != null) {
			intent.putExtras(extras);
			intent.removeExtra(EXTRA_OPEN);
		}
		startActivity(intent);
		finish();
		return true;
	}

	private void writeIni(final File iniFile, final String dataSources) throws IOException {
		// The touch thresholds are the player's own (Options › Touch) and outlive a change of game data.
		final String touch = EngineSettings.touchSection(iniFile);
		try (InputStream template = getAssets().open(INI_TEMPLATE);
				OutputStream out = new FileOutputStream(iniFile)) {
			out.write(dataSources.getBytes(StandardCharsets.UTF_8));
			out.write('\n');
			final byte[] buffer = new byte[4096];
			int read;
			while ((read = template.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
			if (!touch.isEmpty()) {
				out.write(("\n" + touch).getBytes(StandardCharsets.UTF_8));
			}
		}
	}
}
