package com.etheller.warsmash.parsers.jass;

import com.etheller.interpreter.ast.value.BooleanJassValue;
import com.etheller.interpreter.ast.value.HandleJassType;
import com.etheller.interpreter.ast.value.StringJassValue;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.RealJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.StringJassValueVisitor;
import com.etheller.warsmash.parsers.jass.Jass2.CommonEnvironment;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.ScriptEffect;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentLightning;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentLightningMovable;

/**
 * Special effect and lightning natives.
 */
final class EffectNatives extends CommonNatives {
	EffectNatives(final CommonEnvironment environment) {
		super(environment);
	}

	@Override
	void register() {
		final HandleJassType effectType = type("effect");
		final HandleJassType lightningType = type("lightning");

		jassProgramVisitor.getJassNativeManager().createNative("AddSpecialEffectTarget",
				(arguments, globalScope, triggerScope) -> {
					final String modelName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					final CWidget targetWidget = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					final String attachPointName = arguments.get(2).visit(StringJassValueVisitor.getInstance());
					return newHandle(effectType,
							war3MapViewer.addSpecialEffectTarget(modelName, targetWidget, attachPointName));
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddSpecialEffectLoc",
				(arguments, globalScope, triggerScope) -> {
					final String modelName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					final AbilityPointTarget positionLoc = arguments.get(1)
							.visit(ObjectJassValueVisitor.getInstance());
					return newHandle(effectType, war3MapViewer.addSpecialEffect(modelName, positionLoc.x,
							positionLoc.y, 0 /* facing */));
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddSpecialEffect",
				(arguments, globalScope, triggerScope) -> {
					final String modelName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					return newHandle(effectType,
							war3MapViewer.addSpecialEffect(modelName, x, y, 0 /* facing */));
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddSpellEffectById",
				(arguments, globalScope, triggerScope) -> {
					final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final CEffectType whichEffectType = arguments.get(1)
							.visit(ObjectJassValueVisitor.getInstance());
					final double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
					final double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());
					return newHandle(effectType, war3MapViewer.spawnSpellEffectEx((float) x, (float) y,
							0 /* facing */, new War3ID(rawcode), whichEffectType, 0));
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddSpellEffectTargetById",
				(arguments, globalScope, triggerScope) -> {
					final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final CEffectType whichEffectType = arguments.get(1)
							.visit(ObjectJassValueVisitor.getInstance());
					final CWidget target = arguments.get(2).visit(ObjectJassValueVisitor.getInstance());
					final String attachmentPoint = nullable(arguments, 3, StringJassValueVisitor.getInstance());
					if (attachmentPoint != null) {
						return newHandle(effectType, war3MapViewer.spawnSpellEffectOnUnitEx(target,
								new War3ID(rawcode), whichEffectType, 0, attachmentPoint));
					}
					else {
						return newHandle(effectType, war3MapViewer.spawnSpellEffectOnUnitEx(target,
								new War3ID(rawcode), whichEffectType, 0));
					}
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddSpellEffectByIdLoc",
				(arguments, globalScope, triggerScope) -> {
					final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final CEffectType whichEffectType = arguments.get(1)
							.visit(ObjectJassValueVisitor.getInstance());
					final AbilityPointTarget loc = arguments.get(2).visit(ObjectJassValueVisitor.getInstance());
					final float x = loc.x;
					final float y = loc.y;
					return newHandle(effectType, war3MapViewer.spawnSpellEffectEx(x, y, 0 /* facing */,
							new War3ID(rawcode), whichEffectType, 0));
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddLightning",
				(arguments, globalScope, triggerScope) -> {
					final String rawcode = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					final double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
					final double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());
					final double x2 = arguments.get(4).visit(RealJassValueVisitor.getInstance());
					final double y2 = arguments.get(5).visit(RealJassValueVisitor.getInstance());
					return newLightning(lightningType,
							war3MapViewer.createLightning(War3ID.fromString(rawcode), (float) x, (float) y,
									war3MapViewer.terrain.getGroundHeight((float) x, (float) y), (float) x2,
									(float) y2, war3MapViewer.terrain.getGroundHeight((float) x2, (float) y2)));
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddLightningEx",
				(arguments, globalScope, triggerScope) -> {
					final String rawcode = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					final double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
					final double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());
					final double z = arguments.get(4).visit(RealJassValueVisitor.getInstance());
					final double x2 = arguments.get(5).visit(RealJassValueVisitor.getInstance());
					final double y2 = arguments.get(6).visit(RealJassValueVisitor.getInstance());
					final double z2 = arguments.get(7).visit(RealJassValueVisitor.getInstance());
					return newLightning(lightningType,
							war3MapViewer.createLightning(War3ID.fromString(rawcode), (float) x, (float) y,
									(float) z, (float) x2, (float) y2, (float) z2));
				});
		jassProgramVisitor.getJassNativeManager().createNative("MoveLightning",
				(arguments, globalScope, triggerScope) -> {
					final SimulationRenderComponentLightningMovable lightning = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
					final double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());
					final double x2 = arguments.get(4).visit(RealJassValueVisitor.getInstance());
					final double y2 = arguments.get(5).visit(RealJassValueVisitor.getInstance());
					if ((lightning != null) && !lightning.isRemoved()) {
						lightning.move((float) x, (float) y, (float) x2, (float) y2);
						return BooleanJassValue.TRUE;
					}
					return BooleanJassValue.FALSE;
				});
		jassProgramVisitor.getJassNativeManager().createNative("MoveLightningEx",
				(arguments, globalScope, triggerScope) -> {
					final SimulationRenderComponentLightningMovable lightning = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
					final double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());
					final double z = arguments.get(4).visit(RealJassValueVisitor.getInstance());
					final double x2 = arguments.get(5).visit(RealJassValueVisitor.getInstance());
					final double y2 = arguments.get(6).visit(RealJassValueVisitor.getInstance());
					final double z2 = arguments.get(7).visit(RealJassValueVisitor.getInstance());
					if ((lightning != null) && !lightning.isRemoved()) {
						lightning.move((float) x, (float) y, (float) z, (float) x2, (float) y2, (float) z2);
						return BooleanJassValue.TRUE;
					}
					return BooleanJassValue.FALSE;
				});
		jassProgramVisitor.getJassNativeManager().createNative("DestroyLightning",
				(arguments, globalScope, triggerScope) -> {
					final SimulationRenderComponentLightning lightning = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					BooleanJassValue wasRemoved = BooleanJassValue.FALSE;
					if ((lightning != null) && !lightning.isRemoved()) {
						lightning.remove();
						wasRemoved = BooleanJassValue.TRUE;
					}
					return wasRemoved;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetLightningColor",
				(arguments, globalScope, triggerScope) -> {
					final SimulationRenderComponentLightningMovable lightning = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final double r = arguments.get(1).visit(RealJassValueVisitor.getInstance());
					final double g = arguments.get(2).visit(RealJassValueVisitor.getInstance());
					final double b = arguments.get(3).visit(RealJassValueVisitor.getInstance());
					final double a = arguments.get(4).visit(RealJassValueVisitor.getInstance());
					if ((lightning != null) && !lightning.isRemoved()) {
						lightning.setColor((float) r, (float) g, (float) b, (float) a);
						return BooleanJassValue.TRUE;
					}
					return BooleanJassValue.FALSE;
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetAbilityEffectById",
				(arguments, globalScope, triggerScope) -> {
					final int abilityRawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final CEffectType artType = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
					final int index = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					return StringJassValue.of(war3MapViewer
							.getEffectAttachmentUI(new War3ID(abilityRawcode), artType, index).getModelPath());
				});
		jassProgramVisitor.getJassNativeManager().createNative("DestroyEffect",
				(arguments, globalScope, triggerScope) -> {
					final ScriptEffect fx = nullable(arguments, 0, ObjectJassValueVisitor.<ScriptEffect>getInstance());
					if (fx != null) {
						fx.setKillWhenDone(true);
						release(fx);
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("BlzRemoveEffect",
				(arguments, globalScope, triggerScope) -> {
					final ScriptEffect fx = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					if (fx != null) {
						fx.removeWithoutDeath();
					}
					return null;
				});
	}
}
