package com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.inventory.CAbilityInventory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.ExternStringMsgTargetCheckReceiver;

public class COrderDropItemAtTargetWidget implements COrder {
	private final int playerIndex;
	private final int abilityHandleId;
	private final int orderId;
	private final int itemHandleId;
	private final int targetHeroHandleId;
	private final boolean queued;

	public COrderDropItemAtTargetWidget(final int playerIndex, final int abilityHandleId, final int orderId,
			final int itemHandleId, final int targetHeroHandleId, final boolean queued) {
		this.playerIndex = playerIndex;
		this.abilityHandleId = abilityHandleId;
		this.orderId = orderId;
		this.itemHandleId = itemHandleId;
		this.targetHeroHandleId = targetHeroHandleId;
		this.queued = queued;
	}

	@Override
	public int getAbilityHandleId() {
		return this.abilityHandleId;
	}

	@Override
	public int getOrderId() {
		return this.orderId;
	}

	@Override
	public int getPlayerIndex() {
		return this.playerIndex;
	}

	@Override
	public CWidget getTarget(final CSimulation game) {
		final CWidget target = game.getWidget(this.targetHeroHandleId);
		return target;
	}

	@Override
	public boolean isQueued() {
		return this.queued;
	}

	@Override
	public CBehavior begin(final CSimulation game, final CUnit caster) {
		final CAbility abilityForHandle = game.getAbility(this.abilityHandleId);
		if (abilityForHandle == null) {
			game.getCommandErrorListener().showInterfaceError(caster.getPlayerIndex(), "NOTEXTERN: No such ability");
			return caster.pollNextOrderBehavior(game);
		}
		final CItem itemToDrop = heldItemToDrop(game, caster, abilityForHandle, this.itemHandleId);
		final CWidget targetWidget = game.getWidget(this.targetHeroHandleId);
		if ((itemToDrop == null) || !(targetWidget instanceof CUnit)) {
			return caster.pollNextOrderBehavior(game);
		}
		final CAbilityInventory ability = (CAbilityInventory) abilityForHandle;
		ability.checkCanUse(game, caster, this.playerIndex, this.orderId, false,
				this.abilityActivationReceiver.reset());
		if (this.abilityActivationReceiver.isUseOk()) {
			final CUnit targetHero = (CUnit) targetWidget;
			final ExternStringMsgTargetCheckReceiver<CWidget> targetReceiver = (ExternStringMsgTargetCheckReceiver<CWidget>) targetCheckReceiver;
			ability.checkCanTarget(game, caster, this.playerIndex, this.orderId, false, targetHero,
					targetReceiver.reset());
			if (targetReceiver.getTarget() != null) {
				return ability.beginDropItem(game, caster, this.playerIndex, this.orderId, itemToDrop, targetHero);
			}
			else {
				game.getCommandErrorListener().showInterfaceError(caster.getPlayerIndex(),
						targetReceiver.getExternStringKey());
				return caster.pollNextOrderBehavior(game);
			}
		}
		else {
			game.getCommandErrorListener().showInterfaceError(caster.getPlayerIndex(),
					this.abilityActivationReceiver.getExternStringKey());
			return caster.pollNextOrderBehavior(game);
		}
	}

	/**
	 * The item a drop/give order moves, or null when the order is malformed: the ability must be the caster's own
	 * inventory and the item must still be in it. Such orders are ignored, like the game ignores them.
	 */
	static CItem heldItemToDrop(final CSimulation game, final CUnit caster, final CAbility ability,
			final int itemHandleId) {
		if (!(ability instanceof CAbilityInventory) || !caster.getAbilities().contains(ability)) {
			return null;
		}
		final CWidget item = game.getWidget(itemHandleId);
		if (!(item instanceof CItem) || (((CAbilityInventory) ability).getSlot((CItem) item) == -1)) {
			return null;
		}
		return (CItem) item;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + this.abilityHandleId;
		result = (prime * result) + this.itemHandleId;
		result = (prime * result) + this.orderId;
		result = (prime * result) + (this.queued ? 1231 : 1237);
		result = (prime * result) + this.targetHeroHandleId;
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final COrderDropItemAtTargetWidget other = (COrderDropItemAtTargetWidget) obj;
		if (this.abilityHandleId != other.abilityHandleId) {
			return false;
		}
		if (this.itemHandleId != other.itemHandleId) {
			return false;
		}
		if (this.orderId != other.orderId) {
			return false;
		}
		if (this.queued != other.queued) {
			return false;
		}
		if (this.targetHeroHandleId != other.targetHeroHandleId) {
			return false;
		}
		return true;
	}

	@Override
	public void fireEvents(final CSimulation game, final CUnit unit) {
		// TODO Auto-generated method stub
	}

}
