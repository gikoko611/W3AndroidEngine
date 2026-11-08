package com.etheller.warsmash.viewer5.handlers.w3x.simulation.replay;

import java.io.IOException;

/** A replay this engine cannot play; the message says why, for the player. */
public class CReplayException extends IOException {
	private static final long serialVersionUID = 1L;

	public CReplayException(final String reason) {
		super(reason);
	}
}
