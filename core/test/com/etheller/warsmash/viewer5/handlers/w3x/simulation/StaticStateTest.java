package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.warsmash.movie.Movies;
import com.etheller.warsmash.parsers.jass.Jass2;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.save.SavedGame;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog.CScriptDialog;

/**
 * The simulation and the script runtime keep a game's state in that game's objects, so a second game in the same
 * process, or a loaded save, starts from what the map and the save give it. A static field that can change, or a
 * static collection or array, is only allowed for what is not a game's: the player's profile and options, tools,
 * per-call scratch objects and tables filled once from constants.
 */
class StaticStateTest {
	private static final Set<String> ALLOWED = new HashSet<>(Arrays.asList(
			// Scratch enumerators, reset before every use within one call.
			"simulation.CUnit#regionCheckerImpl", "simulation.CUnit#autoAttackTargetFinderEnum",
			"simulation.CUnit#autocastTargetFinderEnum",
			// The checksum switch and its file, which holds every game the process runs.
			"simulation.SimulationChecksum#enabled", "simulation.SimulationChecksum#fileName",
			"simulation.SimulationChecksum#detailTicks", "simulation.SimulationChecksum#writer",
			"simulation.SimulationChecksum#current", "simulation.SimulationChecksum#games",
			"simulation.SimulationChecksum#startedFile", "simulation.SimulationChecksum#shutdownHook",
			"simulation.SimulationChecksum#ended",
			// What the checksum keeps hashed of seldom changing objects, by weak key; the game never reads it.
			"simulation.SimulationChecksum#SCOPE_LINES", "simulation.SimulationChecksum#DESTRUCTABLE_LINES",
			// The player profile: its files, campaign progress and game caches as saved on disk.
			"simulation.campaign.CSaveStorage#profile", "simulation.campaign.CSaveStorage#lastBackgroundWrite",
			"simulation.campaign.CCampaignProgress#campaignList", "simulation.campaign.CCampaignProgress#loaded",
			"simulation.campaign.CCampaignProgress#PROGRESS", "simulation.campaign.CGameCache#loadedFromDisk",
			"simulation.campaign.CGameCache#FILES",
			// Where the game caches are read from: the profile's file, or what a replay recorded.
			"simulation.campaign.CGameCache#disk",
			// The player's sound options, and which game's volume groups the sound device plays by.
			"simulation.sound.CSoundMixer#soundOption", "simulation.sound.CSoundMixer#musicOption",
			"simulation.sound.CSoundMixer#ambientOption", "simulation.sound.CSoundMixer#movementOption",
			"simulation.sound.CSoundMixer#unitOption", "simulation.sound.CSoundMixer#audible",
			// Filled once from constants.
			"simulation.CUnitClassification#UNIT_EDITOR_KEY_TO_CLASSIFICATION",
			"simulation.SimulationTypeCodes#CODES",
			"simulation.SimulationChecksum#X_POWER_2N",
			"simulation.orders.OrderIdUtils#orderIdToString", "simulation.orders.OrderIdUtils#stringToOrderId",
			"simulation.orders.OrderIdUtils#NO_STRING", "simulation.data.CUnitRace#keyToRace",
			"simulation.abilities.harvest.CAbilityWispHarvest#TREE_ALIVE_TYPE_ONLY",
			"simulation.abilities.skills.human.archmage.CAbilityMassTeleport#TELEPORTED_UNITS",
			"simulation.abilities.skills.nightelf.units.CAbilityBouncingMissileFilter#KINDS",
			"simulation.combat.attacks.listeners.CUnitAttackDamageTakenListener#ENEMY_TARGET",
			// Which missing destructors were reported already, for the log.
			"jass.CommonNatives#REPORTED_MISSING_DESTRUCTORS",
			// The script log file.
			"interpreter.ast.util.JassLog#jassLogWriter",
			// Tables of the generated script lexer.
			"interpreter.JassLexer#channelNames", "interpreter.JassLexer#modeNames",
			// Shared immutable small script integers.
			"interpreter.ast.value.IntegerJassValue#CACHE",
			// What the saved games list shows of saves that failed to load.
			"save.SavedGame#FAILED_LOADS", "save.SavedGame#unreportedFailure",
			// Saving and loading: what is known of classes and of script functions, and the size the last save needed.
			"save.SavedGameRules#DROPPED_CLASSES", "save.SavedObjectGraph#FIELDS",
			"save.SavedObjectGraph#UNSTORABLE_CLASSES", "save.SavedObjectGraph#LIST_TYPES",
			"save.SavedObjectGraph#HASH_COLLECTIONS", "save.SavedObjectGraph#MAP_TYPES",
			"save.SavedObjectGraph#lastObjectCount", "save.SavedObjectGraph#PRIMITIVES",
			"save.SavedObjectGraph#FORMER_CLASS_NAMES",
			"save.SavedObjectGraph#FORMER_CLASS_NAMES_BEFORE_NAMED_TIMER_EVENT",
			"save.SavedObjectGraph#FORMER_CLASS_NAMES_SINCE_NAMED_TIMER_EVENT",
			"save.SavedScriptPositions$Program#FUNCTION_HASHES",
			"save.SavedScriptPositions#HASHED_FIELDS",
			// The platform's movie player.
			"movie.Movies#player",
			// The engine's settings, read once from its ini file.
			"constants#MAX_PLAYERS", "constants#GAME_VERSION", "constants#MODEL_DETAIL_PARTICLE_FACTOR",
			"constants#CATCH_CURSOR", "constants#FULL_SCREEN_MENU_BACKDROP", "constants#FIX_FLAT_FILES_TILESET_LOADING",
			"constants#ENABLE_MUSIC", "constants#LOAD_UNITS_FROM_WORLDEDIT_DATA",
			"constants#CRASH_ON_INCOMPATIBLE_132_FEATURES", "constants#INPUT_HOTKEY_MODE",
			"constants#PARSE_REIGN_OF_CHAOS_BETA_MODELS_INSTEAD", "constants#USE_NINE_ITEM_INVENTORY",
			"constants#RACE_MANAGER", "constants#JASS_FILE_LIST", "constants#ABILITY_COMPATIBILITY",
			// Arrays of constants, never written.
			"simulation.behaviors.cargohold.CargoReach#NEIGHBOR_X",
			"simulation.behaviors.cargohold.CargoReach#NEIGHBOR_Y",
			"simulation.config.War3MapConfigStartLoc#NO_INDICES",
			"simulation.config.War3MapConfigStartLoc#NO_PRIORITIES", "simulation.data.CUnitData#REQUIRES_TIER_X",
			"simulation.data.CUpgradeData#EFFECT", "simulation.data.CUpgradeData#EFFECT_BASE",
			"simulation.data.CUpgradeData#EFFECT_MOD", "simulation.data.CUpgradeData#EFFECT_CODE",
			"simulation.SimulationChecksum#CATEGORIES", "simulation.sound.SoundFileDuration#MPEG1_BITRATES",
			"simulation.sound.SoundFileDuration#MPEG2_BITRATES",
			"simulation.sound.SoundFileDuration#MPEG1_SAMPLE_RATES", "simulation.util.NoRenderController#WHITE",
			"jass.ai.AIAttack#SIEGE_UNITS", "save.SavedGameRules#DROPPED_PREFIXES", "movie.Movies#WIDTHS",
			"movie.Movies#AUDIO_EXTENSIONS", "constants#DEFAULT_JASS_FILE_LIST",
			"interpreter.JassParser#_decisionToDFA", "interpreter.JassParser#ruleNames",
			"interpreter.JassParser#_LITERAL_NAMES", "interpreter.JassParser#_SYMBOLIC_NAMES",
			"interpreter.JassParser#tokenNames", "interpreter.JassLexer#_decisionToDFA",
			"interpreter.JassLexer#ruleNames", "interpreter.JassLexer#_LITERAL_NAMES",
			"interpreter.JassLexer#_SYMBOLIC_NAMES", "interpreter.JassLexer#tokenNames",
			"interpreter.ast.value.ArrayJassValue#EMPTY",
			// The search area of one call, cleared at its start.
			"simulation.behaviors.cargohold.CargoReach#visited", "simulation.behaviors.cargohold.CargoReach#queue",
			"simulation.behaviors.cargohold.CargoReach#transportReach"));

	@Test
	void gameStateIsNotStatic() throws Exception {
		final List<String> found = new ArrayList<>();
		scan(CSimulation.class, "com.etheller.warsmash.viewer5.handlers.w3x.simulation", "simulation", found);
		scan(Jass2.class, "com.etheller.warsmash.parsers.jass", "jass", found);
		scan(GlobalScope.class, "com.etheller.interpreter", "interpreter", found);
		scan(CScriptDialog.class, "com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog", "dialog", found);
		scan(SavedGame.class, "com.etheller.warsmash.viewer5.handlers.w3x.save", "save", found);
		scan(Movies.class, "com.etheller.warsmash.movie", "movie", found);
		scanClass(WarsmashConstants.class, "constants", found);
		final List<String> unexpected = new ArrayList<>();
		for (final String field : found) {
			if (!ALLOWED.contains(field)) {
				unexpected.add(field);
			}
		}
		assertEquals(Collections.emptyList(), unexpected);
		assertTrue(found.size() > 10, "the class folders were not found: " + found);
	}

	private static void scanClass(final Class<?> type, final String shortName, final List<String> found) {
		for (final Field field : type.getDeclaredFields()) {
			if (isGameState(type, field)) {
				found.add(shortName + "#" + field.getName());
			}
		}
	}

	private static void scan(final Class<?> anchor, final String packageName, final String shortName,
			final List<String> found) throws IOException, URISyntaxException {
		final Path location = Paths.get(anchor.getProtectionDomain().getCodeSource().getLocation().toURI());
		if (Files.isDirectory(location)) {
			scan(anchor, location, packageName, shortName, found);
			return;
		}
		try (FileSystem jar = FileSystems.newFileSystem(location, (ClassLoader) null)) {
			scan(anchor, jar.getPath("/"), packageName, shortName, found);
		}
	}

	private static void scan(final Class<?> anchor, final Path root, final String packageName,
			final String shortName, final List<String> found) throws IOException {
		final Path folder = root.resolve(packageName.replace('.', '/'));
		try (Stream<Path> files = Files.walk(folder)) {
			files.filter(file -> file.toString().endsWith(".class")).forEach(file -> {
				final String relative = root.relativize(file).toString().replace('\\', '/');
				final String className = relative.substring(0, relative.length() - ".class".length()).replace('/',
						'.');
				final Class<?> type;
				final Field[] fields;
				try {
					type = Class.forName(className, false, anchor.getClassLoader());
					fields = type.getDeclaredFields();
				}
				catch (final ClassNotFoundException | LinkageError e) {
					// Needs a library the tests do not have (graphics, Android); nothing of a game's state.
					return;
				}
				for (final Field field : fields) {
					if (isGameState(type, field)) {
						found.add(shortName + className.substring(packageName.length()) + "#" + field.getName());
					}
				}
			});
		}
	}

	private static boolean isGameState(final Class<?> type, final Field field) {
		final int modifiers = field.getModifiers();
		if (!Modifier.isStatic(modifiers) || field.isSynthetic()) {
			return false;
		}
		if (type.isEnum() && field.getName().equals("VALUES") && field.getType().isArray()) {
			// values() copied once; never written.
			return false;
		}
		if (!Modifier.isFinal(modifiers)) {
			return true;
		}
		return Collection.class.isAssignableFrom(field.getType()) || Map.class.isAssignableFrom(field.getType())
				|| field.getType().isArray();
	}
}
