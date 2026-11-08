package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitClassification;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.inventory.CAbilityInventory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.inventory.ItemChargeSpentOnEffect;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityUnitOrPointTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentModel;

/**
 * Town Portal: the user and up to Data A of its own units within the area go to the targeted own town hall, or to
 * the target point when an own town hall is within the area of it, once the casting time has passed; with clustering
 * (Data B) the units land around the user. Measured on 1.29: the casting time counts from the start of the cast, the
 * scroll is used up only when the teleport happens, a unit target must be a town hall, and a point target is the
 * landing spot itself.
 */
public class CAbilityItemTownPortal extends CAbilityUnitOrPointTargetSpellBase implements ItemChargeSpentOnEffect {
	private int numberOfUnits;
	private boolean useClustering;
	private float areaOfEffect;

	private int teleportTick;
	private CUnit destination;
	private float destinationX;
	private float destinationY;
	private CItem item;
	private SimulationRenderComponentModel sourceEffect;
	private SimulationRenderComponentModel targetEffect;

	public CAbilityItemTownPortal(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public void setItemAbility(final CItem item, final int slot) {
		this.item = item;
	}

	@Override
	public CItem getItem() {
		return this.item;
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.itemtownportal;
	}

	@Override
	public void populateData(final GameObject worldEditorAbility, final int level) {
		this.numberOfUnits = worldEditorAbility.getFieldAsInteger(AbilityFields.DATA_A + level, 0);
		this.useClustering = worldEditorAbility.getFieldAsBoolean(AbilityFields.DATA_B + level, 0);
		this.areaOfEffect = worldEditorAbility.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
	}

	private static boolean isOwnTownHall(final CUnit caster, final CUnit unit) {
		return (unit.getPlayerIndex() == caster.getPlayerIndex()) && !unit.isDead()
				&& unit.getClassifications().contains(CUnitClassification.TOWNHALL)
				&& !unit.isConstructingOrUpgrading();
	}

	@Override
	protected void innerCheckCanTarget(final CSimulation game, final CUnit unit, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		final CUnit targetUnit = target.visit(AbilityTargetVisitor.UNIT);
		if ((targetUnit == null) || !isOwnTownHall(unit, targetUnit)) {
			receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_FRIENDLY_TOWN_HALL);
			return;
		}
		super.innerCheckCanTarget(game, unit, orderId, target, receiver);
	}

	@Override
	protected void innerCheckCanTarget(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityPointTarget target, final AbilityTargetCheckReceiver<AbilityPointTarget> receiver) {
		final CUnit hall = findTownHall(game, unit, target.getX(), target.getY());
		if ((hall == null) || (hall.distanceSquaredNoCollision(target.getX(), target.getY()) > (this.areaOfEffect * this.areaOfEffect))) {
			receiver.targetCheckFailed(CommandStringErrorKeys.THERE_ARE_NO_FRIENDLY_TOWN_HALLS_TO_TOWN_PORTAL_TO);
			return;
		}
		super.innerCheckCanTarget(game, unit, orderId, target, receiver);
	}

	private CUnit findTownHall(final CSimulation simulation, final CUnit caster, final float x, final float y) {
		CUnit best = null;
		double bestDistance = Double.MAX_VALUE;
		for (final CUnit unit : simulation.getUnits()) {
			if (isOwnTownHall(caster, unit)) {
				final double distance = unit.distanceSquaredNoCollision(x, y);
				if (distance < bestDistance) {
					bestDistance = distance;
					best = unit;
				}
			}
		}
		return best;
	}

	@Override
	public boolean doEffect(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		final CUnit targetUnit = target.visit(AbilityTargetVisitor.UNIT);
		this.destination = targetUnit != null ? targetUnit
				: findTownHall(simulation, caster, target.getX(), target.getY());
		if (this.destination == null) {
			return false;
		}
		if (targetUnit != null) {
			this.destinationX = targetUnit.getX();
			this.destinationY = targetUnit.getY();
		}
		else {
			this.destinationX = target.getX();
			this.destinationY = target.getY();
		}
		final float delay = Math.max(0, getCastingTime() - caster.getUnitType().getCastPoint());
		this.teleportTick = simulation.getGameTurnTick()
				+ CTimer.ceilSteps(delay);
		this.sourceEffect = simulation.spawnSpellEffectOnPoint(caster.getX(), caster.getY(), 0, getAlias(),
				CEffectType.CASTER, 0);
		this.targetEffect = simulation.spawnSpellEffectOnPoint(this.destinationX, this.destinationY, 0,
				getAlias(), CEffectType.AREA_EFFECT, 0);
		return true;
	}

	@Override
	public boolean doChannelTick(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		if (simulation.getGameTurnTick() < this.teleportTick) {
			return true;
		}
		final CUnit hall = this.destination;
		if ((hall == null) || hall.isDead()) {
			return false;
		}
		final CAbilityInventory inventory = caster.getInventoryData();
		if ((getItem() != null) && (inventory != null)) {
			inventory.spendItemCharge(simulation, caster, getItem());
		}
		final List<CUnit> units = new ArrayList<>();
		final float casterX = caster.getX();
		final float casterY = caster.getY();
		simulation.getWorldCollision().enumUnitsInRange(casterX, casterY, this.areaOfEffect, (enumUnit) -> {
			if ((enumUnit != caster) && (enumUnit.getPlayerIndex() == caster.getPlayerIndex())
					&& !enumUnit.isBuilding() && !enumUnit.isDead() && !enumUnit.isHidden()
					&& !enumUnit.isMovementDisabled()) {
				units.add(enumUnit);
			}
			return false;
		});
		// Measured on 1.29: the units take the free spots around the user in the order they were made.
		units.sort(Comparator.comparingInt(CUnit::getHandleId));
		while (units.size() > Math.max(0, this.numberOfUnits - 1)) {
			units.remove(units.size() - 1);
		}
		simulation.spawnSpellEffectOnPoint(casterX, casterY, 0, getAlias(), CEffectType.SPECIAL, 0).remove();
		final float destX = this.destinationX;
		final float destY = this.destinationY;
		caster.setPointAndCheckUnstuck(destX, destY, simulation);
		for (final CUnit unit : units) {
			// Measured on 1.29: the units brought along stand where they land instead of going back to what they did.
			unit.order(simulation, OrderIds.stop, null);
			if (this.useClustering) {
				unit.setPointAndCheckUnstuck(caster.getX(), caster.getY(), simulation);
			}
			else {
				unit.setPointAndCheckUnstuck(destX + (unit.getX() - casterX), destY + (unit.getY() - casterY),
						simulation);
			}
		}
		simulation.spawnSpellEffectOnPoint(caster.getX(), caster.getY(), 0, getAlias(), CEffectType.SPECIAL, 0)
				.remove();
		return false;
	}

	@Override
	public void doChannelEnd(final CSimulation game, final CUnit unit, final AbilityTarget target,
			final boolean interrupted) {
		if (this.sourceEffect != null) {
			this.sourceEffect.remove();
			this.sourceEffect = null;
		}
		if (this.targetEffect != null) {
			this.targetEffect.remove();
			this.targetEffect = null;
		}
	}

	@Override
	public boolean isMagic() {
		return false;
	}

	@Override
	public CAbilityCategory getAbilityCategory() {
		return CAbilityCategory.ITEM;
	}
}
