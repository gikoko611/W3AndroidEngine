package com.etheller.warsmash.parsers.jass.triggers;

import com.badlogic.gdx.math.Rectangle;
import com.etheller.interpreter.ast.util.CAllocatedHandle;

/** A script rect: the bounds and the handle id they were made with. */
public class CRect extends Rectangle implements CAllocatedHandle {
	private int handleId;
	private boolean handleReleased;
	private boolean handleIdKept;
	private int referenceCount;

	public CRect(final float x, final float y, final float width, final float height) {
		super(x, y, width, height);
	}

	@Override
	public int getHandleId() {
		return this.handleId;
	}

	@Override
	public void setHandleId(final int handleId) {
		this.handleId = handleId;
	}

	@Override
	public boolean releaseHandle() {
		if (this.handleReleased) {
			return false;
		}
		this.handleReleased = true;
		return true;
	}

	@Override
	public void keepHandleId() {
		this.handleIdKept = true;
	}

	@Override
	public boolean isHandleIdKept() {
		return this.handleIdKept;
	}

	@Override
	public int changeReferenceCount(final int delta) {
		this.referenceCount += delta;
		return this.referenceCount;
	}

	@Override
	public int getReferenceCount() {
		return this.referenceCount;
	}
}
