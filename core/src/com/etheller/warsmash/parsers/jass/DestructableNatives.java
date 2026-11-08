package com.etheller.warsmash.parsers.jass;

import com.badlogic.gdx.math.Rectangle;
import com.etheller.interpreter.ast.scope.trigger.TriggerBooleanExpression;
import com.etheller.interpreter.ast.value.CodeJassValue;
import com.etheller.interpreter.ast.value.HandleJassType;
import com.etheller.interpreter.ast.value.HandleJassValue;
import com.etheller.interpreter.ast.value.IntegerJassValue;
import com.etheller.interpreter.ast.value.RealJassValue;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.CodeJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.RealJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.StringJassValueVisitor;
import com.etheller.warsmash.parsers.jass.Jass2.CommonEnvironment;
import com.etheller.warsmash.parsers.jass.scope.CommonTriggerExecutionScope;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;

/**
 * Destructable natives.
 */
final class DestructableNatives extends CommonNatives {
	DestructableNatives(final CommonEnvironment environment) {
		super(environment);
	}

	@Override
	void register() {
		final HandleJassType destructableType = type("destructable");

		// TODO past this point things are inconsistent about ordering
		jassProgramVisitor.getJassNativeManager().createNative("EnumDestructablesInRect",
				(arguments, globalScope, triggerScope) -> {
					final Rectangle rect = arguments.get(0).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
					final TriggerBooleanExpression filter = nullable(arguments, 1,
							ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
					final CodeJassValue actionFunc = nullable(arguments, 2, CodeJassValueVisitor.getInstance());
					this.simulation.getWorldCollision().enumDestructablesInRect(rect, (unit) -> {
						if (rectCellsContain(rect, unit.getX(), unit.getY()) && ((filter == null)
								|| filter.evaluate(globalScope,
										CommonTriggerExecutionScope.filterScope(triggerScope, unit)))) {
							globalScope.runThreadUntilCompletion(globalScope.createThread(actionFunc,
									CommonTriggerExecutionScope.enumScope(triggerScope, unit)));
						}
						return false;
					});
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateDestructable",
				(arguments, globalScope, triggerScope) -> {
					final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float facing = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float scale = arguments.get(4).visit(RealJassValueVisitor.getInstance()).floatValue();
					final int variation = arguments.get(5).visit(IntegerJassValueVisitor.getInstance());
					return new HandleJassValue(destructableType, this.simulation
							.createDestructable(new War3ID(rawcode), x, y, facing, scale, variation));
				});
		jassProgramVisitor.getJassNativeManager().createNative("BlzCreateDestructableWithSkin",
				(arguments, globalScope, triggerScope) -> {
					int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float facing = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float scale = arguments.get(4).visit(RealJassValueVisitor.getInstance()).floatValue();
					final int variation = arguments.get(5).visit(IntegerJassValueVisitor.getInstance());
					final int skinId = arguments.get(6).visit(IntegerJassValueVisitor.getInstance());
					if (skinId != rawcode) {
						// throw new IllegalStateException("Our engine does not support
						// DestructableSkinID != DestructableID (skinId="+ new War3ID(skinId) + ",
						// destId=" + new War3ID(rawcode) + ")");
						rawcode = skinId;
					}
					return new HandleJassValue(destructableType, this.simulation
							.createDestructable(new War3ID(rawcode), x, y, facing, scale, variation));
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateDestructableZ",
				(arguments, globalScope, triggerScope) -> {
					final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float z = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float facing = arguments.get(4).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float scale = arguments.get(5).visit(RealJassValueVisitor.getInstance()).floatValue();
					final int variation = arguments.get(6).visit(IntegerJassValueVisitor.getInstance());
					return new HandleJassValue(destructableType, this.simulation
							.createDestructableZ(new War3ID(rawcode), x, y, z, facing, scale, variation));
				});
		// The 3.0 creation natives for destructables, in the argument order of common.j: object id, x, y, [z], face,
		// [roll, pitch], scale, variation, [skin id], [color]. Roll, pitch and color are read but not shown yet.
		for (final String name : new String[] { "BlzCreateDestructablePitchRoll", "BlzCreateDestructableZPitchRoll",
				"BlzCreateDestructableWithSkinPitchRoll", "BlzCreateDestructableZWithSkinPitchRoll",
				"BlzCreateDestructableZWithSkin", "BlzCreateDestructableWithColor", "BlzCreateDestructableZWithColor",
				"BlzCreateDestructableWithSkinColor", "BlzCreateDestructableZWithSkinColor",
				"BlzCreateDestructableWithSkinPitchRollColor", "BlzCreateDestructableZWithSkinPitchRollColor" }) {
			final boolean hasZ = name.startsWith("BlzCreateDestructableZ");
			final boolean hasPitchRoll = name.contains("PitchRoll");
			final boolean hasSkin = name.contains("WithSkin");
			jassProgramVisitor.getJassNativeManager().createNative(name, (arguments, globalScope, triggerScope) -> {
				int index = 0;
				int rawcode = arguments.get(index++).visit(IntegerJassValueVisitor.getInstance());
				final float x = arguments.get(index++).visit(RealJassValueVisitor.getInstance()).floatValue();
				final float y = arguments.get(index++).visit(RealJassValueVisitor.getInstance()).floatValue();
				final float z = hasZ
						? arguments.get(index++).visit(RealJassValueVisitor.getInstance()).floatValue()
						: 0f;
				final float facing = arguments.get(index++).visit(RealJassValueVisitor.getInstance()).floatValue();
				if (hasPitchRoll) {
					index += 2;
				}
				final float scale = arguments.get(index++).visit(RealJassValueVisitor.getInstance()).floatValue();
				final int variation = arguments.get(index++).visit(IntegerJassValueVisitor.getInstance());
				if (hasSkin) {
					rawcode = arguments.get(index++).visit(IntegerJassValueVisitor.getInstance());
				}
				final CDestructable destructable = hasZ
						? this.simulation.createDestructableZ(new War3ID(rawcode), x, y, z, facing,
								scale, variation)
						: this.simulation.createDestructable(new War3ID(rawcode), x, y, facing,
								scale, variation);
				return new HandleJassValue(destructableType, destructable);
			});
		}
		jassProgramVisitor.getJassNativeManager().createNative("KillDestructable",
				(arguments, globalScope, triggerScope) -> {
					final CDestructable dest = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					if (dest != null) {
						dest.setLife(this.simulation, 0f);
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("RemoveDestructable",
				(arguments, globalScope, triggerScope) -> {
					final CDestructable dest = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					if (dest == null) {
						return null;
					}
					this.simulation.removeDestructable(dest);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetDestructableX",
				(arguments, globalScope, triggerScope) -> {
					final CWidget whichWidget = inGame(nullable(arguments, 0, ObjectJassValueVisitor.getInstance()));
					return RealJassValue.of(whichWidget == null ? 0 : whichWidget.getX());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetDestructableY",
				(arguments, globalScope, triggerScope) -> {
					final CWidget whichWidget = inGame(nullable(arguments, 0, ObjectJassValueVisitor.getInstance()));
					return RealJassValue.of(whichWidget == null ? 0 : whichWidget.getY());
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetDestructableInvulnerable",
				(arguments, globalScope, triggerScope) -> {
					final CDestructable whichWidget = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
					whichWidget.setInvulnerable(flag);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetDestructableTypeId",
				(arguments, globalScope, triggerScope) -> {
					final CDestructable whichDestructable = inGame(nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance()));
					if (whichDestructable == null) {
						return IntegerJassValue.ZERO;
					}
					return IntegerJassValue.of(whichDestructable.getDestType().getTypeId().getValue());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetDestructableLife",
				(arguments, globalScope, triggerScope) -> {
					final CDestructable whichDestructable = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					if (whichDestructable == null) {
						return RealJassValue.ZERO;
					}
					return RealJassValue.of(whichDestructable.getLife());
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetDestructableLife",
				(arguments, globalScope, triggerScope) -> {
					final CDestructable whichDestructable = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final float life = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					if (whichDestructable == null) {
						return RealJassValue.ZERO;
					}
					whichDestructable.setLife(this.simulation, life);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetDestructableMaxLife",
				(arguments, globalScope, triggerScope) -> {
					final CDestructable whichDestructable = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					if (whichDestructable == null) {
						return RealJassValue.ZERO;
					}
					return RealJassValue.of(whichDestructable.getMaxLife());
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetDestructableAnimation",
				(arguments, globalScope, triggerScope) -> {
					final CDestructable whichDestructable = inGame(nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance()));
					final String animation = this.gameUI
							.getTrigStr(arguments.get(1).visit(StringJassValueVisitor.getInstance()));
					final RenderDestructable renderPeer = whichDestructable == null ? null
							: war3MapViewer.getRenderPeer(whichDestructable);
					if (renderPeer != null) {
						renderPeer.setAnimation(animation);
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("DestructableRestoreLife",
				(arguments, globalScope, triggerScope) -> {
					final CDestructable whichDestructable = inGame(nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance()));
					if (whichDestructable == null) {
						return null;
					}
					final float life = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final boolean birth = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
					// Measured on 1.29: only a dead destructable is restored; life 0 restores it to its max life
					// and any other value is kept within 0.5 and the max life.
					if (!whichDestructable.isDead()) {
						return null;
					}
					final float maxLife = whichDestructable.getMaxLife();
					final float restoredLife = (life == 0) ? maxLife : Math.min(maxLife, Math.max(0.5f, life));
					whichDestructable.setLife(this.simulation, restoredLife);
					if (!birth) {
						final RenderDestructable renderPeer = war3MapViewer.getRenderPeer(whichDestructable);
						if (renderPeer != null) {
							renderPeer.notifyLifeRestored();
						}
					} // else birth plays automatically
					return null;
				});
	}
}
