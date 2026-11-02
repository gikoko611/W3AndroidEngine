package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import com.badlogic.gdx.Input;
import com.etheller.warsmash.parsers.fdf.frames.GlueTextButtonFrame;
import com.etheller.warsmash.parsers.fdf.frames.StringFrame;
import com.etheller.warsmash.parsers.fdf.frames.UIFrame;

/**
 * The key of a menu button. The game data has no shortcut table: the key is the letter the button's label paints white
 * ("|CffffffffS|Rave Game" is S, "Pause Ga|Cffffffffm|Re" is M), so a translated label brings its own key.
 */
final class MenuHotkey {
	private static final String WHITE = "|cffffffff";

	private MenuHotkey() {
	}

	static int of(final GlueTextButtonFrame button) {
		final UIFrame label = button.getButtonText();
		if (!(label instanceof StringFrame)) {
			return Input.Keys.UNKNOWN;
		}
		return of(((StringFrame) label).getText());
	}

	static int of(final String label) {
		if (label == null) {
			return Input.Keys.UNKNOWN;
		}
		final int at = label.toLowerCase().indexOf(WHITE);
		if ((at < 0) || ((at + WHITE.length()) >= label.length())) {
			return Input.Keys.UNKNOWN;
		}
		final String letter = label.substring(at + WHITE.length(), at + WHITE.length() + 1).toUpperCase();
		final int keycode = Input.Keys.valueOf(letter);
		return keycode < 0 ? Input.Keys.UNKNOWN : keycode;
	}

	/** Presses the first enabled, shown button whose key this is. */
	static boolean press(final int keycode, final GlueTextButtonFrame... buttons) {
		if (keycode == Input.Keys.UNKNOWN) {
			return false;
		}
		for (final GlueTextButtonFrame button : buttons) {
			if ((button != null) && button.isEnabled() && button.isVisibleOnScreen() && (of(button) == keycode)) {
				button.onClick(Input.Buttons.LEFT);
				return true;
			}
		}
		return false;
	}
}
