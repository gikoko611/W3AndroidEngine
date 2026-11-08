package com.etheller.warsmash.viewer5.handlers.w3x.ui.command;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;

public class SettableCommandErrorListener implements CommandErrorListener {
	private CommandErrorListener delegate;

	@Override
	public void showInterfaceError(final int playerIndex, final String message) {
		this.delegate.showInterfaceError(playerIndex, message);
	}

	@Override
	public void showCommandErrorWithoutSound(int playerIndex, String message) {
		this.delegate.showCommandErrorWithoutSound(playerIndex, message);
	}

	public void setDelegate(final CommandErrorListener delegate) {
		this.delegate = delegate;
	}

	@Override
	public void showUpgradeCompleteAlert(int playerIndex, War3ID queuedRawcode, int level) {
		this.delegate.showUpgradeCompleteAlert(playerIndex, queuedRawcode, level);
	}

	@Override
	public void noteAlertPosition(final int playerIndex, final float x, final float y) {
		if (this.delegate != null) {
			this.delegate.noteAlertPosition(playerIndex, x, y);
		}
	}

	@Override
	public void unitAttacked(final CUnit unit, final CUnit attacker) {
		if (this.delegate != null) {
			this.delegate.unitAttacked(unit, attacker);
		}
	}

	@Override
	public void heroDied(final CUnit hero) {
		if (this.delegate != null) {
			this.delegate.heroDied(hero);
		}
	}
}
