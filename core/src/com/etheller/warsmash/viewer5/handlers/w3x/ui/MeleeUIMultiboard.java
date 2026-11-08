package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.parsers.fdf.datamodel.AnchorDefinition;
import com.etheller.warsmash.parsers.fdf.datamodel.FramePoint;
import com.etheller.warsmash.parsers.fdf.datamodel.TextJustify;
import com.etheller.warsmash.parsers.fdf.frames.AbstractRenderableFrame;
import com.etheller.warsmash.parsers.fdf.frames.GlueTextButtonFrame;
import com.etheller.warsmash.parsers.fdf.frames.SetPoint;
import com.etheller.warsmash.parsers.fdf.frames.StringFrame;
import com.etheller.warsmash.parsers.fdf.frames.TextureFrame;
import com.etheller.warsmash.parsers.fdf.frames.UIFrame;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CMultiboard;

/**
 * Draws the shown multiboard from the frames of Multiboard.fdf: the minimize button in the top right corner, the title
 * bar to its left and the grid under both. Each cell shows its icon and its value text, as its style says, in a column
 * as wide as the cell's width; a cell width is a fraction of the screen width. A minimized board shows only its title
 * bar. The button minimizes the board for the local player only; the script's MultiboardMinimize sets it again.
 */
final class MeleeUIMultiboard {
	private static final float BUTTON_SIZE = 0.024f;
	private static final float TITLE_FONT_HEIGHT = 0.011f;
	private static final float CELL_FONT_HEIGHT = 0.009f;
	private static final float TITLE_MIN_WIDTH = 0.06f;
	private static final float TITLE_PADDING = 0.02f;
	private static final float BODY_INSET_X = 0.008f;
	private static final float BODY_INSET_TOP = 0.009f;
	private static final float BODY_INSET_BOTTOM = 0.008f;
	private static final float ROW_HEIGHT = 0.0125f;
	private static final float ICON_SIZE = 0.0105f;
	private static final float ICON_GAP = 0.002f;
	private static final float TITLE_GAP = 0.0057f;
	/** The 4:3 area all frame sizes are given in is 0.8 wide; cell widths are fractions of it. */
	private static final float SCREEN_WIDTH = 0.8f;

	private final MeleeUI ui;
	private UIFrame boardFrame;
	private UIFrame titleBackdrop;
	private StringFrame titleFrame;
	private UIFrame bodyBackdrop;
	private final List<StringFrame> cellTexts = new ArrayList<>();
	private final List<TextureFrame> cellIcons = new ArrayList<>();
	private CMultiboard shownBoard;
	private int shownVersion = -1;
	private boolean scriptMinimized;
	private boolean localMinimized;
	private float anchorRight = Float.NaN;
	private float anchorTop = Float.NaN;

	MeleeUIMultiboard(final MeleeUI ui) {
		this.ui = ui;
	}

	boolean isShowing() {
		return (this.boardFrame != null) && this.boardFrame.isVisible();
	}

	/** Called every frame with the board the simulation shows, or null. */
	void update(final CMultiboard board) {
		final CMultiboard shown = (board != null) && board.isDisplayed() ? board : null;
		if (shown == null) {
			if (this.boardFrame != null) {
				setShown(false);
			}
			this.shownBoard = null;
			return;
		}
		if (this.boardFrame == null) {
			createFrames();
		}
		if ((shown != this.shownBoard) || (shown.getVersion() != this.shownVersion)) {
			if ((shown != this.shownBoard) || (shown.isMinimized() != this.scriptMinimized)) {
				this.scriptMinimized = shown.isMinimized();
				this.localMinimized = this.scriptMinimized;
			}
			this.shownBoard = shown;
			this.shownVersion = shown.getVersion();
			layout();
		}
		if (!this.boardFrame.isVisible()) {
			setShown(true);
		}
	}

	/** Under the timer dialogs, where the leaderboard would be: a shown multiboard takes its place. */
	void anchor(final float right, final float top) {
		if ((right == this.anchorRight) && (top == this.anchorTop)) {
			return;
		}
		this.anchorRight = right;
		this.anchorTop = top;
		if (this.boardFrame != null) {
			this.boardFrame.addAnchor(new AnchorDefinition(FramePoint.TOPRIGHT, right, top));
			positionAll();
		}
	}

	void resetAnchor() {
		this.anchorRight = Float.NaN;
		this.anchorTop = Float.NaN;
	}

	private void createFrames() {
		final GameUI root = this.ui.rootFrame;
		this.boardFrame = root.createFrame("Multiboard", root, 0, 0);
		this.titleBackdrop = root.getFrameByName("MultiboardTitleBackdrop", 0);
		this.titleFrame = (StringFrame) root.getFrameByName("MultiboardTitle", 0);
		this.bodyBackdrop = root.getFrameByName("MultiboardBackdrop", 0);
		final UIFrame button = root.getFrameByName("MultiboardMinimizeButton", 0);
		// The file ends the title bar with SetAllPoints after its two points; the game keeps the points, which put the
		// bar left of the button.
		this.titleBackdrop.setSetAllPoints(false);
		if (this.titleBackdrop instanceof AbstractRenderableFrame) {
			((AbstractRenderableFrame) this.titleBackdrop).clearFramePointAssignments();
		}
		final float titleGap = GameUI.convertX(this.ui.uiViewport, TITLE_GAP);
		this.titleBackdrop.addSetPoint(new SetPoint(FramePoint.TOPRIGHT, button, FramePoint.TOPLEFT, titleGap, 0));
		this.titleBackdrop
				.addSetPoint(new SetPoint(FramePoint.BOTTOMRIGHT, button, FramePoint.BOTTOMLEFT, titleGap, 0));
		if (button instanceof GlueTextButtonFrame) {
			((GlueTextButtonFrame) button).setOnClick(() -> {
				this.localMinimized = !this.localMinimized;
				layout();
			});
		}
		this.boardFrame.setWidth(GameUI.convertX(this.ui.uiViewport, BUTTON_SIZE));
		this.boardFrame.setHeight(GameUI.convertY(this.ui.uiViewport, BUTTON_SIZE));
		if (!Float.isNaN(this.anchorRight)) {
			this.boardFrame.addAnchor(new AnchorDefinition(FramePoint.TOPRIGHT, this.anchorRight, this.anchorTop));
		}
		this.boardFrame.setVisible(false);
	}

	private void setShown(final boolean shown) {
		this.boardFrame.setVisible(shown);
		if (!shown) {
			for (final StringFrame text : this.cellTexts) {
				text.setVisible(false);
			}
			for (final TextureFrame icon : this.cellIcons) {
				icon.setVisible(false);
			}
		}
		else {
			layout();
		}
	}

	private void layout() {
		final CMultiboard board = this.shownBoard;
		if ((board == null) || (this.boardFrame == null)) {
			return;
		}
		final GameUI root = this.ui.rootFrame;
		root.setText(this.titleFrame, board.getTitle());
		final int titleColor = board.getTitleColor();
		this.titleFrame.setColor(titleColor == -1 ? new Color(0xFFCC00FF) : rgba(titleColor));

		final List<List<CMultiboard.Item>> rows = board.getRows();
		final int columnCount = board.getColumnCount();
		final float[] columnWidths = new float[columnCount];
		for (final List<CMultiboard.Item> row : rows) {
			for (int column = 0; column < columnCount; column++) {
				columnWidths[column] = Math.max(columnWidths[column], row.get(column).width * SCREEN_WIDTH);
			}
		}
		float gridWidth = 0;
		for (final float width : columnWidths) {
			gridWidth += width;
		}
		final float titleWidth = Math.max(TITLE_MIN_WIDTH,
				Math.max(gridWidth + (2 * BODY_INSET_X) - BUTTON_SIZE, measureTitle(board.getTitle()) + TITLE_PADDING));
		this.titleBackdrop.setWidth(GameUI.convertX(this.ui.uiViewport, titleWidth));

		final boolean showGrid = !this.localMinimized && !rows.isEmpty() && (columnCount > 0);
		this.bodyBackdrop.setVisible(showGrid);
		this.bodyBackdrop.setHeight(GameUI.convertY(this.ui.uiViewport,
				BODY_INSET_TOP + (rows.size() * ROW_HEIGHT) + BODY_INSET_BOTTOM));

		int cell = 0;
		if (showGrid) {
			float y = BODY_INSET_TOP;
			for (final List<CMultiboard.Item> row : rows) {
				float x = BODY_INSET_X;
				for (int column = 0; column < columnCount; column++) {
					layoutCell(cell++, row.get(column), x, y, columnWidths[column]);
					x += columnWidths[column];
				}
				y += ROW_HEIGHT;
			}
		}
		for (int i = cell; i < this.cellTexts.size(); i++) {
			this.cellTexts.get(i).setVisible(false);
			this.cellIcons.get(i).setVisible(false);
		}
		positionAll();
	}

	private void layoutCell(final int index, final CMultiboard.Item item, final float x, final float y,
			final float width) {
		final GameUI root = this.ui.rootFrame;
		while (this.cellTexts.size() <= index) {
			final int n = this.cellTexts.size();
			final StringFrame text = root.createStringFrame("SmashMultiboardCell" + n, this.bodyBackdrop,
					Color.WHITE, TextJustify.LEFT, TextJustify.MIDDLE, CELL_FONT_HEIGHT);
			text.setHeight(GameUI.convertY(this.ui.uiViewport, ROW_HEIGHT));
			this.cellTexts.add(text);
			final TextureFrame icon = root.createTextureFrame("SmashMultiboardIcon" + n, this.bodyBackdrop, false,
					null);
			icon.setWidth(GameUI.convertX(this.ui.uiViewport, ICON_SIZE));
			icon.setHeight(GameUI.convertY(this.ui.uiViewport, ICON_SIZE));
			this.cellIcons.add(icon);
		}
		final StringFrame text = this.cellTexts.get(index);
		final TextureFrame icon = this.cellIcons.get(index);
		final boolean iconShown = item.showIcon && (item.icon != null) && !item.icon.isEmpty()
				&& this.ui.war3MapViewer.dataSource.has(item.icon);
		float textLeft = x;
		if (iconShown) {
			icon.setTexture(item.icon, root);
			icon.addSetPoint(new SetPoint(FramePoint.LEFT, this.bodyBackdrop, FramePoint.TOPLEFT,
					GameUI.convertX(this.ui.uiViewport, x),
					GameUI.convertY(this.ui.uiViewport, -(y + (ROW_HEIGHT / 2)))));
			textLeft += ICON_SIZE + ICON_GAP;
		}
		icon.setVisible(iconShown);
		final boolean textShown = item.showValue && (item.value != null) && !item.value.isEmpty();
		if (textShown) {
			text.setWidth(GameUI.convertX(this.ui.uiViewport, Math.max(0.001f, (x + width) - textLeft)));
			text.addSetPoint(new SetPoint(FramePoint.TOPLEFT, this.bodyBackdrop, FramePoint.TOPLEFT,
					GameUI.convertX(this.ui.uiViewport, textLeft), GameUI.convertY(this.ui.uiViewport, -y)));
			text.setColor(item.valueColor == -1 ? Color.WHITE : rgba(item.valueColor));
			root.setText(text, item.value);
		}
		text.setVisible(textShown);
	}

	private float measureTitle(final String title) {
		if ((title == null) || title.isEmpty()) {
			return 0;
		}
		// Close enough for sizing the bar: the title font is about half as wide per letter as it is high.
		return title.replaceAll("\\|c[0-9a-fA-F]{8}|\\|r", "").length() * TITLE_FONT_HEIGHT * 0.5f;
	}

	private void positionAll() {
		final GameUI root = this.ui.rootFrame;
		this.boardFrame.positionBounds(root, this.ui.uiViewport);
		// The cell frames belong to the root frame, so moving the board does not move them.
		for (int i = 0; i < this.cellTexts.size(); i++) {
			if (this.cellTexts.get(i).isVisible()) {
				this.cellTexts.get(i).positionBounds(root, this.ui.uiViewport);
			}
			if (this.cellIcons.get(i).isVisible()) {
				this.cellIcons.get(i).positionBounds(root, this.ui.uiViewport);
			}
		}
	}

	private static Color rgba(final int color) {
		return new Color(((color >>> 24) & 0xFF) / 255f, ((color >>> 16) & 0xFF) / 255f,
				((color >>> 8) & 0xFF) / 255f, (color & 0xFF) / 255f);
	}
}
