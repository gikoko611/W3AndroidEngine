package com.etheller.warsmash.launcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One player slot of a launched custom game, as the launch contract writes it: "player:TYPE:race:team:color", slots
 * separated by ';'. TYPE is the engine's lobby type name (USER for the local player); a number of -1, or one left
 * out, keeps what the map has.
 */
public final class LauncherSlot {
	public static final int MAP_VALUE = -1;

	public final int player;
	public final String type;
	public final int race;
	public final int team;
	public final int color;

	public LauncherSlot(final int player, final String type, final int race, final int team, final int color) {
		this.player = player;
		this.type = type;
		this.race = race;
		this.team = team;
		this.color = color;
	}

	public boolean isUser() {
		return "USER".equals(this.type);
	}

	/** The slot's number, else the map's own. */
	public static int orMap(final int slotValue, final int mapValue) {
		return slotValue < 0 ? mapValue : slotValue;
	}

	/** The slots of a launch; entries that are not slots are left out. */
	public static List<LauncherSlot> parse(final String slots) {
		if (slots == null) {
			return Collections.emptyList();
		}
		final List<LauncherSlot> parsed = new ArrayList<>();
		for (final String entry : slots.split(";")) {
			final String[] parts = entry.trim().split(":");
			if ((parts.length < 2) || parts[1].trim().isEmpty()) {
				continue;
			}
			try {
				parsed.add(new LauncherSlot(Integer.parseInt(parts[0].trim()), parts[1].trim(), number(parts, 2),
						number(parts, 3), number(parts, 4)));
			}
			catch (final NumberFormatException e) {
				continue;
			}
		}
		return parsed;
	}

	/** The first user slot of a launch, null when it has none. */
	public static LauncherSlot firstUser(final String slots) {
		for (final LauncherSlot slot : parse(slots)) {
			if (slot.isUser()) {
				return slot;
			}
		}
		return null;
	}

	/** The slot of a player, the last one when a launch lists it twice; null when it is not listed. */
	public static LauncherSlot of(final List<LauncherSlot> slots, final int player) {
		LauncherSlot found = null;
		for (final LauncherSlot slot : slots) {
			if (slot.player == player) {
				found = slot;
			}
		}
		return found;
	}

	/** Appends this slot as the launch contract writes it. */
	public StringBuilder appendTo(final StringBuilder slots) {
		return slots.append(this.player).append(':').append(this.type).append(':').append(this.race).append(':')
				.append(this.team).append(':').append(this.color).append(';');
	}

	private static int number(final String[] parts, final int index) {
		if (parts.length <= index) {
			return MAP_VALUE;
		}
		final int value = Integer.parseInt(parts[index].trim());
		return value < 0 ? MAP_VALUE : value;
	}
}
