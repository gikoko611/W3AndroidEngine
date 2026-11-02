package com.etheller.warsmash.input.touch;

/**
 * A screen with sliders. A finger usually rests on a thumb before sliding it, so a hold there must not end the press
 * the way a long press does elsewhere.
 */
public interface SliderHost {
	boolean isOverSlider(int screenX, int screenY);
}
