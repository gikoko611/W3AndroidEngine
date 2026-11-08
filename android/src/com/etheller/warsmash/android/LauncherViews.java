package com.etheller.warsmash.android;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.etheller.warsmash.launcher.LauncherBoard;
import com.etheller.warsmash.launcher.SkinArt;

/**
 * The views the launcher's pages are made of: placed in the design's pixels of the activity's board and drawn
 * with the skin's art it prepared.
 */
abstract class LauncherViews {
	static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
	static final int ERROR = Color.rgb(0xE0, 0x56, 0x4E);

	final LauncherActivity activity;

	LauncherViews(final LauncherActivity activity) {
		this.activity = activity;
	}

	String getString(final int id) {
		return this.activity.getString(id);
	}

	String getString(final int id, final Object... formatArgs) {
		return this.activity.getString(id, formatArgs);
	}

	int px(final float design) {
		return this.activity.px(design);
	}

	/** A tappable view that clicks like the glue buttons: big for the main menu choices, the mouse click else. */
	static void onTap(final View view, final boolean main, final View.OnClickListener listener) {
		view.setOnClickListener(v -> {
			if (LauncherKit.clicks != null) {
				LauncherKit.clicks.play(main);
			}
			listener.onClick(v);
		});
	}

	void place(final FrameLayout parent, final View view, final float x, final float y, final float w,
			final float h) {
		final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				w < 0 ? ViewGroup.LayoutParams.WRAP_CONTENT : px(w), h < 0 ? ViewGroup.LayoutParams.WRAP_CONTENT : px(h));
		params.leftMargin = px(x);
		params.topMargin = px(y);
		parent.addView(view, params);
	}

	TextView text(final CharSequence value, final float size, final int color) {
		final TextView view = new TextView(this.activity);
		view.setText(value);
		view.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * this.activity.scale);
		view.setTextColor(color);
		view.setTypeface(this.activity.art != null ? this.activity.art.font() : null);
		view.setIncludeFontPadding(false);
		view.setSingleLine(true);
		view.setEllipsize(TextUtils.TruncateAt.END);
		view.setShadowLayer(1, size * this.activity.scale * 0.07f, size * this.activity.scale * 0.07f, Color.BLACK);
		return view;
	}

	/** A few lines of text that wrap within their width. */
	TextView lines(final CharSequence value, final float size, final int color, final int maxLines) {
		final TextView view = text(value, size, color);
		view.setSingleLine(false);
		view.setMaxLines(maxLines);
		view.setLineSpacing(0, 1.15f);
		return view;
	}

	LauncherKit.GlueButton button(final CharSequence label, final float height, final float textSize,
			final View.OnClickListener listener) {
		final LauncherKit.GlueButton button = new LauncherKit.GlueButton(this.activity, this.activity.art, px(height),
				textSize * this.activity.scale);
		button.setText(label);
		button.setOnClickListener(listener);
		return button;
	}

	String glue(final String key, final String fallback) {
		return this.activity.art.string(key, fallback);
	}

	/** A vertical or horizontal gradient with stops, which GradientDrawable cannot place. */
	static Drawable gradient(final boolean vertical, final int[] colors, final float[] stops) {
		final PaintDrawable drawable = new PaintDrawable();
		drawable.setShape(new RectShape());
		drawable.setShaderFactory(new ShapeDrawable.ShaderFactory() {
			@Override
			public Shader resize(final int width, final int height) {
				return new LinearGradient(0, 0, vertical ? 0 : width, vertical ? height : 0, colors, stops,
						Shader.TileMode.CLAMP);
			}
		});
		return drawable;
	}

	Drawable panelFrame(final float border, final int fill) {
		return new LauncherKit.FrameDrawable(this.activity.art.bitmap(SkinArt.PANEL_BORDER), px(border)).filled(fill)
				.fillInset(px(border * 0.35f)).fallback(Color.argb(0xEB, 0x03, 0x04, 0x0A))
				.line(this.activity.art.line());
	}

	FrameLayout.LayoutParams frameParams(final float x, final float y, final float w, final float h) {
		final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(px(w), px(h));
		params.leftMargin = px(x);
		params.topMargin = px(y);
		return params;
	}

	/** A row of tab buttons over the page content, as wide as six would be; the chosen one stays pressed. */
	float addTabs(final FrameLayout content, final int[] labels, final int selected,
			final IntConsumer open) {
		final LauncherBoard b = this.activity.board;
		final float tabW = (b.contentW - (5 * b.tabGap)) / 6;
		for (int i = 0; i < labels.length; i++) {
			final int tab = i;
			final LauncherKit.GlueButton button = button(getString(labels[i]), b.tabH, b.tabText,
					v -> open.accept(tab));
			button.setSelected(i == selected);
			place(content, button, i * (tabW + b.tabGap), 0, tabW, b.tabH);
		}
		return b.tabH + (b.tablet ? 40 : 14);
	}

	FrameLayout content(final FrameLayout view, final float extraHeight) {
		final LauncherBoard b = this.activity.board;
		final FrameLayout content = new FrameLayout(this.activity);
		place(view, content, b.contentX, b.contentY, b.contentW, b.contentH + extraHeight);
		return content;
	}

	/** The page's frame while the catalog reads what the page lists. */
	int loadingPage(final int rail, final int kind) {
		final FrameLayout view = this.activity.homePage.shell(rail);
		final FrameLayout content = content(view, 0);
		this.activity.pageNote = text(getString(R.string.game_loading_list), this.activity.board.body,
				this.activity.art.text2());
		place(content, this.activity.pageNote, 0, 0, this.activity.board.contentW, -1);
		this.activity.setPage(view, kind);
		return this.activity.generation;
	}

	void unreadable() {
		if (this.activity.pageNote != null) {
			this.activity.pageNote.setTextColor(ERROR);
			this.activity.pageNote.setText(R.string.game_unreadable);
		}
	}

	/** A panel in the skin's frame at a place of the content. */
	FrameLayout framed(final FrameLayout parent, final float x, final float y, final float w, final float h) {
		final FrameLayout panel = new FrameLayout(this.activity);
		panel.setBackground(panelFrame(this.activity.board.pageFrame,
				this.activity.art.reforged() ? Color.argb(0xDB, 0x0E, 0x0C, 0x0A) : Color.argb(0xC7, 0, 0, 0)));
		place(parent, panel, x, y, w, h);
		return panel;
	}

	/** The edit box frame of the difficulty box and the drop-down menus. */
	Drawable editFrame(final float border) {
		final LauncherKit.FrameDrawable frame = new LauncherKit.FrameDrawable(
				this.activity.art.bitmap(SkinArt.EDIT_BORDER), px(border))
						.tiled(this.activity.art.bitmap(SkinArt.EDIT_BACKGROUND),
								px(this.activity.board.tablet ? 110 : 60))
						.fillInset(px(border * 0.4f)).fallback(Color.argb(0xCC, 0x05, 0x06, 0x0C))
						.line(this.activity.art.line());
		return this.activity.art.reforged() ? frame.centre() : frame;
	}

	LinearLayout scrollList(final FrameLayout parent, final float x, final float y, final float w,
			final float h) {
		final ScrollView scroll = new ScrollView(this.activity);
		scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
		scroll.setVerticalScrollBarEnabled(true);
		final LinearLayout rows = new LinearLayout(this.activity);
		rows.setOrientation(LinearLayout.VERTICAL);
		scroll.addView(rows, new FrameLayout.LayoutParams(MATCH, ViewGroup.LayoutParams.WRAP_CONTENT));
		place(parent, scroll, x, y, w, h);
		return rows;
	}

	/** Scrolls a list so that its chosen row is in view. */
	static void reveal(final View row) {
		row.post(() -> {
			final ScrollView scroll = (ScrollView) row.getParent().getParent();
			final int visible = scroll.getHeight();
			if ((row.getTop() < scroll.getScrollY()) || ((row.getBottom()) > (scroll.getScrollY() + visible))) {
				scroll.scrollTo(0, Math.max(0, row.getTop() - ((visible - row.getHeight()) / 2)));
			}
		});
	}

	/** The chosen row of a list carries the skin's row highlight. */
	void markRow(final View row, final boolean chosen) {
		if (!chosen) {
			row.setBackground(null);
			return;
		}
		final Bitmap glow = this.activity.art.bitmap(SkinArt.ROW_HIGHLIGHT);
		row.setBackground(glow != null ? new BitmapDrawable(this.activity.getResources(), glow)
				: new ColorDrawable(Color.argb(0x40, 0xFC, 0xD3, 0x12)));
	}

	ImageView image(final String name, final ImageView.ScaleType scaleType) {
		final ImageView view = new ImageView(this.activity);
		view.setScaleType(scaleType);
		view.setImageBitmap(this.activity.art.bitmap(name));
		return view;
	}

	/** The action bar: 0 is Back, 1 the secondary place, 2 the main action. */
	LauncherKit.GlueButton action(final FrameLayout view, final int slot, final CharSequence label,
			final View.OnClickListener listener) {
		final LauncherBoard b = this.activity.board;
		final float[] place = b.actionSlot(slot);
		final float x = place[0];
		final float w = place[1];
		final LauncherKit.GlueButton button = button(label, b.backH, b.tablet ? 36 : 30, listener);
		button.setMain(slot == 2);
		if (slot == 2) {
			button.setTone(LauncherKit.GlueButton.TONE_CONFIRM);
		}
		place(view, button, x, b.backY, w, b.backH);
		return button;
	}

	CharSequence glueLabel(final String key, final String fallback) {
		return LauncherKit.glueText(glue(key, fallback));
	}

	String[] glueList(final String[][] table, final int keyColumn) {
		final String[] labels = new String[table.length];
		for (int i = 0; i < table.length; i++) {
			labels[i] = glue(table[i][keyColumn], table[i][keyColumn + 1]);
		}
		return labels;
	}

	/** A drop-down menu as the glue screens have it: the glue button, the chosen item and the pull-down arrow. */
	View dropdown(final String[] items, final int selected, final boolean enabled, final float height,
			final float textSize, final IntConsumer chosen) {
		return dropdown(items, selected, enabled, height, textSize, chosen, null);
	}

	/** @param colors with them the menu shows a colour (a player colour menu), its items named in their colour */
	View dropdown(final String[] items, final int selected, final boolean enabled, final float height,
			final float textSize, final IntConsumer chosen, final int[] colors) {
		final FrameLayout box = new FrameLayout(this.activity);
		box.setBackground(LauncherKit.GlueButton.backdrop(this.activity.art, px(height),
				LauncherKit.GlueButton.TONE_PLAIN));
		// Inside the button's rim, as the button's own text sits.
		final float rim = this.activity.art.reforged() ? Math.max(8, height * 0.3f) : Math.max(4, height * 0.22f);
		final float arrowBox = Math.min(height, 72);
		final float arrowSize = arrowBox * 0.42f;
		final TextView label = text(LauncherKit.glueText(items[selected]), textSize,
				enabled ? LauncherKit.GlueButton.restText(this.activity.art) : LauncherKit.DISABLED);
		label.setGravity(Gravity.CENTER_VERTICAL);
		final FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(MATCH, MATCH);
		labelParams.leftMargin = px(rim + 4);
		labelParams.rightMargin = px(Math.max(rim + 4, arrowBox * 0.8f));
		box.addView(label, labelParams);
		final View swatch = colors != null ? new View(this.activity) : null;
		if (swatch != null) {
			// The colour menu shows only the colour, in the middle of the button, without the arrow.
			label.setVisibility(View.GONE);
			swatch.setBackgroundColor(colors[selected]);
			swatch.setAlpha(enabled ? 1f : 0.5f);
			box.addView(swatch, new FrameLayout.LayoutParams(px(height * 0.9f),
					px(Math.max(8, (height - (2 * rim)) * 0.55f)), Gravity.CENTER));
		}
		else {
			final ImageView arrow = image(SkinArt.PULLDOWN_ARROW, ImageView.ScaleType.FIT_CENTER);
			arrow.setAlpha(enabled ? 1f : 0.4f);
			final FrameLayout.LayoutParams arrowParams = new FrameLayout.LayoutParams(px(arrowSize), px(arrowSize),
					Gravity.END | Gravity.CENTER_VERTICAL);
			arrowParams.rightMargin = px((arrowBox * 0.75f) - arrowSize);
			box.addView(arrow, arrowParams);
		}
		box.setEnabled(enabled);
		box.setContentDescription(LauncherKit.glueText(items[selected]).toString());
		if (enabled && (chosen != null)) {
			final int[] current = { selected };
			onTap(box, false,
					v -> this.activity.dialogs.showChoices(box, items, current[0], textSize, colors, choice -> {
						current[0] = choice;
						label.setText(LauncherKit.glueText(items[choice]));
						if (swatch != null) {
							swatch.setBackgroundColor(colors[choice]);
						}
						box.setContentDescription(LauncherKit.glueText(items[choice]).toString());
						chosen.accept(choice);
					}));
		}
		return box;
	}

	/** A check box row: the whole row is the touch target. */
	View checkLine(final CharSequence label, final boolean value, final float height, final float size,
			final Consumer<Boolean> changed) {
		final LinearLayout row = new LinearLayout(this.activity);
		row.setGravity(Gravity.CENTER_VERTICAL);
		final LauncherKit.CheckDrawable check = new LauncherKit.CheckDrawable(this.activity.art);
		check.setChecked(value);
		final View box = new View(this.activity);
		box.setBackground(check);
		final float boxSize = Math.min(height * 0.6f, this.activity.board.control);
		row.addView(box, new LinearLayout.LayoutParams(px(boxSize), px(boxSize)));
		final TextView name = text(label, size, this.activity.art.text());
		final LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0,
				ViewGroup.LayoutParams.WRAP_CONTENT, 1);
		nameParams.leftMargin = px(this.activity.board.tablet ? 18 : 12);
		row.addView(name, nameParams);
		final boolean[] on = { value };
		onTap(row, false, v -> {
			on[0] = !on[0];
			check.setChecked(on[0]);
			changed.accept(on[0]);
		});
		row.setContentDescription(label.toString());
		return row;
	}

	/** The minimap's frame: a black border with a grey line round it. */
	Drawable minimapFrame(final float border) {
		final GradientDrawable frame = new GradientDrawable();
		frame.setColor(Color.BLACK);
		frame.setStroke(Math.max(1, px(3)), Color.rgb(0x55, 0x55, 0x55));
		return frame;
	}

	/** A fact of Map Info: the gold label on the left, its value on the right; returns the value. */
	TextView fact(final FrameLayout parent, final String label, final String value, final float x,
			final float y, final float w, final float size) {
		place(parent, text(LauncherKit.glueText(label), size, this.activity.art.gold()), x, y, w * 0.5f, -1);
		final TextView shown = text(value, size, this.activity.art.text());
		shown.setGravity(Gravity.END);
		place(parent, shown, x + (w * 0.4f), y, w * 0.6f, -1);
		return shown;
	}

	/** The original edit box with a profile name: one line, at most the name's length. */
	EditText nameField(final float size) {
		final EditText field = new EditText(this.activity);
		field.setBackground(editFrame(this.activity.board.tablet ? 14 : 8));
		field.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * this.activity.scale);
		field.setTextColor(this.activity.art.text());
		field.setHintTextColor(LauncherKit.DISABLED);
		field.setHint(glue("NAME", getString(R.string.profile_name)));
		field.setTypeface(this.activity.art.font());
		field.setSingleLine(true);
		field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
		field.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
		field.setFilters(new InputFilter[] { new InputFilter.LengthFilter(Profiles.MAX_NAME) });
		final int pad = px(this.activity.board.tablet ? 24 : 14);
		field.setPadding(pad, 0, pad, 0);
		field.setGravity(Gravity.CENTER_VERTICAL);
		return field;
	}

	/** Runs the check given after every change of the field's text. */
	static void afterTextChanged(final EditText field, final Runnable check) {
		field.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(final CharSequence text, final int start, final int count, final int after) {
			}

			@Override
			public void onTextChanged(final CharSequence text, final int start, final int before, final int count) {
			}

			@Override
			public void afterTextChanged(final Editable text) {
				check.run();
			}
		});
	}

	/** The keyboard's Done taps the confirming button while it is enabled. */
	static void doneTaps(final EditText field, final View confirm) {
		field.setOnEditorActionListener((v, action, event) -> {
			if ((action == EditorInfo.IME_ACTION_DONE) && confirm.isEnabled()) {
				confirm.performClick();
			}
			return false;
		});
	}
}
