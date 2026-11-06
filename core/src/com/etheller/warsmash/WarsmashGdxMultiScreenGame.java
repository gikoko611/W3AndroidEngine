package com.etheller.warsmash;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.etheller.warsmash.input.touch.TouchInputRouter;
import com.etheller.warsmash.viewer5.CanvasProvider;

public class WarsmashGdxMultiScreenGame extends Game implements CanvasProvider {

	@Override
	public void create() {
	}

	/**
	 * A map screen left by setScreen disposes itself on the next frame. A game that ends back to the launcher leaves
	 * it for no screen and closes the activity at once, so that frame may never come; dispose() then still has it.
	 */
	private WarsmashGdxMapScreen leftMapScreen;

	@Override
	public void setScreen(final Screen screen) {
		final Screen previous = getScreen();
		super.setScreen(screen);
		if ((previous instanceof WarsmashGdxMapScreen) && (previous != screen)) {
			this.leftMapScreen = (WarsmashGdxMapScreen) previous;
		}
	}

	@Override
	public void render() {
		TouchInputRouter.drainInstalled();
		super.render();
		if ((this.leftMapScreen != null) && this.leftMapScreen.isDisposed()) {
			this.leftMapScreen = null;
		}
	}

	/**
	 * A closing activity (the game ended back to the launcher, Back, Exit Program) disposes the game here. The map
	 * screen's hide() leaves its dispose to the next frame, which a closing app never draws; undisposed, the finished
	 * game stayed on the heap through statics the map's dispose releases, and the next game in the same process ran
	 * out of Java heap while loading.
	 */
	@Override
	public void dispose() {
		Screen closing = getScreen();
		final WarsmashGdxMapScreen left = this.leftMapScreen;
		this.leftMapScreen = null;
		super.dispose();
		if ((left != null) && (left != closing)) {
			left.dispose();
			// A game that ended back to the launcher left no screen; its menu goes with it.
			if (closing == null) {
				closing = left.getMenuScreen();
			}
		}
		try {
			if (closing instanceof WarsmashGdxMapScreen) {
				final WarsmashGdxMapScreen mapScreen = (WarsmashGdxMapScreen) closing;
				mapScreen.dispose();
				if (mapScreen.getMenuScreen() != null) {
					mapScreen.getMenuScreen().dispose();
				}
			}
			else if (closing != null) {
				closing.dispose();
			}
		}
		catch (final RuntimeException e) {
			e.printStackTrace();
		}
	}

	@Override
	public float getWidth() {
		return Gdx.graphics.getWidth();
	}

	@Override
	public float getHeight() {
		return Gdx.graphics.getHeight();
	}

}
