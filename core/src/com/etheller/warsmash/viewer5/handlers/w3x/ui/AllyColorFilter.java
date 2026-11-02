package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.ArrayList;
import java.util.List;

import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.units.Element;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;

/**
 * The ally color mode (Alt-A, the minimap's ally button, SetAllyColorFilterState): 0 shows player colors, 1 colors
 * the minimap by ally and enemy, 2 also recolors units in the world as you, allies, creeps and enemies. Colors come
 * from MiscData: [TeamColorFilter] for the world, the FogColor entries of [FogOfWar] for minimap dots.
 */
public final class AllyColorFilter {
	public static final int MODE_COUNT = 3;
	private static final int YOU = 0;
	private static final int ALLY = 1;
	private static final int CREEP = 2;
	private static final int ENEMY = 3;

	private int state;
	private final int[] worldColors = { 1, 2, 12, 0 };
	private final float[][] minimapColors = { { 1, 1, 1 }, { 0, 1, 210 / 255f }, { 1, 0, 0 }, { 1, 0, 0 } };
	private final List<Runnable> listeners = new ArrayList<>();

	public AllyColorFilter(final DataTable miscData) {
		final Element filter = miscData == null ? null : miscData.get("TeamColorFilter");
		if (filter != null) {
			readColorIndex(filter, "ColorIndexPlayer", YOU);
			readColorIndex(filter, "ColorIndexAlly", ALLY);
			readColorIndex(filter, "ColorIndexCreep", CREEP);
			readColorIndex(filter, "ColorIndexEnemy", ENEMY);
		}
		final Element fog = miscData == null ? null : miscData.get("FogOfWar");
		if (fog != null) {
			readArgb(fog, "FogColorPlayer", YOU);
			readArgb(fog, "FogColorAlly", ALLY);
			readArgb(fog, "FogColorEnemy", CREEP);
			readArgb(fog, "FogColorEnemy", ENEMY);
		}
	}

	private void readColorIndex(final Element element, final String field, final int relation) {
		if (element.hasField(field)) {
			this.worldColors[relation] = element.getFieldValue(field);
		}
	}

	private void readArgb(final Element element, final String field, final int relation) {
		if (element.hasField(field)) {
			for (int i = 0; i < 3; i++) {
				this.minimapColors[relation][i] = element.getFieldValue(field, i + 1) / 255f;
			}
		}
	}

	public int getState() {
		return this.state;
	}

	/** Measured on 1.29: the state is kept as given, even outside 0 to 2. */
	public void setState(final int state) {
		this.state = state;
		for (final Runnable listener : this.listeners) {
			listener.run();
		}
	}

	public void addListener(final Runnable listener) {
		this.listeners.add(listener);
	}

	public boolean isMinimapFiltered() {
		return this.state >= 1;
	}

	public boolean isWorldFiltered() {
		return this.state >= 2;
	}

	private static int relation(final CSimulation game, final int localPlayer, final int owner) {
		if (owner == localPlayer) {
			return YOU;
		}
		if (game.isNeutralAggressive(owner)) {
			return CREEP;
		}
		if (game.getPlayer(localPlayer).hasAlliance(owner, CAllianceType.PASSIVE)) {
			return ALLY;
		}
		return ENEMY;
	}

	/** The team color a unit of the owner is drawn with in the world. */
	public int worldColor(final CSimulation game, final int localPlayer, final int owner, final int playerColor) {
		if (!isWorldFiltered()) {
			return playerColor;
		}
		return this.worldColors[relation(game, localPlayer, owner)];
	}

	/** The color of the owner's minimap dots, or null when the minimap shows player colors. */
	public float[] minimapColor(final CSimulation game, final int localPlayer, final int owner) {
		if (!isMinimapFiltered()) {
			return null;
		}
		return this.minimapColors[relation(game, localPlayer, owner)];
	}
}
