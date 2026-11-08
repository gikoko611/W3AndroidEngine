package com.etheller.warsmash.parsers.jass.triggers;

import java.util.ArrayList;

import com.etheller.interpreter.ast.util.CAllocatedHandle;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerJass;

/** A script force: its players in the order they were added. */
public class CForce extends ArrayList<CPlayerJass> implements CAllocatedHandle {
	private int handleId;
	private boolean handleReleased;
	private boolean handleIdKept;
	private int referenceCount;

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
