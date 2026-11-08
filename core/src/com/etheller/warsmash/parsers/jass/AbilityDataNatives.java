package com.etheller.warsmash.parsers.jass;

import com.etheller.interpreter.ast.debug.JassException;
import com.etheller.interpreter.ast.function.JassFunction;
import com.etheller.interpreter.ast.scope.trigger.TriggerIntegerExpression;
import com.etheller.interpreter.ast.util.JassProgram;
import com.etheller.interpreter.ast.value.BooleanJassValue;
import com.etheller.interpreter.ast.value.CodeJassValue;
import com.etheller.interpreter.ast.value.HandleJassType;
import com.etheller.interpreter.ast.value.HandleJassTypeConstructor;
import com.etheller.interpreter.ast.value.HandleJassValue;
import com.etheller.interpreter.ast.value.IntegerJassValue;
import com.etheller.interpreter.ast.value.JassType;
import com.etheller.interpreter.ast.value.JassValue;
import com.etheller.interpreter.ast.value.PrimitiveJassType;
import com.etheller.interpreter.ast.value.RealJassValue;
import com.etheller.interpreter.ast.value.StringJassValue;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.CodeJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.RealJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.StringJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.WrappedStringJassValueVisitor;
import com.etheller.warsmash.parsers.jass.Jass2.CommonEnvironment;
import com.etheller.warsmash.parsers.jass.scope.CommonTriggerExecutionScope;
import com.etheller.warsmash.parsers.jass.triggers.EnumSetHandle;
import com.etheller.warsmash.parsers.jass.triggers.HandleList;
import com.etheller.warsmash.parsers.jass.triggers.IntExpr;
import com.etheller.warsmash.parsers.jass.triggers.LocationJass;
import com.etheller.warsmash.parsers.jass.triggers.StringList;
import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.units.manager.MutableObjectData.WorldEditorDataType;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.mdx.Sequence;
import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens.PrimaryTag;
import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens.SecondaryTag;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.AutocastType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitorJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.ability.AbilityBuilderAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.ABAction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.event.ABTimeOfDayEvent;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.jass.ABActionJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.parser.AbilityBuilderConfiguration;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.parser.AbilityBuilderDupe;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.parser.AbilityBuilderParser;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.parser.AbilityBuilderType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.parser.template.DataFieldLetter;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.timer.ABTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.types.impl.CAbilityTypeAbilityBuilderLevelData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CProjectile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.ExternStringMsgTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.ResourceType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Warsmash ability API: lists, ability type data, user data, local stores, configurations, game objects, targeting, threads and timers.
 */
final class AbilityDataNatives extends CommonNatives {
	AbilityDataNatives(final CommonEnvironment environment) {
		super(environment);
	}

	@Override
	void register() {
		registerCollections();
		registerAbilityTypeData();
		registerUserData();
		registerLocalStores();
		registerConfigurations();
		registerGameObjects();
		registerTargeting();
		registerThreadsAndTimers();
	}

	private void registerCollections() {
		final HandleJassType handleType = type("handle");
		final HandleJassType intexprType = type("intexpr");
		final HandleJassType handlelistType = type("handlelist");
		final HandleJassType behaviorcategoryType = type("behaviorcategory");
		final HandleJassType abilitycategoryType = type("abilitycategory");
		final HandleJassType stringlistType = type("stringlist");

		jassProgramVisitor.getJassNativeManager().createNative("ConvertAbilityCategory",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(abilitycategoryType, CAbilityCategory.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertBehaviorCategory",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(behaviorcategoryType, CBehaviorCategory.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateHandleList",
				(arguments, globalScope, triggerScope) -> {
					return new HandleJassValue(handlelistType,
							new HandleList(this.simulation.getHandleIdAllocator().createHandleId()));
				});
		jassProgramVisitor.getJassNativeManager().createNative("HandleListAdd",
				(arguments, globalScope, triggerScope) -> {
					final HandleList whichList = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final JassValue x = arguments.get(1);
					if (whichList != null) {
						whichList.add(x);
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("HandleListRemove",
				(arguments, globalScope, triggerScope) -> {
					final HandleList whichList = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final JassValue x = arguments.get(1);
					if (whichList != null) {
						return BooleanJassValue.of(whichList.remove(x));
					}
					return BooleanJassValue.FALSE;
				});
		jassProgramVisitor.getJassNativeManager().createNative("HandleListSize",
				(arguments, globalScope, triggerScope) -> {
					final HandleList whichList = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					if (whichList != null) {
						return IntegerJassValue.of(whichList.size());
					}
					return IntegerJassValue.ZERO;
				});
		jassProgramVisitor.getJassNativeManager().createNative("HandleListGet",
				(arguments, globalScope, triggerScope) -> {
					final HandleList whichList = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final int x = nullable(arguments, 1, IntegerJassValueVisitor.getInstance());
					if (whichList != null) {
						return whichList.get(x);
					}
					return handleType.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateStringList",
				(arguments, globalScope, triggerScope) -> {
					return new HandleJassValue(stringlistType,
							new StringList(this.simulation.getHandleIdAllocator().createHandleId()));
				});
		jassProgramVisitor.getJassNativeManager().createNative("StringListAdd",
				(arguments, globalScope, triggerScope) -> {
					final StringList whichList = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final StringJassValue x = nullable(arguments, 1, WrappedStringJassValueVisitor.getInstance());
					if (whichList != null) {
						whichList.add(x);
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("StringListRemove",
				(arguments, globalScope, triggerScope) -> {
					final StringList whichList = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final StringJassValue x = nullable(arguments, 1, WrappedStringJassValueVisitor.getInstance());
					if (whichList != null) {
						return BooleanJassValue.of(whichList.remove(x));
					}
					return BooleanJassValue.FALSE;
				});
		jassProgramVisitor.getJassNativeManager().createNative("StringListSize",
				(arguments, globalScope, triggerScope) -> {
					final StringList whichList = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					if (whichList != null) {
						return IntegerJassValue.of(whichList.size());
					}
					return IntegerJassValue.ZERO;
				});
		jassProgramVisitor.getJassNativeManager().createNative("StringListGet",
				(arguments, globalScope, triggerScope) -> {
					final StringList whichList = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final int x = nullable(arguments, 1, IntegerJassValueVisitor.getInstance());
					if (whichList != null) {
						return whichList.get(x);
					}
					return PrimitiveJassType.STRING.getNullValue();
				});
		// Script file natives
		jassProgramVisitor.getJassNativeManager().createNative("LoadScriptFile",
				(arguments, globalScope, triggerScope) -> {
					final String filePath = nullable(arguments, 0, StringJassValueVisitor.getInstance());
					String funcToCall = "main";
					if (arguments.size() > 1) {
						final String mainFunction = nullable(arguments, 1, StringJassValueVisitor.getInstance());
						if (mainFunction != null) {
							funcToCall = mainFunction;
						}
					}
					if (filePath != null) {
						Jass2.doPreloadScript(dataSource, uiViewport, uiScene, war3MapViewer, filePath, meleeUI,
								originalFiles, jassProgramVisitor, funcToCall, true);
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("ForFiles",
				(arguments, globalScope, triggerScope) -> {
					final String directoryPath = nullable(arguments, 0, StringJassValueVisitor.getInstance());
					final CodeJassValue callback = nullable(arguments, 1, CodeJassValueVisitor.getInstance());

					if (directoryPath == null) {
						return null;
					}
					final String lowerCaseDirectoryPath = directoryPath.toLowerCase(Locale.US);
					final String lowerCaseDirectoryPathLinux = directoryPath.toLowerCase(Locale.US).replace("\\",
							"/");

					final Collection<String> listfile = dataSource.getListfile();
					final Map<String, String> fixedListfile = new HashMap<>();
					for (final String path : listfile) {
						fixedListfile.put(path.toLowerCase(Locale.US), path);
					}
					for (final Map.Entry<String, String> pathAndPath : fixedListfile.entrySet()) {
						final String lowerCasePath = pathAndPath.getKey();
						final String realPath = pathAndPath.getValue();
						if (lowerCasePath.startsWith(lowerCaseDirectoryPath)
								|| lowerCasePath.startsWith(lowerCaseDirectoryPathLinux)) {
							try {
								globalScope.runThreadUntilCompletion(globalScope.createThread(callback,
										CommonTriggerExecutionScope.enumFileScope(triggerScope, realPath)));
							}
							catch (final Exception e) {
								throw new JassException(globalScope, "Exception during ForFiles", e);
							}
						}
					}

					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetEnumFilePath",
				(arguments, globalScope, triggerScope) -> {
					return new StringJassValue(((CommonTriggerExecutionScope) triggerScope).getEnumFilePath());
				});
		// Ability Builder from jass natives:
		jassProgramVisitor.getJassNativeManager().createNative("IntExpr",
				(arguments, globalScope, triggerScope) -> {
					final CodeJassValue func = arguments.get(0).visit(CodeJassValueVisitor.getInstance());
					return new HandleJassValue(intexprType, new IntExpr(func));
				});
		jassProgramVisitor.getJassNativeManager().createNative("DestroyIntExpr",
				(arguments, globalScope, triggerScope) -> {
					final TriggerIntegerExpression boolexpr = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					reportMissingDestructorOnce("DestroyIntExpr");
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetGameObjectFieldAsStringList",
				(arguments, globalScope, triggerScope) -> {
					final GameObject gameObject = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final String key = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					if (gameObject != null) {
						final List<String> javaValueList = gameObject.getFieldAsList(key);
						if (javaValueList != null) {
							final StringList stringList = new StringList(
									this.simulation.getHandleIdAllocator().createHandleId(), javaValueList);
							return new HandleJassValue(stringlistType, stringList);
						}
					}
					return stringlistType.getNullValue();
				});
	}

	private void registerAbilityTypeData() {
		final HandleJassType targettypeType = type("targettype");
		final HandleJassType datafieldletterType = type("datafieldletter");

		jassProgramVisitor.getJassNativeManager().createNative("ConvertTargetType",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(targettypeType, CTargetType.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("AbilityTypeLevelDataAddTargetAllowed",
				(arguments, globalScope, triggerScope) -> {
					final List<CAbilityTypeAbilityBuilderLevelData> levelData = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final int level = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final CTargetType targetType = nullable(arguments, 2, ObjectJassValueVisitor.getInstance());
					if ((levelData != null) && (targetType != null)) {
						final EnumSet<CTargetType> targetsAllowed = levelData.get(level).getTargetsAllowed();
						targetsAllowed.add(targetType);
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("AbilityTypeLevelDataRemoveTargetAllowed",
				(arguments, globalScope, triggerScope) -> {
					final List<CAbilityTypeAbilityBuilderLevelData> levelData = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final int level = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final CTargetType targetType = nullable(arguments, 2, ObjectJassValueVisitor.getInstance());
					if ((levelData != null) && (targetType != null)) {
						final EnumSet<CTargetType> targetsAllowed = levelData.get(level).getTargetsAllowed();
						targetsAllowed.remove(targetType);
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertDataFieldLetter",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(datafieldletterType, DataFieldLetter.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetAbilityTypeLevelDataReal",
				(arguments, globalScope, triggerScope) -> {
					final List<CAbilityTypeAbilityBuilderLevelData> levelData = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final int level = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final DataFieldLetter dataField = nullable(arguments, 2, ObjectJassValueVisitor.getInstance());
					if ((levelData != null) && (dataField != null)) {
						final String data = levelData.get(level).getData().get(dataField.getIndex());
						if (data.equals("-") || data.isEmpty()) {
							return JassType.REAL.getNullValue();
						}
						return RealJassValue.of(Float.parseFloat(data));
					}
					return JassType.REAL.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetAbilityTypeLevelDataInteger",
				(arguments, globalScope, triggerScope) -> {
					final List<CAbilityTypeAbilityBuilderLevelData> levelData = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final int level = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final DataFieldLetter dataField = nullable(arguments, 2, ObjectJassValueVisitor.getInstance());
					if ((levelData != null) && (dataField != null)) {
						final String data = levelData.get(level).getData().get(dataField.getIndex());
						if (data.equals("-") || data.isEmpty()) {
							return JassType.INTEGER.getNullValue();
						}
						return IntegerJassValue.of(Integer.parseInt(data));
					}
					return JassType.INTEGER.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetAbilityTypeLevelDataAsID",
				(arguments, globalScope, triggerScope) -> {
					final List<CAbilityTypeAbilityBuilderLevelData> levelData = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final int level = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final DataFieldLetter dataField = nullable(arguments, 2, ObjectJassValueVisitor.getInstance());
					if ((levelData != null) && (dataField != null)) {
						final String data = levelData.get(level).getData().get(dataField.getIndex());
						if (data.equals("-") || data.isEmpty()) {
							return JassType.INTEGER.getNullValue();
						}
						return IntegerJassValue.of(War3ID.fromString(data).getValue());
					}
					return JassType.INTEGER.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetAbilityTypeLevelDataBoolean",
				(arguments, globalScope, triggerScope) -> {
					final List<CAbilityTypeAbilityBuilderLevelData> levelData = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final int level = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final DataFieldLetter dataField = nullable(arguments, 2, ObjectJassValueVisitor.getInstance());
					if ((levelData != null) && (dataField != null)) {
						final String data = levelData.get(level).getData().get(dataField.getIndex());
						if (data.equals("-") || data.isEmpty()) {
							return JassType.BOOLEAN.getNullValue();
						}
						return BooleanJassValue.of(Integer.parseInt(data) == 1);
					}
					return JassType.BOOLEAN.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetAbilityTypeLevelDataString",
				(arguments, globalScope, triggerScope) -> {
					final List<CAbilityTypeAbilityBuilderLevelData> levelData = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final int level = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final DataFieldLetter dataField = nullable(arguments, 2, ObjectJassValueVisitor.getInstance());
					if ((levelData != null) && (dataField != null)) {
						final String data = levelData.get(level).getData().get(dataField.getIndex());
						if (data.isEmpty()) {
							return JassType.STRING.getNullValue();
						}
						return StringJassValue.of(data);
					}
					return JassType.STRING.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetAbilityTypeLevelDataFirstBuffId",
				(arguments, globalScope, triggerScope) -> {
					final List<CAbilityTypeAbilityBuilderLevelData> levelData = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final int level = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					if (levelData != null) {

						final List<War3ID> buffs = levelData.get(level).getBuffs();
						if ((buffs != null) && !buffs.isEmpty()) {
							return IntegerJassValue.of(buffs.get(0).getValue());
						}
					}
					return JassType.INTEGER.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetAbilityTypeLevelDataDurationNormal",
				(arguments, globalScope, triggerScope) -> {
					final List<CAbilityTypeAbilityBuilderLevelData> levelData = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final int level = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					if (levelData != null) {
						return RealJassValue.of(levelData.get(level).getDurationNormal());
					}
					return JassType.REAL.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetAbilityTypeLevelDataDurationHero",
				(arguments, globalScope, triggerScope) -> {
					final List<CAbilityTypeAbilityBuilderLevelData> levelData = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final int level = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					if (levelData != null) {
						return RealJassValue.of(levelData.get(level).getDurationHero());
					}
					return JassType.REAL.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetAbilityTypeLevelDataCastTime",
				(arguments, globalScope, triggerScope) -> {
					final List<CAbilityTypeAbilityBuilderLevelData> levelData = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final int level = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					if (levelData != null) {
						return RealJassValue.of(levelData.get(level).getCastTime());
					}
					return JassType.REAL.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetAbilityTypeLevelUnitID",
				(arguments, globalScope, triggerScope) -> {
					final List<CAbilityTypeAbilityBuilderLevelData> levelData = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final int level = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					if (levelData != null) {
						final War3ID data = levelData.get(level).getUnitId();
						if (data != null) {
							return IntegerJassValue.of(data.getValue());
						}
					}
					return JassType.INTEGER.getNullValue();
				});
	}

	private void registerUserData() {
		final HandleJassType unitType = type("unit");
		final HandleJassType destructableType = type("destructable");
		final HandleJassType abilityType = type("ability");
		final HandleJassType buffType = type("buff");
		final HandleJassType timerType = type("timer");
		final HandleJassType locationType = type("location");
		final HandleJassType abilitytypeleveldataType = type("abilitytypeleveldata");
		final HandleJassType localstoreType = type("localstore");
		final HandleJassType destructablebuffType = type("destructablebuff");
		final HandleJassType abtimeofdayeventType = type("abtimeofdayevent");
		final HandleJassType gameobjectType = type("gameobject");
		final HandleJassType projectileType = type("projectile");
		final HandleJassType nonstackingstatbuffType = type("nonstackingstatbonus");
		final HandleJassType abtimerType = type("abtimer");

		jassProgramVisitor.getJassNativeManager().createNative("GetAbilityUserDataString",
				(arguments, globalScope, triggerScope) -> {
					final CAbility abilityFromHandle = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					if (abilityFromHandle instanceof AbilityBuilderAbility) {
						final AbilityBuilderAbility ability = (AbilityBuilderAbility) abilityFromHandle;
						final Map<String, Object> localStore = ability.getLocalStore();
						final Object object = localStore.get(childKey);
						if (object != null) {
							return new StringJassValue((String) object);
						}
					}
					return JassType.STRING.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetAbilityUserDataInteger",
				(arguments, globalScope, triggerScope) -> {
					final CAbility abilityFromHandle = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					if (abilityFromHandle instanceof AbilityBuilderAbility) {
						final AbilityBuilderAbility ability = (AbilityBuilderAbility) abilityFromHandle;
						final Map<String, Object> localStore = ability.getLocalStore();
						final Object object = localStore.get(childKey);
						if (object != null) {
							return IntegerJassValue.of((Integer) object);
						}
					}
					return IntegerJassValue.ZERO;
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetAbilityUserDataBoolean",
				(arguments, globalScope, triggerScope) -> {
					final CAbility abilityFromHandle = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					if (abilityFromHandle instanceof AbilityBuilderAbility) {
						final AbilityBuilderAbility ability = (AbilityBuilderAbility) abilityFromHandle;
						final Map<String, Object> localStore = ability.getLocalStore();
						final Object object = localStore.get(childKey);
						if (object != null) {
							return BooleanJassValue.of((Boolean) object);
						}
					}
					return BooleanJassValue.FALSE;
				});
		registerAbilityUserDataHandleNatives(jassProgramVisitor, abilitytypeleveldataType, "AbilityTypeLevelData");
		registerAbilityUserDataHandleNatives(jassProgramVisitor, abilityType, "Ability");
		registerAbilityUserDataHandleNatives(jassProgramVisitor, buffType, "Buff");
		registerAbilityUserDataHandleNatives(jassProgramVisitor, unitType, "Unit");
		registerAbilityUserDataHandleNatives(jassProgramVisitor, destructableType, "Destructable");
		registerAbilityUserDataHandleNatives(jassProgramVisitor, destructablebuffType, "DestructableBuff");
		registerAbilityUserDataHandleNatives(jassProgramVisitor, abtimeofdayeventType, "ABTimeOfDayEvent");
		registerAbilityUserDataHandleNatives(jassProgramVisitor, gameobjectType, "GameObject");
		registerAbilityUserDataHandleNatives(jassProgramVisitor, nonstackingstatbuffType, "NonStackingStatBonus");
		registerAbilityUserDataHandleNatives(jassProgramVisitor, projectileType, "Projectile");
		registerAbilityUserDataHandleNatives(jassProgramVisitor, locationType, "Location");
		registerAbilityUserDataHandleNatives(jassProgramVisitor, timerType, "Timer");
		registerAbilityUserDataHandleNatives(jassProgramVisitor, abtimerType, "ABTimer");
		registerAbilityUserDataHandleNatives(jassProgramVisitor, localstoreType, "LocalStore");
		// TODO below is overwriting what already exists
		jassProgramVisitor.getJassNativeManager().createNative("GetLocalStoreLocationHandle",
				(arguments, globalScope, triggerScope) -> {
					final Map<String, Object> localStore = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					Object object = localStore.get(childKey);
					if (object != null) {
						if (object instanceof AbilityPointTarget) {
							final AbilityPointTarget apt = (AbilityPointTarget) object;
							object = new LocationJass(apt.x, apt.y,
									this.simulation.getHandleIdAllocator().createHandleId());
						}
						return new HandleJassValue(locationType, object);
					}
					return locationType.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetLocalStoreHandle",
				(arguments, globalScope, triggerScope) -> {
					final Map<String, Object> localStore = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final Object unwrappedHandleUnderlyingJavaObject = nullable(arguments, 2,
							ObjectJassValueVisitor.getInstance());
					final Object object = localStore.put(childKey, unwrappedHandleUnderlyingJavaObject);
					return BooleanJassValue.of(object != null);
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetAbilityUserDataString",
				(arguments, globalScope, triggerScope) -> {
					final CAbility abilityFromHandle = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final String value = nullable(arguments, 2, StringJassValueVisitor.getInstance());
					if (abilityFromHandle instanceof AbilityBuilderAbility) {
						final AbilityBuilderAbility ability = (AbilityBuilderAbility) abilityFromHandle;
						final Map<String, Object> localStore = ability.getLocalStore();
						final Object object = localStore.put(childKey, value);
						return BooleanJassValue.of(object != null);
					}
					return BooleanJassValue.FALSE;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetAbilityUserDataInteger",
				(arguments, globalScope, triggerScope) -> {
					final CAbility abilityFromHandle = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final Integer value = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					if (abilityFromHandle instanceof AbilityBuilderAbility) {
						final AbilityBuilderAbility ability = (AbilityBuilderAbility) abilityFromHandle;
						final Map<String, Object> localStore = ability.getLocalStore();
						final Object object = localStore.put(childKey, value);
						return BooleanJassValue.of(object != null);
					}
					return BooleanJassValue.FALSE;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetAbilityUserDataBoolean",
				(arguments, globalScope, triggerScope) -> {
					final CAbility abilityFromHandle = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final Boolean value = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
					if (abilityFromHandle instanceof AbilityBuilderAbility) {
						final AbilityBuilderAbility ability = (AbilityBuilderAbility) abilityFromHandle;
						final Map<String, Object> localStore = ability.getLocalStore();
						final Object object = localStore.put(childKey, value);
						return BooleanJassValue.of(object != null);
					}
					return BooleanJassValue.FALSE;
				});
		jassProgramVisitor.getJassNativeManager().createNative("HasAbilityUserData",
				(arguments, globalScope, triggerScope) -> {
					final CAbility abilityFromHandle = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					if (abilityFromHandle instanceof AbilityBuilderAbility) {
						final AbilityBuilderAbility ability = (AbilityBuilderAbility) abilityFromHandle;
						final Map<String, Object> localStore = ability.getLocalStore();
						return BooleanJassValue.of(localStore.containsKey(childKey));
					}
					return BooleanJassValue.FALSE;
				});
		jassProgramVisitor.getJassNativeManager().createNative("FlushParentAbilityUserData",
				(arguments, globalScope, triggerScope) -> {
					final CAbility abilityFromHandle = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					if (abilityFromHandle instanceof AbilityBuilderAbility) {
						final AbilityBuilderAbility ability = (AbilityBuilderAbility) abilityFromHandle;
						if (ability != null) {
							final Map<String, Object> localStore = ability.getLocalStore();
							localStore.clear();
						}
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("FlushChildAbilityUserData",
				(arguments, globalScope, triggerScope) -> {
					final CAbility abilityFromHandle = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					if (abilityFromHandle instanceof AbilityBuilderAbility) {
						final AbilityBuilderAbility ability = (AbilityBuilderAbility) abilityFromHandle;
						final Map<String, Object> localStore = ability.getLocalStore();
						return BooleanJassValue.of(localStore.remove(childKey) != null);
					}
					return BooleanJassValue.FALSE;
				});
	}

	private void registerLocalStores() {
		final HandleJassType localstoreType = type("localstore");

		// ===== local store =====
		jassProgramVisitor.getJassNativeManager().createNative("CreateLocalStore",
				(arguments, globalScope, triggerScope) -> {
					return new HandleJassValue(localstoreType, new HashMap<String, Object>());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetLocalStoreString",
				(arguments, globalScope, triggerScope) -> {
					final Map<String, Object> localStore = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final Object object = localStore.get(childKey);
					if (object != null) {
						return new StringJassValue((String) object);
					}
					return JassType.STRING.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetLocalStoreInteger",
				(arguments, globalScope, triggerScope) -> {
					final Map<String, Object> localStore = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final Object object = localStore.get(childKey);
					if (object != null) {
						if (object instanceof War3ID) {
							return IntegerJassValue.of(((War3ID) object).getValue());
						}
						return IntegerJassValue.of((Integer) object);
					}
					return IntegerJassValue.ZERO;
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetLocalStoreReal",
				(arguments, globalScope, triggerScope) -> {
					final Map<String, Object> localStore = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final Object object = localStore.get(childKey);
					if (object != null) {
						return RealJassValue.of(((Number) object).doubleValue());
					}
					return IntegerJassValue.ZERO;
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetLocalStoreCode",
				(arguments, globalScope, triggerScope) -> {
					final Map<String, Object> localStore = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final Object object = localStore.get(childKey);
					if (object instanceof CodeJassValue) {
						return (CodeJassValue) object;
					}
					return JassType.CODE.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetLocalStoreBoolean",
				(arguments, globalScope, triggerScope) -> {
					final Map<String, Object> localStore = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final Object object = localStore.get(childKey);
					if (object != null) {
						return BooleanJassValue.of((Boolean) object);
					}
					return BooleanJassValue.FALSE;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetLocalStoreString",
				(arguments, globalScope, triggerScope) -> {
					final Map<String, Object> localStore = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final String value = nullable(arguments, 2, StringJassValueVisitor.getInstance());
					final Object object = localStore.put(childKey, value);
					return BooleanJassValue.of(object != null);
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetLocalStoreInteger",
				(arguments, globalScope, triggerScope) -> {
					final Map<String, Object> localStore = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final Integer value = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					final Object object = localStore.put(childKey, value);
					return BooleanJassValue.of(object != null);
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetLocalStoreReal",
				(arguments, globalScope, triggerScope) -> {
					final Map<String, Object> localStore = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final float value = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					final Object object = localStore.put(childKey, value);
					return BooleanJassValue.of(object != null);
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetLocalStoreBoolean",
				(arguments, globalScope, triggerScope) -> {
					final Map<String, Object> localStore = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final Boolean value = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
					final Object object = localStore.put(childKey, value);
					return BooleanJassValue.of(object != null);
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetLocalStoreCode",
				(arguments, globalScope, triggerScope) -> {
					final Map<String, Object> localStore = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final CodeJassValue value = arguments.get(2).visit(CodeJassValueVisitor.getInstance());
					final Object object = localStore.put(childKey, value);
					return BooleanJassValue.of(object != null);
				});
		jassProgramVisitor.getJassNativeManager().createNative("LocalStoreContainsKey",
				(arguments, globalScope, triggerScope) -> {
					final Map<String, Object> localStore = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					return BooleanJassValue.of(localStore.containsKey(childKey));
				});
		final JassFunction flushParentLocalStore = (arguments, globalScope, triggerScope) -> {
			final Map<String, Object> localStore = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
			localStore.clear();
			return null;
		};
		jassProgramVisitor.getJassNativeManager().createNative("FlushParentLocalStore", flushParentLocalStore);
		jassProgramVisitor.getJassNativeManager().createNative("DestroyLocalStore", flushParentLocalStore);
		jassProgramVisitor.getJassNativeManager().createNative("FlushChildLocalStore",
				(arguments, globalScope, triggerScope) -> {
					final Map<String, Object> localStore = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					return BooleanJassValue.of(localStore.remove(childKey) != null);
				});
		jassProgramVisitor.getJassNativeManager().createNative("LocalStoreCleanUpCastInstance",
				(arguments, globalScope, triggerScope) -> {
					final Map<String, Object> localStore = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final int castId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final Set<String> keySet = new HashSet<>(localStore.keySet());
					for (final String key : keySet) {
						if (key.contains("#" + castId)) {
							localStore.remove(key);
						}
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetAbilityLocalStore",
				(arguments, globalScope, triggerScope) -> {
					final CAbility abilityFromHandle = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					if (abilityFromHandle instanceof AbilityBuilderAbility) {
						final AbilityBuilderAbility ability = (AbilityBuilderAbility) abilityFromHandle;
						final Map<String, Object> localStore = ability.getLocalStore();
						return new HandleJassValue(localstoreType, localStore);
					}
					return localstoreType.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetTriggerLocalStore",
				(arguments, globalScope, triggerScope) -> {
					return new HandleJassValue(localstoreType,
							((CommonTriggerExecutionScope) triggerScope).getTriggerLocalStore());
				});
	}

	private void registerConfigurations() {
		final HandleJassType autocasttypeType = type("autocasttype");
		final HandleJassType abconftypeType = type("abconftype");
		final HandleJassType abilitybuilderconfigurationType = type("abilitybuilderconfiguration");

		// ==== end of local store ====

		// Ability Builder Configuration
		jassProgramVisitor.getJassNativeManager().createNative("CreateAbilityBuilderConfiguration",
				(arguments, globalScope, triggerScope) -> {
					final AbilityBuilderConfiguration emptyConfiguration = new AbilityBuilderConfiguration(
							new AbilityBuilderParser(), new AbilityBuilderDupe());
					return new HandleJassValue(abilitybuilderconfigurationType, emptyConfiguration);
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetABConfCastId",
				(arguments, globalScope, triggerScope) -> {
					final AbilityBuilderConfiguration abConf = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final String castId = nullable(arguments, 1, StringJassValueVisitor.getInstance());

					abConf.setCastId(castId);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetABConfUncastId",
				(arguments, globalScope, triggerScope) -> {
					final AbilityBuilderConfiguration abConf = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final String castId = nullable(arguments, 1, StringJassValueVisitor.getInstance());

					abConf.setUncastId(castId);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetABConfAutoCastOnId",
				(arguments, globalScope, triggerScope) -> {
					final AbilityBuilderConfiguration abConf = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final String castId = nullable(arguments, 1, StringJassValueVisitor.getInstance());

					abConf.setAutoCastOnId(castId);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetABConfAutoCastOffId",
				(arguments, globalScope, triggerScope) -> {
					final AbilityBuilderConfiguration abConf = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final String castId = nullable(arguments, 1, StringJassValueVisitor.getInstance());

					abConf.setAutoCastOffId(castId);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetABConfAutoCastType",
				(arguments, globalScope, triggerScope) -> {
					final AbilityBuilderConfiguration abConf = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final AutocastType whichAutocastType = nullable(arguments, 1,
							ObjectJassValueVisitor.getInstance());

					abConf.setAutoCastType(whichAutocastType);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetABConfType",
				(arguments, globalScope, triggerScope) -> {
					final AbilityBuilderConfiguration abConf = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final AbilityBuilderType whichType = nullable(arguments, 1,
							ObjectJassValueVisitor.getInstance());

					abConf.setType(whichType);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertAutocastType",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(autocasttypeType, AutocastType.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertABConfType",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(abconftypeType, AbilityBuilderType.VALUES, i);
				});
		// ==Begin section of generated code (these were all the same and not created
		// manually)===
		jassProgramVisitor.getJassNativeManager().createNative("AddABConfAddAbilityAction",
				(arguments, globalScope, triggerScope) -> {
					final AbilityBuilderConfiguration abConf = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final CodeJassValue callback = nullable(arguments, 1, CodeJassValueVisitor.getInstance());
					if ((abConf != null) && (callback != null)) {
						List<ABAction> list = abConf.getOnAddAbility();
						if (list == null) {
							list = new ArrayList<>();
							abConf.setOnAddAbility(list);
						}
						list.add(new ABActionJass(callback));
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddABConfAddDisabledAbilityAction",
				(arguments, globalScope, triggerScope) -> {
					final AbilityBuilderConfiguration abConf = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final CodeJassValue callback = nullable(arguments, 1, CodeJassValueVisitor.getInstance());
					if ((abConf != null) && (callback != null)) {
						List<ABAction> list = abConf.getOnAddDisabledAbility();
						if (list == null) {
							list = new ArrayList<>();
							abConf.setOnAddDisabledAbility(list);
						}
						list.add(new ABActionJass(callback));
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddABConfRemoveAbilityAction",
				(arguments, globalScope, triggerScope) -> {
					final AbilityBuilderConfiguration abConf = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final CodeJassValue callback = nullable(arguments, 1, CodeJassValueVisitor.getInstance());
					if ((abConf != null) && (callback != null)) {
						List<ABAction> list = abConf.getOnRemoveAbility();
						if (list == null) {
							list = new ArrayList<>();
							abConf.setOnRemoveAbility(list);
						}
						list.add(new ABActionJass(callback));
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddABConfRemoveDisabledAbilityAction",
				(arguments, globalScope, triggerScope) -> {
					final AbilityBuilderConfiguration abConf = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final CodeJassValue callback = nullable(arguments, 1, CodeJassValueVisitor.getInstance());
					if ((abConf != null) && (callback != null)) {
						List<ABAction> list = abConf.getOnRemoveDisabledAbility();
						if (list == null) {
							list = new ArrayList<>();
							abConf.setOnRemoveDisabledAbility(list);
						}
						list.add(new ABActionJass(callback));
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddABConfDeathPreCastAction",
				(arguments, globalScope, triggerScope) -> {
					final AbilityBuilderConfiguration abConf = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final CodeJassValue callback = nullable(arguments, 1, CodeJassValueVisitor.getInstance());
					if ((abConf != null) && (callback != null)) {
						List<ABAction> list = abConf.getOnDeathPreCast();
						if (list == null) {
							list = new ArrayList<>();
							abConf.setOnDeathPreCast(list);
						}
						list.add(new ABActionJass(callback));
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddABConfCancelPreCastAction",
				(arguments, globalScope, triggerScope) -> {
					final AbilityBuilderConfiguration abConf = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final CodeJassValue callback = nullable(arguments, 1, CodeJassValueVisitor.getInstance());
					if ((abConf != null) && (callback != null)) {
						List<ABAction> list = abConf.getOnCancelPreCast();
						if (list == null) {
							list = new ArrayList<>();
							abConf.setOnCancelPreCast(list);
						}
						list.add(new ABActionJass(callback));
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddABConfOrderIssuedAction",
				(arguments, globalScope, triggerScope) -> {
					final AbilityBuilderConfiguration abConf = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final CodeJassValue callback = nullable(arguments, 1, CodeJassValueVisitor.getInstance());
					if ((abConf != null) && (callback != null)) {
						List<ABAction> list = abConf.getOnOrderIssued();
						if (list == null) {
							list = new ArrayList<>();
							abConf.setOnOrderIssued(list);
						}
						list.add(new ABActionJass(callback));
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddABConfActivateAction",
				(arguments, globalScope, triggerScope) -> {
					final AbilityBuilderConfiguration abConf = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final CodeJassValue callback = nullable(arguments, 1, CodeJassValueVisitor.getInstance());
					if ((abConf != null) && (callback != null)) {
						List<ABAction> list = abConf.getOnActivate();
						if (list == null) {
							list = new ArrayList<>();
							abConf.setOnActivate(list);
						}
						list.add(new ABActionJass(callback));
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddABConfDeactivateAction",
				(arguments, globalScope, triggerScope) -> {
					final AbilityBuilderConfiguration abConf = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final CodeJassValue callback = nullable(arguments, 1, CodeJassValueVisitor.getInstance());
					if ((abConf != null) && (callback != null)) {
						List<ABAction> list = abConf.getOnDeactivate();
						if (list == null) {
							list = new ArrayList<>();
							abConf.setOnDeactivate(list);
						}
						list.add(new ABActionJass(callback));
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddABConfLevelChangeAction",
				(arguments, globalScope, triggerScope) -> {
					final AbilityBuilderConfiguration abConf = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final CodeJassValue callback = nullable(arguments, 1, CodeJassValueVisitor.getInstance());
					if ((abConf != null) && (callback != null)) {
						List<ABAction> list = abConf.getOnLevelChange();
						if (list == null) {
							list = new ArrayList<>();
							abConf.setOnLevelChange(list);
						}
						list.add(new ABActionJass(callback));
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddABConfBeginCastingAction",
				(arguments, globalScope, triggerScope) -> {
					final AbilityBuilderConfiguration abConf = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final CodeJassValue callback = nullable(arguments, 1, CodeJassValueVisitor.getInstance());
					if ((abConf != null) && (callback != null)) {
						List<ABAction> list = abConf.getOnBeginCasting();
						if (list == null) {
							list = new ArrayList<>();
							abConf.setOnBeginCasting(list);
						}
						list.add(new ABActionJass(callback));
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddABConfEndCastingAction",
				(arguments, globalScope, triggerScope) -> {
					final AbilityBuilderConfiguration abConf = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final CodeJassValue callback = nullable(arguments, 1, CodeJassValueVisitor.getInstance());
					if ((abConf != null) && (callback != null)) {
						List<ABAction> list = abConf.getOnEndCasting();
						if (list == null) {
							list = new ArrayList<>();
							abConf.setOnEndCasting(list);
						}
						list.add(new ABActionJass(callback));
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddABConfChannelTickAction",
				(arguments, globalScope, triggerScope) -> {
					final AbilityBuilderConfiguration abConf = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final CodeJassValue callback = nullable(arguments, 1, CodeJassValueVisitor.getInstance());
					if ((abConf != null) && (callback != null)) {
						List<ABAction> list = abConf.getOnChannelTick();
						if (list == null) {
							list = new ArrayList<>();
							abConf.setOnChannelTick(list);
						}
						list.add(new ABActionJass(callback));
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("AddABConfEndChannelAction",
				(arguments, globalScope, triggerScope) -> {
					final AbilityBuilderConfiguration abConf = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final CodeJassValue callback = nullable(arguments, 1, CodeJassValueVisitor.getInstance());
					if ((abConf != null) && (callback != null)) {
						List<ABAction> list = abConf.getOnEndChannel();
						if (list == null) {
							list = new ArrayList<>();
							abConf.setOnEndChannel(list);
						}
						list.add(new ABActionJass(callback));
					}
					return null;
				});
		// ==== end section of generated code ===

		jassProgramVisitor.getJassNativeManager().createNative("RegisterABConf",
				(arguments, globalScope, triggerScope) -> {
					final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final AbilityBuilderConfiguration abConf = nullable(arguments, 1,
							ObjectJassValueVisitor.getInstance());
					this.simulation.getAbilityData().registerAbilityBuilderType(new War3ID(rawcode), abConf);
					return null;
				});
	}

	private void registerGameObjects() {
		final HandleJassType worldeditordatatypeType = type("worldeditordatatype");
		final HandleJassType gameobjectType = type("gameobject");

		// SLK and game objects
		jassProgramVisitor.getJassNativeManager().createNative("ConvertWorldEditorDataType",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(worldeditordatatypeType, WorldEditorDataType.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetGameObjectById",
				(arguments, globalScope, triggerScope) -> {
					final WorldEditorDataType dataType = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final int rawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					return new HandleJassValue(gameobjectType,
							war3MapViewer.getAllObjectData().getDataByType(dataType).get(new War3ID(rawcode)));
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetGameObjectFieldAsString",
				(arguments, globalScope, triggerScope) -> {
					final GameObject gameObject = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final String key = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final int index = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					if (gameObject != null) {
						return new StringJassValue(gameObject.getFieldAsString(key, index));
					}
					return JassType.STRING.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetGameObjectFieldAsInteger",
				(arguments, globalScope, triggerScope) -> {
					final GameObject gameObject = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final String key = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final int index = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					if (gameObject != null) {
						return IntegerJassValue.of(gameObject.getFieldAsInteger(key, index));
					}
					return JassType.INTEGER.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetGameObjectFieldAsReal",
				(arguments, globalScope, triggerScope) -> {
					final GameObject gameObject = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final String key = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final int index = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					if (gameObject != null) {
						return RealJassValue.of(gameObject.getFieldAsFloat(key, index));
					}
					return JassType.REAL.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetGameObjectFieldAsBoolean",
				(arguments, globalScope, triggerScope) -> {
					final GameObject gameObject = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final String key = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final int index = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					if (gameObject != null) {
						return BooleanJassValue.of(gameObject.getFieldAsBoolean(key, index));
					}
					return JassType.BOOLEAN.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetGameObjectFieldAsID",
				(arguments, globalScope, triggerScope) -> {
					final GameObject gameObject = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final String key = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final int index = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					if (gameObject != null) {
						return IntegerJassValue
								.of(War3ID.fromString(gameObject.getFieldAsString(key, index)).getValue());
					}
					return JassType.INTEGER.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetGameObjectField",
				(arguments, globalScope, triggerScope) -> {
					final GameObject gameObject = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final String key = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					if (gameObject != null) {
						return new StringJassValue(gameObject.getField(key));
					}
					return JassType.STRING.getNullValue();
				});
	}

	private void registerTargeting() {
		final HandleJassType targettypeType = type("targettype");
		final HandleJassType targettypesType = type("targettypes");
		final HandleJassType abilitytargetvisitorType = type("abilitytargetvisitor");
		final HandleJassType primarytagType = type("primarytag");
		final HandleJassType primarytagsType = type("primarytags");
		final HandleJassType secondarytagType = type("secondarytag");
		final HandleJassType secondarytagsType = type("secondarytags");

		jassProgramVisitor.getJassNativeManager().createNative("ParseTargetTypes",
				(arguments, globalScope, triggerScope) -> {
					final StringList stringList = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					if (stringList != null) {
						return new HandleJassValue(targettypesType,
								CTargetType.parseTargetTypeSet(stringList.asJavaValue()));
					}
					return targettypesType.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("ParseTargetType",
				(arguments, globalScope, triggerScope) -> {
					final String key = nullable(arguments, 0, StringJassValueVisitor.getInstance());
					if (key != null) {
						return new HandleJassValue(targettypeType, CTargetType.parseTargetType(key));
					}
					return targettypeType.getNullValue();
				});
		// unit api
		jassProgramVisitor.getJassNativeManager().createNative("CheckUnitForAbilityEffectReaction",
				(arguments, globalScope, triggerScope) -> {
					final CUnit targetUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final CUnit casterUnit = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
					final CAbility ability = nullable(arguments, 2, ObjectJassValueVisitor.getInstance());
					if (targetUnit != null) {
						return BooleanJassValue
								.of(targetUnit.checkForAbilityEffectReaction(this.simulation, casterUnit, ability));
					}
					return JassType.BOOLEAN.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("CheckUnitForAbilityProjReaction",
				(arguments, globalScope, triggerScope) -> {
					final CUnit targetUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final CUnit casterUnit = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
					final CProjectile projectile = nullable(arguments, 2, ObjectJassValueVisitor.getInstance());
					if (targetUnit != null) {
						return BooleanJassValue.of(
								targetUnit.checkForAbilityProjReaction(this.simulation, casterUnit, projectile));
					}
					return JassType.BOOLEAN.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("IsUnitValidTarget",
				(arguments, globalScope, triggerScope) -> {
					final CUnit targetUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final CUnit casterUnit = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
					final List<CAbilityTypeAbilityBuilderLevelData> levelData = nullable(arguments, 2,
							ObjectJassValueVisitor.getInstance());
					final int level = arguments.get(3).visit(IntegerJassValueVisitor.getInstance());
					final boolean targetedEffect = arguments.get(4).visit(BooleanJassValueVisitor.getInstance());
					if (targetUnit != null) {

						final EnumSet<CTargetType> targetsAllowed = levelData.get(level).getTargetsAllowed();
						if (targetsAllowed.isEmpty()) {
							return BooleanJassValue.TRUE;
						}

						return BooleanJassValue.of(targetUnit.canBeTargetedBy(this.simulation, casterUnit,
								targetedEffect, targetsAllowed,
								BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset()));
					}
					return JassType.BOOLEAN.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetUnitTargetError",
				(arguments, globalScope, triggerScope) -> {
					final CUnit targetUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final CUnit casterUnit = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
					final EnumSet<CTargetType> targetsAllowed = nullable(arguments, 2,
							ObjectJassValueVisitor.getInstance());
					final boolean targetedEffect = arguments.get(3).visit(BooleanJassValueVisitor.getInstance());
					if (targetUnit != null) {
						if (targetsAllowed == null) {
							return JassType.STRING.getNullValue();
						}
						final ExternStringMsgTargetCheckReceiver<CWidget> externStringMsgReceiver = ExternStringMsgTargetCheckReceiver
								.<CWidget>getInstance().reset();
						targetUnit.canBeTargetedBy(this.simulation, casterUnit, targetedEffect, targetsAllowed,
								externStringMsgReceiver);
						return StringJassValue.of(externStringMsgReceiver.getExternStringKey());
					}
					return JassType.STRING.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("IsValidTarget",
				(arguments, globalScope, triggerScope) -> {
					final CWidget targetUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final CUnit casterUnit = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
					final List<CAbilityTypeAbilityBuilderLevelData> levelData = nullable(arguments, 2,
							ObjectJassValueVisitor.getInstance());
					final int level = arguments.get(3).visit(IntegerJassValueVisitor.getInstance());
					if (targetUnit != null) {

						final EnumSet<CTargetType> targetsAllowed = levelData.get(level).getTargetsAllowed();
						if (targetsAllowed.isEmpty()) {
							return BooleanJassValue.TRUE;
						}

						return BooleanJassValue.of(targetUnit.canBeTargetedBy(this.simulation, casterUnit,
								targetsAllowed, BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset()));
					}
					return JassType.BOOLEAN.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetTargetError",
				(arguments, globalScope, triggerScope) -> {
					final CWidget targetUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final CUnit casterUnit = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
					final EnumSet<CTargetType> targetsAllowed = nullable(arguments, 2,
							ObjectJassValueVisitor.getInstance());
					if (targetUnit != null) {
						if (targetsAllowed == null) {
							return JassType.STRING.getNullValue();
						}

						final ExternStringMsgTargetCheckReceiver<CWidget> externStringMsgReceiver = ExternStringMsgTargetCheckReceiver
								.<CWidget>getInstance().reset();
						targetUnit.canBeTargetedBy(this.simulation, casterUnit, targetsAllowed,
								externStringMsgReceiver);
						return StringJassValue.of(externStringMsgReceiver.getExternStringKey());
					}
					return JassType.STRING.getNullValue();
				});
		// TargetTypes
		jassProgramVisitor.getJassNativeManager().createNative("CreateTargetTypes",
				(arguments, globalScope, triggerScope) -> {
					return new HandleJassValue(targettypesType, EnumSet.noneOf(CTargetType.class));
				});
		jassProgramVisitor.getJassNativeManager().createNative("TargetTypesAdd",
				(arguments, globalScope, triggerScope) -> {
					final EnumSet<CTargetType> whichSet = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					final CTargetType whichType = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					return BooleanJassValue.of(whichSet.add(whichType));
				});
		jassProgramVisitor.getJassNativeManager().createNative("TargetTypesRemove",
				(arguments, globalScope, triggerScope) -> {
					final EnumSet<CTargetType> whichSet = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					final CTargetType whichType = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					return BooleanJassValue.of(whichSet.remove(whichType));
				});
		jassProgramVisitor.getJassNativeManager().createNative("TargetTypesContains",
				(arguments, globalScope, triggerScope) -> {
					final EnumSet<CTargetType> whichSet = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					final CTargetType whichType = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					return BooleanJassValue.of(whichSet.contains(whichType));
				});
		// PrimaryTags
		jassProgramVisitor.getJassNativeManager().createNative("ConvertPrimaryTag",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(primarytagType, PrimaryTag.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreatePrimaryTags",
				(arguments, globalScope, triggerScope) -> {
					return new HandleJassValue(primarytagsType, new EnumSetHandle<>(
							this.simulation.getHandleIdAllocator().createHandleId(), EnumSet.noneOf(PrimaryTag.class)));
				});
		jassProgramVisitor.getJassNativeManager().createNative("PrimaryTagsAdd",
				(arguments, globalScope, triggerScope) -> {
					final EnumSetHandle<PrimaryTag> whichSet = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					final PrimaryTag whichType = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					return BooleanJassValue.of(whichSet.getEnumSet().add(whichType));
				});
		jassProgramVisitor.getJassNativeManager().createNative("PrimaryTagsRemove",
				(arguments, globalScope, triggerScope) -> {
					final EnumSetHandle<PrimaryTag> whichSet = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					final PrimaryTag whichType = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					return BooleanJassValue.of(whichSet.getEnumSet().remove(whichType));
				});
		jassProgramVisitor.getJassNativeManager().createNative("PrimaryTagsContains",
				(arguments, globalScope, triggerScope) -> {
					final EnumSetHandle<PrimaryTag> whichSet = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					final PrimaryTag whichType = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					return BooleanJassValue.of(whichSet.getEnumSet().contains(whichType));
				});
		jassProgramVisitor.getJassNativeManager().createNative("PrimaryTagsSize",
				(arguments, globalScope, triggerScope) -> {
					final EnumSetHandle<PrimaryTag> whichSet = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					return IntegerJassValue.of(whichSet.getEnumSet().size());
				});
		jassProgramVisitor.getJassNativeManager().createNative("PrimaryTagsAny",
				(arguments, globalScope, triggerScope) -> {
					final EnumSetHandle<PrimaryTag> whichSet = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					return new HandleJassValue(primarytagType, Sequence.any(whichSet.getEnumSet()));
				});
		jassProgramVisitor.getJassNativeManager().createNative("DestroyPrimaryTags",
				(arguments, globalScope, triggerScope) -> {
					return null;
				});
		primarytagsType.setConstructorNative(new HandleJassTypeConstructor("CreatePrimaryTags"));
		primarytagsType.setDestructorNative(new HandleJassTypeConstructor("DestroyPrimaryTags"));
		// SecondaryTags
		jassProgramVisitor.getJassNativeManager().createNative("ConvertSecondaryTag",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(secondarytagType, SecondaryTag.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateSecondaryTags",
				(arguments, globalScope, triggerScope) -> {
					return new HandleJassValue(secondarytagsType, new EnumSetHandle<>(
							this.simulation.getHandleIdAllocator().createHandleId(), EnumSet.noneOf(SecondaryTag.class)));
				});
		jassProgramVisitor.getJassNativeManager().createNative("SecondaryTagsAdd",
				(arguments, globalScope, triggerScope) -> {
					final EnumSetHandle<SecondaryTag> whichSet = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					final SecondaryTag whichType = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					return BooleanJassValue.of(whichSet.getEnumSet().add(whichType));
				});
		jassProgramVisitor.getJassNativeManager().createNative("SecondaryTagsRemove",
				(arguments, globalScope, triggerScope) -> {
					final EnumSetHandle<SecondaryTag> whichSet = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					final SecondaryTag whichType = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					return BooleanJassValue.of(whichSet.getEnumSet().remove(whichType));
				});
		jassProgramVisitor.getJassNativeManager().createNative("SecondaryTagsContains",
				(arguments, globalScope, triggerScope) -> {
					final EnumSetHandle<SecondaryTag> whichSet = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					final SecondaryTag whichType = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					return BooleanJassValue.of(whichSet.getEnumSet().contains(whichType));
				});
		jassProgramVisitor.getJassNativeManager().createNative("SecondaryTagsSize",
				(arguments, globalScope, triggerScope) -> {
					final EnumSetHandle<SecondaryTag> whichSet = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					return IntegerJassValue.of(whichSet.getEnumSet().size());
				});
		jassProgramVisitor.getJassNativeManager().createNative("SecondaryTagsAny",
				(arguments, globalScope, triggerScope) -> {
					final EnumSetHandle<SecondaryTag> whichSet = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					return new HandleJassValue(secondarytagType, Sequence.anySecondary(whichSet.getEnumSet()));
				});
		jassProgramVisitor.getJassNativeManager().createNative("DestroySecondaryTags",
				(arguments, globalScope, triggerScope) -> {
					return null;
				});
		secondarytagsType.setConstructorNative(new HandleJassTypeConstructor("CreateSecondaryTags"));
		secondarytagsType.setDestructorNative(new HandleJassTypeConstructor("DestroySecondaryTags"));
		jassProgramVisitor.getJassNativeManager().createNative("PopulateTags",
				(arguments, globalScope, triggerScope) -> {
					final EnumSetHandle<PrimaryTag> whichPrimarySet = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					final EnumSetHandle<SecondaryTag> whichSecondarySet = arguments.get(1)
							.visit(ObjectJassValueVisitor.getInstance());
					final String animationSelector = nullable(arguments, 2, StringJassValueVisitor.getInstance());

					Sequence.populateTags(whichPrimarySet.getEnumSet(), whichSecondarySet.getEnumSet(),
							animationSelector);

					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetAbilityTargetX",
				(arguments, globalScope, triggerScope) -> {
					final AbilityTarget target = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					return RealJassValue.of(target.getX());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetAbilityTargetY",
				(arguments, globalScope, triggerScope) -> {
					final AbilityTarget target = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					return RealJassValue.of(target.getY());
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateAbilityTargetVisitor",
				(arguments, globalScope, triggerScope) -> {
					return new HandleJassValue(abilitytargetvisitorType, new AbilityTargetVisitorJass(
							this.simulation.getHandleIdAllocator().createHandleId(), globalScope));
				});
		jassProgramVisitor.getJassNativeManager().createNative("DestroyAbilityTargetVisitor",
				(arguments, globalScope, triggerScope) -> {
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("AbilityTargetAcceptVisitor",
				(arguments, globalScope, triggerScope) -> {
					final AbilityTarget target = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final AbilityTargetVisitorJass visitor = arguments.get(1)
							.visit(ObjectJassValueVisitor.getInstance());
					target.visit(visitor);
					return null;
				});
		abilitytargetvisitorType.setConstructorNative(new HandleJassTypeConstructor("CreateAbilityTargetVisitor"));
		abilitytargetvisitorType.setDestructorNative(new HandleJassTypeConstructor("DestroyAbilityTargetVisitor"));
	}

	private void registerThreadsAndTimers() {
		final HandleJassType damagetypeType = type("damagetype");
		final HandleJassType abtimerType = type("abtimer");
		final HandleJassType resourcetypeType = type("resourcetype");

		jassProgramVisitor.getJassNativeManager().createNative("ConvertResourceType",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(resourcetypeType, ResourceType.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("String2DamageType",
				(arguments, globalScope, triggerScope) -> {
					final String key = nullable(arguments, 0, StringJassValueVisitor.getInstance());
					return new HandleJassValue(damagetypeType, CDamageType.valueOf(key));
				});
		// code api
		jassProgramVisitor.getJassNativeManager().createNative("StartThread",
				(arguments, globalScope, triggerScope) -> {
					final CodeJassValue threadFunction = arguments.get(0).visit(CodeJassValueVisitor.getInstance());
					globalScope.queueThread(globalScope.createThread(threadFunction));
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("StartAbilityBuilderThread",
				(arguments, globalScope, triggerScope) -> {
					final CodeJassValue threadFunction = arguments.get(0).visit(CodeJassValueVisitor.getInstance());

					final CUnit casterUnit = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
					final Map<String, Object> localStore = nullable(arguments, 2,
							ObjectJassValueVisitor.getInstance());
					final int castId = arguments.get(3).visit(IntegerJassValueVisitor.getInstance());

					globalScope.queueThread(globalScope.createThread(threadFunction,
							CommonTriggerExecutionScope.abilityBuilder(casterUnit, localStore, castId)));
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("CreateABTimer",
				(arguments, globalScope, triggerScope) -> {
					final CUnit casterUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final Map<String, Object> localStore = nullable(arguments, 1,
							ObjectJassValueVisitor.getInstance());
					final int castId = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					final CodeJassValue threadFunction = nullable(arguments, 3, CodeJassValueVisitor.getInstance());

					return new HandleJassValue(abtimerType,
							new ABTimer(casterUnit, localStore, ABActionJass.wrap(threadFunction), castId));
				});
		jassProgramVisitor.getJassNativeManager().createNative("ABTimerSetRepeats",
				(arguments, globalScope, triggerScope) -> {
					final ABTimer timer = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
					timer.setRepeats(flag);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("ABTimerSetTimeoutTime",
				(arguments, globalScope, triggerScope) -> {
					final ABTimer timer = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final float timeoutTime = arguments.get(1).visit(RealJassValueVisitor.getInstance())
							.floatValue();
					timer.setTimeoutTime(timeoutTime);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("ABTimerStartRepeatingTimerWithDelay",
				(arguments, globalScope, triggerScope) -> {
					final ABTimer timer = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final float delay = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					timer.startRepeatingTimerWithDelay(this.simulation, delay);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("ABTimerStart",
				(arguments, globalScope, triggerScope) -> {
					final ABTimer timer = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					timer.start(this.simulation);
					return null;
				});
	}

	private static void registerAbilityUserDataHandleNatives(final JassProgram jassProgramVisitor,
			final HandleJassType handleType, final String nameSuffix) {
		jassProgramVisitor.getJassNativeManager().createNative("GetAbilityUserData" + nameSuffix + "Handle",
				(arguments, globalScope, triggerScope) -> {
					final CAbility abilityFromHandle = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					if (abilityFromHandle instanceof AbilityBuilderAbility) {
						final AbilityBuilderAbility ability = (AbilityBuilderAbility) abilityFromHandle;
						final Map<String, Object> localStore = ability.getLocalStore();
						final Object object = localStore.get(childKey);
						if (object != null) {
							return new HandleJassValue(handleType, object);
						}
					}
					return handleType.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetAbilityUserData" + nameSuffix + "Handle",
				(arguments, globalScope, triggerScope) -> {
					final CAbility abilityFromHandle = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final Object unwrappedHandleUnderlyingJavaObject = nullable(arguments, 2,
							ObjectJassValueVisitor.getInstance());
					if (abilityFromHandle instanceof AbilityBuilderAbility) {
						final AbilityBuilderAbility ability = (AbilityBuilderAbility) abilityFromHandle;
						final Map<String, Object> localStore = ability.getLocalStore();
						final Object object = localStore.put(childKey, unwrappedHandleUnderlyingJavaObject);
						return BooleanJassValue.of(object != null);
					}
					return BooleanJassValue.FALSE;
				});

		jassProgramVisitor.getJassNativeManager().createNative("GetLocalStore" + nameSuffix + "Handle",
				(arguments, globalScope, triggerScope) -> {
					final Map<String, Object> localStore = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final Object object = localStore.get(childKey);
					if (object != null) {
						return new HandleJassValue(handleType, object);
					}
					return handleType.getNullValue();
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetLocalStore" + nameSuffix + "Handle",
				(arguments, globalScope, triggerScope) -> {
					final Map<String, Object> localStore = nullable(arguments, 0,
							ObjectJassValueVisitor.getInstance());
					final String childKey = nullable(arguments, 1, StringJassValueVisitor.getInstance());
					final Object unwrappedHandleUnderlyingJavaObject = nullable(arguments, 2,
							ObjectJassValueVisitor.getInstance());
					final Object object = localStore.put(childKey, unwrappedHandleUnderlyingJavaObject);
					return BooleanJassValue.of(object != null);
				});
	}
}
