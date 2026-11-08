package com.etheller.warsmash.viewer5.handlers.w3x.ui.command;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;

public interface CommandErrorListener {
	void showInterfaceError(int playerIndex, String message);
	void showCommandErrorWithoutSound(int playerIndex, String message);

	void showUpgradeCompleteAlert(int playerIndex, War3ID queuedRawcode, int level);

	/** Where an alert of the player happened: a unit is ready, a building or research is done. */
	void noteAlertPosition(int playerIndex, float x, float y);

	/** An enemy unit damaged the unit; the players it concerns may get an "under attack" alert. */
	void unitAttacked(CUnit unit, CUnit attacker);

	/** A hero died; its owner is told so. */
	void heroDied(CUnit hero);

}
