package com.etheller.warsmash.viewer5.handlers.w3x.simulation.players;

import com.etheller.interpreter.ast.util.CHandle;

public class CRacePreference implements CHandle {
	private int id;

	public CRacePreference(int id) {
		this.id = id;
	}

	public int ordinal() {
		return id - 1;
	}

	public int getBitMaskValue() {
		// ConvertRacePref(0) has no race bit; a negative shift would set the sign bit.
		return ordinal() < 0 ? 0 : 1 << ordinal();
	}

	@Override
	public int getHandleId() {
		return getBitMaskValue();
	}
}
