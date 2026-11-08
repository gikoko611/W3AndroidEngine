package com.etheller.warsmash.parsers.fdf.frames;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.command.ClickableFrame;

/**
 * The console's check box (SIMPLECHECKBOX): a normal texture, a pushed one while held and the check drawn over them
 * while checked. A click toggles it.
 */
public class SimpleCheckBoxFrame extends AbstractRenderableFrame implements ClickableFrame {
	private Texture normalTexture;
	private Texture pushedTexture;
	private Texture checkedTexture;
	private boolean checked;
	private boolean pushed;
	private boolean enabled = true;
	private Runnable onClick;
	private UIFrame label;
	private float minimumTouchHeight;
	private final Rectangle touchBounds = new Rectangle();

	public SimpleCheckBoxFrame(final String name, final UIFrame parent) {
		super(name, parent);
	}

	public void setTextures(final Texture normalTexture, final Texture pushedTexture, final Texture checkedTexture) {
		this.normalTexture = normalTexture;
		this.pushedTexture = pushedTexture;
		this.checkedTexture = checkedTexture;
	}

	public boolean isChecked() {
		return this.checked;
	}

	public void setChecked(final boolean checked) {
		this.checked = checked;
	}

	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	public void setOnClick(final Runnable onClick) {
		this.onClick = onClick;
	}

	/**
	 * The label next to the box takes clicks too, and the whole is at least this tall: a finger hits the box's row,
	 * not the small box.
	 */
	public void setTouchArea(final UIFrame label, final float minimumHeight) {
		this.label = label;
		this.minimumTouchHeight = minimumHeight;
	}

	@Override
	protected void innerPositionBounds(final GameUI gameUI, final Viewport viewport) {
	}

	@Override
	protected void internalRender(final SpriteBatch batch, final BitmapFont baseFont, final GlyphLayout glyphLayout) {
		final Texture box = this.pushed && (this.pushedTexture != null) ? this.pushedTexture : this.normalTexture;
		if (!this.enabled) {
			batch.setColor(0.5f, 0.5f, 0.5f, 1f);
		}
		if (box != null) {
			batch.draw(box, this.renderBounds.x, this.renderBounds.y, this.renderBounds.width,
					this.renderBounds.height);
		}
		if (this.checked && (this.checkedTexture != null)) {
			batch.draw(this.checkedTexture, this.renderBounds.x, this.renderBounds.y, this.renderBounds.width,
					this.renderBounds.height);
		}
		if (!this.enabled) {
			batch.setColor(1f, 1f, 1f, 1f);
		}
	}

	private boolean hits(final float screenX, final float screenY) {
		if (!isVisible() || !this.enabled) {
			return false;
		}
		this.touchBounds.set(this.renderBounds);
		if ((this.label instanceof AbstractRenderableFrame) && this.label.isVisible()) {
			this.touchBounds.merge(((AbstractRenderableFrame) this.label).getRenderBounds());
		}
		if (this.touchBounds.height < this.minimumTouchHeight) {
			final float grow = this.minimumTouchHeight - this.touchBounds.height;
			this.touchBounds.y -= grow / 2;
			this.touchBounds.height = this.minimumTouchHeight;
		}
		return this.touchBounds.contains(screenX, screenY);
	}

	@Override
	public UIFrame touchDown(final float screenX, final float screenY, final int button) {
		if (hits(screenX, screenY)) {
			return this;
		}
		return super.touchDown(screenX, screenY, button);
	}

	@Override
	public UIFrame touchUp(final float screenX, final float screenY, final int button) {
		if (hits(screenX, screenY)) {
			return this;
		}
		return super.touchUp(screenX, screenY, button);
	}

	@Override
	public UIFrame getFrameChildUnderMouse(final float screenX, final float screenY) {
		if (hits(screenX, screenY)) {
			return this;
		}
		return super.getFrameChildUnderMouse(screenX, screenY);
	}

	@Override
	public void mouseDown(final GameUI gameUI, final Viewport uiViewport) {
		this.pushed = this.enabled;
	}

	@Override
	public void mouseUp(final GameUI gameUI, final Viewport uiViewport) {
		this.pushed = false;
	}

	@Override
	public void mouseEnter(final GameUI gameUI, final Viewport uiViewport) {
	}

	@Override
	public void mouseExit(final GameUI gameUI, final Viewport uiViewport) {
		this.pushed = false;
	}

	@Override
	public void onClick(final int button) {
		if (!this.enabled) {
			return;
		}
		this.checked = !this.checked;
		if (this.onClick != null) {
			this.onClick.run();
		}
	}

	@Override
	public String getSoundKey() {
		return SOUND_KEY_INTERFACE_CLICK;
	}

	@Override
	public void mouseDragged(final GameUI rootFrame, final Viewport uiViewport, final float x, final float y) {
	}
}
