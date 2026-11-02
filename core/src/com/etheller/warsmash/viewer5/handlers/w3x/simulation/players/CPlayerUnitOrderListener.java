package com.etheller.warsmash.viewer5.handlers.w3x.simulation.players;

public interface CPlayerUnitOrderListener {
	void issueTargetOrder(int unitHandleId, int abilityHandleId, int orderId, int targetHandleId, boolean queue);

	void issuePointOrder(int unitHandleId, int abilityHandleId, int orderId, float x, float y, boolean queue);

	/**
	 * One point order for several units, each already sent to its own formation slot. Kept as separate orders where
	 * the group cannot travel as one (network), so the units only lose walking at the group's speed.
	 */
	default void issueGroupPointOrder(final int[] unitHandleIds, final int[] abilityHandleIds, final int orderId,
			final float[] xs, final float[] ys, final boolean queue) {
		for (int i = 0; i < unitHandleIds.length; i++) {
			issuePointOrder(unitHandleIds[i], abilityHandleIds[i], orderId, xs[i], ys[i], queue);
		}
	}

	void issueDropItemAtPointOrder(int unitHandleId, int abilityHandleId, int orderId, int targetHandleId, float x,
			float y, final boolean queue);

	void issueDropItemAtTargetOrder(int unitHandleId, int abilityHandleId, int orderId, int targetItemHandleId,
			int targetHeroHandleId, final boolean queue);

	void issueImmediateOrder(int unitHandleId, int abilityHandleId, int orderId, boolean queue);

	void unitCancelTrainingItem(int unitHandleId, int cancelIndex);

	void issueGuiPlayerEvent(int eventId);
}
