package com.etheller.warsmash.viewer5.handlers.w3x.ui.menu;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.utils.viewport.Viewport;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.parsers.fdf.datamodel.FramePoint;
import com.etheller.warsmash.parsers.fdf.frames.SetPoint;
import com.etheller.warsmash.parsers.fdf.frames.SimpleFrame;
import com.etheller.warsmash.parsers.fdf.frames.StringFrame;
import com.etheller.warsmash.parsers.fdf.frames.TextButtonFrame;
import com.etheller.warsmash.parsers.fdf.frames.UIFrame;

public class CampaignMenuUI extends SimpleFrame {
	private final GameUI rootFrame;
	private final Viewport uiViewport;
	private static final float BUTTON_SPACING = 0.0645f;
	private final List<CampaignButtonUI> buttonUIs = new ArrayList<>();
	private final List<Integer> gapsBefore = new ArrayList<>();
	private int pendingGaps;

	public CampaignMenuUI(final String name, final UIFrame parent, final GameUI rootFrame, final Viewport uiViewport) {
		super(name, parent);
		this.rootFrame = rootFrame;
		this.uiViewport = uiViewport;
	}

	public void addGap() {
		this.pendingGaps++;
	}

	public CampaignButtonUI addButton(final String header, final String name, final Runnable onClick) {
		final CampaignButtonUI campaignButtonUI = new CampaignButtonUI(null, this);
		final TextButtonFrame campaignArrowButton = (TextButtonFrame) this.rootFrame
				.createFrame("CampaignArrowButtonTemplate", campaignButtonUI, 0, 0);
		campaignButtonUI.setButtonArt(campaignArrowButton);
		campaignButtonUI.add(campaignArrowButton);
		campaignArrowButton.addSetPoint(new SetPoint(FramePoint.TOPLEFT, campaignButtonUI, FramePoint.TOPLEFT, 0, 0));
		campaignArrowButton.setOnClick(onClick);

		final StringFrame headerText = (StringFrame) this.rootFrame.createFrame("StandardSmallTextTemplate",
				campaignButtonUI, 0, 0);
		this.rootFrame.setDecoratedText(headerText, header);
		campaignButtonUI.add(headerText);
		final StringFrame nameText = (StringFrame) this.rootFrame.createFrame("StandardValueTextTemplate",
				campaignButtonUI, 0, 0);
		this.rootFrame.setDecoratedText(nameText, name);
		headerText.addSetPoint(new SetPoint(FramePoint.TOPLEFT, campaignArrowButton, FramePoint.TOPRIGHT, 0, 0));
		nameText.addSetPoint(new SetPoint(FramePoint.TOPLEFT, headerText, FramePoint.BOTTOMLEFT, 0, 0));
		campaignButtonUI.add(nameText);
		campaignButtonUI.setHeaderText(headerText);
		campaignButtonUI.setNameText(nameText);
		campaignButtonUI.setHeight(GameUI.convertY(this.uiViewport, 0.032f));

		add(campaignButtonUI);
		this.buttonUIs.add(campaignButtonUI);
		this.gapsBefore.add(this.pendingGaps);
		this.pendingGaps = 0;
		return campaignButtonUI;
	}

	@Override
	protected void innerPositionBounds(final GameUI gameUI, final Viewport viewport) {
		super.innerPositionBounds(gameUI, viewport);
		// Measured in 1.29: the list starts at the top, a blank list entry takes half a row and a campaign the
		// profile has not opened takes none.
		float rows = 0;
		int gaps = 0;
		for (int i = 0; i < this.buttonUIs.size(); i++) {
			gaps += this.gapsBefore.get(i);
			if (this.buttonUIs.get(i).isVisible()) {
				rows += 1 + (gaps * 0.5f);
				gaps = 0;
			}
		}
		final float myHeight = this.renderBounds.height;
		final float buttonSpacing = rows == 0 ? 0
				: Math.min(myHeight / rows, GameUI.convertY(this.uiViewport, BUTTON_SPACING));

		float yOffset = 0;
		gaps = 0;
		for (int i = 0; i < this.buttonUIs.size(); i++) {
			final CampaignButtonUI campaignButtonUI = this.buttonUIs.get(i);
			gaps += this.gapsBefore.get(i);
			if (!campaignButtonUI.isVisible()) {
				continue;
			}
			yOffset += gaps * 0.5f * buttonSpacing;
			gaps = 0;
			campaignButtonUI.addSetPoint(new SetPoint(FramePoint.TOPLEFT, this, FramePoint.TOPLEFT, 0, -yOffset));
			campaignButtonUI.addSetPoint(new SetPoint(FramePoint.TOPRIGHT, this, FramePoint.TOPRIGHT, 0, -yOffset));
			yOffset += buttonSpacing;
		}
		super.innerPositionBounds(gameUI, viewport);
	}

}
