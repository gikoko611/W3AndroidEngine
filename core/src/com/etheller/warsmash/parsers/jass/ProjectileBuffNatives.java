package com.etheller.warsmash.parsers.jass;

import com.etheller.interpreter.ast.scope.trigger.TriggerBooleanExpression;
import com.etheller.interpreter.ast.value.BooleanJassValue;
import com.etheller.interpreter.ast.value.CodeJassValue;
import com.etheller.interpreter.ast.value.HandleJassType;
import com.etheller.interpreter.ast.value.HandleJassTypeConstructor;
import com.etheller.interpreter.ast.value.HandleJassValue;
import com.etheller.interpreter.ast.value.RealJassValue;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.CodeJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.RealJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.StringJassValueVisitor;
import com.etheller.warsmash.parsers.jass.Jass2.CommonEnvironment;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.CBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.CDestructableBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffStun;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimedLife;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff.ABDestructableBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff.ABPermanentPassiveBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff.ABTargetingBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff.ABTimedArtBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff.ABTimedBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff.ABTimedTargetingBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff.ABTimedTickingBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff.ABTimedTickingPausedBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff.ABTimedTickingPostDeathBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.event.ABTimeOfDayEvent;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.jass.ABActionJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.jass.ABConditionJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.projectile.ABCollisionProjectileListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.projectile.ABProjectileListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAbilityProjectileListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAttackProjectile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CProjectile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;
import java.util.Map;

/**
 * Warsmash ability API: projectiles, buffs, time of day events, stat bonuses and state mods.
 */
final class ProjectileBuffNatives extends CommonNatives {
	ProjectileBuffNatives(final CommonEnvironment environment) {
		super(environment);
	}

	@Override
	void register() {
		registerProjectiles();
		registerBuffs();
		registerTimeOfDayEvents();
		registerStatBonuses();
	}

	private void registerProjectiles() {
		final HandleJassType unitType = type("unit");
		final HandleJassType projectileType = type("projectile");

		// projectile api
		jassProgramVisitor.getJassNativeManager().createNative("CreateLocationTargetedCollisionProjectile",
				(arguments, globalScope, triggerScope) -> {
					int argIndex = 0;
					final CUnit casterUnit = nullable(arguments, argIndex++, ObjectJassValueVisitor.getInstance());
					final Map<String, Object> localStore = nullable(arguments, argIndex++,
							ObjectJassValueVisitor.getInstance());
					final int castId = arguments.get(argIndex++).visit(IntegerJassValueVisitor.getInstance());

					final CUnit sourceUnit = nullable(arguments, argIndex++, ObjectJassValueVisitor.getInstance());
					final AbilityPointTarget sourceLocation = nullable(arguments, argIndex++,
							ObjectJassValueVisitor.getInstance());
					final AbilityPointTarget targetLocation = nullable(arguments, argIndex++,
							ObjectJassValueVisitor.getInstance());
					final int projectileRawcode = arguments.get(argIndex++)
							.visit(IntegerJassValueVisitor.getInstance());
					final float speed = arguments.get(argIndex++).visit(RealJassValueVisitor.getInstance())
							.floatValue();
					final boolean homing = arguments.get(argIndex++).visit(BooleanJassValueVisitor.getInstance());
					final CodeJassValue onLaunchAction = nullable(arguments, argIndex++,
							CodeJassValueVisitor.getInstance());
					final CodeJassValue onPreHitsAction = nullable(arguments, argIndex++,
							CodeJassValueVisitor.getInstance());
					final TriggerBooleanExpression canHitTarget = nullable(arguments, argIndex++,
							ObjectJassValueVisitor.getInstance());
					final CodeJassValue onHitAction = nullable(arguments, argIndex++,
							CodeJassValueVisitor.getInstance());
					final int maxHits = arguments.get(9).visit(IntegerJassValueVisitor.getInstance());
					final int hitsPerTarget = arguments.get(argIndex++)
							.visit(IntegerJassValueVisitor.getInstance());
					final float startingRadius = arguments.get(argIndex++).visit(RealJassValueVisitor.getInstance())
							.floatValue();
					final float endingRadius = arguments.get(argIndex++).visit(RealJassValueVisitor.getInstance())
							.floatValue();
					final float collisionInterval = arguments.get(argIndex++)
							.visit(RealJassValueVisitor.getInstance()).floatValue();
					final boolean provideCounts = arguments.get(argIndex++)
							.visit(BooleanJassValueVisitor.getInstance());

					final War3ID projectileId = new War3ID(projectileRawcode);

					final AbilityPointTarget target = new AbilityPointTarget(targetLocation.x, targetLocation.y);

					final ABCollisionProjectileListener listener = new ABCollisionProjectileListener(
							ABActionJass.wrap(onLaunchAction), ABActionJass.wrap(onPreHitsAction),
							ABConditionJass.wrap(canHitTarget), ABActionJass.wrap(onHitAction), casterUnit,
							localStore, castId);

					final CProjectile proj = this.simulation.createCollisionProjectile(sourceUnit, projectileId,
							sourceLocation.getX(), sourceLocation.getY(), (float) sourceUnit.angleTo(target), speed,
							homing, target, maxHits, hitsPerTarget, startingRadius, endingRadius, collisionInterval,
							listener, provideCounts);
					return new HandleJassValue(projectileType, proj);
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateLocationTargetedProjectile",
				(arguments, globalScope, triggerScope) -> {
					int argIndex = 0;
					final CUnit casterUnit = nullable(arguments, argIndex++, ObjectJassValueVisitor.getInstance());
					final Map<String, Object> localStore = nullable(arguments, argIndex++,
							ObjectJassValueVisitor.getInstance());
					final int castId = arguments.get(argIndex++).visit(IntegerJassValueVisitor.getInstance());

					final CUnit sourceUnit = nullable(arguments, argIndex++, ObjectJassValueVisitor.getInstance());
					final AbilityPointTarget sourceLocation = nullable(arguments, argIndex++,
							ObjectJassValueVisitor.getInstance());
					final AbilityPointTarget targetLocation = nullable(arguments, argIndex++,
							ObjectJassValueVisitor.getInstance());
					final int projectileRawcode = arguments.get(argIndex++)
							.visit(IntegerJassValueVisitor.getInstance());
					final float speed = arguments.get(argIndex++).visit(RealJassValueVisitor.getInstance())
							.floatValue();
					final boolean homing = arguments.get(argIndex++).visit(BooleanJassValueVisitor.getInstance());
					final CodeJassValue onLaunchAction = nullable(arguments, argIndex++,
							CodeJassValueVisitor.getInstance());
					final CodeJassValue onHitAction = nullable(arguments, argIndex++,
							CodeJassValueVisitor.getInstance());

					final War3ID projectileId = new War3ID(projectileRawcode);

					final AbilityPointTarget target = new AbilityPointTarget(targetLocation.x, targetLocation.y);

					final CAbilityProjectileListener listener = new ABProjectileListener(
							ABActionJass.wrap(onLaunchAction), ABActionJass.wrap(onHitAction), casterUnit,
							localStore, castId);

					final CProjectile proj = this.simulation.createProjectile(sourceUnit, projectileId,
							sourceLocation.getX(), sourceLocation.getY(), (float) sourceUnit.angleTo(target), speed,
							homing, target, listener);

					return new HandleJassValue(projectileType, proj);
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateLocationTargetedPseudoProjectile",
				(arguments, globalScope, triggerScope) -> {
					int argIndex = 0;
					final CUnit casterUnit = nullable(arguments, argIndex++, ObjectJassValueVisitor.getInstance());
					final Map<String, Object> localStore = nullable(arguments, argIndex++,
							ObjectJassValueVisitor.getInstance());
					final int castId = arguments.get(argIndex++).visit(IntegerJassValueVisitor.getInstance());

					final CUnit sourceUnit = nullable(arguments, argIndex++, ObjectJassValueVisitor.getInstance());
					final AbilityPointTarget sourceLocation = nullable(arguments, argIndex++,
							ObjectJassValueVisitor.getInstance());
					final AbilityPointTarget targetLocation = nullable(arguments, argIndex++,
							ObjectJassValueVisitor.getInstance());
					final int projectileRawcode = arguments.get(argIndex++)
							.visit(IntegerJassValueVisitor.getInstance());
					final CEffectType whichEffectType = nullable(arguments, argIndex++,
							ObjectJassValueVisitor.getInstance());
					final int whichEffectTypeIndex = arguments.get(argIndex++)
							.visit(IntegerJassValueVisitor.getInstance());
					final float speed = arguments.get(argIndex++).visit(RealJassValueVisitor.getInstance())
							.floatValue();
					final boolean homing = arguments.get(argIndex++).visit(BooleanJassValueVisitor.getInstance());
					final CodeJassValue onLaunchAction = nullable(arguments, argIndex++,
							CodeJassValueVisitor.getInstance());
					final CodeJassValue onPreHitsAction = nullable(arguments, argIndex++,
							CodeJassValueVisitor.getInstance());
					final TriggerBooleanExpression canHitTarget = nullable(arguments, argIndex++,
							ObjectJassValueVisitor.getInstance());
					final CodeJassValue onHitAction = nullable(arguments, argIndex++,
							CodeJassValueVisitor.getInstance());
					final int maxHits = arguments.get(9).visit(IntegerJassValueVisitor.getInstance());
					final int hitsPerTarget = arguments.get(argIndex++)
							.visit(IntegerJassValueVisitor.getInstance());
					final float startingRadius = arguments.get(argIndex++).visit(RealJassValueVisitor.getInstance())
							.floatValue();
					final float endingRadius = arguments.get(argIndex++).visit(RealJassValueVisitor.getInstance())
							.floatValue();
					final float projectileStepInterval = arguments.get(argIndex++)
							.visit(RealJassValueVisitor.getInstance()).floatValue();
					final int projectileArtSkip = arguments.get(argIndex++)
							.visit(IntegerJassValueVisitor.getInstance());
					final boolean provideCounts = arguments.get(argIndex++)
							.visit(BooleanJassValueVisitor.getInstance());

					final War3ID projectileId = new War3ID(projectileRawcode);

					final AbilityPointTarget target = new AbilityPointTarget(targetLocation.x, targetLocation.y);

					final ABCollisionProjectileListener listener = new ABCollisionProjectileListener(
							ABActionJass.wrap(onLaunchAction), ABActionJass.wrap(onPreHitsAction),
							ABConditionJass.wrap(canHitTarget), ABActionJass.wrap(onHitAction), casterUnit,
							localStore, castId);

					final CProjectile proj = this.simulation.createPseudoProjectile(sourceUnit, projectileId,
							whichEffectType, whichEffectTypeIndex, sourceLocation.getX(), sourceLocation.getY(),
							(float) sourceUnit.angleTo(target), speed, projectileStepInterval, projectileArtSkip,
							homing, target, maxHits, hitsPerTarget, startingRadius, endingRadius, listener,
							provideCounts);

					return new HandleJassValue(projectileType, proj);
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateUnitTargetedCollisionProjectile",
				(arguments, globalScope, triggerScope) -> {
					int argIndex = 0;
					final CUnit casterUnit = nullable(arguments, argIndex++, ObjectJassValueVisitor.getInstance());
					final Map<String, Object> localStore = nullable(arguments, argIndex++,
							ObjectJassValueVisitor.getInstance());
					final int castId = arguments.get(argIndex++).visit(IntegerJassValueVisitor.getInstance());

					final CUnit sourceUnit = nullable(arguments, argIndex++, ObjectJassValueVisitor.getInstance());
					final AbilityPointTarget sourceLocation = nullable(arguments, argIndex++,
							ObjectJassValueVisitor.getInstance());
					final CUnit target = nullable(arguments, argIndex++, ObjectJassValueVisitor.getInstance());
					final int projectileRawcode = arguments.get(argIndex++)
							.visit(IntegerJassValueVisitor.getInstance());
					final float speed = arguments.get(argIndex++).visit(RealJassValueVisitor.getInstance())
							.floatValue();
					final boolean homing = arguments.get(argIndex++).visit(BooleanJassValueVisitor.getInstance());
					final CodeJassValue onLaunchAction = nullable(arguments, argIndex++,
							CodeJassValueVisitor.getInstance());
					final CodeJassValue onPreHitsAction = nullable(arguments, argIndex++,
							CodeJassValueVisitor.getInstance());
					final TriggerBooleanExpression canHitTarget = nullable(arguments, argIndex++,
							ObjectJassValueVisitor.getInstance());
					final CodeJassValue onHitAction = nullable(arguments, argIndex++,
							CodeJassValueVisitor.getInstance());
					final int maxHits = arguments.get(9).visit(IntegerJassValueVisitor.getInstance());
					final int hitsPerTarget = arguments.get(argIndex++)
							.visit(IntegerJassValueVisitor.getInstance());
					final float startingRadius = arguments.get(argIndex++).visit(RealJassValueVisitor.getInstance())
							.floatValue();
					final float endingRadius = arguments.get(argIndex++).visit(RealJassValueVisitor.getInstance())
							.floatValue();
					final float collisionInterval = arguments.get(argIndex++)
							.visit(RealJassValueVisitor.getInstance()).floatValue();
					final boolean provideCounts = arguments.get(argIndex++)
							.visit(BooleanJassValueVisitor.getInstance());

					final War3ID projectileId = new War3ID(projectileRawcode);

					final ABCollisionProjectileListener listener = new ABCollisionProjectileListener(
							ABActionJass.wrap(onLaunchAction), ABActionJass.wrap(onPreHitsAction),
							ABConditionJass.wrap(canHitTarget), ABActionJass.wrap(onHitAction), casterUnit,
							localStore, castId);

					final CProjectile proj = this.simulation.createCollisionProjectile(sourceUnit, projectileId,
							sourceLocation.getX(), sourceLocation.getY(), (float) sourceUnit.angleTo(target), speed,
							homing, target, maxHits, hitsPerTarget, startingRadius, endingRadius, collisionInterval,
							listener, provideCounts);
					return new HandleJassValue(projectileType, proj);
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateUnitTargetedProjectile",
				(arguments, globalScope, triggerScope) -> {
					int argIndex = 0;
					final CUnit casterUnit = nullable(arguments, argIndex++, ObjectJassValueVisitor.getInstance());
					final Map<String, Object> localStore = nullable(arguments, argIndex++,
							ObjectJassValueVisitor.getInstance());
					final int castId = arguments.get(argIndex++).visit(IntegerJassValueVisitor.getInstance());

					final CUnit sourceUnit = nullable(arguments, argIndex++, ObjectJassValueVisitor.getInstance());
					final AbilityPointTarget sourceLocation = nullable(arguments, argIndex++,
							ObjectJassValueVisitor.getInstance());
					final CUnit targetUnit = nullable(arguments, argIndex++, ObjectJassValueVisitor.getInstance());
					final int projectileRawcode = arguments.get(argIndex++)
							.visit(IntegerJassValueVisitor.getInstance());
					final float speed = arguments.get(argIndex++).visit(RealJassValueVisitor.getInstance())
							.floatValue();
					final boolean homing = arguments.get(argIndex++).visit(BooleanJassValueVisitor.getInstance());
					final CodeJassValue onLaunchAction = nullable(arguments, argIndex++,
							CodeJassValueVisitor.getInstance());
					final CodeJassValue onHitAction = nullable(arguments, argIndex++,
							CodeJassValueVisitor.getInstance());

					final War3ID projectileId = new War3ID(projectileRawcode);

					final ABProjectileListener listener = new ABProjectileListener(
							ABActionJass.wrap(onLaunchAction), ABActionJass.wrap(onHitAction), casterUnit,
							localStore, castId);

					final CProjectile proj = this.simulation.createProjectile(sourceUnit, projectileId,
							sourceLocation.getX(), sourceLocation.getY(), (float) sourceUnit.angleTo(targetUnit),
							speed, homing, targetUnit, listener);
					return new HandleJassValue(projectileType, proj);
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetAttackProjectileDamage",
				(arguments, globalScope, triggerScope) -> {
					final CProjectile projectile = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final float damage = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();

					if (projectile instanceof CAttackProjectile) {
						((CAttackProjectile) projectile).setDamage(damage);
					}

					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetProjectileDone",
				(arguments, globalScope, triggerScope) -> {
					final CProjectile projectile = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final boolean done = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());

					projectile.setDone(done);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetProjectileReflected",
				(arguments, globalScope, triggerScope) -> {
					final CProjectile projectile = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final boolean reflected = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());

					projectile.setReflected(reflected);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetProjectileTargetUnit",
				(arguments, globalScope, triggerScope) -> {
					final CProjectile projectile = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final CUnit target = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());

					projectile.setTarget(target);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetProjectileTargetLoc",
				(arguments, globalScope, triggerScope) -> {
					final CProjectile projectile = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final AbilityPointTarget target = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());

					projectile.setTarget(new AbilityPointTarget(target.x, target.y));
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("IsProjectileReflected",
				(arguments, globalScope, triggerScope) -> {
					final CProjectile projectile = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());

					return BooleanJassValue.of(projectile.isReflected());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetProjectileX",
				(arguments, globalScope, triggerScope) -> {
					final CProjectile projectile = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());

					return RealJassValue.of(projectile.getX());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetProjectileY",
				(arguments, globalScope, triggerScope) -> {
					final CProjectile projectile = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());

					return RealJassValue.of(projectile.getY());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetProjectileSource",
				(arguments, globalScope, triggerScope) -> {
					final CProjectile projectile = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());

					return new HandleJassValue(unitType, projectile.getSource());
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateJassProjectile",
				(arguments, globalScope, triggerScope) -> {
					int argIndex = 0;
					final CUnit casterUnit = nullable(arguments, argIndex++, ObjectJassValueVisitor.getInstance());

					final int projectileRawcode = arguments.get(argIndex++)
							.visit(IntegerJassValueVisitor.getInstance());

					final float launchX = arguments.get(argIndex++).visit(RealJassValueVisitor.getInstance())
							.floatValue();
					final float launchY = arguments.get(argIndex++).visit(RealJassValueVisitor.getInstance())
							.floatValue();
					final float launchFacing = arguments.get(argIndex++).visit(RealJassValueVisitor.getInstance())
							.floatValue();
					final float speed = arguments.get(argIndex++).visit(RealJassValueVisitor.getInstance())
							.floatValue();
					final boolean homing = arguments.get(argIndex++).visit(BooleanJassValueVisitor.getInstance());

					final AbilityTarget target = nullable(arguments, argIndex++,
							ObjectJassValueVisitor.getInstance());

					final CProjectile proj = this.simulation.createProjectile(casterUnit,
							new War3ID(projectileRawcode), launchX, launchY, launchFacing, speed, homing, target);

					return new HandleJassValue(projectileType, proj);
				});
		projectileType.setConstructorNative(new HandleJassTypeConstructor("CreateJassProjectile"));
		projectileType.setDestructorNative(new HandleJassTypeConstructor("SetProjectileDone"));
	}

	private void registerBuffs() {
		final HandleJassType buffType = type("buff");
		final HandleJassType destructablebuffType = type("destructablebuff");

		// buff api
		jassProgramVisitor.getJassNativeManager().createNative("AddUnitNonStackingDisplayBuff",
				(arguments, globalScope, triggerScope) -> {
					final CUnit unit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final String stackingKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final CBuff buff = arguments.get(2).visit(ObjectJassValueVisitor.getInstance());
					unit.addNonStackingDisplayBuff(this.simulation, stackingKey, buff);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("RemoveUnitNonStackingDisplayBuff",
				(arguments, globalScope, triggerScope) -> {
					final CUnit unit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final String stackingKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final CBuff buff = arguments.get(2).visit(ObjectJassValueVisitor.getInstance());
					unit.removeNonStackingDisplayBuff(this.simulation, stackingKey, buff);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreatePassiveBuff",
				(arguments, globalScope, triggerScope) -> {
					final int buffRawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final boolean showIcon = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
					final CodeJassValue onAddAction = nullable(arguments, 2, CodeJassValueVisitor.getInstance());
					final CodeJassValue onRemoveAction = nullable(arguments, 3, CodeJassValueVisitor.getInstance());
					final CEffectType artType = nullable(arguments, 4, ObjectJassValueVisitor.getInstance());
					final boolean showFx = arguments.get(5).visit(BooleanJassValueVisitor.getInstance());
					final boolean playSfx = arguments.get(6).visit(BooleanJassValueVisitor.getInstance());
					final Map<String, Object> localStore = nullable(arguments, 7,
							ObjectJassValueVisitor.getInstance());
					final int castId = arguments.get(8).visit(IntegerJassValueVisitor.getInstance());
					final boolean leveled = arguments.get(9).visit(BooleanJassValueVisitor.getInstance());
					final boolean positive = arguments.get(10).visit(BooleanJassValueVisitor.getInstance());

					final ABPermanentPassiveBuff ability = new ABPermanentPassiveBuff(
							this.simulation.getHandleIdAllocator().createId(),
							new War3ID(buffRawcode), null, null, localStore, ABActionJass.wrap(onAddAction),
							ABActionJass.wrap(onRemoveAction), showIcon, castId, leveled, positive);
					if (artType != null) {
						ability.setArtType(artType);
					}
					ability.setShowFx(showFx);
					ability.setPlaySfx(playSfx);

					return new HandleJassValue(buffType, ability);
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateTargetingBuff",
				(arguments, globalScope, triggerScope) -> {
					final int buffRawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final Map<String, Object> localStore = nullable(arguments, 1,
							ObjectJassValueVisitor.getInstance());

					final CBuff ability = new ABTargetingBuff(
							this.simulation.getHandleIdAllocator().createId(),
							new War3ID(buffRawcode), localStore, null, null);

					return new HandleJassValue(buffType, ability);
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateTimedArtBuff",
				(arguments, globalScope, triggerScope) -> {
					final int buffRawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final float duration = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final boolean showIcon = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
					final CEffectType artType = nullable(arguments, 3, ObjectJassValueVisitor.getInstance());
					final boolean leveled = arguments.get(4).visit(BooleanJassValueVisitor.getInstance());
					final boolean positive = arguments.get(5).visit(BooleanJassValueVisitor.getInstance());
					final boolean dispellable = arguments.get(6).visit(BooleanJassValueVisitor.getInstance());
					final Map<String, Object> localStore = nullable(arguments, 7,
							ObjectJassValueVisitor.getInstance());

					final ABTimedArtBuff ability = new ABTimedArtBuff(
							this.simulation.getHandleIdAllocator().createId(), new War3ID(buffRawcode), localStore,
							null, null, duration, showIcon, leveled, positive, dispellable);
					if (artType != null) {
						ability.setArtType(artType);
					}

					return new HandleJassValue(buffType, ability);
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateTimedBuff",
				(arguments, globalScope, triggerScope) -> {
					final int buffRawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final float duration = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final boolean showTimedLifeBar = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
					final CodeJassValue onAddAction = nullable(arguments, 3, CodeJassValueVisitor.getInstance());
					final CodeJassValue onRemoveAction = nullable(arguments, 4, CodeJassValueVisitor.getInstance());
					final CodeJassValue onExpireAction = nullable(arguments, 5, CodeJassValueVisitor.getInstance());
					final boolean showIcon = arguments.get(6).visit(BooleanJassValueVisitor.getInstance());
					final CEffectType artType = nullable(arguments, 7, ObjectJassValueVisitor.getInstance());
					final Map<String, Object> localStore = nullable(arguments, 8,
							ObjectJassValueVisitor.getInstance());
					final int castId = arguments.get(9).visit(IntegerJassValueVisitor.getInstance());
					final boolean leveled = arguments.get(10).visit(BooleanJassValueVisitor.getInstance());
					final boolean positive = arguments.get(11).visit(BooleanJassValueVisitor.getInstance());
					final boolean dispellable = arguments.get(12).visit(BooleanJassValueVisitor.getInstance());

					final ABTimedBuff ability = new ABTimedBuff(this.simulation.getHandleIdAllocator().createId(),
							new War3ID(buffRawcode), localStore, null, null, duration, showTimedLifeBar,
							ABActionJass.wrap(onAddAction), ABActionJass.wrap(onRemoveAction),
							ABActionJass.wrap(onExpireAction), showIcon, castId, leveled, positive, dispellable);
					ability.setArtType(artType);

					return new HandleJassValue(buffType, ability);
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateTimedLifeBuff",
				(arguments, globalScope, triggerScope) -> {
					final int buffRawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final float duration = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final boolean explode = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());

					final CBuffTimedLife ability = new CBuffTimedLife(
							this.simulation.getHandleIdAllocator().createId(), new War3ID(buffRawcode), duration,
							explode);

					return new HandleJassValue(buffType, ability);
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateTimedTargetingBuff",
				(arguments, globalScope, triggerScope) -> {
					final int buffRawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final float duration = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final Map<String, Object> localStore = nullable(arguments, 2,
							ObjectJassValueVisitor.getInstance());

					final CBuff ability = new ABTimedTargetingBuff(
							this.simulation.getHandleIdAllocator().createId(), new War3ID(buffRawcode), localStore,
							null, null, duration);

					return new HandleJassValue(buffType, ability);
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateTimedTickingBuff",
				(arguments, globalScope, triggerScope) -> {
					final int buffRawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final float duration = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final boolean showTimedLifeBar = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
					final CodeJassValue onAddAction = nullable(arguments, 3, CodeJassValueVisitor.getInstance());
					final CodeJassValue onRemoveAction = nullable(arguments, 4, CodeJassValueVisitor.getInstance());
					final CodeJassValue onExpireAction = nullable(arguments, 5, CodeJassValueVisitor.getInstance());
					final CodeJassValue onTickAction = nullable(arguments, 6, CodeJassValueVisitor.getInstance());
					final boolean showIcon = arguments.get(7).visit(BooleanJassValueVisitor.getInstance());
					final CEffectType artType = nullable(arguments, 8, ObjectJassValueVisitor.getInstance());
					final Map<String, Object> localStore = nullable(arguments, 9,
							ObjectJassValueVisitor.getInstance());
					final int castId = arguments.get(10).visit(IntegerJassValueVisitor.getInstance());
					final boolean leveled = arguments.get(11).visit(BooleanJassValueVisitor.getInstance());
					final boolean positive = arguments.get(12).visit(BooleanJassValueVisitor.getInstance());
					final boolean dispellable = arguments.get(13).visit(BooleanJassValueVisitor.getInstance());

					final ABTimedTickingBuff ability = new ABTimedTickingBuff(
							this.simulation.getHandleIdAllocator().createId(), new War3ID(buffRawcode), localStore,
							null, null, duration, showTimedLifeBar, ABActionJass.wrap(onAddAction),
							ABActionJass.wrap(onRemoveAction), ABActionJass.wrap(onExpireAction),
							ABActionJass.wrap(onTickAction), showIcon, castId, leveled, positive, dispellable);
					ability.setArtType(artType);

					return new HandleJassValue(buffType, ability);
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateTimedTickingPausedBuff",
				(arguments, globalScope, triggerScope) -> {
					final int buffRawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final float duration = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final boolean showTimedLifeBar = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
					final CodeJassValue onAddAction = nullable(arguments, 3, CodeJassValueVisitor.getInstance());
					final CodeJassValue onRemoveAction = nullable(arguments, 4, CodeJassValueVisitor.getInstance());
					final CodeJassValue onExpireAction = nullable(arguments, 5, CodeJassValueVisitor.getInstance());
					final CodeJassValue onTickAction = nullable(arguments, 6, CodeJassValueVisitor.getInstance());
					final boolean showIcon = arguments.get(7).visit(BooleanJassValueVisitor.getInstance());
					final CEffectType artType = nullable(arguments, 8, ObjectJassValueVisitor.getInstance());
					final Map<String, Object> localStore = nullable(arguments, 9,
							ObjectJassValueVisitor.getInstance());
					final int castId = arguments.get(10).visit(IntegerJassValueVisitor.getInstance());
					final boolean leveled = arguments.get(11).visit(BooleanJassValueVisitor.getInstance());
					final boolean positive = arguments.get(12).visit(BooleanJassValueVisitor.getInstance());
					final boolean dispellable = arguments.get(13).visit(BooleanJassValueVisitor.getInstance());

					final ABTimedTickingPausedBuff ability = new ABTimedTickingPausedBuff(
							this.simulation.getHandleIdAllocator().createId(), new War3ID(buffRawcode), localStore,
							null, null, duration, showTimedLifeBar, ABActionJass.wrap(onAddAction),
							ABActionJass.wrap(onRemoveAction), ABActionJass.wrap(onExpireAction),
							ABActionJass.wrap(onTickAction), showIcon, castId, leveled, positive, dispellable);
					ability.setArtType(artType);

					return new HandleJassValue(buffType, ability);
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateTimedTickingPostDeathBuff",
				(arguments, globalScope, triggerScope) -> {
					final int buffRawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final float duration = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final boolean showTimedLifeBar = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
					final CodeJassValue onAddAction = nullable(arguments, 3, CodeJassValueVisitor.getInstance());
					final CodeJassValue onRemoveAction = nullable(arguments, 4, CodeJassValueVisitor.getInstance());
					final CodeJassValue onExpireAction = nullable(arguments, 5, CodeJassValueVisitor.getInstance());
					final CodeJassValue onTickAction = nullable(arguments, 6, CodeJassValueVisitor.getInstance());
					final boolean showIcon = arguments.get(7).visit(BooleanJassValueVisitor.getInstance());
					final CEffectType artType = nullable(arguments, 8, ObjectJassValueVisitor.getInstance());
					final Map<String, Object> localStore = nullable(arguments, 9,
							ObjectJassValueVisitor.getInstance());
					final int castId = arguments.get(10).visit(IntegerJassValueVisitor.getInstance());
					final boolean leveled = arguments.get(11).visit(BooleanJassValueVisitor.getInstance());
					final boolean positive = arguments.get(12).visit(BooleanJassValueVisitor.getInstance());
					final boolean dispellable = arguments.get(13).visit(BooleanJassValueVisitor.getInstance());

					final ABTimedTickingPostDeathBuff ability = new ABTimedTickingPostDeathBuff(
							this.simulation.getHandleIdAllocator().createId(), new War3ID(buffRawcode), localStore,
							null, null, duration, showTimedLifeBar, ABActionJass.wrap(onAddAction),
							ABActionJass.wrap(onRemoveAction), ABActionJass.wrap(onExpireAction),
							ABActionJass.wrap(onTickAction), showIcon, castId, leveled, positive, dispellable);
					ability.setArtType(artType);

					return new HandleJassValue(buffType, ability);
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateStunBuff",
				(arguments, globalScope, triggerScope) -> {
					final int buffRawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final float duration = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();

					final CBuffStun ability = new CBuffStun(this.simulation.getHandleIdAllocator().createId(),
							new War3ID(buffRawcode), duration);
					return new HandleJassValue(buffType, ability);
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddDestructableBuff",
				(arguments, globalScope, triggerScope) -> {
					final CDestructable unit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final CDestructableBuff buff = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					unit.add(this.simulation, buff);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("RemoveDestructableBuff",
				(arguments, globalScope, triggerScope) -> {
					final CDestructable unit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final CDestructableBuff buff = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					unit.remove(this.simulation, buff);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateDestructableBuff",
				(arguments, globalScope, triggerScope) -> {
					final CUnit casterUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final int buffRawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final int level = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					final Map<String, Object> localStore = nullable(arguments, 3,
							ObjectJassValueVisitor.getInstance());
					final CodeJassValue onAddAction = nullable(arguments, 4, CodeJassValueVisitor.getInstance());
					final CodeJassValue onRemoveAction = nullable(arguments, 5, CodeJassValueVisitor.getInstance());
					final CodeJassValue onDeathAction = nullable(arguments, 6, CodeJassValueVisitor.getInstance());
					final int castId = arguments.get(7).visit(IntegerJassValueVisitor.getInstance());

					final CDestructableBuff ability = new ABDestructableBuff(
							this.simulation.getHandleIdAllocator().createId(), new War3ID(buffRawcode), level,
							localStore, ABActionJass.wrap(onAddAction), ABActionJass.wrap(onRemoveAction),
							ABActionJass.wrap(onDeathAction), castId, casterUnit, true);

					return new HandleJassValue(destructablebuffType, ability);
				});
	}

	private void registerTimeOfDayEvents() {
		final HandleJassType abtimeofdayeventType = type("abtimeofdayevent");

		// abtimeofdayeventType
		jassProgramVisitor.getJassNativeManager().createNative("CreateABTimeOfDayEvent",
				(arguments, globalScope, triggerScope) -> {
					final CodeJassValue actions = nullable(arguments, 0, CodeJassValueVisitor.getInstance());
					final float startTime = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float endTime = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					final String equalityId = nullable(arguments, 3, StringJassValueVisitor.getInstance());
					final CUnit casterUnit = nullable(arguments, 4, ObjectJassValueVisitor.getInstance());
					final Map<String, Object> localStore = nullable(arguments, 5,
							ObjectJassValueVisitor.getInstance());
					final int castId = arguments.get(6).visit(IntegerJassValueVisitor.getInstance());

					final ABTimeOfDayEvent abTimeOfDayEvent = new ABTimeOfDayEvent(this.simulation, casterUnit,
							localStore, castId, ABActionJass.wrap(actions), startTime, endTime, equalityId);

					return new HandleJassValue(abtimeofdayeventType, abTimeOfDayEvent);
				});
		jassProgramVisitor.getJassNativeManager().createNative("RegisterABTimeOfDayEvent",
				(arguments, globalScope, triggerScope) -> {
					final ABTimeOfDayEvent event = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					if (event != null) {
						if (!this.simulation.isTimeOfDayEventRegistered(event)) {
							this.simulation.registerTimeOfDayEvent(event);
						}
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("UnregisterABTimeOfDayEvent",
				(arguments, globalScope, triggerScope) -> {
					final ABTimeOfDayEvent event = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					if (event != null) {
						this.simulation.unregisterTimeOfDayEvent(event);
					}
					return null;
				});
	}

	private void registerStatBonuses() {
		final HandleJassType nonstackingstatbuffType = type("nonstackingstatbonus");
		final HandleJassType nonstackingstatbufftypeType = type("nonstackingstatbonustype");
		final HandleJassType statemodType = type("statemod");
		final HandleJassType statemodtypeType = type("statemodtype");

		// non stacking stat buffs
		jassProgramVisitor.getJassNativeManager().createNative("String2NonStackingStatBonusType",
				(arguments, globalScope, triggerScope) -> {
					final String key = nullable(arguments, 0, StringJassValueVisitor.getInstance());
					return new HandleJassValue(nonstackingstatbuffType, NonStackingStatBuffType.valueOf(key));
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertNonStackingStatBonusType",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(nonstackingstatbufftypeType, NonStackingStatBuffType.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateNonStackingStatBonus",
				(arguments, globalScope, triggerScope) -> {
					final NonStackingStatBuffType whichType = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final String stackingKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final float value = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					return new HandleJassValue(nonstackingstatbuffType,
							new NonStackingStatBuff(whichType, stackingKey, value));
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddUnitNonStackingStatBonus",
				(arguments, globalScope, triggerScope) -> {
					final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final NonStackingStatBuff buff = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					unit.addNonStackingStatBuff(this.simulation, buff);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("RemoveUnitNonStackingStatBonus",
				(arguments, globalScope, triggerScope) -> {
					final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final NonStackingStatBuff buff = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					unit.removeNonStackingStatBuff(this.simulation, buff);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("RecomputeStatBonusesOnUnit",
				(arguments, globalScope, triggerScope) -> {
					final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final NonStackingStatBuffType whichBuffType = arguments.get(1)
							.visit(ObjectJassValueVisitor.getInstance());
					if (whichBuffType.isHeroStat()) {
						unit.computeDerivedHeroFields(this.simulation, whichBuffType);
					}
					else {
						unit.computeDerivedFields(whichBuffType);
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("UpdateNonStackingStatBonus",
				(arguments, globalScope, triggerScope) -> {
					final NonStackingStatBuff buff = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final float value = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					buff.setValue(value);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetNonStackingStatBonusType",
				(arguments, globalScope, triggerScope) -> {
					final NonStackingStatBuff buff = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					return new HandleJassValue(nonstackingstatbufftypeType, buff.getBuffType());
				});
		// state mod buffs
		jassProgramVisitor.getJassNativeManager().createNative("String2StateModType",
				(arguments, globalScope, triggerScope) -> {
					final String key = nullable(arguments, 0, StringJassValueVisitor.getInstance());
					return new HandleJassValue(statemodtypeType, StateModBuffType.valueOf(key));
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertStateModType",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(statemodtypeType, StateModBuffType.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateStateMod",
				(arguments, globalScope, triggerScope) -> {
					final StateModBuffType whichType = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final int value = arguments.get(1).visit(IntegerJassValueVisitor.getInstance()).intValue();
					return new HandleJassValue(statemodType, new StateModBuff(whichType, value));
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddUnitStateMod",
				(arguments, globalScope, triggerScope) -> {
					final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final StateModBuff buff = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					unit.addStateModBuff(buff);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("RemoveUnitStateMod",
				(arguments, globalScope, triggerScope) -> {
					final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final StateModBuff buff = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					unit.removeStateModBuff(buff);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("RecomputeStateModsOnUnit",
				(arguments, globalScope, triggerScope) -> {
					final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final StateModBuffType whichBuffType = arguments.get(1)
							.visit(ObjectJassValueVisitor.getInstance());
					unit.computeUnitState(this.simulation, whichBuffType);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("UpdateStateMod",
				(arguments, globalScope, triggerScope) -> {
					final StateModBuff buff = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final int value = arguments.get(1).visit(IntegerJassValueVisitor.getInstance()).intValue();
					buff.setValue(value);
					return null;
				});
	}
}
