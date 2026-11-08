package com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityRanged;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold.CAbilityCargoHoldBurrow;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold.CAbilityLoad;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.neutral.CAbilityWayGate;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorMoveIntoRangeFor.PairAbilityLocator;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.cargohold.CBehaviorBoard;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;

public class CBehaviorBoardTransport extends CBehaviorMoveIntoRangeFor implements PairAbilityLocator {
	private final CBehaviorBoard boardMovingTransport;

	public CBehaviorBoardTransport(final CUnit unit) {
		super(unit);
		this.boardMovingTransport = new CBehaviorBoard(unit);
	}

	public CBehavior reset(final CSimulation game, final int higlightOrderId, final CWidget target) {
		final CUnit transport = target.visit(AbilityTargetVisitor.UNIT);
		if (transport != null) {
			final CAbilityLoad transportLoad = CAbilityLoad.getTransportLoad(game, this.unit, transport, true, false);
			if ((transportLoad != null) && transportLoad.isMovingTransport()) {
				return this.boardMovingTransport.reset(game, OrderIds.board, transport, transportLoad);
			}
		}
		return super.reset(game, higlightOrderId, target, this);
	}

	@Override
	public CAbilityRanged getPartnerAbility(final CSimulation game, final CUnit caster, final CUnit transport,
			final boolean ignoreRange, final boolean ignoreDisabled) {
		final CAbilityLoad transportLoad = CAbilityLoad.getTransportLoad(game, caster, transport, ignoreRange,
				ignoreDisabled);
		if (transportLoad == null) {
			final CAbilityCargoHoldBurrow burrow = CAbilityCargoHoldBurrow.getBurrowLoad(game, caster, transport,
					ignoreRange);
			if (burrow != null) {
				return burrow;
			}
			return CAbilityWayGate.getWayGateAbility(game, caster, transport,
					ignoreRange, ignoreDisabled);
		}
		return transportLoad;
	}
}
