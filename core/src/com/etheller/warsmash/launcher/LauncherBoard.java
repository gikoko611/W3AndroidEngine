package com.etheller.warsmash.launcher;

/**
 * The launcher's drawing board in design pixels: the tablet board is 2960 x 1848, the phone board 1600 x 720. A
 * device screen shows the whole board scaled to fit; a desktop window instead stretches the board to its own shape
 * at the chosen interface scale, so the rail stays on the left and the pages take the width the window gives.
 */
public final class LauncherBoard {
	public static final int UI_MIN = 80;
	public static final int UI_MAX = 150;
	/** Above this interface scale the tablet layout no longer fits a window and the compact one takes over. */
	public static final int UI_TABLET_MAX = 115;

	public final boolean tablet;
	/** A window board: its size follows the window rather than a device board. */
	public final boolean window;
	/** Screen pixels per design pixel. */
	public final float scale;
	public final float width, height;
	public final float railW, logoX, logoY, logoW, logoH;
	public final float holderX, holderY, holderW, holderH, holderPitch, quitY;
	public final float chipX, chipY, chipH, name;
	public final float bannerW, frame;
	public final float panelW, panelH, panelFrame, title, body, barInset, barH, buttonH, buttonText;
	public final float contentX, contentY, contentW, contentH, backX, backY, backW, backH;
	public final float tabH, tabGap, tabText, rowH, pageFrame, pageInset, labelX, controlX, control, small;

	/** The board of a device screen: phones are much wider than they are high, tablets come close to 16:10. */
	public static LauncherBoard device(final float screenW, final float screenH) {
		final float longSide = Math.max(screenW, screenH);
		final float shortSide = Math.min(screenW, screenH);
		final boolean tablet = (longSide / shortSide) < 1.9f;
		final LauncherBoard shape = new LauncherBoard(tablet, false, 0, 0, 1);
		return new LauncherBoard(tablet, false, 0, 0, Math.min(longSide / shape.width, shortSide / shape.height));
	}

	/** The interface scale a window of this height starts with, in percent. */
	public static int autoScale(final int windowHeight) {
		return Math.min(UI_MAX, Math.max(100, Math.round(((810f / windowHeight) * 100) / 5) * 5));
	}

	/** The board of a desktop window at an interface scale in percent. */
	public static LauncherBoard window(final int windowW, final int windowH, final int uiPercent) {
		final float u = uiPercent / 100f;
		final boolean tablet = uiPercent <= UI_TABLET_MAX;
		final float scale;
		if (tablet) {
			scale = Math.min(windowW / 2960f, windowH / 1848f) * u;
		}
		else {
			// The compact layout starts where its rail buttons are as tall as the tablet ones at that scale.
			scale = Math.min(Math.min((146 * windowH * u) / (1848f * 96), windowH / 720f), windowW / 1600f);
		}
		return new LauncherBoard(tablet, true, windowW / scale, windowH / scale, scale);
	}

	public LauncherBoard(final boolean tablet) {
		this(tablet, false, 0, 0, 1);
	}

	private LauncherBoard(final boolean tablet, final boolean window, final float windowW, final float windowH,
			final float scale) {
		this.tablet = tablet;
		this.window = window;
		this.scale = scale;
		float logoX, logoW, logoH, holderY, holderH, pitch, quitY;
		final float baseW, baseH, contentX, contentY, edge, bottom, cap;
		if (tablet) {
			baseW = 2960;
			baseH = 1848;
			this.railW = 640;
			logoX = 40;
			this.logoY = 24;
			logoW = 560;
			logoH = 250;
			this.holderX = 37;
			holderY = 296;
			this.holderW = 560;
			holderH = 146;
			pitch = 156;
			quitY = 1420;
			this.chipX = 720;
			this.chipY = 24;
			this.chipH = 168;
			this.name = 56;
			this.bannerW = 880;
			this.frame = 40;
			this.panelW = 1600;
			this.panelH = 620;
			this.panelFrame = 44;
			this.title = 56;
			this.body = 32;
			this.barInset = 72;
			this.barH = 72;
			this.buttonH = 112;
			this.buttonText = 40;
			contentX = 720;
			contentY = 208;
			this.backW = 480;
			this.backH = 128;
			this.tabH = 104;
			this.tabGap = 16;
			this.tabText = 32;
			this.rowH = 170;
			this.pageFrame = 40;
			// The design sets panel contents 8 px inside the 40 px frame.
			this.pageInset = 48;
			this.labelX = 40;
			this.controlX = 1100;
			this.control = 64;
			this.small = 28;
			edge = 40;
			bottom = 168;
			cap = 2860;
		}
		else {
			baseW = 1600;
			baseH = 720;
			this.railW = 384;
			logoX = 1336;
			this.logoY = 8;
			logoW = 248;
			logoH = 108;
			this.holderX = 16;
			holderY = 24;
			this.holderW = 346;
			holderH = 96;
			pitch = 96;
			quitY = -1;
			this.chipX = 416;
			this.chipY = 6;
			this.chipH = 114;
			this.name = 40;
			this.bannerW = 440;
			this.frame = 24;
			this.panelW = 1200;
			this.panelH = 440;
			this.panelFrame = 30;
			this.title = 40;
			this.body = 28;
			this.barInset = 44;
			this.barH = 52;
			this.buttonH = 96;
			this.buttonText = 30;
			contentX = 416;
			contentY = 130;
			this.backW = 280;
			this.backH = 96;
			// Every touch target on the phone is at least 48 dp: tabs and option rows are 96 px high.
			this.tabH = 96;
			this.tabGap = 8;
			this.tabText = 22;
			this.rowH = 96;
			this.pageFrame = 20;
			this.pageInset = 16;
			this.labelX = 16;
			this.controlX = 560;
			this.control = 44;
			this.small = 24;
			edge = 16;
			bottom = 104;
			cap = 1500;
		}
		final float width = window ? windowW : baseW;
		final float height = window ? windowH : baseH;
		if (window && tablet && (height < baseH)) {
			// A larger interface scale leaves less design height: the logo and the rail pitch give way so that Quit
			// stays above the footer.
			final float lost = baseH - height;
			logoH = Math.max(150, 250 - (lost * 0.4f));
			logoW = (560 * logoH) / 250;
			logoX = 40 + ((560 - logoW) / 2);
			holderY = this.logoY + logoH + 22;
			pitch = Math.min(156, (height - 258 - holderY - 178) / 7);
			holderH = Math.min(146, pitch - 10);
			quitY = holderY + (7 * pitch) + 32;
		}
		if (window && !tablet) {
			logoX = width - 264;
			if (height >= 816) {
				quitY = height - 24 - 96;
			}
		}
		this.width = width;
		this.height = height;
		this.logoX = logoX;
		this.logoW = logoW;
		this.logoH = logoH;
		this.holderY = holderY;
		this.holderH = holderH;
		this.holderPitch = pitch;
		this.quitY = quitY;
		this.contentY = contentY;
		if (window) {
			final float free = width - contentX - edge;
			this.contentW = Math.min(free, cap);
			this.contentX = contentX + ((free - this.contentW) / 2);
			this.backY = height - bottom;
			this.contentH = this.backY - contentY - (tablet ? 40 : 12);
		}
		else {
			this.contentX = contentX;
			this.contentW = tablet ? 2200 : 1168;
			this.backY = tablet ? 1680 : 616;
			this.contentH = tablet ? 1432 : 474;
		}
		this.backX = this.contentX;
	}

	/** Design pixels to screen pixels. */
	public float px(final float design) {
		return design * this.scale;
	}

	/** Where the choices of an options radio row go, in design pixels from the row's left edge. */
	public static final class RadioPlaces {
		/** Each choice's left edge and its width with the mark. */
		public final float[] x, w;
		/** The choices stand on a line of their own under the label, which then keeps the label line's height. */
		public final boolean ownLine;
		public final float labelH, choicesY, height;

		RadioPlaces(final float[] x, final float[] w, final boolean ownLine, final float labelH, final float rowH) {
			this.x = x;
			this.w = w;
			this.ownLine = ownLine;
			this.labelH = ownLine ? labelH : rowH;
			this.choicesY = ownLine ? labelH : 0;
			this.height = ownLine ? labelH + rowH : rowH;
		}
	}

	/** From a radio mark to its name. */
	public static final float RADIO_NAME_GAP = 14;

	/**
	 * Places the choices of an options radio row. They keep the design's even spacing after the control column when
	 * every name fits it, else stand as close as their names allow; when even that leaves the control column (long
	 * names such as a locked graphics set on a narrow phone) they move to a line of their own under the label, where
	 * names that still do not fit share the width and are cut.
	 *
	 * @param names    each choice's name in the body font, in design pixels
	 * @param rowWidth the row from the left edge of the label to the right edge of the panel's inside
	 */
	public RadioPlaces radioPlaces(final float[] names, final float rowWidth) {
		final float end = rowWidth - (this.tablet ? 24 : 16);
		final float[] x = new float[names.length];
		final float[] w = new float[names.length];
		if (placeChoices(names, this.controlX - this.pageFrame, end, x, w)) {
			return new RadioPlaces(x, w, false, 0, this.rowH);
		}
		final float labelH = this.tablet ? 88 : 56;
		if (!placeChoices(names, this.labelX, end, x, w)) {
			final float mark = this.control + RADIO_NAME_GAP;
			final float gap = choiceGap();
			float total = 0;
			for (final float name : names) {
				total += name;
			}
			final float factor = Math.max(0,
					(end - this.labelX - (names.length * mark) - ((names.length - 1) * gap)) / Math.max(1, total));
			float left = this.labelX;
			for (int i = 0; i < names.length; i++) {
				x[i] = left;
				w[i] = mark + (names[i] * factor);
				left += w[i] + gap;
			}
		}
		return new RadioPlaces(x, w, true, labelH, this.rowH);
	}

	private float choiceGap() {
		return this.tablet ? 24 : 16;
	}

	private boolean placeChoices(final float[] names, final float start, final float end, final float[] x,
			final float[] w) {
		final int count = names.length;
		if (count == 0) {
			return true;
		}
		final float mark = this.control + RADIO_NAME_GAP;
		final float gap = choiceGap();
		float widest = 0;
		float packed = 0;
		for (final float name : names) {
			widest = Math.max(widest, mark + name);
			packed += mark + name + gap;
		}
		final float last = mark + names[count - 1];
		final float design = count > 2 ? (this.tablet ? 340 : 190) : (this.tablet ? 440 : 240);
		final float spacing = Math.max(design, widest + gap);
		if ((start + ((count - 1) * spacing) + last) <= end) {
			for (int i = 0; i < count; i++) {
				x[i] = start + (i * spacing);
				w[i] = spacing - gap;
			}
			return true;
		}
		if ((start + packed) - gap > end) {
			return false;
		}
		float left = start;
		for (int i = 0; i < count; i++) {
			x[i] = left;
			w[i] = mark + names[i];
			left += w[i] + gap;
		}
		return true;
	}

	/** The left edge of the campaign list on the campaign page, in content coordinates. */
	public float campaignListX() {
		return this.contentW - (this.tablet ? 964 : 636);
	}

	/** A button of the page's bottom row, x and width: 0 is Back or Cancel on the left, 1 and 2 sit on the right. */
	public float[] actionSlot(final int slot) {
		final float right = this.contentX + this.contentW;
		if (slot == 0) {
			return new float[] { this.backX, this.backW };
		}
		if (slot == 1) {
			return this.tablet ? new float[] { right - 1040, 480 } : new float[] { right - 704, 320 };
		}
		return this.tablet ? new float[] { right - 520, 520 } : new float[] { right - 360, 360 };
	}
}
