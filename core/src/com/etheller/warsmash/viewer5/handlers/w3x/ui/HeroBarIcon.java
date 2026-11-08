package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.parsers.fdf.frames.SingleStringFrame;
import com.etheller.warsmash.parsers.fdf.frames.SpriteFrame;
import com.etheller.warsmash.parsers.fdf.frames.TextureFrame;
import com.etheller.warsmash.parsers.fdf.frames.UIFrame;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.command.MultiSelectionIconListener;

/**
 * A hero button: besides the hero's icon and bars, the count of its unspent skill points on the number overlay and
 * the glowing frame model around the button while it has any.
 */
final class HeroBarIcon extends MultiSelectionIcon {
	private TextureFrame pointsOverlay;
	private SingleStringFrame pointsText;
	private SpriteFrame pointsGlow;
	private int shownPoints = -1;

	HeroBarIcon(final String name, final UIFrame parent, final MultiSelectionIconListener clickListener,
			final int index) {
		super(name, parent, clickListener, index);
	}

	void setSkillPointFrames(final TextureFrame pointsOverlay, final SingleStringFrame pointsText,
			final SpriteFrame pointsGlow) {
		this.pointsOverlay = pointsOverlay;
		this.pointsText = pointsText;
		this.pointsGlow = pointsGlow;
	}

	/** The glow is a model of the interface scene, so it is hidden here when the bar or the button is. */
	void setSkillPoints(final int points, final boolean barShown) {
		final boolean show = points > 0;
		if (points != this.shownPoints) {
			this.shownPoints = points;
			this.pointsOverlay.setVisible(show);
			this.pointsText.setVisible(show);
			if (show) {
				this.pointsText.setText(Integer.toString(points));
			}
		}
		final boolean glow = show && barShown && isVisible();
		if ((this.pointsGlow != null) && (this.pointsGlow.isVisible() != glow)) {
			this.pointsGlow.setVisible(glow);
		}
	}

	@Override
	public void setVisible(final boolean visible) {
		super.setVisible(visible);
		if (!visible && (this.pointsGlow != null) && this.pointsGlow.isVisible()) {
			this.pointsGlow.setVisible(false);
		}
	}

	@Override
	protected void innerPositionBounds(final GameUI gameUI, final Viewport viewport) {
		super.innerPositionBounds(gameUI, viewport);
		if (this.pointsOverlay != null) {
			this.pointsOverlay.positionBounds(gameUI, viewport);
			this.pointsText.positionBounds(gameUI, viewport);
		}
		if (this.pointsGlow != null) {
			this.pointsGlow.positionBounds(gameUI, viewport);
		}
	}

	@Override
	protected void internalRender(final SpriteBatch batch, final BitmapFont baseFont, final GlyphLayout glyphLayout) {
		super.internalRender(batch, baseFont, glyphLayout);
		if (this.pointsOverlay != null) {
			this.pointsOverlay.render(batch, baseFont, glyphLayout);
			this.pointsText.render(batch, baseFont, glyphLayout);
		}
	}
}
