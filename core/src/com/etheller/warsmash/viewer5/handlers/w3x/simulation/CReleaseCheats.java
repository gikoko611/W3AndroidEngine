package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import java.util.Locale;

import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;

/**
 * Measured on 1.29: the release chat cheats work from a script and affect every player; the developer cheats campaign
 * AI scripts call (showmap, av, dg, vision N, gold N, ...) do nothing in a release game.
 */
public final class CReleaseCheats {
	private CReleaseCheats() {
	}

	public static void apply(final CSimulation simulation, final String text) {
		if (text == null) {
			return;
		}
		final String cheat = text.trim().toLowerCase(Locale.US);
		final int space = cheat.indexOf(' ');
		final String code = space < 0 ? cheat : cheat.substring(0, space);
		final String argument = space < 0 ? "" : cheat.substring(space + 1).trim();
		switch (code) {
		case "greedisgood":
		case "keysersoze":
		case "leafittome": {
			final int amount;
			try {
				amount = Integer.parseInt(argument);
			}
			catch (final NumberFormatException e) {
				return;
			}
			for (int i = 0; i < WarsmashConstants.MAX_PLAYERS; i++) {
				final CPlayer player = simulation.getPlayer(i);
				if (!code.equals("leafittome")) {
					player.addGold(amount);
				}
				if (!code.equals("keysersoze")) {
					player.addLumber(amount);
				}
			}
			break;
		}
		case "iseedeadpeople":
			simulation.toggleCheatRevealMap();
			break;
		case "warpten":
			simulation.toggleCheatBuildSpeed();
			break;
		default:
			break;
		}
	}
}
