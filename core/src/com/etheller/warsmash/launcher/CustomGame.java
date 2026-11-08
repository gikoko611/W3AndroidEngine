package com.etheller.warsmash.launcher;

import java.util.Arrays;
import java.util.List;

/** The launcher's skirmish lobby: the slots of the chosen map, the advanced options and the game they start. */
public final class CustomGame {
	/** The slot choices of the skirmish lobby: the engine's lobby type, the glue string and its fallback. */
	public static final String[][] SLOT_TYPES = { { "OPEN", "OPEN", "Open" }, { "CLOSED", "CLOSED", "Closed" },
		{ "COMPUTER_NEWBIE", "COMPUTER_NEWBIE", "Computer (Easy)" },
		{ "COMPUTER_NORMAL", "COMPUTER_NORMAL", "Computer (Normal)" },
		{ "COMPUTER_INSANE", "COMPUTER_INSANE", "Computer (Insane)" } };
	public static final int SLOT_YOU = -1;
	public static final int SLOT_HIDDEN = -2;
	public static final int SLOT_OPEN = 0;
	public static final int SLOT_COMPUTER_NORMAL = 3;
	/** Race item indexes as the lobby and the map information count them: 0 is random. */
	public static final String[][] RACES = { { "RANDOM", "Random" }, { "HUMAN", "Human" }, { "ORC", "Orc" },
		{ "UNDEAD", "Undead" }, { "NIGHT_ELF", "Night Elf" } };
	/** Advanced options in the order of the launch contract's names; Lock Teams is on in a new lobby. */
	public static final String[] OPTION_NAMES = { "lockTeams", "teamsTogether", "sharedControl", "randomRaces",
		"randomHero" };
	/** The options the page shows; teams together is not among them and stays on, as the original starts it. */
	public static final int[] OPTION_SHOWN = { 0, 2, 3, 4 };
	public static final int OPTION_TEAMS_TOGETHER = 1;
	/** The player colours by number: the glue string and the text used when the game data lacks it. */
	public static final String[][] COLOR_NAMES = { { "COLOR_RED", "Red" }, { "COLOR_BLUE", "Blue" },
		{ "COLOR_TEAL", "Teal" }, { "COLOR_PURPLE", "Purple" }, { "COLOR_YELLOW", "Yellow" },
		{ "COLOR_ORANGE", "Orange" }, { "COLOR_GREEN", "Green" }, { "COLOR_PINK", "Pink" }, { "COLOR_GRAY", "Gray" },
		{ "COLOR_LIGHT_BLUE", "Light Blue" }, { "COLOR_DARK_GREEN", "Dark Green" }, { "COLOR_BROWN", "Brown" } };
	public static final int[] PLAYER_COLORS = { 0xFFFF0303, 0xFF0042FF, 0xFF1CE6B9, 0xFF540081, 0xFFFFFC00,
		0xFFFE8A0E, 0xFF20C000, 0xFFE55BB0, 0xFF959697, 0xFF7EBFF1, 0xFF106246, 0xFF4E2A04 };

	public int[] slotTypes = new int[0];
	public int[] slotRaces = new int[0];
	/** The team of a slot, -1 for the map's own. */
	public int[] slotTeams = new int[0];
	/** The colour of a slot, -1 for the map's own. */
	public int[] slotColors = new int[0];
	public boolean[] advanced = new boolean[OPTION_NAMES.length];

	/** Fixed player settings hold only with custom forces; melee maps carry the flag without them. */
	public static boolean fixedSlots(final GameCatalog.MapDetails details) {
		return details.fixedSettings && details.customForces;
	}

	/** The advanced options as the launcher stores them: the names of those that are on. */
	public static boolean[] advancedOptions(final String stored) {
		final List<String> on = Arrays.asList((stored == null ? OPTION_NAMES[0] : stored).split(","));
		final boolean[] options = new boolean[OPTION_NAMES.length];
		for (int i = 0; i < options.length; i++) {
			options[i] = on.contains(OPTION_NAMES[i]);
		}
		return options;
	}

	/** Turns an advanced option on or off and gives what the launcher stores for them. */
	public String setOption(final int option, final boolean on) {
		this.advanced[option] = on;
		final StringBuilder names = new StringBuilder();
		for (int i = 0; i < OPTION_NAMES.length; i++) {
			if (this.advanced[i]) {
				names.append(names.length() > 0 ? "," : "").append(OPTION_NAMES[i]);
			}
		}
		return names.toString();
	}

	/**
	 * The lobby's first setup: the first user slot is the player, the computer slots the map plays (all of them with
	 * fixed settings), and when the map gives no computer, the next user slot is Computer (Normal) as in the original
	 * single player lobby. The others are open.
	 */
	public void chooseSlots(final GameCatalog.MapDetails details) {
		final int count = details.slots.size();
		this.slotTypes = new int[count];
		this.slotRaces = new int[count];
		this.slotTeams = new int[count];
		this.slotColors = new int[count];
		Arrays.fill(this.slotTeams, -1);
		Arrays.fill(this.slotColors, -1);
		boolean you = false;
		boolean computer = false;
		for (int i = 0; i < count; i++) {
			final GameCatalog.Slot slot = details.slots.get(i);
			this.slotRaces[i] = (slot.race >= 0) && (slot.race < RACES.length) ? slot.race : 0;
			if (slot.type == 1) {
				this.slotTypes[i] = you ? SLOT_OPEN : SLOT_YOU;
				you = true;
			}
			else if (slot.type == 2) {
				this.slotTypes[i] = !computer || details.fixedSettings ? SLOT_COMPUTER_NORMAL : SLOT_OPEN;
				computer = true;
			}
			else {
				this.slotTypes[i] = SLOT_HIDDEN;
			}
		}
		if (!computer && !fixedSlots(details)) {
			for (int i = 0; i < count; i++) {
				if ((details.slots.get(i).type == 1) && (this.slotTypes[i] == SLOT_OPEN)) {
					this.slotTypes[i] = SLOT_COMPUTER_NORMAL;
					break;
				}
			}
		}
	}

	/** The team a slot starts in: its force when the map sets forces, else a team of its own. */
	public static int defaultTeam(final GameCatalog.MapDetails details, final int index) {
		final GameCatalog.Slot slot = details.slots.get(index);
		if (details.customForces && (slot.force >= 0) && (slot.force < details.forces.size())) {
			return slot.force;
		}
		return index;
	}

	/** The colour a slot shows: the one chosen, else the map's player number. */
	public int colorOf(final GameCatalog.MapDetails details, final int index) {
		return this.slotColors[index] >= 0 ? this.slotColors[index]
				: Math.abs(details.slots.get(index).id) % PLAYER_COLORS.length;
	}

	/** The race the player chose (lobby race index), 0 for random or no player slot. */
	public int playerRace() {
		int race = 0;
		for (int i = 0; i < this.slotTypes.length; i++) {
			if (this.slotTypes[i] == SLOT_YOU) {
				race = this.slotRaces[i];
			}
		}
		return race;
	}

	/** The game this lobby starts on a map. */
	public LaunchRequest request(final GameCatalog.MapDetails details) {
		final StringBuilder slots = new StringBuilder();
		if (!fixedSlots(details)) {
			for (int i = 0; i < details.slots.size(); i++) {
				if (this.slotTypes[i] == SLOT_HIDDEN) {
					continue;
				}
				final String type = this.slotTypes[i] == SLOT_YOU ? "USER" : SLOT_TYPES[this.slotTypes[i]][0];
				slots.append(details.slots.get(i).id).append(':').append(type).append(':').append(this.slotRaces[i])
						.append(':').append(this.slotTeams[i]).append(':').append(this.slotColors[i]).append(';');
			}
		}
		final StringBuilder options = new StringBuilder();
		for (int i = 0; i < OPTION_NAMES.length; i++) {
			if (this.advanced[i] || (i == OPTION_TEAMS_TOGETHER)) {
				options.append(options.length() > 0 ? "," : "").append(OPTION_NAMES[i]);
			}
		}
		final LaunchRequest request = new LaunchRequest();
		request.customMap = details.path;
		request.mapFile = details.path;
		request.slots = slots.toString();
		request.options = options.toString();
		return request;
	}
}
