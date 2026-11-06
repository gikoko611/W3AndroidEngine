package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.item;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitDeathReplacementEffect;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitDeathReplacementEffectPriority;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitDeathReplacementResult;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitDeathReplacementStacking;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;

/**
 * Soul Gem (Soul Trap): traps the soul of an enemy hero in the gem. Measured on 1.29: the hero is hidden at once,
 * still alive, unpaused and at full life, and the gem is used up; when the holder dies, the hero is shown again where
 * the holder fell.
 */
public class CAbilitySoulTrap extends CAbilityTargetSpellBase {

	public CAbilitySoulTrap(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.itemsoultrap;
	}

	@Override
	public void populateData(final GameObject worldEditorAbility, final int level) {
	}

	@Override
	public boolean doEffect(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		final CUnit targetUnit = target.visit(AbilityTargetVisitor.UNIT);
		if (targetUnit == null) {
			return false;
		}
		simulation.createTemporarySpellEffectOnUnit(targetUnit, getAlias(), CEffectType.TARGET);
		targetUnit.setHidden(true);
		caster.addDeathReplacementEffect(CUnitDeathReplacementEffectPriority.GENERALONDEATHACTIONS,
				new SoulRelease(targetUnit));
		return false;
	}

	private static final class SoulRelease implements CUnitDeathReplacementEffect {
		private final CUnit trapped;

		private SoulRelease(final CUnit trapped) {
			this.trapped = trapped;
		}

		@Override
		public CUnitDeathReplacementStacking onDeath(final CSimulation simulation, final CUnit holder,
				final CUnit killer, final CUnitDeathReplacementResult result) {
			holder.removeDeathReplacementEffect(CUnitDeathReplacementEffectPriority.GENERALONDEATHACTIONS, this);
			if (!this.trapped.isDead() && this.trapped.isHidden()) {
				this.trapped.setPointAndCheckUnstuck(holder.getX(), holder.getY(), simulation);
				this.trapped.setHidden(false);
			}
			return new CUnitDeathReplacementStacking();
		}
	}
}
