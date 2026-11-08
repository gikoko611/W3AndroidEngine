package com.etheller.warsmash.viewer5.handlers.w3x.simulation.config;

import java.util.Arrays;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CStartLocPrio;

public class War3MapConfigStartLoc {
	private static final int[] NO_INDICES = new int[0];
	private static final CStartLocPrio[] NO_PRIORITIES = new CStartLocPrio[0];

	private float x;
	private float y;
	private int[] otherStartIndices = NO_INDICES;
	private CStartLocPrio[] otherStartLocPriorities = NO_PRIORITIES;
	private int[] enemyStartIndices = NO_INDICES;
	private CStartLocPrio[] enemyStartLocPriorities = NO_PRIORITIES;

	public void setX(final float x) {
		this.x = x;
	}

	public void setY(final float y) {
		this.y = y;
	}

	public float getX() {
		return this.x;
	}

	public float getY() {
		return this.y;
	}

	public int[] getOtherStartIndices() {
		return this.otherStartIndices;
	}

	public CStartLocPrio[] getOtherStartLocPriorities() {
		return this.otherStartLocPriorities;
	}

	public int getStartLocPrioSlot(final int prioSlotIndex) {
		return slotOther(this.otherStartIndices, prioSlotIndex);
	}

	public CStartLocPrio getStartLocPrio(final int prioSlotIndex) {
		return slotPriority(this.otherStartLocPriorities, prioSlotIndex);
	}

	public void setStartLocPrioCount(final int startLocPrioCount) {
		this.otherStartIndices = resize(this.otherStartIndices, startLocPrioCount);
		this.otherStartLocPriorities = resize(this.otherStartLocPriorities, startLocPrioCount);
	}

	public void setStartLocPrio(final int prioSlotIndex, final int otherStartLocIndex, final CStartLocPrio priority) {
		if ((prioSlotIndex >= 0) && (prioSlotIndex < this.otherStartIndices.length)) {
			this.otherStartIndices[prioSlotIndex] = otherStartLocIndex;
			this.otherStartLocPriorities[prioSlotIndex] = priority;
		}
	}

	/**
	 * Start locations an enemy of this location's player should (or should not) get, as the map editor's enemy
	 * priorities; the ally priorities above are those for allied players.
	 */
	public int[] getEnemyStartIndices() {
		return this.enemyStartIndices;
	}

	public CStartLocPrio[] getEnemyStartLocPriorities() {
		return this.enemyStartLocPriorities;
	}

	public void setEnemyStartLocPrioCount(final int startLocPrioCount) {
		this.enemyStartIndices = resize(this.enemyStartIndices, startLocPrioCount);
		this.enemyStartLocPriorities = resize(this.enemyStartLocPriorities, startLocPrioCount);
	}

	public void setEnemyStartLocPrio(final int prioSlotIndex, final int otherStartLocIndex,
			final CStartLocPrio priority) {
		if ((prioSlotIndex >= 0) && (prioSlotIndex < this.enemyStartIndices.length)) {
			this.enemyStartIndices[prioSlotIndex] = otherStartLocIndex;
			this.enemyStartLocPriorities[prioSlotIndex] = priority;
		}
	}

	private static int slotOther(final int[] indices, final int prioSlotIndex) {
		return (prioSlotIndex >= 0) && (prioSlotIndex < indices.length) ? indices[prioSlotIndex] : 0;
	}

	private static CStartLocPrio slotPriority(final CStartLocPrio[] priorities, final int prioSlotIndex) {
		return (prioSlotIndex >= 0) && (prioSlotIndex < priorities.length) ? priorities[prioSlotIndex] : null;
	}

	/**
	 * Measured on 1.29: a new count keeps the slots it still covers, and a slot never set names start location -1.
	 */
	private static int[] resize(final int[] old, final int count) {
		final int[] resized = new int[Math.max(0, count)];
		Arrays.fill(resized, -1);
		System.arraycopy(old, 0, resized, 0, Math.min(old.length, resized.length));
		return resized;
	}

	private static CStartLocPrio[] resize(final CStartLocPrio[] old, final int count) {
		final CStartLocPrio[] resized = new CStartLocPrio[Math.max(0, count)];
		System.arraycopy(old, 0, resized, 0, Math.min(old.length, resized.length));
		return resized;
	}
}
