package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import org.junit.jupiter.api.Test;

import com.badlogic.gdx.files.FileHandle;
import com.etheller.warsmash.testing.TestObjects;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff.ABBuffSlow;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CGameExit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound.CSoundMixer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CSoundVolumeGroup;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuff;

/**
 * State a game changes as it runs belongs to that game: two simulations in one process (a second game, a replay run
 * beside the game) do not see each other's.
 */
class GameStateIsolationTest {
	private static CSimulation simulation() {
		final CSimulation simulation = TestObjects.allocate(CSimulation.class);
		TestObjects.set(simulation, "activeTimers", new LinkedList<CTimer>());
		TestObjects.set(simulation, "addedTimers", new ArrayList<CTimer>());
		TestObjects.set(simulation, "timerFiringExactTime", Double.NaN);
		TestObjects.set(simulation, "queuedCinematics", new ArrayList<String>());
		return simulation;
	}

	private static class ScriptTimer extends CTimer {
		private Runnable callback = () -> {
		};

		ScriptTimer(final float timeout) {
			setTimeoutTime(timeout);
		}

		@Override
		protected boolean isExactTiming() {
			return true;
		}

		@Override
		public void onFire(final CSimulation simulation) {
			this.callback.run();
		}
	}

	@Test
	void anExactTimerCallbackSetsTheClockOfItsOwnGameOnly() {
		final CSimulation first = simulation();
		final CSimulation second = simulation();
		TestObjects.set(first, "gameTurnTick", 30);
		final ScriptTimer firing = new ScriptTimer(1.0f);
		firing.start(first);
		final ScriptTimer startedInSecond = new ScriptTimer(0.5f);
		final double[] seen = new double[2];
		firing.callback = () -> {
			seen[0] = first.getTimerFiringExactTime();
			seen[1] = second.getTimerFiringExactTime();
			startedInSecond.start(second);
		};
		TestObjects.set(first, "gameTurnTick", 50);
		firing.fire(first);

		assertEquals(2.5, seen[0], 1e-6);
		assertTrue(Double.isNaN(seen[1]));
		assertTrue(Double.isNaN(first.getTimerFiringExactTime()));
		// The second game's clock is its own step 0, not the first game's firing time.
		assertEquals(10, startedInSecond.getEngineFireTick());
	}

	@Test
	void theStandardSlowFollowsTheGamesOwnConstants() {
		final ABBuffSlow normal = slow(constants(0.25f, 0.3f));
		final ABBuffSlow modded = slow(constants(0.5f, 0.6f));
		final ABBuffSlow normalAgain = slow(constants(0.25f, 0.3f));

		assertEquals(-0.25f, debuff(normal, "attackSpeedDebuff").getValue());
		assertEquals(-0.3f, debuff(normal, "moveSpeedDebuff").getValue());
		assertEquals(-0.5f, debuff(modded, "attackSpeedDebuff").getValue());
		assertEquals(-0.6f, debuff(modded, "moveSpeedDebuff").getValue());
		assertEquals(-0.25f, debuff(normalAgain, "attackSpeedDebuff").getValue());
	}

	@Test
	void randomTypeListsBelongToTheirGame() {
		final CSimulation first = simulation();
		final CSimulation second = simulation();

		assertSame(first.getRandomTypes(), first.getRandomTypes());
		assertNotSame(first.getRandomTypes(), second.getRandomTypes());
	}

	@Test
	void aGameExitStartsEmpty() {
		final CGameExit exit = new CGameExit();
		assertEquals(false, exit.hasNextLevel());
		assertEquals(false, exit.isCampaignSelectScreenForced());
		exit.setNextLevel("Maps/Campaign/Human02.w3m", "1,2");
		assertEquals("Maps/Campaign/Human02.w3m", exit.getNextLevel());
		assertEquals("1,2", exit.getNextComputerSlots());
		assertEquals(false, new CGameExit().hasNextLevel());
	}

	@Test
	void aGameExitCarriesTheSaveToLoad() {
		final CGameExit exit = new CGameExit();
		assertNull(exit.getSavedGame());
		final FileHandle save = new FileHandle("Human02.w3z");
		exit.setNextLevel("Maps/Campaign/Human02.w3m", null);
		exit.setSavedGame(save, true);
		assertSame(save, exit.getSavedGame());
		assertTrue(exit.isSavedGameLoadedByScript());
		assertNull(new CGameExit().getSavedGame());
	}

	@Test
	void cinematicsPlayAfterTheGameThatAskedForThem() {
		final CSimulation first = simulation();
		final CSimulation second = simulation();
		first.queueCinematic("HumanEd");
		first.queueCinematic("");
		first.queueCinematic("OutroX");
		second.queueCinematic("OrcEd");

		assertEquals(Arrays.asList("HumanEd", "OutroX"), first.takeQueuedCinematics());
		assertEquals(Collections.emptyList(), first.takeQueuedCinematics());
		assertEquals(Arrays.asList("OrcEd"), second.takeQueuedCinematics());
	}

	@Test
	void volumeGroupsBelongToTheirGame() {
		final CSoundMixer first = new CSoundMixer();
		final CSoundMixer second = new CSoundMixer();
		first.makeAudible();
		first.setGroupScale(CSoundVolumeGroup.MUSIC, 0.25f);
		assertEquals(0.25f, first.getGroupScale(CSoundVolumeGroup.MUSIC));
		assertEquals(1f, second.getGroupScale(CSoundVolumeGroup.MUSIC));
		assertSame(first, CSoundMixer.audible());

		second.release();
		assertSame(first, CSoundMixer.audible());
		first.release();
		assertNotSame(first, CSoundMixer.audible());
		assertEquals(1f, CSoundMixer.audible().getGroupScale(CSoundVolumeGroup.MUSIC));
	}

	private static CGameplayConstants constants(final float attackSpeed, final float moveSpeed) {
		final CGameplayConstants constants = TestObjects.allocate(CGameplayConstants.class);
		TestObjects.set(constants, "frostAttackSpeedDecrease", attackSpeed);
		TestObjects.set(constants, "frostMoveSpeedDecrease", moveSpeed);
		return constants;
	}

	private static ABBuffSlow slow(final CGameplayConstants constants) {
		final CSimulation game = simulation();
		TestObjects.set(game, "gameplayConstants", constants);
		return new ABBuffSlow(game, 1, War3ID.fromString("Bslo"), new HashMap<>(), null, null, 10f, false);
	}

	private static NonStackingStatBuff debuff(final ABBuffSlow slow, final String field) {
		return (NonStackingStatBuff) TestObjects.get(slow, field);
	}
}
