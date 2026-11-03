package com.etheller.warsmash.viewer5.handlers.w3x.save;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The value a field gets when a saved game from before the field existed is loaded. Loaded objects are not
 * constructed, so a field added later is otherwise left 0, false or null (a final collection or map field gets a new
 * empty one). A field whose constructor gives it anything else needs this, or its class implements
 * {@link SavedAddedFields}.
 *
 * The text is read as the field's type: a number, true or false, a character, a string, or an enum constant's name.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SavedDefault {
	String value();
}
