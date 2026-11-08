package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.creeps;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.mountainking.CAbilityThunderBolt;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;

/**
 * Hurl Boulder: the creep Storm Bolt (rock golems), with its own order.
 */
public class CAbilityHurlBoulder extends CAbilityThunderBolt {
	public CAbilityHurlBoulder(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.creepthunderbolt;
	}
}
