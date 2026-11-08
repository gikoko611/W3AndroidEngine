package com.etheller.warsmash.viewer5.handlers.w3x.simulation.players;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;

/**
 * A player's selection as part of the game, so that scripts see the same selection on every machine and in a replay.
 * <p>
 * Measured on 1.29: a selection made by clicking or by SelectUnit reaches the game (GroupEnumUnitsSelected, the
 * selection events) a step later, through the player's commands; IsUnitSelected sees a script's SelectUnit and
 * ClearSelection at once. A unit that dies or is hidden by ShowUnit leaves the selection a little later without a
 * deselection event; another player's unit that goes out of sight fires the deselection event once and stays in the
 * selection, and fires none again, neither when it comes back into sight nor when it is deselected.
 */
public final class CPlayerSelection {
	private final List<CUnit> units = new ArrayList<>();
	private final List<CUnit> shown = new ArrayList<>();
	private final List<CUnit> outOfSight = new ArrayList<>();
	private final List<CUnit> goingOutOfSight = new ArrayList<>();
	private final List<CUnit> hiddenByScript = new ArrayList<>();
	private final List<CUnit> leaving = new ArrayList<>();
	/** Pairs: a selected unit about to be removed, then the unit that takes its place in the selection. */
	private final List<CUnit> replacements = new ArrayList<>();

	public interface Events {
		void fire(CUnit unit, boolean selected);
	}

	/**
	 * The selection GroupEnumUnitsSelected and the selection events work with.
	 */
	public List<CUnit> getUnits() {
		return Collections.unmodifiableList(this.units);
	}

	/**
	 * IsUnitSelected: the game's selection with the script's own changes that have not reached it yet.
	 */
	public boolean isSelected(final CUnit unit) {
		return this.shown.contains(unit);
	}

	public void scriptSelect(final CUnit unit, final boolean select) {
		if (select) {
			if (selectable(unit) && !this.shown.contains(unit)
					&& (this.shown.size() < WarsmashConstants.MAX_SELECTION_SIZE)) {
				this.shown.add(unit);
			}
		}
		else {
			this.shown.remove(unit);
		}
	}

	public void scriptClear() {
		this.shown.clear();
	}

	/**
	 * The player's selection command: what was deselected, then what was selected.
	 */
	public void apply(final List<CUnit> deselected, final List<CUnit> selected, final Events events) {
		for (final CUnit unit : deselected) {
			this.shown.remove(unit);
			if (this.units.remove(unit) && !this.outOfSight.remove(unit)) {
				events.fire(unit, false);
			}
		}
		for (final CUnit unit : selected) {
			if (!selectable(unit) || this.units.contains(unit)
					|| (this.units.size() >= WarsmashConstants.MAX_SELECTION_SIZE)) {
				continue;
			}
			this.units.add(unit);
			if (!this.shown.contains(unit)) {
				this.shown.add(unit);
			}
			events.fire(unit, true);
		}
	}

	/**
	 * ShowUnit(false): the unit leaves the selection like a unit that died.
	 */
	public void unitHidden(final CUnit unit) {
		if ((this.units.contains(unit) || this.shown.contains(unit)) && !this.hiddenByScript.contains(unit)) {
			this.hiddenByScript.add(unit);
		}
	}

	/**
	 * A selected unit that is about to go (a merge, a dismount) names the unit that takes its place when it leaves.
	 */
	public void unitReplaced(final CUnit unit, final CUnit replacement) {
		if ((replacement != null) && (this.units.contains(unit) || this.shown.contains(unit))) {
			this.replacements.add(unit);
			this.replacements.add(replacement);
		}
	}

	/**
	 * Once a step. A unit that is gone (dead, hidden) is noticed at the step after it went and leaves the selection
	 * the step after that, without an event; another player's unit out of sight fires the deselection event the
	 * same way, once, and stays in the selection.
	 *
	 * @param stays   whether a hidden unit stays selected (a worker inside a mine or a building it builds)
	 * @param inSight whether the player still sees the unit
	 */
	public void update(final Predicate<CUnit> stays, final Predicate<CUnit> inSight, final Events events) {
		replace(this.units);
		replace(this.shown);
		this.units.removeAll(this.leaving);
		this.shown.removeAll(this.leaving);
		for (int i = this.replacements.size() - 2; i >= 0; i -= 2) {
			final CUnit unit = this.replacements.get(i);
			if (this.leaving.contains(unit) || (!this.units.contains(unit) && !this.shown.contains(unit))) {
				this.replacements.remove(i + 1);
				this.replacements.remove(i);
			}
		}
		this.leaving.clear();
		noticeGone(this.units, stays);
		noticeGone(this.shown, stays);
		this.hiddenByScript.clear();
		// A unit back in sight fires no selection event and, deselected for the scripts already, none when the
		// player deselects it either (1.29).
		for (int i = this.outOfSight.size() - 1; i >= 0; i--) {
			if (!this.units.contains(this.outOfSight.get(i))) {
				this.outOfSight.remove(i);
			}
		}
		for (int i = this.goingOutOfSight.size() - 1; i >= 0; i--) {
			final CUnit unit = this.goingOutOfSight.get(i);
			if (!this.units.contains(unit) || inSight.test(unit)) {
				this.goingOutOfSight.remove(i);
			}
		}
		for (int i = 0; i < this.units.size(); i++) {
			final CUnit unit = this.units.get(i);
			if (this.outOfSight.contains(unit) || inSight.test(unit)) {
				continue;
			}
			if (this.goingOutOfSight.remove(unit)) {
				this.outOfSight.add(unit);
				events.fire(unit, false);
			}
			else {
				this.goingOutOfSight.add(unit);
			}
		}
	}

	/** The game puts the replacement in as it takes the leaving unit out, without an event. */
	private void replace(final List<CUnit> list) {
		for (int i = 0; i < this.replacements.size(); i += 2) {
			final CUnit unit = this.replacements.get(i);
			final CUnit replacement = this.replacements.get(i + 1);
			if (this.leaving.contains(unit) && list.contains(unit) && selectable(replacement)
					&& !list.contains(replacement)) {
				list.add(replacement);
			}
		}
	}

	private void noticeGone(final List<CUnit> list, final Predicate<CUnit> stays) {
		for (int i = 0; i < list.size(); i++) {
			final CUnit unit = list.get(i);
			if ((unit.isDead() || this.hiddenByScript.contains(unit) || (unit.isHidden() && !stays.test(unit)))
					&& !this.leaving.contains(unit)) {
				this.leaving.add(unit);
			}
		}
	}

	private static boolean selectable(final CUnit unit) {
		return (unit != null) && !unit.isDead() && !unit.isHidden();
	}
}
