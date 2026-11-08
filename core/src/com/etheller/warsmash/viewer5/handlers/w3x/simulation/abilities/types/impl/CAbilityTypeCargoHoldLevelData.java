package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.impl;

import java.util.EnumSet;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.CAbilityTypeLevelData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;

public class CAbilityTypeCargoHoldLevelData extends CAbilityTypeLevelData {
	private final int cargoCapcity;
	private final float duration;
	private final float castRange;
	private final float area;

	public CAbilityTypeCargoHoldLevelData(final EnumSet<CTargetType> targetsAllowed, final int cargoCapcity,
			final float duration, final float castRange) {
		this(targetsAllowed, cargoCapcity, duration, castRange, 0);
	}

	public CAbilityTypeCargoHoldLevelData(final EnumSet<CTargetType> targetsAllowed, final int cargoCapcity,
			final float duration, final float castRange, final float area) {
		super(targetsAllowed);
		this.cargoCapcity = cargoCapcity;
		this.duration = duration;
		this.castRange = castRange;
		this.area = area;
	}

	public int getCargoCapcity() {
		return this.cargoCapcity;
	}

	public float getDuration() {
		return this.duration;
	}

	public float getCastRange() {
		return this.castRange;
	}

	public float getArea() {
		return this.area;
	}

}
