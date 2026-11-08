package com.etheller.interpreter.ast.util;

/**
 * A handle numbered from the game's handle table when it is made. Its id goes back to the table once, when the handle
 * is destroyed; a script may still hold it afterwards and must read the same id from it.
 */
public interface CAllocatedHandle extends CHandle {
	void setHandleId(int handleId);

	/** @return true the first time only, so a handle destroyed twice gives its id back once */
	boolean releaseHandle();

	/**
	 * A local variable still pointed at the handle when its function returned. 1.29 then never gives the id again,
	 * not even after the handle is destroyed (parameters do not count).
	 */
	void keepHandleId();

	boolean isHandleIdKept();

	/**
	 * Script variables and hashtable entries that point at the handle; a destroyed handle's id waits until none do.
	 *
	 * @return the count after the change
	 */
	int changeReferenceCount(int delta);

	int getReferenceCount();
}
