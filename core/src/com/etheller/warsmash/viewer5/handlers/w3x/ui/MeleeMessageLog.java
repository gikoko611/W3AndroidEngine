package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.ArrayDeque;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.parsers.fdf.datamodel.AnchorDefinition;
import com.etheller.warsmash.parsers.fdf.datamodel.FramePoint;
import com.etheller.warsmash.parsers.fdf.frames.AbstractRenderableFrame;
import com.etheller.warsmash.parsers.fdf.frames.GlueTextButtonFrame;
import com.etheller.warsmash.parsers.fdf.frames.SetPoint;
import com.etheller.warsmash.parsers.fdf.frames.SimpleFrame;
import com.etheller.warsmash.parsers.fdf.frames.TextAreaFrame;
import com.etheller.warsmash.parsers.fdf.frames.UIFrame;

/** The Message Log (F12 in a single player game): the messages shown on screen so far, newest last. */
final class MeleeMessageLog {
	/** LogArea's TextAreaMaxLines. */
	private static final int MAX_LINES = 128;

	private final GameUI rootFrame;
	private final Viewport uiViewport;
	private final SimpleFrame holder;
	private final UIFrame dialog;
	private final TextAreaFrame area;
	private final GlueTextButtonFrame okButton;
	private final ArrayDeque<String> lines = new ArrayDeque<>();

	MeleeMessageLog(final GameUI rootFrame, final Viewport uiViewport) {
		this.rootFrame = rootFrame;
		this.uiViewport = uiViewport;
		this.holder = (SimpleFrame) rootFrame.createSimpleFrame("SmashLogDialog", rootFrame, 0);
		this.holder.addAnchor(new AnchorDefinition(FramePoint.CENTER, 0, 0));
		this.dialog = rootFrame.createFrame("LogDialog", this.holder, 0, 0);
		this.dialog.addSetPoint(new SetPoint(FramePoint.CENTER, this.holder, FramePoint.CENTER, 0, 0));
		this.holder.add(this.dialog);
		this.holder.setWidth(this.dialog.getAssignedWidth());
		this.holder.setHeight(this.dialog.getAssignedHeight());
		this.area = (TextAreaFrame) rootFrame.getFrameByName("LogArea", 0);
		this.okButton = (GlueTextButtonFrame) rootFrame.getFrameByName("LogOkButton", 0);
		this.okButton.setOnClick(() -> setVisible(false));
		this.holder.setVisible(false);
	}

	UIFrame getFrame() {
		return this.holder;
	}

	void add(final String message) {
		if ((message == null) || message.isEmpty()) {
			return;
		}
		for (final String line : message.split("\\|n|\\|N|\n")) {
			this.lines.addLast(line);
			while (this.lines.size() > MAX_LINES) {
				this.lines.removeFirst();
			}
		}
		if (isVisible()) {
			refresh();
		}
	}

	boolean isVisible() {
		return this.holder.isVisible();
	}

	boolean contains(final float uiX, final float uiY) {
		return isVisible() && ((AbstractRenderableFrame) this.dialog).getRenderBounds().contains(uiX, uiY);
	}

	void setVisible(final boolean visible) {
		this.holder.setVisible(visible);
		if (visible) {
			this.holder.positionBounds(this.rootFrame, this.uiViewport);
			refresh();
		}
	}

	boolean keyDown(final int keycode) {
		if ((keycode == Input.Keys.ESCAPE) || (keycode == Input.Keys.ENTER)) {
			setVisible(false);
			return true;
		}
		return MenuHotkey.press(keycode, this.okButton);
	}

	private void refresh() {
		final StringBuilder text = new StringBuilder();
		for (final String line : this.lines) {
			if (text.length() > 0) {
				text.append('\n');
			}
			text.append(line);
		}
		this.area.setText(text.toString(), this.rootFrame, this.uiViewport);
		if (this.area.getScrollBarFrame() != null) {
			this.area.getScrollBarFrame().setValue(this.rootFrame, this.uiViewport, 0);
		}
	}
}
