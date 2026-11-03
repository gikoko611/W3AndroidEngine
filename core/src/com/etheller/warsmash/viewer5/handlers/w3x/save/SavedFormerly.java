package com.etheller.warsmash.viewer5.handlers.w3x.save;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Earlier names of a renamed field, so saved games that stored it under such a name still fill it. A renamed class is
 * listed in {@link SavedObjectGraph}'s former class names instead.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SavedFormerly {
	String[] value();
}
