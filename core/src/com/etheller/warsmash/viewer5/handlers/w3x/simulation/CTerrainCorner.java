package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import com.etheller.warsmash.parsers.w3x.w3e.Corner;

/** A terrain corner with what the cliff layout works out about the cell it is the bottom left of. */
public class CTerrainCorner extends Corner {
	/** The cell spans more than one cliff level. */
	public boolean cliff;
	/** A ramp model stands on the cell, so units walk it between the levels. */
	public boolean romp;
	/** A terrain doodad (a bridge, a ramp of its own) took the cell out of the cliff layout. */
	public boolean hideCliff;

	public CTerrainCorner(final Corner corner) {
		super(corner);
	}
}
