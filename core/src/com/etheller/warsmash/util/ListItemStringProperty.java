package com.etheller.warsmash.util;

import com.badlogic.gdx.graphics.Color;

public class ListItemStringProperty extends AbstractListItemProperty {
    private Color color;

    public ListItemStringProperty(String rawValue) {
        super(ListItemEnum.ITEM_STRING, rawValue);
    }

    public ListItemStringProperty(String rawValue, Color color) {
        this(rawValue);
        this.color = color;
    }

    /** Null for the list's usual white. */
    public Color getColor() {
        return color;
    }
    
}
