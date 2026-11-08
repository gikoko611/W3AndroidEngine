package com.etheller.warsmash.viewer5.handlers.w3x.save;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A field holding a part of its object that was split off into a class of its own. Saved games from before the split
 * have the part's fields in the holder, under the same names; loading them makes the part and fills it from those.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SavedComponent {
}
