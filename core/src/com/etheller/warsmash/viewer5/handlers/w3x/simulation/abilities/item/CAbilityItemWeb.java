package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.units.CAbilityWeb;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;

/**
 * Spider Silk Broach: Web under the item order, without autocast.
 */
public class CAbilityItemWeb extends CAbilityWeb {
	public CAbilityItemWeb(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.itemweb;
	}

	@Override
	public int getAutoCastOnOrderId() {
		return 0;
	}

	@Override
	public int getAutoCastOffOrderId() {
		return 0;
	}

	@Override
	public CAbilityCategory getAbilityCategory() {
		return CAbilityCategory.ITEM;
	}
}
