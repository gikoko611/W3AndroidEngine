package com.etheller.warsmash.viewer5.handlers.w3x.save;

import java.util.Set;

/**
 * For a class whose newer fields need values that depend on the rest of the object or on other objects, so a
 * {@link SavedDefault} constant does not do.
 */
public interface SavedAddedFields {
	/**
	 * Called once the whole saved game has been read, for an object whose save had none of these fields. They hold
	 * their {@link SavedDefault} or type default already.
	 *
	 * @param fieldNames the names of the fields the save lacked, declared by this class or its superclasses
	 */
	void initAddedFields(Set<String> fieldNames);
}
