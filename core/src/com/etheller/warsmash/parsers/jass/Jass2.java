package com.etheller.warsmash.parsers.jass;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentWeather;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;
import java.util.function.Predicate;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.etheller.interpreter.ast.debug.JassException;
import com.etheller.interpreter.ast.execution.JassThread;
import com.etheller.interpreter.ast.function.JassFunction;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.scope.trigger.RemovableTriggerEvent;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.scope.trigger.TriggerBooleanExpression;
import com.etheller.interpreter.ast.scope.trigger.TriggerIntegerExpression;
import com.etheller.interpreter.ast.scope.variableevent.CLimitOp;
import com.etheller.interpreter.ast.util.CHandle;
import com.etheller.interpreter.ast.util.JassLog;
import com.etheller.interpreter.ast.util.JassProgram;
import com.etheller.interpreter.ast.util.JassSettings;
import com.etheller.interpreter.ast.value.BooleanJassValue;
import com.etheller.interpreter.ast.value.CodeJassValue;
import com.etheller.interpreter.ast.value.HandleJassType;
import com.etheller.interpreter.ast.value.HandleJassTypeConstructor;
import com.etheller.interpreter.ast.value.HandleJassValue;
import com.etheller.interpreter.ast.value.IntegerJassValue;
import com.etheller.interpreter.ast.value.JassType;
import com.etheller.interpreter.ast.value.JassValue;
import com.etheller.interpreter.ast.value.JassValueVisitor;
import com.etheller.interpreter.ast.value.PrimitiveJassType;
import com.etheller.interpreter.ast.value.RealJassValue;
import com.etheller.interpreter.ast.value.StaticStructTypeJassValue;
import com.etheller.interpreter.ast.value.StringJassValue;
import com.etheller.interpreter.ast.value.UnknownEnumHandle;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.CodeJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.JassTypeGettingValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.RealJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.StaticStructTypeJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.StringJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.WrappedStringJassValueVisitor;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.parsers.fdf.GameSkin;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.parsers.fdf.datamodel.AnchorDefinition;
import com.etheller.warsmash.parsers.fdf.datamodel.FramePoint;
import com.etheller.warsmash.parsers.fdf.frames.SetPoint;
import com.etheller.warsmash.parsers.fdf.frames.StringFrame;
import com.etheller.warsmash.parsers.fdf.frames.UIFrame;
import com.etheller.warsmash.parsers.jass.scope.CommonTriggerExecutionScope;
import com.etheller.warsmash.parsers.jass.triggers.BoolExprAnd;
import com.etheller.warsmash.parsers.jass.triggers.BoolExprCondition;
import com.etheller.warsmash.parsers.jass.triggers.BoolExprFilter;
import com.etheller.warsmash.parsers.jass.triggers.BoolExprNot;
import com.etheller.warsmash.parsers.jass.triggers.BoolExprOr;
import com.etheller.warsmash.parsers.jass.triggers.EnumSetHandle;
import com.etheller.warsmash.parsers.jass.triggers.HandleList;
import com.etheller.warsmash.parsers.jass.triggers.IntExpr;
import com.etheller.warsmash.parsers.jass.triggers.LocationJass;
import com.etheller.warsmash.parsers.jass.triggers.StringList;
import com.etheller.warsmash.parsers.jass.triggers.TriggerAction;
import com.etheller.warsmash.parsers.jass.triggers.TriggerCondition;
import com.etheller.warsmash.parsers.jass.triggers.UnitGroup;
import com.etheller.warsmash.units.Element;
import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.units.manager.MutableObjectData.WorldEditorDataType;
import com.etheller.warsmash.util.RawcodeUtils;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.Bounds;
import com.etheller.warsmash.viewer5.Scene;
import com.etheller.warsmash.viewer5.handlers.mdx.Sequence;
import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens.PrimaryTag;
import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens.SecondaryTag;
import com.etheller.warsmash.viewer5.handlers.w3x.TextTag;
import com.etheller.warsmash.viewer5.handlers.w3x.UnitSound;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.camera.CustomCameraSetup;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.ScriptEffect;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.ability.AbilityDataUI;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.ability.IconUI;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.ability.ItemUI;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.ability.OrderButtonUI;
import com.etheller.warsmash.viewer5.handlers.w3x.save.SavedGame;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructableType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItemType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitClassification;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitEnumFunction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUpgradeType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.HandleIdAllocator;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.build.AbstractCAbilityBuild;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityDisableType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.COrderButton;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.COrderButton.JassOrderButtonType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.GetAbilityByRawcodeVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.AutocastType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.CBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.CDestructableBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.CLevelingAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.GenericSingleIconActiveAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.harvest.CAbilityHarvest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.hero.CAbilityHero;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.inventory.CAbilityInventory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.shop.CAbilitySellItems;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.shop.CAbilitySellUnits;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.jass.CAbilityJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.jass.CAbilityOrderButtonJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.jass.CBuffJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.mine.CAbilityBlightedGoldMine;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.neutral.CAbilityWayGate;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.test.CAbilityCarrionSwarmDummy;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilitySpell;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.units.CBuffEnsnare;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffStun;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimedLife;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitorJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.CAbilityType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.jass.CAbilityTypeJassDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.jass.CodeJassValueBehaviorExpr;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.ability.AbilityBuilderAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.ability.AbilityBuilderActiveAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.ability.GetABAbilityByRawcodeVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff.ABDestructableBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff.ABPermanentPassiveBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff.ABTargetingBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff.ABTimedArtBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff.ABTimedBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff.ABTimedTargetingBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff.ABTimedTickingBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff.ABTimedTickingPausedBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff.ABTimedTickingPostDeathBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.ABAction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.event.ABTimeOfDayEvent;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.jass.ABActionJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.jass.ABConditionJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.parser.AbilityBuilderConfiguration;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.parser.AbilityBuilderDupe;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.parser.AbilityBuilderParser;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.parser.AbilityBuilderType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.parser.template.DataFieldLetter;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.projectile.ABCollisionProjectileListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.projectile.ABProjectileListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.timer.ABTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.types.impl.CAbilityTypeAbilityBuilderLevelData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.ai.AIDifficulty;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.BehaviorTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorAttackListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CGroupMove;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.jass.CAbstractRangedBehaviorJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.jass.CBehaviorJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.jass.CRangedBehaviorJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CGenericDamageFlags;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAbilityProjectileListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAttackProjectile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CProjectile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.CBasePlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.CPlayerAPI;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.War3MapConfig;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.item.CItemTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIdUtils;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CMapControl;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CMapFlag;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CMapPlacement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerColor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerGameResult;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerScore;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CRace;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CRacePreference;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CStartLocPrio;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision.CCircleFogModifier;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision.CCrippledPlayerFogModifier;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision.CFogModifierJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision.CFogModifierJassMulti;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision.CFogModifierJassSingle;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision.CRectFogModifier;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CDefeatCondition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuestItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.region.CRegion;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.region.CRegionTriggerEnter;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.region.CRegionTriggerLeave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound.CMIDISound;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound.CSound;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound.CSoundFilename;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound.CSoundFromLabel;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.state.CGameState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.state.CUnitState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimerExpireTriggerEvent;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimerJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimerJassBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimerNativeEvent;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimerSleepAction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.JassGameEventsWar3;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CAttackTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CBlendMode;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CCameraField;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CFogState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CGameSpeed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CGameType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CMapDensity;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CMapDifficulty;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CPathingTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CPlayerSlotState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CRarityControl;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CSoundType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CSoundVolumeGroup;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CTexMapFlags;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CVersion;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CWeaponSoundTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.CUnitTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CHashtable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CWidgetAbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.ExternStringMsgTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.PointAbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.ResourceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentLightning;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentLightningMovable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.TextTagConfigType;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.WarsmashUI;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog.CScriptDialog;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog.CScriptDialogButton;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog.CTimerDialog;

import net.warsmash.parsers.jass.SmashJassParser;

public class Jass2 {
	public static final boolean REPORT_SYNTAX_ERRORS = true;

	private static final java.util.Set<String> REPORTED_MISSING_DESTRUCTORS = java.util.concurrent.ConcurrentHashMap
			.newKeySet();

	// Campaign scripts call these natives tens of thousands of times per map.
	private static void reportMissingDestructorOnce(final String nativeName) {
		if (REPORTED_MISSING_DESTRUCTORS.add(nativeName)) {
			System.err.println(nativeName
					+ " called but in Java we don't have a destructor, so we need to unregister later when that is implemented (reported once)");
		}
	}

	public static CommonEnvironment loadCommon(final DataSource dataSource, final Viewport uiViewport,
			final Scene uiScene, final War3MapViewer war3MapViewer, final WarsmashUI meleeUI, final String... files) {

		final JassProgram jassProgramVisitor = new JassProgram();
		final CommonEnvironment environment = new CommonEnvironment(jassProgramVisitor, dataSource, uiViewport, uiScene,
				war3MapViewer, meleeUI, files);
		for (final String file : files) {
			String jassFilePath = file;
			if (!dataSource.has(jassFilePath)) {
				jassFilePath = jassFilePath
						.substring(Math.max(jassFilePath.lastIndexOf('/'), jassFilePath.lastIndexOf('\\')) + 1);
			}
			if (!dataSource.has(jassFilePath)) {
				final String lowerCaseDirectoryPath = file.toLowerCase(Locale.US);
				final String lowerCaseDirectoryPathLinux = file.toLowerCase(Locale.US).replace("\\", "/");

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
							readJassFile(dataSource, jassProgramVisitor, realPath);
						}
						catch (final Exception e) {
							e.printStackTrace();
							JassLog.report(e);
						}
					}
				}
			}
			else {
				readJassFile(dataSource, jassProgramVisitor, jassFilePath);
			}
		}
		try {
			jassProgramVisitor.initialize();
		}
		catch (final Exception e) {
			e.printStackTrace();
			JassLog.report(e);
		}
		jassProgramVisitor.getJassNativeManager().checkUnregisteredNatives();
		return environment;
	}

	private static void readJassFile(final DataSource dataSource, final JassProgram jassProgramVisitor,
			final String jassFilePath) {
		final String jassFile = jassFilePath;
		try {
			try (InputStreamReader reader = new InputStreamReader(dataSource.getResourceAsStream(jassFile))) {
				final SmashJassParser smashJassParser = new SmashJassParser(reader);
				smashJassParser.scanAndParse(jassFile, jassProgramVisitor);
			}
		}
		catch (final Exception e) {
			e.printStackTrace();
			JassLog.report(e);
		}
	}

	public static ConfigEnvironment loadConfig(final DataSource dataSource, final Viewport uiViewport,
			final Scene uiScene, final GameUI gameUI, final War3MapConfig mapConfig, final String... files) {
		mapConfig.selectGameType(CGameType.MELEE);
		final JassProgram jassProgramVisitor = new JassProgram();
		final ConfigEnvironment environment = new ConfigEnvironment(jassProgramVisitor, dataSource, uiViewport, uiScene,
				gameUI, mapConfig);
		for (final String file : files) {
			String jassFilePath = file;
			if (!dataSource.has(jassFilePath)) {
				jassFilePath = jassFilePath
						.substring(Math.max(jassFilePath.lastIndexOf('/'), jassFilePath.lastIndexOf('\\')) + 1);
			}
			if (!dataSource.has(jassFilePath)) {
				final String lowerCaseDirectoryPath = file.toLowerCase(Locale.US);
				final String lowerCaseDirectoryPathLinux = file.toLowerCase(Locale.US).replace("\\", "/");

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
							readJassFile(dataSource, jassProgramVisitor, realPath);
						}
						catch (final Exception e) {
							e.printStackTrace();
							JassLog.report(e);
						}
					}
				}
			}
			else {
				readJassFile(dataSource, jassProgramVisitor, jassFilePath);
			}
		}
		try {
			jassProgramVisitor.initialize();
		}
		catch (final Exception e) {
			e.printStackTrace();
			JassLog.report(e);
		}
		jassProgramVisitor.getJassNativeManager().checkUnregisteredNatives();
		return environment;
	}

	public static JUIEnvironment loadJUI(final DataSource dataSource, final Viewport uiViewport, final Scene uiScene,
			final War3MapViewer war3MapViewer, final RootFrameListener rootFrameListener, final String... files) {

		final JassProgram jassProgramVisitor = new JassProgram();
		final JUIEnvironment environment = new JUIEnvironment(jassProgramVisitor, dataSource, uiViewport, uiScene,
				war3MapViewer, rootFrameListener);
		for (final String jassFile : files) {
			try {
				try (InputStreamReader reader = new InputStreamReader(dataSource.getResourceAsStream(jassFile))) {
					final SmashJassParser smashJassParser = new SmashJassParser(reader);
					smashJassParser.scanAndParse(jassFile, jassProgramVisitor);
				}
			}
			catch (final Exception e) {
				e.printStackTrace();
			}
		}
		try {
			jassProgramVisitor.initialize();
		}
		catch (final Exception e) {
			e.printStackTrace();
			JassLog.report(e);
		}
		jassProgramVisitor.getJassNativeManager().checkUnregisteredNatives();
		return environment;
	}

	public static interface RootFrameListener {
		void onCreate(GameUI rootFrame);
	}

	private static final class JUIEnvironment {
		private GameUI gameUI;
		private Element skin;

		public JUIEnvironment(final JassProgram jassProgramVisitor, final DataSource dataSource,
				final Viewport uiViewport, final Scene uiScene, final War3MapViewer war3MapViewer,
				final RootFrameListener rootFrameListener) {
			final GlobalScope globals = jassProgramVisitor.getGlobalScope();
			final HandleJassType frameHandleType = globals.registerHandleType("framehandle");
			final HandleJassType framePointType = globals.registerHandleType("framepointtype");
			final HandleJassType triggerType = globals.registerHandleType("trigger");
			final HandleJassType triggerActionType = globals.registerHandleType("triggeraction");
			final HandleJassType triggerConditionType = globals.registerHandleType("triggercondition");
			final HandleJassType boolExprType = globals.registerHandleType("boolexpr");
			final HandleJassType conditionFuncType = globals.registerHandleType("conditionfunc");
			final HandleJassType filterType = globals.registerHandleType("filterfunc");
			final HandleJassType eventidType = globals.registerHandleType("eventid");
			jassProgramVisitor.getJassNativeManager().createNative("LogError",
					(arguments, globalScope, triggerScope) -> {
						final String stringValue = arguments.get(0).visit(StringJassValueVisitor.getInstance());
						System.err.println(stringValue);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("ConvertFramePointType",
					(arguments, globalScope, triggerScope) -> {
						final int value = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						return enumValue(framePointType, FramePoint.values(), value);
					});
			jassProgramVisitor.getJassNativeManager().createNative("CreateRootFrame",
					(arguments, globalScope, triggerScope) -> {
						final String skinArg = arguments.get(0).visit(StringJassValueVisitor.getInstance());
						final GameSkin skin = GameUI.loadSkin(dataSource, skinArg);
						final GameUI gameUI = new GameUI(dataSource, skin, uiViewport, uiScene, war3MapViewer, 0,
								war3MapViewer.getAllObjectData().getWts());
						JUIEnvironment.this.gameUI = gameUI;
						JUIEnvironment.this.skin = skin.getSkin();
						rootFrameListener.onCreate(gameUI);
						return new HandleJassValue(frameHandleType, gameUI);
					});
			jassProgramVisitor.getJassNativeManager().createNative("LoadTOCFile",
					(arguments, globalScope, triggerScope) -> {
						final String tocFileName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
						try {
							JUIEnvironment.this.gameUI.loadTOCFile(tocFileName);
						}
						catch (final IOException e) {
							throw new RuntimeException(e);
						}
						return BooleanJassValue.TRUE;
					});
			jassProgramVisitor.getJassNativeManager().createNative("CreateSimpleFrame",
					(arguments, globalScope, triggerScope) -> {
						final String templateName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
						final UIFrame ownerFrame = arguments.get(1)
								.visit(ObjectJassValueVisitor.<UIFrame>getInstance());
						final int createContext = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						final UIFrame simpleFrame = JUIEnvironment.this.gameUI.createSimpleFrame(templateName,
								ownerFrame, createContext);
						return new HandleJassValue(frameHandleType, simpleFrame);
					});
			jassProgramVisitor.getJassNativeManager().createNative("CreateFrame",
					(arguments, globalScope, triggerScope) -> {
						final String templateName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
						final UIFrame ownerFrame = arguments.get(1)
								.visit(ObjectJassValueVisitor.<UIFrame>getInstance());
						final int priority = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						final int createContext = arguments.get(3).visit(IntegerJassValueVisitor.getInstance());
						final UIFrame simpleFrame = JUIEnvironment.this.gameUI.createFrame(templateName, ownerFrame,
								priority, createContext);
						return new HandleJassValue(frameHandleType, simpleFrame);
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetFrameByName",
					(arguments, globalScope, triggerScope) -> {
						final String templateName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
						final int createContext = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final UIFrame simpleFrame = JUIEnvironment.this.gameUI.getFrameByName(templateName,
								createContext);
						return new HandleJassValue(frameHandleType, simpleFrame);
					});
			jassProgramVisitor.getJassNativeManager().createNative("FrameSetAnchor",
					(arguments, globalScope, triggerScope) -> {
						final UIFrame frame = arguments.get(0).visit(ObjectJassValueVisitor.<UIFrame>getInstance());
						final FramePoint framePoint = arguments.get(1)
								.visit(ObjectJassValueVisitor.<FramePoint>getInstance());
						final double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
						final double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());

						frame.addAnchor(new AnchorDefinition(framePoint, GameUI.convertX(uiViewport, (float) x),
								GameUI.convertY(uiViewport, (float) y)));
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("FrameSetAbsPoint",
					(arguments, globalScope, triggerScope) -> {
						final UIFrame frame = arguments.get(0).visit(ObjectJassValueVisitor.<UIFrame>getInstance());
						final FramePoint framePoint = arguments.get(1)
								.visit(ObjectJassValueVisitor.<FramePoint>getInstance());
						final double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
						final double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());
						frame.setFramePointX(framePoint, GameUI.convertX(uiViewport, (float) x));
						frame.setFramePointY(framePoint, GameUI.convertY(uiViewport, (float) y));
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("FrameSetPoint",
					(arguments, globalScope, triggerScope) -> {
						final UIFrame frame = arguments.get(0).visit(ObjectJassValueVisitor.<UIFrame>getInstance());
						final FramePoint framePoint = arguments.get(1)
								.visit(ObjectJassValueVisitor.<FramePoint>getInstance());
						final UIFrame otherFrame = arguments.get(2)
								.visit(ObjectJassValueVisitor.<UIFrame>getInstance());
						final FramePoint otherPoint = arguments.get(3)
								.visit(ObjectJassValueVisitor.<FramePoint>getInstance());
						final double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
						final double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());

						frame.addSetPoint(new SetPoint(framePoint, otherFrame, otherPoint,
								GameUI.convertX(uiViewport, (float) x), GameUI.convertY(uiViewport, (float) y)));
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("FrameSetText",
					(arguments, globalScope, triggerScope) -> {
						final StringFrame frame = arguments.get(0)
								.visit(ObjectJassValueVisitor.<StringFrame>getInstance());
						final String text = arguments.get(1).visit(StringJassValueVisitor.getInstance());

						JUIEnvironment.this.gameUI.setText(frame, text);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("FrameSetTextColor",
					(arguments, globalScope, triggerScope) -> {
						final StringFrame frame = arguments.get(0)
								.visit(ObjectJassValueVisitor.<StringFrame>getInstance());
						final int colorInt = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						frame.setColor(new Color(colorInt));
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("ConvertColor",
					(arguments, globalScope, triggerScope) -> {
						final int a = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final int r = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final int g = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						final int b = arguments.get(3).visit(IntegerJassValueVisitor.getInstance());
						return IntegerJassValue.of(a | (b << 8) | (g << 16) | (r << 24));
					});
			jassProgramVisitor.getJassNativeManager().createNative("FramePositionBounds",
					(arguments, globalScope, triggerScope) -> {
						final UIFrame frame = arguments.get(0).visit(ObjectJassValueVisitor.<UIFrame>getInstance());
						frame.positionBounds(JUIEnvironment.this.gameUI, uiViewport);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SkinGetField",
					(arguments, globalScope, triggerScope) -> {
						final String fieldName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
						return new StringJassValue(JUIEnvironment.this.skin.getField(fieldName));
					});
			setupTriggerAPI(jassProgramVisitor, triggerType, triggerActionType, triggerConditionType, boolExprType,
					conditionFuncType, filterType, eventidType);
		}
	}

	public static final class CommonEnvironment {
		private static final War3ID SELL_ITEMS_ABILITY_ID = War3ID.fromString("Asid");
		private static final War3ID SELL_UNITS_ABILITY_ID = War3ID.fromString("Asud");

		private GameUI gameUI;
		private Element skin;
		private final JassProgram jassProgramVisitor;
		private CSimulation simulation;
		private List<String> preloadGenLines;
		private boolean cameraAllowsHotkeyTargetLock = true;
		private final SavedGame.AiScriptStarter aiScriptStarter;
		private final Map<String, Set<War3ID>> typeAndUpgradesByLegacyName = new HashMap<>();
		private final Map<Long, CCrippledPlayerFogModifier> crippleModifiers = new HashMap<>();

		/**
		 * CripplePlayer: with the flag the player's units are shown to every player in the force; without it they are
		 * hidden from those players again, or from everyone when the force is empty (Blizzard.j resets that way).
		 */
		private void crippleExpose(final int crippledIndex, final List<CPlayerJass> toWhichPlayers,
				final boolean flag) {
			if (!flag && ((toWhichPlayers == null) || toWhichPlayers.isEmpty())) {
				for (int i = 0; i < WarsmashConstants.MAX_PLAYERS; i++) {
					setCrippleExposed(crippledIndex, i, false);
				}
				return;
			}
			if (toWhichPlayers != null) {
				for (final CPlayerJass viewer : toWhichPlayers) {
					setCrippleExposed(crippledIndex, viewer.getId(), flag);
				}
			}
		}

		private void setCrippleExposed(final int crippledIndex, final int viewerIndex, final boolean exposed) {
			final long key = ((long) crippledIndex << 32) | viewerIndex;
			final CCrippledPlayerFogModifier current = this.crippleModifiers.get(key);
			if (exposed && (current == null)) {
				final CCrippledPlayerFogModifier modifier = new CCrippledPlayerFogModifier(crippledIndex);
				this.crippleModifiers.put(key, modifier);
				this.simulation.getPlayer(viewerIndex).addFogModifer(this.simulation, modifier, false);
			}
			else if (!exposed && (current != null)) {
				this.crippleModifiers.remove(key);
				this.simulation.getPlayer(viewerIndex).removeFogModifer(this.simulation, current);
			}
		}

		/**
		 * The unit types named by a legacy name plus every type they upgrade into, so that a Keep counts as a
		 * "townhall" when GetPlayerTypedUnitCount includes upgrades (the melee town hall check).
		 */
		private Set<War3ID> typeAndUpgradesForLegacyName(final String legacyName) {
			return this.typeAndUpgradesByLegacyName.computeIfAbsent(legacyName, (name) -> {
				final CUnitData unitData = this.simulation.getUnitData();
				final Set<War3ID> result = new HashSet<>();
				final List<War3ID> open = new ArrayList<>();
				for (final CUnitType type : unitData.getAllUnitTypesLightweight()) {
					if (name.equals(type.getLegacyName())) {
						open.add(type.getTypeId());
					}
				}
				while (!open.isEmpty()) {
					final War3ID typeId = open.remove(open.size() - 1);
					if (result.add(typeId)) {
						final CUnitType type = unitData.getUnitType(typeId);
						if (type != null) {
							open.addAll(type.getUpgradesTo());
						}
					}
				}
				return result;
			});
		}

		private CommonEnvironment(final JassProgram jassProgramVisitor, final DataSource dataSource,
				final Viewport uiViewport, final Scene uiScene, final War3MapViewer war3MapViewer,
				final WarsmashUI meleeUI, final String[] originalFiles) {
			this.jassProgramVisitor = jassProgramVisitor;
			this.gameUI = war3MapViewer.getGameUI();
			final Rectangle tempRect = new Rectangle();
			this.simulation = war3MapViewer.simulation;
			war3MapViewer.setScriptEnvironment(this, uiScene.audioContext);
			this.aiScriptStarter = entry -> {
				if (entry.playerIndex < 0) {
					doPreloadScript(dataSource, uiViewport, uiScene, war3MapViewer, entry.scriptFile, meleeUI,
							originalFiles, jassProgramVisitor, entry.mainFunction, false);
				}
				else {
					doAIScript(dataSource, uiViewport, uiScene, war3MapViewer,
							this.simulation.getPlayer(entry.playerIndex), entry.scriptFile, meleeUI, originalFiles,
							jassProgramVisitor, this.simulation, entry.mainFunction, false);
				}
			};
			final GlobalScope globals = jassProgramVisitor.getGlobalScope();
			final HandleJassType handleType = globals.registerHandleType("handle");
			final HandleJassType agentType = globals.registerHandleType("agent");
			final HandleJassType eventType = globals.registerHandleType("event");
			final HandleJassType playerType = globals.registerHandleType("player");
			final HandleJassType widgetType = globals.registerHandleType("widget");
			final HandleJassType unitType = globals.registerHandleType("unit");
			final HandleJassType destructableType = globals.registerHandleType("destructable");
			final HandleJassType itemType = globals.registerHandleType("item");
			final HandleJassType abilityType = globals.registerHandleType("ability");
			final HandleJassType buffType = globals.registerHandleType("buff");
			final HandleJassType forceType = globals.registerHandleType("force");
			final HandleJassType groupType = globals.registerHandleType("group");
			final HandleJassType triggerType = globals.registerHandleType("trigger");
			final HandleJassType triggerconditionType = globals.registerHandleType("triggercondition");
			final HandleJassType triggeractionType = globals.registerHandleType("triggeraction");
			final HandleJassType timerType = globals.registerHandleType("timer");
			final HandleJassType locationType = globals.registerHandleType("location");
			final HandleJassType regionType = globals.registerHandleType("region");
			final HandleJassType rectType = globals.registerHandleType("rect");
			final HandleJassType boolexprType = globals.registerHandleType("boolexpr");
			final HandleJassType soundType = globals.registerHandleType("sound");
			final HandleJassType conditionfuncType = globals.registerHandleType("conditionfunc");
			final HandleJassType filterfuncType = globals.registerHandleType("filterfunc");
			final HandleJassType unitpoolType = globals.registerHandleType("unitpool");
			final HandleJassType itempoolType = globals.registerHandleType("itempool");
			final HandleJassType raceType = globals.registerHandleType("race");
			final HandleJassType alliancetypeType = globals.registerHandleType("alliancetype");
			final HandleJassType racepreferenceType = globals.registerHandleType("racepreference");
			final HandleJassType gamestateType = globals.registerHandleType("gamestate");
			final HandleJassType igamestateType = globals.registerHandleType("igamestate");
			final HandleJassType fgamestateType = globals.registerHandleType("fgamestate");
			final HandleJassType playerstateType = globals.registerHandleType("playerstate");
			final HandleJassType playerscoreType = globals.registerHandleType("playerscore");
			final HandleJassType playergameresultType = globals.registerHandleType("playergameresult");
			final HandleJassType unitstateType = globals.registerHandleType("unitstate");
			final HandleJassType aidifficultyType = globals.registerHandleType("aidifficulty");
			final HandleJassType eventidType = globals.registerHandleType("eventid");
			final HandleJassType gameeventType = globals.registerHandleType("gameevent");
			final HandleJassType playereventType = globals.registerHandleType("playerevent");
			final HandleJassType playeruniteventType = globals.registerHandleType("playerunitevent");
			final HandleJassType uniteventType = globals.registerHandleType("unitevent");
			final HandleJassType limitopType = globals.registerHandleType("limitop");
			final HandleJassType widgeteventType = globals.registerHandleType("widgetevent");
			final HandleJassType dialogeventType = globals.registerHandleType("dialogevent");
			final HandleJassType unittypeType = globals.registerHandleType("unittype");
			final HandleJassType gamespeedType = globals.registerHandleType("gamespeed");
			final HandleJassType gamedifficultyType = globals.registerHandleType("gamedifficulty");
			final HandleJassType gametypeType = globals.registerHandleType("gametype");
			final HandleJassType mapflagType = globals.registerHandleType("mapflag");
			final HandleJassType mapvisibilityType = globals.registerHandleType("mapvisibility");
			final HandleJassType mapsettingType = globals.registerHandleType("mapsetting");
			final HandleJassType mapdensityType = globals.registerHandleType("mapdensity");
			final HandleJassType mapcontrolType = globals.registerHandleType("mapcontrol");
			final HandleJassType playerslotstateType = globals.registerHandleType("playerslotstate");
			final HandleJassType volumegroupType = globals.registerHandleType("volumegroup");
			final HandleJassType camerafieldType = globals.registerHandleType("camerafield");
			final HandleJassType camerasetupType = globals.registerHandleType("camerasetup");
			final HandleJassType playercolorType = globals.registerHandleType("playercolor");
			final HandleJassType placementType = globals.registerHandleType("placement");
			final HandleJassType startlocprioType = globals.registerHandleType("startlocprio");
			final HandleJassType raritycontrolType = globals.registerHandleType("raritycontrol");
			final HandleJassType blendmodeType = globals.registerHandleType("blendmode");
			final HandleJassType texmapflagsType = globals.registerHandleType("texmapflags");
			final HandleJassType effectType = globals.registerHandleType("effect");
			final HandleJassType effecttypeType = globals.registerHandleType("effecttype");
			final HandleJassType weathereffectType = globals.registerHandleType("weathereffect");
			final HandleJassType terraindeformationType = globals.registerHandleType("terraindeformation");
			final HandleJassType fogstateType = globals.registerHandleType("fogstate");
			final HandleJassType fogmodifierType = globals.registerHandleType("fogmodifier");
			final HandleJassType dialogType = globals.registerHandleType("dialog");
			final HandleJassType buttonType = globals.registerHandleType("button");
			final HandleJassType questType = globals.registerHandleType("quest");
			final HandleJassType questitemType = globals.registerHandleType("questitem");
			final HandleJassType defeatconditionType = globals.registerHandleType("defeatcondition");
			final HandleJassType timerdialogType = globals.registerHandleType("timerdialog");
			final HandleJassType leaderboardType = globals.registerHandleType("leaderboard");
			final HandleJassType multiboardType = globals.registerHandleType("multiboard");
			final HandleJassType multiboarditemType = globals.registerHandleType("multiboarditem");
			final HandleJassType trackableType = globals.registerHandleType("trackable");
			final HandleJassType gamecacheType = globals.registerHandleType("gamecache");
			final HandleJassType versionType = globals.registerHandleType("version");
			final HandleJassType itemtypeType = globals.registerHandleType("itemtype");
			final HandleJassType texttagType = globals.registerHandleType("texttag");
			final HandleJassType attacktypeType = globals.registerHandleType("attacktype");
			final HandleJassType damagetypeType = globals.registerHandleType("damagetype");
			final HandleJassType weapontypeType = globals.registerHandleType("weapontype");
			final HandleJassType soundtypeType = globals.registerHandleType("soundtype");
			final HandleJassType lightningType = globals.registerHandleType("lightning");
			final HandleJassType pathingtypeType = globals.registerHandleType("pathingtype");
			final HandleJassType imageType = globals.registerHandleType("image");
			final HandleJassType ubersplatType = globals.registerHandleType("ubersplat");
			final HandleJassType hashtableType = globals.registerHandleType("hashtable");
			final HandleJassType frameHandleType = globals.registerHandleType("framehandle");
			final HandleJassType minimapiconType = globals.registerHandleType("minimapicon");

			// Warsmash Ability API
			final HandleJassType abilitytypeType = globals.registerHandleType("abilitytype");
			final HandleJassType orderbuttonType = globals.registerHandleType("orderbutton");
			final HandleJassType orderbuttontypeType = globals.registerHandleType("orderbuttontype");
			final HandleJassType abilitybehaviorType = globals.registerHandleType("behavior");
			final HandleJassType behaviorexprType = globals.registerHandleType("behaviorexpr");
			final HandleJassType iconuiType = globals.registerHandleType("iconui");

			// Warsmash Ability API 2 "Ability Builder ported to Jass"
			final HandleJassType abilitytypeleveldataType = globals.registerHandleType("abilitytypeleveldata");
			final HandleJassType targettypeType = globals.registerHandleType("targettype");
			final HandleJassType targettypesType = globals.registerHandleType("targettypes");
			final HandleJassType texttagconfigtypeType = globals.registerHandleType("texttagconfigtype");
			final HandleJassType activeabilityType = globals.registerHandleType("activeability");
			final HandleJassType localstoreType = globals.registerHandleType("localstore");
			final HandleJassType destructablebuffType = globals.registerHandleType("destructablebuff");
			final HandleJassType abtimeofdayeventType = globals.registerHandleType("abtimeofdayevent");
			final HandleJassType worldeditordatatypeType = globals.registerHandleType("worldeditordatatype");
			final HandleJassType gameobjectType = globals.registerHandleType("gameobject");
			final HandleJassType projectileType = globals.registerHandleType("projectile");
			final HandleJassType abilityprojectileType = globals.registerHandleType("abilityprojectile");
			final HandleJassType nonstackingstatbuffType = globals.registerHandleType("nonstackingstatbonus");
			final HandleJassType nonstackingstatbufftypeType = globals.registerHandleType("nonstackingstatbonustype");
			final HandleJassType statemodType = globals.registerHandleType("statemod");
			final HandleJassType statemodtypeType = globals.registerHandleType("statemodtype");
			final HandleJassType datafieldletterType = globals.registerHandleType("datafieldletter");
			final HandleJassType autocasttypeType = globals.registerHandleType("autocasttype");
			final HandleJassType abconftypeType = globals.registerHandleType("abconftype");
			final HandleJassType abilitybuilderconfigurationType = globals
					.registerHandleType("abilitybuilderconfiguration");
			final HandleJassType abtimerType = globals.registerHandleType("abtimer");
			final HandleJassType abilitydisabletypeType = globals.registerHandleType("abilitydisabletype");
			final HandleJassType resourcetypeType = globals.registerHandleType("resourcetype");
			final HandleJassType intexprType = globals.registerHandleType("intexpr");

			// Warsmash Ability API 3
			final HandleJassType handlelistType = globals.registerHandleType("handlelist");
			final HandleJassType behaviorcategoryType = globals.registerHandleType("behaviorcategory");
			final HandleJassType abilitycategoryType = globals.registerHandleType("abilitycategory");
			final HandleJassType stringlistType = globals.registerHandleType("stringlist");
			final HandleJassType abilitytargetType = globals.registerHandleType("abilitytarget");
			final HandleJassType abilitytargetvisitorType = globals.registerHandleType("abilitytargetvisitor");

			final HandleJassType primarytagType = globals.registerHandleType("primarytag");
			final HandleJassType primarytagsType = globals.registerHandleType("primarytags");
			final HandleJassType secondarytagType = globals.registerHandleType("secondarytag");
			final HandleJassType secondarytagsType = globals.registerHandleType("secondarytags");

			final HandleJassType rangedbehaviorType = globals.registerHandleType("rangedbehavior");
			final HandleJassType abstractrangedbehaviorType = globals.registerHandleType("abstractrangedbehavior");

			registerTypingNatives(jassProgramVisitor, raceType, alliancetypeType, racepreferenceType, igamestateType,
					fgamestateType, playerstateType, playerscoreType, playergameresultType, unitstateType,
					aidifficultyType, gameeventType, playereventType, playeruniteventType, uniteventType, limitopType,
					widgeteventType, dialogeventType, unittypeType, gamespeedType, gamedifficultyType, gametypeType,
					mapflagType, mapvisibilityType, mapsettingType, mapdensityType, mapcontrolType, playerslotstateType,
					volumegroupType, camerafieldType, playercolorType, placementType, startlocprioType,
					raritycontrolType, blendmodeType, texmapflagsType, effecttypeType, fogstateType, versionType,
					itemtypeType, attacktypeType, damagetypeType, weapontypeType, soundtypeType, pathingtypeType);

			jassProgramVisitor.getJassNativeManager().createNative("ConvertOrderButtonType",
					(arguments, globalScope, triggerScope) -> {
						final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						return enumValue(orderbuttontypeType, COrderButton.JassOrderButtonType.VALUES, i);
					});

			jassProgramVisitor.getJassNativeManager().createNative("UnitId", (arguments, globalScope, triggerScope) -> {
				final String idString = arguments.get(0).visit(StringJassValueVisitor.getInstance());
				final CUnitType unitTypeTmp = CommonEnvironment.this.simulation.getUnitData()
						.getUnitTypeByJassLegacyName(idString);
				return IntegerJassValue.of((unitTypeTmp == null) ? 0 : unitTypeTmp.getTypeId().getValue());
			});
			jassProgramVisitor.getJassNativeManager().createNative("UnitId2String",
					(arguments, globalScope, triggerScope) -> {
						final Integer id = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final War3ID war3id = new War3ID(id);
						final CUnitType unitTypeTmp = CommonEnvironment.this.simulation.getUnitData()
								.getUnitType(war3id);
						return unitTypeTmp == null ? JassType.STRING.getNullValue()
								: new StringJassValue(unitTypeTmp.getLegacyName());
					});
			jassProgramVisitor.getJassNativeManager().createNative("AbilityId",
					(arguments, globalScope, triggerScope) -> {
						return IntegerJassValue.of(0);
					});
			jassProgramVisitor.getJassNativeManager().createNative("AbilityId2String",
					(arguments, globalScope, triggerScope) -> {
						return new StringJassValue("");
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetObjectName",
					(arguments, globalScope, triggerScope) -> {
						final Integer id = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final War3ID war3id = new War3ID(id);
						final CUnitType unitTypeTmp = CommonEnvironment.this.simulation.getUnitData()
								.getUnitType(war3id);
						if (unitTypeTmp != null) {
							return new StringJassValue(unitTypeTmp.getName());
						}
						// TODO for now this looks in the ability editor data, not the fast symbol table
						// layer on top, because the layer on top forgot to have a name value...
						final GameObject abilityEditorData = war3MapViewer.getAllObjectData().getAbilities()
								.get(war3id);
						if (abilityEditorData != null) {
							return new StringJassValue(abilityEditorData.getName());
						}
						final ItemUI itemUI = war3MapViewer.getAbilityDataUI().getItemUI(war3id);
						if (itemUI != null) {
							return new StringJassValue(itemUI.getName());
						}
						final CDestructableType destructableTypeTmp = CommonEnvironment.this.simulation
								.getDestructableData().getUnitType(war3id);
						if (destructableTypeTmp != null) {
							return new StringJassValue(destructableTypeTmp.getName());
						}
						return new StringJassValue("");
					});

			jassProgramVisitor.getJassNativeManager().createNative("GetUnitName",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichWidget = nullable(arguments, 0, ObjectJassValueVisitor.<CUnit>getInstance());
						if (whichWidget == null) {
							return JassType.STRING.getNullValue();
						}
						return new StringJassValue(
								whichWidget.isRemovedFromGame() ? "" : whichWidget.getName());
					});
			registerConversionAndStringNatives(jassProgramVisitor, war3MapViewer.getGameUI());
			final War3MapConfig mapConfig = war3MapViewer.getMapConfig();
			registerConfigNatives(jassProgramVisitor, mapConfig, startlocprioType, gametypeType, placementType,
					gamespeedType, gamedifficultyType, mapdensityType, locationType, playerType, playercolorType,
					mapcontrolType, playerslotstateType, this.simulation, this.simulation.getHandleIdAllocator());

			// ============================================================================
			// Timer API
			//
			final JassFunction createTimerFxn = (arguments, globalScope, triggerScope) -> {
				return new HandleJassValue(timerType,
						new CTimerJass(CommonEnvironment.this.simulation.getHandleIdAllocator().createId()));
			};
			jassProgramVisitor.getJassNativeManager().createNative("CreateTimer", createTimerFxn);
			timerType.setConstructorNative(new HandleJassTypeConstructor("CreateTimer"));
			jassProgramVisitor.getJassNativeManager().createNative("DestroyTimer",
					(arguments, globalScope, triggerScope) -> {
						final CTimer timer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (timer != null) {
							timer.kill(CommonEnvironment.this.simulation);
						}
						return null;
					});
			timerType.setDestructorNative(new HandleJassTypeConstructor("DestroyTimer"));
			jassProgramVisitor.getJassNativeManager().createNative("TimerStart",
					(arguments, globalScope, triggerScope) -> {
						final CTimerJassBase timer = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final Double timeout = arguments.get(1).visit(RealJassValueVisitor.getInstance());
						final boolean periodic = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
						final CodeJassValue handlerFunc = nullable(arguments, 3, CodeJassValueVisitor.getInstance());
						if ((timer != null) && timer.isRunning()) {
							timer.pause(this.simulation);
						}
						if ((timer != null)) {
							timer.setTimeoutTime(timeout.floatValue());
							timer.setRepeats(periodic);
							timer.setHandlerFunc(handlerFunc);
							timer.start(CommonEnvironment.this.simulation);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("TimerGetElapsed",
					(arguments, globalScope, triggerScope) -> {
						final CTimer timer = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (timer == null) {
							return RealJassValue.ZERO;
						}
						return RealJassValue.of(timer.getElapsed(CommonEnvironment.this.simulation));
					});
			jassProgramVisitor.getJassNativeManager().createNative("TimerGetRemaining",
					(arguments, globalScope, triggerScope) -> {
						final CTimer timer = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (timer == null) {
							return RealJassValue.ZERO;
						}
						return RealJassValue.of(timer.getRemaining(CommonEnvironment.this.simulation));
					});
			jassProgramVisitor.getJassNativeManager().createNative("TimerGetTimeout",
					(arguments, globalScope, triggerScope) -> {
						final CTimer timer = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (timer == null) {
							return RealJassValue.ZERO;
						}
						return RealJassValue.of(timer.getTimeoutTime());
					});
			jassProgramVisitor.getJassNativeManager().createNative("PauseTimer",
					(arguments, globalScope, triggerScope) -> {
						final CTimer timer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						timer.pause(CommonEnvironment.this.simulation);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("ResumeTimer",
					(arguments, globalScope, triggerScope) -> {
						final CTimer timer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						timer.resume(CommonEnvironment.this.simulation);
						return null;
					});

			// ============================================================================
			// Group API
			//
			jassProgramVisitor.getJassNativeManager().createNative("CreateGroup", new JassFunction() {
				int stupidGroupHandleId = 0x100000;

				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(groupType, new UnitGroup(this.stupidGroupHandleId++));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("DestroyGroup",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
						reportMissingDestructorOnce("DestroyGroup");
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GroupAddUnit",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = nullable(arguments, 0,
								ObjectJassValueVisitor.<List<CUnit>>getInstance());
						final CUnit whichUnit = arguments.get(1).visit(ObjectJassValueVisitor.<CUnit>getInstance());
						if (group != null && whichUnit != null) {
							if (!group.contains(whichUnit)) {
								group.add(whichUnit);
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GroupRemoveUnit",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
						final CUnit whichUnit = arguments.get(1).visit(ObjectJassValueVisitor.<CUnit>getInstance());
						group.remove(whichUnit);
						return null;
					});
			final JassFunction groupAddGroupFast = (arguments, globalScope, triggerScope) -> {
				final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
				final List<CUnit> addGroup = arguments.get(1).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
				// TODO: this is not fast. This is slow. use HashSet<Unit> to be fast.
				for (final CUnit unit : addGroup) {
					if (!group.contains(unit)) {
						group.add(unit);
					}
				}
				return null;
			};
			jassProgramVisitor.getJassNativeManager().createNative("BlzGroupAddGroupFast", groupAddGroupFast);
			jassProgramVisitor.getJassNativeManager().createNative("GroupAddGroupFast", groupAddGroupFast);
			final JassFunction groupRemoveGroupFast = (arguments, globalScope, triggerScope) -> {
				final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
				final List<CUnit> removeGroup = arguments.get(1)
						.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
				group.removeAll(removeGroup);
				return null;
			};
			jassProgramVisitor.getJassNativeManager().createNative("BlzGroupRemoveGroupFast", groupRemoveGroupFast);
			jassProgramVisitor.getJassNativeManager().createNative("GroupRemoveGroupFast", groupRemoveGroupFast);
			jassProgramVisitor.getJassNativeManager().createNative("GroupClear",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
						group.clear();
						return null;
					});
			final JassFunction groupGetSize = (arguments, globalScope, triggerScope) -> {
				final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
				return IntegerJassValue.of(group.size());
			};
			jassProgramVisitor.getJassNativeManager().createNative("BlzGroupGetSize", groupGetSize);
			jassProgramVisitor.getJassNativeManager().createNative("GroupGetSize", groupGetSize);
			final JassFunction groupGetUnitAt = (arguments, globalScope, triggerScope) -> {
				final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
				final int index = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(unitType, group.get(index));
			};
			jassProgramVisitor.getJassNativeManager().createNative("BlzGroupUnitAt", groupGetUnitAt);
			jassProgramVisitor.getJassNativeManager().createNative("GroupUnitAt", groupGetUnitAt);
			jassProgramVisitor.getJassNativeManager().createNative("GroupEnumUnitsOfType",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
						final String unitname = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						final TriggerBooleanExpression filter = nullable(arguments, 2,
								ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
						for (final CUnit unit : CommonEnvironment.this.simulation.getUnitsIncludingNew()) {
							if (unitname.equals(unit.getUnitType().getLegacyName())) {
								if ((filter == null) || filter.evaluate(globalScope,
										CommonTriggerExecutionScope.filterScope(triggerScope, unit))) {
									// TODO the trigger scope for evaluation here might need to be a clean one?
									group.add(unit);
								}
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GroupEnumUnitsOfPlayer",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
						final CPlayerJass player = arguments.get(1)
								.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
						final TriggerBooleanExpression filter = nullable(arguments, 2,
								ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
						if (player == null) {
							return null;
						}
						for (final CUnit unit : CommonEnvironment.this.simulation.getUnitsIncludingNew()) {
							if (unit.getPlayerIndex() == player.getId()) {
								if ((filter == null) || filter.evaluate(globalScope,
										CommonTriggerExecutionScope.filterScope(triggerScope, unit))) {
									// TODO the trigger scope for evaluation here might need to be a clean one?
									group.add(unit);
								}
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("IssuePointOrderLoc",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (whichUnit == null) {
							return BooleanJassValue.FALSE;
						}
						final String orderString = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						final AbilityPointTarget whichLocation = arguments.get(2)
								.visit(ObjectJassValueVisitor.getInstance());
						final CPlayerUnitOrderExecutor defaultPlayerUnitOrderExecutor = CommonEnvironment.this.simulation
								.getDefaultPlayerUnitOrderExecutor(whichUnit.getPlayerIndex());
						final BooleanAbilityActivationReceiver activationReceiver = BooleanAbilityActivationReceiver.INSTANCE.reset();
						final int orderId = OrderIdUtils.getOrderId(orderString);
						int abilityHandleId = 0;
						AbilityPointTarget targetAsPoint = new AbilityPointTarget(whichLocation.x, whichLocation.y);
						for (final CAbility ability : whichUnit.getAbilities()) {
							ability.checkCanUse(CommonEnvironment.this.simulation, whichUnit,
									whichUnit.getPlayerIndex(), orderId, false, activationReceiver);
							if (activationReceiver.isOk()) {
								final PointAbilityTargetCheckReceiver targetReceiver = PointAbilityTargetCheckReceiver.INSTANCE;
								ability.checkCanTarget(CommonEnvironment.this.simulation, whichUnit,
										whichUnit.getPlayerIndex(), orderId, false, targetAsPoint,
										targetReceiver.reset());
								if (targetReceiver.getTarget() != null) {
									targetAsPoint = targetReceiver.getTarget();
									abilityHandleId = ability.getHandleId();
								}
							}
						}
						if (abilityHandleId != 0) {
							defaultPlayerUnitOrderExecutor.issuePointOrder(whichUnit.getHandleId(), abilityHandleId,
									orderId, targetAsPoint.x, targetAsPoint.y, false);
						}
						return BooleanJassValue.of(abilityHandleId != 0);
					});
			final JassFunction issuePointOrderById = (arguments, globalScope, triggerScope) -> {
				final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
				return BooleanJassValue.of((whichUnit != null) && issuePointOrder(whichUnit.getPlayerIndex(), whichUnit,
						arguments.get(1).visit(IntegerJassValueVisitor.getInstance()),
						arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue(),
						arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue()));
			};
			jassProgramVisitor.getJassNativeManager().createNative("IssuePointOrderById", issuePointOrderById);
			// Scripts call these for the local player, usually inside a GetLocalPlayer() check.
			jassProgramVisitor.getJassNativeManager().createNative("SelectUnit",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (whichUnit != null) {
							meleeUI.scriptSelectUnit(whichUnit,
									arguments.get(1).visit(BooleanJassValueVisitor.getInstance()));
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("ClearSelection",
					(arguments, globalScope, triggerScope) -> {
						meleeUI.scriptClearSelection();
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GroupEnumUnitsSelected",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
						final CPlayerJass player = arguments.get(1)
								.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
						final TriggerBooleanExpression filter = nullable(arguments, 2,
								ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
						for (final CUnit unit : meleeUI.getSelectedSimulationUnits(player.getId())) {
							if ((filter == null) || filter.evaluate(globalScope,
									CommonTriggerExecutionScope.filterScope(triggerScope, unit))) {
								group.add(unit);
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitSelected",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CPlayerJass player = arguments.get(1)
								.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
						return BooleanJassValue.of((whichUnit != null)
								&& meleeUI.getSelectedSimulationUnits(player.getId()).contains(whichUnit));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetCameraTargetPositionX",
					(arguments, globalScope, triggerScope) -> RealJassValue
							.of(meleeUI.getCameraManager().target.x));
			jassProgramVisitor.getJassNativeManager().createNative("GetCameraTargetPositionY",
					(arguments, globalScope, triggerScope) -> RealJassValue
							.of(meleeUI.getCameraManager().target.y));
			jassProgramVisitor.getJassNativeManager().createNative("IssuePointOrder",
					(arguments, globalScope, triggerScope) -> {
						final String orderString = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						return issuePointOrderById.call(Arrays.asList(arguments.get(0),
								IntegerJassValue.of(OrderIdUtils.getOrderId(orderString)), arguments.get(2),
								arguments.get(3)), globalScope, triggerScope);
					});
			jassProgramVisitor.getJassNativeManager().createNative("IssuePointOrderByIdLoc",
					(arguments, globalScope, triggerScope) -> {
						final AbilityPointTarget whichLocation = arguments.get(2)
								.visit(ObjectJassValueVisitor.getInstance());
						if (whichLocation == null) {
							return BooleanJassValue.FALSE;
						}
						return issuePointOrderById.call(Arrays.asList(arguments.get(0), arguments.get(1),
								new RealJassValue(whichLocation.x), new RealJassValue(whichLocation.y)), globalScope,
								triggerScope);
					});
			// TODO if BuildOrderById is actually different from PointOrderById then this
			// needs to be fixed:
			jassProgramVisitor.getJassNativeManager().createNative("IssueBuildOrderById", issuePointOrderById);
			jassProgramVisitor.getJassNativeManager().createNative("IssueTargetOrder",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichUnit != null) && issueTargetOrder(whichUnit.getPlayerIndex(), whichUnit,
								OrderIdUtils.getOrderId(arguments.get(1).visit(StringJassValueVisitor.getInstance())),
								nullable(arguments, 2, ObjectJassValueVisitor.getInstance())));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IssueTargetOrderById",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichUnit != null) && issueTargetOrder(whichUnit.getPlayerIndex(), whichUnit,
								arguments.get(1).visit(IntegerJassValueVisitor.getInstance()),
								nullable(arguments, 2, ObjectJassValueVisitor.getInstance())));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IssueImmediateOrder",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichUnit != null) && issueImmediateOrder(whichUnit.getPlayerIndex(), whichUnit,
								OrderIdUtils.getOrderId(arguments.get(1).visit(StringJassValueVisitor.getInstance()))));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IssueImmediateOrderById",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichUnit != null) && issueImmediateOrder(whichUnit.getPlayerIndex(), whichUnit,
								arguments.get(1).visit(IntegerJassValueVisitor.getInstance())));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IssueBuildOrder",
					(arguments, globalScope, triggerScope) -> {
						final CUnitType buildType = CommonEnvironment.this.simulation.getUnitData()
								.getUnitTypeByJassLegacyName(arguments.get(1).visit(StringJassValueVisitor.getInstance()));
						if (buildType == null) {
							return BooleanJassValue.FALSE;
						}
						return issuePointOrderById.call(Arrays.asList(arguments.get(0),
								IntegerJassValue.of(buildType.getTypeId().getValue()), arguments.get(2), arguments.get(3)),
								globalScope, triggerScope);
					});
			jassProgramVisitor.getJassNativeManager().createNative("IssueInstantPointOrder",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichUnit != null) && issuePointOrder(whichUnit.getPlayerIndex(), whichUnit,
								OrderIdUtils.getOrderId(arguments.get(1).visit(StringJassValueVisitor.getInstance())),
								arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue(),
								arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue()));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IssueInstantPointOrderById",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichUnit != null) && issuePointOrder(whichUnit.getPlayerIndex(), whichUnit,
								arguments.get(1).visit(IntegerJassValueVisitor.getInstance()),
								arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue(),
								arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue()));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IssueInstantTargetOrder",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichUnit != null) && issueTargetOrder(whichUnit.getPlayerIndex(), whichUnit,
								OrderIdUtils.getOrderId(arguments.get(1).visit(StringJassValueVisitor.getInstance())),
								nullable(arguments, 2, ObjectJassValueVisitor.getInstance())));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IssueInstantTargetOrderById",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichUnit != null) && issueTargetOrder(whichUnit.getPlayerIndex(), whichUnit,
								arguments.get(1).visit(IntegerJassValueVisitor.getInstance()),
								nullable(arguments, 2, ObjectJassValueVisitor.getInstance())));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IssueNeutralImmediateOrder",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer forWhichPlayer = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CUnit neutralStructure = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						if ((forWhichPlayer == null) || (neutralStructure == null)) {
							return BooleanJassValue.FALSE;
						}
						return BooleanJassValue.of(issueImmediateOrder(forWhichPlayer.getId(), neutralStructure,
								OrderIdUtils.getOrderId(arguments.get(2).visit(StringJassValueVisitor.getInstance()))));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IssueNeutralPointOrder",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer forWhichPlayer = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CUnit neutralStructure = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						if ((forWhichPlayer == null) || (neutralStructure == null)) {
							return BooleanJassValue.FALSE;
						}
						return BooleanJassValue.of(issuePointOrder(forWhichPlayer.getId(), neutralStructure,
								OrderIdUtils.getOrderId(arguments.get(2).visit(StringJassValueVisitor.getInstance())),
								arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue(),
								arguments.get(4).visit(RealJassValueVisitor.getInstance()).floatValue()));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IssueNeutralTargetOrder",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer forWhichPlayer = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CUnit neutralStructure = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						if ((forWhichPlayer == null) || (neutralStructure == null)) {
							return BooleanJassValue.FALSE;
						}
						return BooleanJassValue.of(issueTargetOrder(forWhichPlayer.getId(), neutralStructure,
								OrderIdUtils.getOrderId(arguments.get(2).visit(StringJassValueVisitor.getInstance())),
								nullable(arguments, 3, ObjectJassValueVisitor.getInstance())));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IssueNeutralImmediateOrderById",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer forWhichPlayer = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CUnit neutralStructure = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						if ((forWhichPlayer == null) || (neutralStructure == null)) {
							return BooleanJassValue.FALSE;
						}
						return BooleanJassValue.of(issueImmediateOrder(forWhichPlayer.getId(), neutralStructure,
								arguments.get(2).visit(IntegerJassValueVisitor.getInstance())));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IssueNeutralPointOrderById",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer forWhichPlayer = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CUnit neutralStructure = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						if ((forWhichPlayer == null) || (neutralStructure == null)) {
							return BooleanJassValue.FALSE;
						}
						return BooleanJassValue.of(issuePointOrder(forWhichPlayer.getId(), neutralStructure,
								arguments.get(2).visit(IntegerJassValueVisitor.getInstance()),
								arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue(),
								arguments.get(4).visit(RealJassValueVisitor.getInstance()).floatValue()));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IssueNeutralTargetOrderById",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer forWhichPlayer = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CUnit neutralStructure = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						if ((forWhichPlayer == null) || (neutralStructure == null)) {
							return BooleanJassValue.FALSE;
						}
						return BooleanJassValue.of(issueTargetOrder(forWhichPlayer.getId(), neutralStructure,
								arguments.get(2).visit(IntegerJassValueVisitor.getInstance()),
								nullable(arguments, 3, ObjectJassValueVisitor.getInstance())));
					});

			jassProgramVisitor.getJassNativeManager().createNative("UnitUseItem",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (whichUnit == null) {
							return BooleanJassValue.FALSE;
						}
						final CItem item = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						final CPlayerUnitOrderExecutor defaultPlayerUnitOrderExecutor = CommonEnvironment.this.simulation
								.getDefaultPlayerUnitOrderExecutor(whichUnit.getPlayerIndex());
						final BooleanAbilityActivationReceiver activationReceiver = BooleanAbilityActivationReceiver.INSTANCE.reset();
						final int orderId = OrderIdUtils.getOrderId(
								"itemuse" + String.format("%02d", item.getContainedInventory().getSlot(item)));
						int abilityHandleId = 0;
						for (final CAbility ability : whichUnit.getAbilities()) {
							ability.checkCanUse(CommonEnvironment.this.simulation, whichUnit, whichUnit.getPlayerIndex(), orderId, false,
									activationReceiver);
							if (activationReceiver.isOk()) {
								final BooleanAbilityTargetCheckReceiver<Void> targetReceiver = BooleanAbilityTargetCheckReceiver
										.<Void>getInstance();
								ability.checkCanTargetNoTarget(CommonEnvironment.this.simulation, whichUnit, whichUnit.getPlayerIndex(), orderId,
										false, targetReceiver.reset());
								if (targetReceiver.isTargetable()) {
									abilityHandleId = ability.getHandleId();
								}
							}
						}
						defaultPlayerUnitOrderExecutor.issueImmediateOrder(whichUnit.getHandleId(), abilityHandleId,
								orderId, false);
						return BooleanJassValue.of(abilityHandleId != 0);
					});
			final JassFunction unitUseItemTarget = (arguments, globalScope, triggerScope) -> {
				final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
				if (whichUnit == null) {
					return BooleanJassValue.FALSE;
				}
				final CItem item = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
				CWidget whichTarget = arguments.get(2).visit(ObjectJassValueVisitor.getInstance());
				final CPlayerUnitOrderExecutor defaultPlayerUnitOrderExecutor = CommonEnvironment.this.simulation
						.getDefaultPlayerUnitOrderExecutor(whichUnit.getPlayerIndex());
				final BooleanAbilityActivationReceiver activationReceiver = BooleanAbilityActivationReceiver.INSTANCE.reset();
				final int orderId = OrderIdUtils
						.getOrderId("itemuse" + String.format("%02d", item.getContainedInventory().getSlot(item)));
				int abilityHandleId = 0;
				for (final CAbility ability : whichUnit.getAbilities()) {
					ability.checkCanUse(CommonEnvironment.this.simulation, whichUnit, whichUnit.getPlayerIndex(), orderId, false,
							activationReceiver);
					if (activationReceiver.isOk()) {
						final CWidgetAbilityTargetCheckReceiver targetReceiver = CWidgetAbilityTargetCheckReceiver.INSTANCE;
						ability.checkCanTarget(CommonEnvironment.this.simulation, whichUnit, whichUnit.getPlayerIndex(), orderId, false,
								whichTarget, targetReceiver.reset());
						if (targetReceiver.getTarget() != null) {
							whichTarget = targetReceiver.getTarget();
							abilityHandleId = ability.getHandleId();
						}
					}
				}
				if (abilityHandleId != 0) {
					defaultPlayerUnitOrderExecutor.issueTargetOrder(whichUnit.getHandleId(), abilityHandleId, orderId,
							whichTarget.getHandleId(), false);
				}
				return BooleanJassValue.of(abilityHandleId != 0);
			};
			jassProgramVisitor.getJassNativeManager().createNative("UnitUseItemTarget", unitUseItemTarget);
			jassProgramVisitor.getJassNativeManager().createNative("UnitUseItemDestructable", unitUseItemTarget);
			jassProgramVisitor.getJassNativeManager().createNative("UnitUseItemPointLoc",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (whichUnit == null) {
							return BooleanJassValue.FALSE;
						}
						final CItem item = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						final AbilityPointTarget whichLocation = arguments.get(2)
								.visit(ObjectJassValueVisitor.getInstance());
						final CPlayerUnitOrderExecutor defaultPlayerUnitOrderExecutor = CommonEnvironment.this.simulation
								.getDefaultPlayerUnitOrderExecutor(whichUnit.getPlayerIndex());
						final BooleanAbilityActivationReceiver activationReceiver = BooleanAbilityActivationReceiver.INSTANCE.reset();
						final int orderId = OrderIdUtils.getOrderId(
								"itemuse" + String.format("%02d", item.getContainedInventory().getSlot(item)));
						int abilityHandleId = 0;
						AbilityPointTarget targetAsPoint = new AbilityPointTarget(whichLocation.x, whichLocation.y);
						for (final CAbility ability : whichUnit.getAbilities()) {
							ability.checkCanUse(CommonEnvironment.this.simulation, whichUnit,
									whichUnit.getPlayerIndex(), orderId, false, activationReceiver);
							if (activationReceiver.isOk()) {
								final PointAbilityTargetCheckReceiver targetReceiver = PointAbilityTargetCheckReceiver.INSTANCE;
								ability.checkCanTarget(CommonEnvironment.this.simulation, whichUnit,
										whichUnit.getPlayerIndex(), orderId, false, targetAsPoint,
										targetReceiver.reset());
								if (targetReceiver.getTarget() != null) {
									targetAsPoint = targetReceiver.getTarget();
									abilityHandleId = ability.getHandleId();
								}
							}
						}
						if (abilityHandleId != 0) {
							defaultPlayerUnitOrderExecutor.issuePointOrder(whichUnit.getHandleId(), abilityHandleId,
									orderId, targetAsPoint.x, targetAsPoint.y, false);
						}
						return BooleanJassValue.of(abilityHandleId != 0);
					});

			jassProgramVisitor.getJassNativeManager().createNative("UnitDamageTarget",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CWidget target = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						final double amount = arguments.get(2).visit(RealJassValueVisitor.getInstance());
						final boolean attack = arguments.get(3).visit(BooleanJassValueVisitor.getInstance());
						final boolean ranged = arguments.get(4).visit(BooleanJassValueVisitor.getInstance());
						final Object attackTypeValue = nullable(arguments, 5, ObjectJassValueVisitor.getInstance());
						if (attackTypeValue instanceof UnknownEnumHandle) {
							// 1.29 deals no damage with an attack type it has no constant for.
							return BooleanJassValue.FALSE;
						}
						CAttackType attackType = (CAttackType) attackTypeValue;
						final Object damageTypeValue = nullable(arguments, 6, ObjectJassValueVisitor.getInstance());
						final Object weaponTypeValue = nullable(arguments, 7, ObjectJassValueVisitor.getInstance());
						// 1.29 (convert-enum-d348) still deals the damage with an unknown damage type of 0 or more, but
						// none with a negative one or with an unknown weapon type.
						if ((weaponTypeValue instanceof UnknownEnumHandle) || ((damageTypeValue instanceof UnknownEnumHandle)
								&& (((UnknownEnumHandle) damageTypeValue).getHandleId() < 0))) {
							return BooleanJassValue.FALSE;
						}
						CDamageType damageType = damageTypeValue instanceof CDamageType ? (CDamageType) damageTypeValue
								: null;
						CWeaponSoundTypeJass weaponType = (CWeaponSoundTypeJass) weaponTypeValue;
						if (whichUnit != null) {
							if (target != null) {
								if (attackType == null) {
									attackType = CAttackType.UNKNOWN;
								}
								if (weaponType == null) {
									weaponType = CWeaponSoundTypeJass.WHOKNOWS;
								}
								if (damageType == null) {
									damageType = CDamageType.UNKNOWN;
								}
								target.damage(CommonEnvironment.this.simulation, whichUnit,
										new CGenericDamageFlags(attack, ranged), attackType, damageType,
										weaponType.name(), (float) amount);
								return BooleanJassValue.TRUE;
							}
						}
						return BooleanJassValue.FALSE;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GroupEnumUnitsOfTypeCounted",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
						final String unitname = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						final TriggerBooleanExpression filter = nullable(arguments, 2,
								ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
						final Integer countLimit = arguments.get(3).visit(IntegerJassValueVisitor.getInstance());
						int count = 0;
						for (final CUnit unit : CommonEnvironment.this.simulation.getUnitsIncludingNew()) {
							if (unitname.equals(unit.getUnitType().getLegacyName())) {
								if ((filter == null) || filter.evaluate(globalScope,
										CommonTriggerExecutionScope.filterScope(triggerScope, unit))) {
									// TODO the trigger scope for evaluation here might need to be a clean one?
									group.add(unit);
									count++;
									if (count >= countLimit) {
										break;
									}
								}
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GroupEnumUnitsInRect",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
						final Rectangle rect = nullable(arguments, 1, ObjectJassValueVisitor.<Rectangle>getInstance());
						if (rect != null) {
							final TriggerBooleanExpression filter = nullable(arguments, 2,
									ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
							// TODO: maybe change so that calling corpse function here as well is not needed
							CommonEnvironment.this.simulation.getWorldCollision().enumUnitsInRect(rect, (unit) -> {
								if (rectCellsContain(rect, unit.getX(), unit.getY()) && ((filter == null)
										|| filter.evaluate(globalScope,
												CommonTriggerExecutionScope.filterScope(triggerScope, unit)))) {
									// TODO the trigger scope for evaluation here might need to be a clean one?
									group.add(unit);
								}
								return false;
							});
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GroupEnumUnitsInRectCounted",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
						final Rectangle rect = arguments.get(1).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
						final TriggerBooleanExpression filter = nullable(arguments, 2,
								ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
						final Integer countLimit = arguments.get(3).visit(IntegerJassValueVisitor.getInstance());
						CommonEnvironment.this.simulation.getWorldCollision().enumUnitsOrCorpsesInRect(rect,
								new CUnitEnumFunction() {
									int count = 0;

									@Override
									public boolean call(final CUnit unit) {
										if (rectCellsContain(rect, unit.getX(), unit.getY()) && ((filter == null)
												|| filter.evaluate(globalScope,
														CommonTriggerExecutionScope.filterScope(triggerScope, unit)))) {
											// TODO the trigger scope for evaluation here might need to be a clean one?
											group.add(unit);
											this.count++;
											if (this.count >= countLimit) {
												return true;
											}
										}
										return false;
									}
								});
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GroupEnumUnitsInRange",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
						final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float radius = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
						final TriggerBooleanExpression filter = nullable(arguments, 4,
								ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
						CommonEnvironment.this.simulation.getWorldCollision().enumUnitsOrCorpsesInRange(x, y, radius,
								(unit) -> {
									if (unit.distance(x, y) <= radius) {
										if ((filter == null) || filter.evaluate(globalScope,
												CommonTriggerExecutionScope.filterScope(triggerScope, unit))) {
											// TODO the trigger scope for evaluation here might need to be a clean one?
											group.add(unit);
										}
									}
									return false;
								});
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GroupEnumUnitsInRangeOfLoc",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
						final AbilityPointTarget whichLocation = arguments.get(1)
								.visit(ObjectJassValueVisitor.<AbilityPointTarget>getInstance());
						final float x = whichLocation.x;
						final float y = whichLocation.y;
						final float radius = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						final TriggerBooleanExpression filter = nullable(arguments, 3,
								ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());

						CommonEnvironment.this.simulation.getWorldCollision().enumUnitsOrCorpsesInRange(x, y, radius,
								(unit) -> {
									if (unit.distance(x, y) <= radius) {
										if ((filter == null) || filter.evaluate(globalScope,
												CommonTriggerExecutionScope.filterScope(triggerScope, unit))) {
											// TODO the trigger scope for evaluation here might need to be a clean one?
											group.add(unit);
										}
									}
									return false;
								});
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GroupEnumUnitsInRangeCounted",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
						final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float radius = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
						final TriggerBooleanExpression filter = nullable(arguments, 4,
								ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
						final Integer countLimit = arguments.get(5).visit(IntegerJassValueVisitor.getInstance());
						CommonEnvironment.this.simulation.getWorldCollision().enumUnitsOrCorpsesInRange(x, y, radius,
								new CUnitEnumFunction() {
									int count = 0;

									@Override
									public boolean call(final CUnit unit) {
										if (unit.distance(x, y) <= radius) {
											if ((filter == null) || filter.evaluate(globalScope,
													CommonTriggerExecutionScope.filterScope(triggerScope, unit))) {
												// TODO the trigger scope for evaluation here might need to be a clean
												// one?
												group.add(unit);
												this.count++;
												if (this.count >= countLimit) {
													return true;
												}
											}
										}
										return false;
									}
								});
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GroupEnumUnitsInRangeOfLocCounted",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
						final AbilityPointTarget whichLocation = arguments.get(1)
								.visit(ObjectJassValueVisitor.<AbilityPointTarget>getInstance());
						final float x = whichLocation.x;
						final float y = whichLocation.y;
						final float radius = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						final TriggerBooleanExpression filter = nullable(arguments, 3,
								ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
						final Integer countLimit = arguments.get(4).visit(IntegerJassValueVisitor.getInstance());
						CommonEnvironment.this.simulation.getWorldCollision().enumUnitsOrCorpsesInRange(x, y, radius,
								new CUnitEnumFunction() {
									int count = 0;

									@Override
									public boolean call(final CUnit unit) {
										if (unit.distance(x, y) <= radius) {
											if ((filter == null) || filter.evaluate(globalScope,
													CommonTriggerExecutionScope.filterScope(triggerScope, unit))) {
												// TODO the trigger scope for evaluation here might need to be a
												// clean one?
												group.add(unit);
												this.count++;
												if (this.count >= countLimit) {
													return true;
												}
											}
										}
										return false;
									}
								});
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GroupImmediateOrder",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
						final String order = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						final int orderId = OrderIdUtils.getOrderId(order);
						boolean success = true;
						for (final CUnit unit : group) {
							success &= unit.order(CommonEnvironment.this.simulation, orderId, null);
						}
						return BooleanJassValue.of(success);
					});
			jassProgramVisitor.getJassNativeManager().createNative("GroupImmediateOrderById",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
						final int order = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						boolean success = true;
						for (final CUnit unit : group) {
							success &= unit.order(CommonEnvironment.this.simulation, order, null);
						}
						return BooleanJassValue.of(success);
					});
			jassProgramVisitor.getJassNativeManager().createNative("GroupPointOrder",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
						final String order = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						final Double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
						final Double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());
						final int orderId = OrderIdUtils.getOrderId(order);
						return BooleanJassValue.of(groupPointOrder(group, orderId, x.floatValue(), y.floatValue()));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GroupPointOrderLoc",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
						final String order = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						final AbilityPointTarget whichLocation = arguments.get(2)
								.visit(ObjectJassValueVisitor.<AbilityPointTarget>getInstance());
						final int orderId = OrderIdUtils.getOrderId(order);
						return BooleanJassValue.of(groupPointOrder(group, orderId, whichLocation.x, whichLocation.y));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GroupPointOrderById",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
						final int orderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final Double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
						final Double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());
						return BooleanJassValue.of(groupPointOrder(group, orderId, x.floatValue(), y.floatValue()));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GroupPointOrderByIdLoc",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
						final int orderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final AbilityPointTarget whichLocation = arguments.get(2)
								.visit(ObjectJassValueVisitor.<AbilityPointTarget>getInstance());
						return BooleanJassValue.of(groupPointOrder(group, orderId, whichLocation.x, whichLocation.y));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GroupTargetOrder",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
						final String order = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						final CWidget target = arguments.get(2).visit(ObjectJassValueVisitor.<CWidget>getInstance());
						return BooleanJassValue.of(groupTargetOrder(group, OrderIdUtils.getOrderId(order), target));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GroupTargetOrderById",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
						final int orderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final CWidget target = arguments.get(2).visit(ObjectJassValueVisitor.<CWidget>getInstance());
						return BooleanJassValue.of(groupTargetOrder(group, orderId, target));
					});
			jassProgramVisitor.getJassNativeManager().createNative("ForGroup",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (group != null) {
							final CodeJassValue callback = arguments.get(1).visit(CodeJassValueVisitor.getInstance());
							try {
								for (final CUnit unit : group) {
									globalScope.runThreadUntilCompletion(globalScope.createThread(callback,
											CommonTriggerExecutionScope.enumScope(triggerScope, unit)));
								}
							}
							catch (final Exception e) {
								throw new JassException(globalScope, "Exception during ForGroup", e);
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("FirstOfGroup",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
						if (group.isEmpty()) {
							return unitType.getNullValue();
						}
						return new HandleJassValue(unitType, group.get(0));
					});
			// ============================================================================
			// Force API
			//
			jassProgramVisitor.getJassNativeManager().createNative("CreateForce",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(forceType, new ArrayList<CPlayerJass>());
					});
			jassProgramVisitor.getJassNativeManager().createNative("DestroyForce",
					(arguments, globalScope, triggerScope) -> {
						final List<CPlayerJass> force = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CPlayerJass>>getInstance());
						reportMissingDestructorOnce("DestroyForce");
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("ForceAddPlayer",
					(arguments, globalScope, triggerScope) -> {
						final List<CPlayerJass> force = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CPlayerJass>>getInstance());
						final CPlayerJass player = arguments.get(1)
								.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
						if (force == null) {
							throw new JassException(globalScope, "force is null", new NullPointerException());
						}
						if (player != null) {
							force.add(player);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("ForceRemovePlayer",
					(arguments, globalScope, triggerScope) -> {
						final List<CPlayerJass> force = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CPlayerJass>>getInstance());
						final CPlayerJass player = arguments.get(1)
								.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
						if (force != null) {
							force.remove(player);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("ForceClear",
					(arguments, globalScope, triggerScope) -> {
						final List<CPlayerJass> force = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CPlayerJass>>getInstance());
						force.clear();
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("ForceEnumPlayers",
					(arguments, globalScope, triggerScope) -> {
						final List<CPlayerJass> force = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CPlayerJass>>getInstance());
						final TriggerBooleanExpression filter = nullable(arguments, 1,
								ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
						for (int i = 0; i < WarsmashConstants.MAX_PLAYERS; i++) {
							final CPlayerJass jassPlayer = CommonEnvironment.this.simulation.getPlayer(i);
							// 1.29 skips empty slots (also the neutral players) but keeps players who left.
							if (jassPlayer.getSlotState() == CPlayerSlotState.EMPTY) {
								continue;
							}
							if ((filter == null) || filter.evaluate(globalScope,
									CommonTriggerExecutionScope.filterScope(triggerScope, jassPlayer))) {
								force.add(jassPlayer);
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("ForceEnumPlayersCounted",
					(arguments, globalScope, triggerScope) -> {
						final List<CPlayerJass> force = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CPlayerJass>>getInstance());
						final TriggerBooleanExpression filter = nullable(arguments, 1,
								ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
						final Integer countLimit = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						int count = 0;
						for (int i = 0; (i < WarsmashConstants.MAX_PLAYERS) && (count < countLimit); i++) {
							final CPlayerJass jassPlayer = CommonEnvironment.this.simulation.getPlayer(i);
							if (jassPlayer.getSlotState() == CPlayerSlotState.EMPTY) {
								continue;
							}
							if ((filter == null) || filter.evaluate(globalScope,
									CommonTriggerExecutionScope.filterScope(triggerScope, jassPlayer))) {
								force.add(jassPlayer);
								count++;
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("ForceEnumAllies",
					(arguments, globalScope, triggerScope) -> {
						final List<CPlayerJass> force = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CPlayerJass>>getInstance());
						final CPlayerJass player = nullable(arguments, 1,
								ObjectJassValueVisitor.<CPlayerJass>getInstance());
						final TriggerBooleanExpression filter = nullable(arguments, 2,
								ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
						if (player == null) {
							return null;
						}
						// 1.29 runs the filter for every used slot (players who left too) and then keeps the players
						// whose own alliance toward the given player matches.
						for (int i = 0; i < WarsmashConstants.MAX_PLAYERS; i++) {
							final CPlayerJass jassPlayer = CommonEnvironment.this.simulation.getPlayer(i);
							if (jassPlayer.getSlotState() == CPlayerSlotState.EMPTY) {
								continue;
							}
							if (((filter == null) || filter.evaluate(globalScope,
									CommonTriggerExecutionScope.filterScope(triggerScope, jassPlayer)))
									&& jassPlayer.hasAlliance(player.getId(), CAllianceType.PASSIVE)) {
								force.add(jassPlayer);
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("ForceEnumEnemies",
					(arguments, globalScope, triggerScope) -> {
						final List<CPlayerJass> force = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CPlayerJass>>getInstance());
						final CPlayerJass player = nullable(arguments, 1,
								ObjectJassValueVisitor.<CPlayerJass>getInstance());
						final TriggerBooleanExpression filter = nullable(arguments, 2,
								ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
						if (player == null) {
							return null;
						}
						// 1.29 runs the filter for every used slot (players who left too) and then keeps the players
						// whose own alliance toward the given player matches.
						for (int i = 0; i < WarsmashConstants.MAX_PLAYERS; i++) {
							final CPlayerJass jassPlayer = CommonEnvironment.this.simulation.getPlayer(i);
							if (jassPlayer.getSlotState() == CPlayerSlotState.EMPTY) {
								continue;
							}
							if (((filter == null) || filter.evaluate(globalScope,
									CommonTriggerExecutionScope.filterScope(triggerScope, jassPlayer)))
									&& !jassPlayer.hasAlliance(player.getId(), CAllianceType.PASSIVE)) {
								force.add(jassPlayer);
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("ForForce",
					(arguments, globalScope, triggerScope) -> {
						final List<CPlayerJass> force = arguments.get(0)
								.visit(ObjectJassValueVisitor.<List<CPlayerJass>>getInstance());
						final CodeJassValue callback = arguments.get(1).visit(CodeJassValueVisitor.getInstance());
						try {
							for (final CPlayerJass player : force) {
								globalScope.runThreadUntilCompletion(globalScope.createThread(callback,
										CommonTriggerExecutionScope.enumScope(triggerScope, player)));
							}
						}
						catch (final Exception e) {
							throw new JassException(globalScope, "Exception during ForForce", e);
						}
						return null;
					});
			// ============================================================================
			// Region and Location API
			//
			jassProgramVisitor.getJassNativeManager().createNative("Rect", (arguments, globalScope, triggerScope) -> {
				final float minx = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
				final float miny = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
				final float maxx = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
				final float maxy = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
				return new HandleJassValue(rectType, new Rectangle(minx, miny, maxx - minx, maxy - miny));
			});
			jassProgramVisitor.getJassNativeManager().createNative("RectFromLoc",
					(arguments, globalScope, triggerScope) -> {
						final AbilityPointTarget min = arguments.get(0)
								.visit(ObjectJassValueVisitor.<AbilityPointTarget>getInstance());
						final AbilityPointTarget max = arguments.get(1)
								.visit(ObjectJassValueVisitor.<AbilityPointTarget>getInstance());
						final float minx = min.x;
						final float miny = min.y;
						final float maxx = max.x;
						final float maxy = max.y;
						return new HandleJassValue(rectType, new Rectangle(minx, miny, maxx - minx, maxy - miny));
					});
			jassProgramVisitor.getJassNativeManager().createNative("RemoveRect",
					(arguments, globalScope, triggerScope) -> {
						final Rectangle rect = arguments.get(0).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
						reportMissingDestructorOnce("RemoveRect");
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetRect",
					(arguments, globalScope, triggerScope) -> {
						final Rectangle rect = arguments.get(0).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
						final float minx = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float miny = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float maxx = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float maxy = arguments.get(4).visit(RealJassValueVisitor.getInstance()).floatValue();
						rect.set(minx, miny, maxx - minx, maxy - miny);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetRectFromLoc",
					(arguments, globalScope, triggerScope) -> {
						final Rectangle rect = arguments.get(0).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
						final AbilityPointTarget min = arguments.get(1)
								.visit(ObjectJassValueVisitor.<AbilityPointTarget>getInstance());
						final AbilityPointTarget max = arguments.get(2)
								.visit(ObjectJassValueVisitor.<AbilityPointTarget>getInstance());
						final float minx = min.x;
						final float miny = min.y;
						final float maxx = max.x;
						final float maxy = max.y;
						rect.set(minx, miny, maxx - minx, maxy - miny);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("MoveRectTo",
					(arguments, globalScope, triggerScope) -> {
						final Rectangle rect = arguments.get(0).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
						final float newCenterX = arguments.get(1).visit(RealJassValueVisitor.getInstance())
								.floatValue();
						final float newCenterY = arguments.get(2).visit(RealJassValueVisitor.getInstance())
								.floatValue();
						rect.setCenter(newCenterX, newCenterY);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("MoveRectToLoc",
					(arguments, globalScope, triggerScope) -> {
						final Rectangle rect = arguments.get(0).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
						final AbilityPointTarget newCenterLoc = arguments.get(1)
								.visit(ObjectJassValueVisitor.<AbilityPointTarget>getInstance());
						rect.setCenter(newCenterLoc.x, newCenterLoc.y);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetRectCenterX", new JassFunction() {
				Vector2 centerHeap = new Vector2();

				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Rectangle rect = nullable(arguments, 0, ObjectJassValueVisitor.<Rectangle>getInstance());
					if (rect == null) {
						return RealJassValue.ZERO;
					}
					return RealJassValue.of(rect.getCenter(this.centerHeap).x);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetRectCenterY", new JassFunction() {
				Vector2 centerHeap = new Vector2();

				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Rectangle rect = nullable(arguments, 0, ObjectJassValueVisitor.<Rectangle>getInstance());
					if (rect == null) {
						return RealJassValue.ZERO;
					}
					return RealJassValue.of(rect.getCenter(this.centerHeap).y);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetRectMinX",
					(arguments, globalScope, triggerScope) -> {
						final Rectangle rect = arguments.get(0).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
						return RealJassValue.of(rect.getX());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetRectMinY",
					(arguments, globalScope, triggerScope) -> {
						final Rectangle rect = arguments.get(0).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
						return RealJassValue.of(rect.getY());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetRectMaxX",
					(arguments, globalScope, triggerScope) -> {
						final Rectangle rect = arguments.get(0).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
						return RealJassValue.of(rect.getX() + rect.getWidth());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetRectMaxY",
					(arguments, globalScope, triggerScope) -> {
						final Rectangle rect = arguments.get(0).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
						return RealJassValue.of(rect.getY() + rect.getHeight());
					});
			jassProgramVisitor.getJassNativeManager().createNative("CreateRegion",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(regionType, new CRegion());
					});
			jassProgramVisitor.getJassNativeManager().createNative("RemoveRegion",
					(arguments, globalScope, triggerScope) -> {
						final CRegion region = arguments.get(0).visit(ObjectJassValueVisitor.<CRegion>getInstance());
						region.remove(CommonEnvironment.this.simulation.getRegionManager());
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("RegionAddRect",
					(arguments, globalScope, triggerScope) -> {
						final CRegion region = arguments.get(0).visit(ObjectJassValueVisitor.<CRegion>getInstance());
						final Rectangle rect = arguments.get(1).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
						region.addRect(rect, CommonEnvironment.this.simulation.getRegionManager());
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("RegionClearRect",
					(arguments, globalScope, triggerScope) -> {
						final CRegion region = arguments.get(0).visit(ObjectJassValueVisitor.<CRegion>getInstance());
						final Rectangle rect = arguments.get(1).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
						region.clearRect(rect, CommonEnvironment.this.simulation.getRegionManager());
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("RegionAddCell",
					(arguments, globalScope, triggerScope) -> {
						final CRegion region = arguments.get(0).visit(ObjectJassValueVisitor.<CRegion>getInstance());
						final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						region.addCell(x, y, CommonEnvironment.this.simulation.getRegionManager());
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("RegionAddCellAtLoc",
					(arguments, globalScope, triggerScope) -> {
						final CRegion region = arguments.get(0).visit(ObjectJassValueVisitor.<CRegion>getInstance());
						final AbilityPointTarget whichLocation = arguments.get(1)
								.visit(ObjectJassValueVisitor.<AbilityPointTarget>getInstance());
						region.addCell(whichLocation.x, whichLocation.y,
								CommonEnvironment.this.simulation.getRegionManager());
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("RegionClearCell",
					(arguments, globalScope, triggerScope) -> {
						final CRegion region = arguments.get(0).visit(ObjectJassValueVisitor.<CRegion>getInstance());
						final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						region.clearCell(x, y, CommonEnvironment.this.simulation.getRegionManager());
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("RegionClearCellAtLoc",
					(arguments, globalScope, triggerScope) -> {
						final CRegion region = arguments.get(0).visit(ObjectJassValueVisitor.<CRegion>getInstance());
						final AbilityPointTarget whichLocation = arguments.get(1)
								.visit(ObjectJassValueVisitor.<AbilityPointTarget>getInstance());
						region.clearCell(whichLocation.x, whichLocation.y,
								CommonEnvironment.this.simulation.getRegionManager());
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("Location",
					(arguments, globalScope, triggerScope) -> {
						final float x = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						return new HandleJassValue(locationType,
								new LocationJass(x, y, this.simulation.getHandleIdAllocator().createId()));
					});
			jassProgramVisitor.getJassNativeManager().createNative("RemoveLocation",
					(arguments, globalScope, triggerScope) -> {
						final AbilityPointTarget whichLocation = arguments.get(0)
								.visit(ObjectJassValueVisitor.<AbilityPointTarget>getInstance());
						reportMissingDestructorOnce("RemoveLocation");
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("MoveLocation",
					(arguments, globalScope, triggerScope) -> {
						final AbilityPointTarget whichLocation = arguments.get(0)
								.visit(ObjectJassValueVisitor.<AbilityPointTarget>getInstance());
						final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						whichLocation.x = x;
						whichLocation.y = y;
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetLocationX",
					(arguments, globalScope, triggerScope) -> {
						final AbilityPointTarget whichLocation = nullable(arguments, 0,
								ObjectJassValueVisitor.getInstance());
						if (whichLocation == null) {
							return RealJassValue.ZERO;
						}
						return RealJassValue.of(whichLocation.x);
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetLocationY",
					(arguments, globalScope, triggerScope) -> {
						final AbilityPointTarget whichLocation = nullable(arguments, 0,
								ObjectJassValueVisitor.getInstance());
						if (whichLocation == null) {
							return RealJassValue.ZERO;
						}
						return RealJassValue.of(whichLocation.y);
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetLocationZ",
					(arguments, globalScope, triggerScope) -> {
						final AbilityPointTarget whichLocation = arguments.get(0)
								.visit(ObjectJassValueVisitor.<AbilityPointTarget>getInstance());
						war3MapViewer.simulation.getTerrainDeformations().sync(war3MapViewer.simulation);
						return RealJassValue
								.of(war3MapViewer.terrain.getLocationZ(whichLocation.x, whichLocation.y));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitInRegion",
					(arguments, globalScope, triggerScope) -> {
						final CRegion whichRegion = arguments.get(0)
								.visit(ObjectJassValueVisitor.<CRegion>getInstance());
						final CUnit whichUnit = arguments.get(1).visit(ObjectJassValueVisitor.<CUnit>getInstance());
						return BooleanJassValue.of(whichUnit.isInRegion(whichRegion));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsPointInRegion",
					(arguments, globalScope, triggerScope) -> {
						final CRegion whichRegion = arguments.get(0)
								.visit(ObjectJassValueVisitor.<CRegion>getInstance());
						final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						return BooleanJassValue
								.of(whichRegion.contains(x, y, CommonEnvironment.this.simulation.getRegionManager()));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsLocationInRegion",
					(arguments, globalScope, triggerScope) -> {
						final CRegion whichRegion = arguments.get(0)
								.visit(ObjectJassValueVisitor.<CRegion>getInstance());
						final AbilityPointTarget whichLocation = arguments.get(1)
								.visit(ObjectJassValueVisitor.<AbilityPointTarget>getInstance());
						return BooleanJassValue.of(whichRegion.contains(whichLocation.x, whichLocation.y,
								CommonEnvironment.this.simulation.getRegionManager()));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetWorldBounds",
					(arguments, globalScope, triggerScope) -> {
						final float worldMinX = CommonEnvironment.this.simulation.getPathingGrid().getWorldX(0) - 16f;
						final float worldMinY = CommonEnvironment.this.simulation.getPathingGrid().getWorldY(0) - 16f;
						final float worldMaxX = CommonEnvironment.this.simulation.getPathingGrid()
								.getWorldX(CommonEnvironment.this.simulation.getPathingGrid().getWidth() - 1) + 16f;
						final float worldMaxY = CommonEnvironment.this.simulation.getPathingGrid()
								.getWorldY(CommonEnvironment.this.simulation.getPathingGrid().getHeight() - 1) + 16f;
						return new HandleJassValue(rectType,
								new Rectangle(worldMinX, worldMinY, worldMaxX - worldMinX, worldMaxY - worldMinY));
					});
			// ============================================================================
			// Native trigger interface
			//
			setupTriggerAPI(jassProgramVisitor, triggerType, triggeractionType, triggerconditionType, boolexprType,
					conditionfuncType, filterfuncType, eventidType);
			jassProgramVisitor.getJassNativeManager().createNative("GetFilterUnit",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getFilterUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetEnumUnit",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getEnumUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetFilterDestructable",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(destructableType,
								((CommonTriggerExecutionScope) triggerScope).getFilterDestructable());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetEnumDestructable",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(destructableType,
								((CommonTriggerExecutionScope) triggerScope).getEnumDestructable());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetFilterItem",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(itemType,
								((CommonTriggerExecutionScope) triggerScope).getFilterItem());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetEnumItem",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(itemType,
								((CommonTriggerExecutionScope) triggerScope).getEnumItem());
					});

			// ============================================================================
			// Trigger Game Event API
			// ============================================================================
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterVariableEvent",
					(arguments, globalScope, triggerScope) -> {
						final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
						final String varName = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						final CLimitOp limitOp = arguments.get(2).visit(ObjectJassValueVisitor.<CLimitOp>getInstance());
						final Double limitval = arguments.get(3).visit(RealJassValueVisitor.getInstance());
						final RemovableTriggerEvent event = globalScope.registerVariableEvent(trigger, varName, limitOp,
								limitval.doubleValue());
						return new HandleJassValue(eventType, event);
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterTimerEvent",
					(arguments, globalScope, triggerScope) -> {
						final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
						final Double timeout = arguments.get(1).visit(RealJassValueVisitor.getInstance());
						final Boolean periodic = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
						final CTimerNativeEvent timer = new CTimerNativeEvent(globalScope, trigger);
						timer.setRepeats(periodic.booleanValue());
						timer.setTimeoutTime(timeout.floatValue());
						timer.start(CommonEnvironment.this.simulation);
						return new HandleJassValue(eventType, new RemovableTriggerEvent(trigger) {
							@Override
							public void remove() {
								// kill, not unregister: removed from its own action it must not re-arm after firing
								timer.kill(CommonEnvironment.this.simulation);
							}
						});
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterTimerExpireEvent",
					(arguments, globalScope, triggerScope) -> {
						final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CTimerJassBase timer = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						timer.addEvent(trigger);
						return new HandleJassValue(eventType, new CTimerExpireTriggerEvent(trigger, timer));
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterGameStateEvent",
					(arguments, globalScope, triggerScope) -> {
						final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
						final CGameState whichState = arguments.get(1)
								.visit(ObjectJassValueVisitor.<CGameState>getInstance());
						final CLimitOp opcode = arguments.get(2).visit(ObjectJassValueVisitor.<CLimitOp>getInstance());
						final Double limitval = arguments.get(3).visit(RealJassValueVisitor.getInstance());
						if (whichState != CGameState.TIME_OF_DAY) {
							// TODO not yet impl
							throw new UnsupportedOperationException(
									"Not yet implemented: TriggerRegisterGameStateEvent");
						}
						return new HandleJassValue(eventType, CommonEnvironment.this.simulation
								.registerTimeOfDayEvent(globalScope, trigger, opcode, limitval.doubleValue()));
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterUnitStateEvent",
					(arguments, globalScope, triggerScope) -> {
						final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CUnit whichUnit = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						final Object whichState = nullable(arguments, 2, ObjectJassValueVisitor.getInstance());
						final Object opcode = nullable(arguments, 3, ObjectJassValueVisitor.getInstance());
						final Double limitval = arguments.get(4).visit(RealJassValueVisitor.getInstance());
						// Measured: a null state or limitop counts as id 0 (life, LESS_THAN); an unknown state gives
						// no event, an unknown limitop an event that never fires.
						final CUnitState state = whichState == null ? CUnitState.LIFE
								: whichState instanceof CUnitState ? (CUnitState) whichState : null;
						if ((whichUnit == null) || (state == null)) {
							return eventType.getNullValue();
						}
						final CLimitOp limitOp = opcode == null ? CLimitOp.LESS_THAN
								: opcode instanceof CLimitOp ? (CLimitOp) opcode : null;
						return new HandleJassValue(eventType, whichUnit.addStateEvent(globalScope, whichTrigger, state,
								limitOp, limitval.doubleValue()));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetEventUnitState",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitstateType,
								((CommonTriggerExecutionScope) triggerScope).getEventUnitState());
					});
			jassProgramVisitor.getJassNativeManager().createNative("CreateQuest",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(questType,
								CommonEnvironment.this.simulation.getQuestLog().createQuest());
					});
			jassProgramVisitor.getJassNativeManager().createNative("DestroyQuest",
					(arguments, globalScope, triggerScope) -> {
						final CQuest quest = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (quest != null) {
							CommonEnvironment.this.simulation.getQuestLog().destroyQuest(quest);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("QuestSetTitle",
					(arguments, globalScope, triggerScope) -> {
						final CQuest quest = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (quest != null) {
							quest.setTitle(CommonEnvironment.this.gameUI
									.getTrigStr(nullable(arguments, 1, StringJassValueVisitor.getInstance())));
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("QuestSetDescription",
					(arguments, globalScope, triggerScope) -> {
						final CQuest quest = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (quest != null) {
							quest.setDescription(CommonEnvironment.this.gameUI
									.getTrigStr(nullable(arguments, 1, StringJassValueVisitor.getInstance())));
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("QuestSetIconPath",
					(arguments, globalScope, triggerScope) -> {
						final CQuest quest = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (quest != null) {
							quest.setIconPath(nullable(arguments, 1, StringJassValueVisitor.getInstance()));
						}
						return null;
					});
			registerQuestFlagNatives(jassProgramVisitor, "Required", CQuest::setRequired, CQuest::isRequired);
			registerQuestFlagNatives(jassProgramVisitor, "Completed", CQuest::setCompleted, CQuest::isCompleted);
			registerQuestFlagNatives(jassProgramVisitor, "Discovered", CQuest::setDiscovered,
					CQuest::isDiscovered);
			registerQuestFlagNatives(jassProgramVisitor, "Failed", CQuest::setFailed, CQuest::isFailed);
			registerQuestFlagNatives(jassProgramVisitor, "Enabled", CQuest::setEnabled, CQuest::isEnabled);
			jassProgramVisitor.getJassNativeManager().createNative("QuestCreateItem",
					(arguments, globalScope, triggerScope) -> {
						final CQuest quest = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if ((quest == null) || quest.isDestroyed()) {
							return questitemType.getNullValue();
						}
						return new HandleJassValue(questitemType, quest.createItem());
					});
			jassProgramVisitor.getJassNativeManager().createNative("QuestItemSetDescription",
					(arguments, globalScope, triggerScope) -> {
						final CQuestItem item = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (item != null) {
							item.setDescription(CommonEnvironment.this.gameUI
									.getTrigStr(nullable(arguments, 1, StringJassValueVisitor.getInstance())));
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("QuestItemSetCompleted",
					(arguments, globalScope, triggerScope) -> {
						final CQuestItem item = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (item != null) {
							item.setCompleted(arguments.get(1).visit(BooleanJassValueVisitor.getInstance()));
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsQuestItemCompleted",
					(arguments, globalScope, triggerScope) -> {
						final CQuestItem item = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((item != null) && item.isCompleted());
					});
			jassProgramVisitor.getJassNativeManager().createNative("CreateDefeatCondition",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(defeatconditionType,
								CommonEnvironment.this.simulation.getQuestLog().createDefeatCondition());
					});
			jassProgramVisitor.getJassNativeManager().createNative("DestroyDefeatCondition",
					(arguments, globalScope, triggerScope) -> {
						final CDefeatCondition condition = nullable(arguments, 0,
								ObjectJassValueVisitor.getInstance());
						if (condition != null) {
							CommonEnvironment.this.simulation.getQuestLog().destroyDefeatCondition(condition);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("DefeatConditionSetDescription",
					(arguments, globalScope, triggerScope) -> {
						final CDefeatCondition condition = nullable(arguments, 0,
								ObjectJassValueVisitor.getInstance());
						if (condition != null) {
							condition.setDescription(CommonEnvironment.this.gameUI
									.getTrigStr(nullable(arguments, 1, StringJassValueVisitor.getInstance())));
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("FlashQuestDialogButton",
					(arguments, globalScope, triggerScope) -> {
						meleeUI.flashQuestDialogButton();
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("ForceQuestDialogUpdate",
					(arguments, globalScope, triggerScope) -> {
						meleeUI.forceQuestDialogUpdate();
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("DialogCreate",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(dialogType, meleeUI.createScriptDialog(globalScope));
					});
			jassProgramVisitor.getJassNativeManager().createNative("DialogDestroy",
					(arguments, globalScope, triggerScope) -> {
						final CScriptDialog dialog = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						meleeUI.destroyDialog(dialog);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("DialogClear",
					(arguments, globalScope, triggerScope) -> {
						final CScriptDialog dialog = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						meleeUI.clearDialog(dialog);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("DialogSetMessage",
					(arguments, globalScope, triggerScope) -> {
						final CScriptDialog dialog = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final String messageText = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						dialog.setTitle(CommonEnvironment.this.gameUI, messageText);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("DialogAddButton",
					(arguments, globalScope, triggerScope) -> {
						final CScriptDialog dialog = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final String buttonText = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						final int hotkeyInt = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						return new HandleJassValue(buttonType,
								meleeUI.createScriptDialogButton(dialog, buttonText, (char) hotkeyInt));
					});
			jassProgramVisitor.getJassNativeManager().createNative("DialogAddQuitButton",
					(arguments, globalScope, triggerScope) -> {
						final CScriptDialog dialog = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final boolean doScoreScreen = arguments.get(1)
								.visit(BooleanJassValueVisitor.getInstance());
						final String buttonText = arguments.get(2).visit(StringJassValueVisitor.getInstance());
						final int hotkeyInt = arguments.get(3).visit(IntegerJassValueVisitor.getInstance());
						final CScriptDialogButton button = meleeUI.createScriptDialogButton(dialog, buttonText,
								(char) hotkeyInt);
						// The melee victory and defeat dialogs quit to the score screen.
						button.setQuitAction(() -> meleeUI.endGame(doScoreScreen));
						return new HandleJassValue(buttonType, button);
					});
			jassProgramVisitor.getJassNativeManager().createNative("DialogDisplay",
					(arguments, globalScope, triggerScope) -> {
						final CPlayerJass player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CScriptDialog whichDialog = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						final boolean flag = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
						if ((player != null) && (whichDialog != null)
								&& (player.getId() == war3MapViewer.getLocalPlayerIndex())) {
							whichDialog.setVisible(flag);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterDialogEvent",
					(arguments, globalScope, triggerScope) -> {
						final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CScriptDialog dialog = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						if (dialog == null) {
							return eventType.getNullValue();
						}
						return new HandleJassValue(eventType, dialog.addEvent(trigger));
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterDialogButtonEvent",
					(arguments, globalScope, triggerScope) -> {
						final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CScriptDialogButton dialogButton = arguments.get(1)
								.visit(ObjectJassValueVisitor.getInstance());
						if (dialogButton == null) {
							return eventType.getNullValue();
						}
						return new HandleJassValue(eventType, dialogButton.addEvent(trigger));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetEventGameState",
					(arguments, globalScope, triggerScope) -> {
						throw new UnsupportedOperationException("Not yet implemented: GetEventGameState");
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterGameEvent",
					(arguments, globalScope, triggerScope) -> {
						final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
						final JassGameEventsWar3 gameEvent = arguments.get(1)
								.visit(ObjectJassValueVisitor.getInstance());
						return new HandleJassValue(eventType,
								CommonEnvironment.this.simulation.registerGameEvent(globalScope, trigger, gameEvent));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetWinningPlayer",
					(arguments, globalScope, triggerScope) -> {
						throw new UnsupportedOperationException("Not yet implemented: GetWinningPlayer");
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterEnterRegion",
					(arguments, globalScope, triggerScope) -> {
						final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
						final CRegion region = arguments.get(1).visit(ObjectJassValueVisitor.<CRegion>getInstance());
						final TriggerBooleanExpression boolexpr = nullable(arguments, 2,
								ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
						return new HandleJassValue(eventType,
								region.add(new CRegionTriggerEnter(globalScope, trigger, boolexpr)));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetTriggeringRegion",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(regionType,
								((CommonTriggerExecutionScope) triggerScope).getTriggeringRegion());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetEnteringUnit",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getEnteringUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterLeaveRegion",
					(arguments, globalScope, triggerScope) -> {
						final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
						final CRegion region = arguments.get(1).visit(ObjectJassValueVisitor.<CRegion>getInstance());
						final TriggerBooleanExpression boolexpr = nullable(arguments, 2,
								ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
						return new HandleJassValue(eventType,
								region.add(new CRegionTriggerLeave(globalScope, trigger, boolexpr)));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetLeavingUnit",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getLeavingUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterTrackableHitEvent",
					(arguments, globalScope, triggerScope) -> {
						throw new UnsupportedOperationException(
								"TriggerRegisterTrackableHitEvent not yet implemented ???");
						// dont feel like implementing this atm, although it probably wouldnt be that
						// hard to do
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterTrackableTrackEvent",
					(arguments, globalScope, triggerScope) -> {
						throw new UnsupportedOperationException(
								"TriggerRegisterTrackableTrackEvent not yet implemented ???");
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetTriggeringTrackable",
					(arguments, globalScope, triggerScope) -> {
						throw new UnsupportedOperationException("GetTriggeringTrackable not yet implemented ???");
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetClickedButton",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(buttonType,
								((CommonTriggerExecutionScope) triggerScope).getClickedButton());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetClickedDialog",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(dialogType,
								((CommonTriggerExecutionScope) triggerScope).getClickedDialog());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetTournamentFinishSoonTimeRemaining",
					(arguments, globalScope, triggerScope) -> {
						throw new UnsupportedOperationException(
								"GetTournamentFinishSoonTimeRemaining not yet implemented ???");
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetTournamentFinishNowRule",
					(arguments, globalScope, triggerScope) -> {
						throw new UnsupportedOperationException("GetTournamentFinishNowRule not yet implemented ???");
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetTournamentFinishNowPlayer",
					(arguments, globalScope, triggerScope) -> {
						throw new UnsupportedOperationException("GetTournamentFinishNowPlayer not yet implemented ???");
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetTournamentScore",
					(arguments, globalScope, triggerScope) -> {
						throw new UnsupportedOperationException("GetTournamentScore not yet implemented ???");
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetSaveBasicFilename",
					(arguments, globalScope, triggerScope) -> {
						if (true) {
							return new StringJassValue(CommonEnvironment.this.simulation.getSaveBasicFilename());
						}
						throw new UnsupportedOperationException("GetSaveBasicFilename not yet implemented ???");
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterPlayerEvent",
					(arguments, globalScope, triggerScope) -> {
						final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CPlayerJass whichPlayer = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						if (whichPlayer == null) {
							return eventType.getNullValue();
						}
						final JassGameEventsWar3 whichPlayerEvent = arguments.get(2)
								.visit(ObjectJassValueVisitor.getInstance());
						return new HandleJassValue(eventType,
								whichPlayer.addEvent(globalScope, whichTrigger, whichPlayerEvent));
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterPlayerStateEvent",
					(arguments, globalScope, triggerScope) -> {
						final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CPlayerJass whichPlayer = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						final CPlayerState whichState = arguments.get(2).visit(ObjectJassValueVisitor.getInstance());
						final CLimitOp opcode = arguments.get(3).visit(ObjectJassValueVisitor.getInstance());
						final Double limitval = arguments.get(4).visit(RealJassValueVisitor.getInstance());
						if (!(whichPlayer instanceof CPlayer) || (whichState == null) || (opcode == null)) {
							return eventType.getNullValue();
						}
						return new HandleJassValue(eventType, ((CPlayer) whichPlayer).addStateEvent(globalScope,
								whichTrigger, whichState, opcode, limitval.doubleValue()));
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterPlayerAllianceChange",
					(arguments, globalScope, triggerScope) -> {
						final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CPlayerJass whichPlayer = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						final CAllianceType whichAlliance = arguments.get(2).visit(ObjectJassValueVisitor.getInstance());
						if (!(whichPlayer instanceof CPlayer) || (whichAlliance == null)) {
							return eventType.getNullValue();
						}
						return new HandleJassValue(eventType,
								((CPlayer) whichPlayer).addAllianceEvent(globalScope, whichTrigger, whichAlliance));
					});
			// TODO past this point things are inconsistent about ordering
			jassProgramVisitor.getJassNativeManager().createNative("EnumDestructablesInRect",
					(arguments, globalScope, triggerScope) -> {
						final Rectangle rect = arguments.get(0).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
						final TriggerBooleanExpression filter = nullable(arguments, 1,
								ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
						final CodeJassValue actionFunc = nullable(arguments, 2, CodeJassValueVisitor.getInstance());
						CommonEnvironment.this.simulation.getWorldCollision().enumDestructablesInRect(rect, (unit) -> {
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

			jassProgramVisitor.getJassNativeManager().createNative("GetCameraMargin",
					(arguments, globalScope, triggerScope) -> {
						final int whichMargin = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final Rectangle playableMapArea = war3MapViewer.terrain.getPlayableMapArea();
						switch (whichMargin) {
						case 0:// CAMERA_MARGIN_LEFT
							return RealJassValue
									.of(war3MapViewer.terrain.getDefaultCameraBounds()[0] - playableMapArea.x);
						case 1:// CAMERA_MARGIN_RIGHT
							return RealJassValue.of((playableMapArea.x + playableMapArea.width)
									- war3MapViewer.terrain.getDefaultCameraBounds()[2]);
						case 2:// CAMERA_MARGIN_TOP
							return RealJassValue.of((playableMapArea.y + playableMapArea.height)
									- war3MapViewer.terrain.getDefaultCameraBounds()[3]);
						case 3:// CAMERA_MARGIN_BOTTOM
							return RealJassValue
									.of(war3MapViewer.terrain.getDefaultCameraBounds()[1] - playableMapArea.y);
						default:
							throw new IllegalArgumentException(
									"Must input one of these constants: [CAMERA_MARGIN_LEFT, CAMERA_MARGIN_RIGHT, CAMERA_MARGIN_TOP, CAMERA_MARGIN_BOTTOM]");
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetCameraBoundMinX",
					(arguments, globalScope, triggerScope) -> {
						return RealJassValue.of(meleeUI.getCameraManager().getCameraBounds().getX());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetCameraBoundMinY",
					(arguments, globalScope, triggerScope) -> {
						return RealJassValue.of(meleeUI.getCameraManager().getCameraBounds().getY());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetCameraBoundMaxX",
					(arguments, globalScope, triggerScope) -> {
						final Rectangle cameraBounds = meleeUI.getCameraManager().getCameraBounds();
						return RealJassValue.of(cameraBounds.getX() + cameraBounds.getWidth());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetCameraBoundMaxY",
					(arguments, globalScope, triggerScope) -> {
						final Rectangle cameraBounds = meleeUI.getCameraManager().getCameraBounds();
						return RealJassValue.of(cameraBounds.getY() + cameraBounds.getHeight());
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetCameraBounds",
					(arguments, globalScope, triggerScope) -> {
						final double x1 = arguments.get(0).visit(RealJassValueVisitor.getInstance());
						final double y1 = arguments.get(1).visit(RealJassValueVisitor.getInstance());
						final double x2 = arguments.get(2).visit(RealJassValueVisitor.getInstance());
						final double y2 = arguments.get(3).visit(RealJassValueVisitor.getInstance());
						final double x3 = arguments.get(4).visit(RealJassValueVisitor.getInstance());
						final double y3 = arguments.get(5).visit(RealJassValueVisitor.getInstance());
						final double x4 = arguments.get(6).visit(RealJassValueVisitor.getInstance());
						final double y4 = arguments.get(7).visit(RealJassValueVisitor.getInstance());
						final float left = (float) Math.min(Math.min(x1, x2), Math.min(x3, x4));
						final float bottom = (float) Math.min(Math.min(y1, y2), Math.min(y3, y4));
						final float right = (float) Math.max(Math.max(x1, x2), Math.max(x3, x4));
						final float top = (float) Math.max(Math.max(y1, y2), Math.max(y3, y4));
						meleeUI.getCameraManager()
								.setCameraBounds(new Rectangle(left, bottom, right - left, top - bottom));
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetDayNightModels",
					(arguments, globalScope, triggerScope) -> {
						final String terrainDNCFile = arguments.get(0).visit(StringJassValueVisitor.getInstance());
						final String unitDNCFile = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						war3MapViewer.setDayNightModels(terrainDNCFile, unitDNCFile);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetTerrainFogEx",
					(arguments, globalScope, triggerScope) -> {
						final Integer fogStyle = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final Double start = arguments.get(1).visit(RealJassValueVisitor.getInstance());
						final Double end = arguments.get(2).visit(RealJassValueVisitor.getInstance());
						final Double density = arguments.get(3).visit(RealJassValueVisitor.getInstance());
						final Double r = arguments.get(4).visit(RealJassValueVisitor.getInstance());
						final Double g = arguments.get(5).visit(RealJassValueVisitor.getInstance());
						final Double b = arguments.get(6).visit(RealJassValueVisitor.getInstance());
						war3MapViewer.worldScene.fogSettings.color = new Color(r.floatValue(), g.floatValue(),
								b.floatValue(), 1.0f);
						war3MapViewer.worldScene.fogSettings.density = density.floatValue();
						war3MapViewer.worldScene.fogSettings.start = start.floatValue();
						war3MapViewer.worldScene.fogSettings.end = end.floatValue();
						war3MapViewer.worldScene.fogSettings.setStyleByIndex(fogStyle + 1);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("ResetTerrainFog",
					(arguments, globalScope, triggerScope) -> {
						war3MapViewer.resetTerrainFog();
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("NewSoundEnvironment",
					(arguments, globalScope, triggerScope) -> {
						final String environmentName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
						System.err.println("#########");
						System.err.println("# Engine requested sound environment: " + environmentName);
						System.err.println("# I don't know how to do that on LibGDX, so for now, I do nothing!");
						System.err.println("#########");
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("CreateMIDISound",
					(arguments, globalScope, triggerScope) -> {
						final String soundLabel = arguments.get(0).visit(StringJassValueVisitor.getInstance());
						final int fadeInRate = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final int fadeOutRate = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						final UnitSound sound = war3MapViewer.getUiSounds().getSound(soundLabel);
						if ((sound == null) || (sound.getSoundCount() == 0)) {
							return new HandleJassValue(soundType, new CMIDISound(soundLabel, fadeInRate, fadeOutRate));
						}
						return new HandleJassValue(soundType, new CSoundFromLabel(sound,
								meleeUI.getUiScene().audioContext, true, false, false, fadeInRate, fadeOutRate));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetFloatGameState",
					(arguments, globalScope, triggerScope) -> {
						final CGameState gameState = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						switch (gameState) {
						case TIME_OF_DAY:
							return RealJassValue.of(CommonEnvironment.this.simulation.getGameTimeOfDay());
						}
						throw new IllegalArgumentException("Not a float game state: " + gameState);
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetIntegerGameState",
					(arguments, globalScope, triggerScope) -> {
						final CGameState gameState = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						switch (gameState) {
						case DISCONNECTED:
							return IntegerJassValue.of(CommonEnvironment.this.simulation.getDisconnectedGameState());
						case DIVINE_INTERVENTION:
							return IntegerJassValue
									.of(CommonEnvironment.this.simulation.getDivineInterventionGameState());
						}
						throw new IllegalArgumentException("Not an integer game state: " + gameState);
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetIntegerGameState",
					(arguments, globalScope, triggerScope) -> {
						final CGameState gameState = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int value = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						if (gameState == CGameState.DISCONNECTED) {
							CommonEnvironment.this.simulation.setDisconnectedGameState(value);
						}
						else if (gameState == CGameState.DIVINE_INTERVENTION) {
							CommonEnvironment.this.simulation.setDivineInterventionGameState(value);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetFloatGameState",
					(arguments, globalScope, triggerScope) -> {
						final CGameState gameState = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final float value = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						switch (gameState) {
						case TIME_OF_DAY:
							CommonEnvironment.this.simulation.setGameTimeOfDay(value);
							return null;
						}
						throw new IllegalArgumentException("Not a float game state: " + gameState);
					});
			jassProgramVisitor.getJassNativeManager().createNative("StartSound",
					(arguments, globalScope, triggerScope) -> {
						final CSound soundHandle = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (soundHandle != null) {
							soundHandle.start();
						}
						return null;
					});
			// Fading out is not supported yet; the sound stops right away.
			jassProgramVisitor.getJassNativeManager().createNative("StopSound",
					(arguments, globalScope, triggerScope) -> {
						final CSound soundHandle = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (soundHandle != null) {
							soundHandle.stop();
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetMapMusic",
					(arguments, globalScope, triggerScope) -> {
						final String musicName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
						final boolean random = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						final int index = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());

						final String musicField = CommonEnvironment.this.gameUI.trySkinField(musicName);
						meleeUI.setMapMusic(musicField, random, index);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("PlayMusic",
					(arguments, globalScope, triggerScope) -> {
						final String musicName = arguments.get(0).visit(StringJassValueVisitor.getInstance());

						final String musicField = CommonEnvironment.this.gameUI.trySkinField(musicName);
						meleeUI.playMusic(musicField, true, 0);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("PlayMusicEx",
					(arguments, globalScope, triggerScope) -> {
						final String musicName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
						final int frommsecs = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final int fadeInMSecs = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());

						final String musicField = CommonEnvironment.this.gameUI.trySkinField(musicName);
						meleeUI.playMusicEx(musicField, true, 0, frommsecs, fadeInMSecs);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("StopMusic",
					(arguments, globalScope, triggerScope) -> {
						final boolean fadeOut = arguments.get(0).visit(BooleanJassValueVisitor.getInstance());
						meleeUI.stopMusic(fadeOut);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("ResumeMusic",
					(arguments, globalScope, triggerScope) -> {
						final boolean fadeOut = arguments.get(0).visit(BooleanJassValueVisitor.getInstance());
						meleeUI.resumeMusic();
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("PlayThematicMusic",
					(arguments, globalScope, triggerScope) -> {
						final String musicName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
						final String musicField = CommonEnvironment.this.gameUI.trySkinField(musicName);
						meleeUI.playMusic(musicField, true, 0);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("PlayThematicMusicEx",
					(arguments, globalScope, triggerScope) -> {
						final String musicName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
						final String musicField = CommonEnvironment.this.gameUI.trySkinField(musicName);
						final int frommsecs = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						meleeUI.playMusicEx(musicField, true, 0, frommsecs, -1);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("EndThematicMusic",
					(arguments, globalScope, triggerScope) -> {
						meleeUI.stopMusic(false);
						meleeUI.playMapMusic();
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetMusicVolume",
					(arguments, globalScope, triggerScope) -> {
						final int volume = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						meleeUI.setMusicVolume(volume);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetMusicPlayPosition",
					(arguments, globalScope, triggerScope) -> {
						final int millisecs = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						meleeUI.setMusicPlayPosition(millisecs);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetThematicMusicPlayPosition",
					(arguments, globalScope, triggerScope) -> {
						final int millisecs = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						meleeUI.setMusicPlayPosition(millisecs);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetSoundDuration",
					(arguments, globalScope, triggerScope) -> {
						final CSound sound = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (sound == null) {
							return IntegerJassValue.ZERO;
						}
						return IntegerJassValue.of(Math.round(sound.getPredictedDuration() * 1000));
					});

			// text tags
			jassProgramVisitor.getJassNativeManager().createNative("CreateTextTag",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(texttagType, CommonEnvironment.this.simulation.createTextTag());
					});
			jassProgramVisitor.getJassNativeManager().createNative("DestroyTextTag",
					(arguments, globalScope, triggerScope) -> {
						final TextTag textTag = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (textTag != null) {
							this.simulation.destroyTextTag(textTag);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetTextTagText",
					(arguments, globalScope, triggerScope) -> {
						final TextTag textTag = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						String textValue = nullable(arguments, 1, StringJassValueVisitor.getInstance());
						textValue = war3MapViewer.getGameUI().getTrigStr(textValue);
						final float height = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						if (textTag != null) {
							textTag.setText(textValue);
							textTag.setFontHeight(height);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetTextTagPos",
					(arguments, globalScope, triggerScope) -> {
						final TextTag textTag = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float heightOffset = arguments.get(3).visit(RealJassValueVisitor.getInstance())
								.floatValue();
						if (textTag != null) {
							textTag.setPosition(x, y, war3MapViewer.terrain.getGroundHeight(x, y) + heightOffset);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetTextTagPosUnit",
					(arguments, globalScope, triggerScope) -> {
						final TextTag textTag = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CUnit whichUnit = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						final float heightOffset = arguments.get(2).visit(RealJassValueVisitor.getInstance())
								.floatValue();
						if ((textTag != null) && (whichUnit != null)) {
							final float x = whichUnit.getX();
							final float y = whichUnit.getY();
							final float flyHeight = whichUnit.getFlyHeight();
							// NOTE: lep jassdoc on github says that the below sum also adds max Z extent of
							// whichUnit model, but according to their notes that value changes from
							// bloodlust but not SetUnitScale, some crazy bugs, we are not reproducing those
							// at the moment
							final RenderUnit renderPeer = war3MapViewer.getRenderPeer(whichUnit);
							final Bounds bounds = renderPeer == null ? null : renderPeer.instance.getBounds();
							float estimatedBoundsMaxZ = 0;
							if (bounds != null) {
								estimatedBoundsMaxZ = bounds.getEstimatedMaxZ();
							}
							textTag.setPosition(x, y, war3MapViewer.terrain.getGroundHeight(x, y) + flyHeight
									+ estimatedBoundsMaxZ + heightOffset);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetTextTagColor",
					(arguments, globalScope, triggerScope) -> {
						final TextTag textTag = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int red = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final int green = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						final int blue = arguments.get(3).visit(IntegerJassValueVisitor.getInstance());
						final int alpha = arguments.get(4).visit(IntegerJassValueVisitor.getInstance());
						if (textTag != null) {
							textTag.setColor(red, green, blue, alpha);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetTextTagVelocity",
					(arguments, globalScope, triggerScope) -> {
						final TextTag textTag = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final float xVelocity = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float yVelocity = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						if (textTag != null) {
							textTag.setVelocity(xVelocity, yVelocity);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetTextTagVisibility",
					(arguments, globalScope, triggerScope) -> {
						final TextTag textTag = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean visiblity = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						if (textTag != null) {
							textTag.setVisible(visiblity);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetTextTagSuspended",
					(arguments, globalScope, triggerScope) -> {
						final TextTag textTag = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						if (textTag != null) {
							textTag.setSuspended(flag);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetTextTagPermanent",
					(arguments, globalScope, triggerScope) -> {
						final TextTag textTag = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						if (textTag != null) {
							textTag.setPermanent(flag);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetTextTagAge",
					(arguments, globalScope, triggerScope) -> {
						final TextTag textTag = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final float age = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						if (textTag != null) {
							textTag.setLifetime(age);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetTextTagLifespan",
					(arguments, globalScope, triggerScope) -> {
						final TextTag textTag = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final float lifespan = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						if (textTag != null) {
							textTag.setLifetimeDuration(lifespan);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetTextTagFadepoint",
					(arguments, globalScope, triggerScope) -> {
						final TextTag textTag = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final float fadepoint = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						if (textTag != null) {
							textTag.setFadeStart(fadepoint);
						}
						return null;
					});

			jassProgramVisitor.getJassNativeManager().createNative("CreateItem",
					(arguments, globalScope, triggerScope) -> {
						final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final double x = arguments.get(1).visit(RealJassValueVisitor.getInstance());
						final double y = arguments.get(2).visit(RealJassValueVisitor.getInstance());
						return new HandleJassValue(itemType, CommonEnvironment.this.simulation
								.createItem(new War3ID(rawcode), (float) x, (float) y));
					});
			jassProgramVisitor.getJassNativeManager().createNative("BlzCreateItemWithSkin",
					(arguments, globalScope, triggerScope) -> {
						final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final double x = arguments.get(1).visit(RealJassValueVisitor.getInstance());
						final double y = arguments.get(2).visit(RealJassValueVisitor.getInstance());
						return new HandleJassValue(itemType, CommonEnvironment.this.simulation
								.createItem(new War3ID(rawcode), (float) x, (float) y));
					});
			jassProgramVisitor.getJassNativeManager().createNative("RemoveItem",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichItem = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (whichItem != null) {
							CommonEnvironment.this.simulation.removeItem(whichItem);
							meleeUI.removedItem(whichItem);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetItemPlayer",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichItem = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (whichItem == null) {
							return IntegerJassValue.of(0);
						}
						final CUnit containedUnit = whichItem.getContainedUnit();
						if (containedUnit != null) {
							return new HandleJassValue(playerType,
									this.simulation.getPlayer(containedUnit.getPlayerIndex()));
						}
						final int itemPlayerIndex = whichItem.getPlayerIndex();
						return new HandleJassValue(playerType, this.simulation.getPlayer(
								itemPlayerIndex < 0 ? WarsmashConstants.MAX_PLAYERS - 1 : itemPlayerIndex));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetItemTypeId",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichItem = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if ((whichItem == null) || (whichItem.isDead() && whichItem.isHidden())) {
							return IntegerJassValue.of(0);
						}
						return IntegerJassValue.of(whichItem.getTypeId().getValue());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetItemX",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichWidget = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return RealJassValue.of(whichWidget == null ? 0 : whichWidget.getX());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetItemY",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichWidget = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return RealJassValue.of(whichWidget == null ? 0 : whichWidget.getY());
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetItemPosition",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichWidget = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						if (whichWidget != null) {
							whichWidget.forceDropIfHeld(this.simulation);
							whichWidget.setPointAndCheckUnstuck(x, y, this.simulation);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetItemDropOnDeath",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichWidget = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						if (whichWidget != null) {
							whichWidget.setDropOnDeath(flag);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetItemDroppable",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichWidget = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						if (whichWidget != null) {
							whichWidget.setDroppable(flag);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetItemPawnable",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichWidget = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						if (whichWidget != null) {
							whichWidget.setPawnable(flag);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetItemInvulnerable",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichWidget = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						if (whichWidget != null) {
							whichWidget.setInvulernable(flag);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsItemInvulnerable",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichItem = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (whichItem == null) {
							return BooleanJassValue.FALSE;
						}
						return BooleanJassValue.of(whichItem.isInvulnerable());
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetItemVisible",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichWidget = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						if (whichWidget != null) {
							whichWidget.setHidden(!flag);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsItemVisible",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichItem = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (whichItem == null) {
							return BooleanJassValue.FALSE;
						}
						return BooleanJassValue.of(!whichItem.isHidden());
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsItemPowerup",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichItem = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (whichItem == null) {
							return BooleanJassValue.FALSE;
						}
						return BooleanJassValue.of(whichItem.getItemType().isUseAutomaticallyWhenAcquired());
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsItemSellable",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichItem = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (whichItem == null) {
							return BooleanJassValue.FALSE;
						}
						return BooleanJassValue.of(whichItem.getItemType().isSellable());
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsItemPawnable",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichItem = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (whichItem == null) {
							return BooleanJassValue.FALSE;
						}
						return BooleanJassValue.of(whichItem.getItemType().isPawnable());
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsItemIdPowerup",
					(arguments, globalScope, triggerScope) -> {
						final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final CItemType itemClass = this.simulation.getItemData().getItemType(new War3ID(rawcode));
						if (itemClass == null) {
							return BooleanJassValue.FALSE;
						}
						return BooleanJassValue.of(itemClass.isUseAutomaticallyWhenAcquired());
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsItemIdSellable",
					(arguments, globalScope, triggerScope) -> {
						final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final CItemType itemClass = this.simulation.getItemData().getItemType(new War3ID(rawcode));
						if (itemClass == null) {
							return BooleanJassValue.FALSE;
						}
						return BooleanJassValue.of(itemClass.isSellable());
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsItemIdPawnable",
					(arguments, globalScope, triggerScope) -> {
						final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final CItemType itemClass = this.simulation.getItemData().getItemType(new War3ID(rawcode));
						if (itemClass == null) {
							return BooleanJassValue.FALSE;
						}
						return BooleanJassValue.of(itemClass.isPawnable());
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsItemIdPerishable",
					(arguments, globalScope, triggerScope) -> {
						final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final CItemType itemClass = this.simulation.getItemData().getItemType(new War3ID(rawcode));
						if (itemClass == null) {
							return BooleanJassValue.FALSE;
						}
						return BooleanJassValue.of(itemClass.isPerishable());
					});
			jassProgramVisitor.getJassNativeManager().createNative("EnumItemsInRect",
					(arguments, globalScope, triggerScope) -> {
						final Rectangle rect = arguments.get(0).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
						final TriggerBooleanExpression filter = nullable(arguments, 1,
								ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
						final CodeJassValue actionFunc = nullable(arguments, 2, CodeJassValueVisitor.getInstance());
						CommonEnvironment.this.simulation.getWorldCollision().enumItemsInRect(rect, (unit) -> {
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
			jassProgramVisitor.getJassNativeManager().createNative("GetItemLevel",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichItem = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (whichItem == null) {
							return IntegerJassValue.of(0);
						}
						return IntegerJassValue.of(whichItem.getItemType().getLevel());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetItemType",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichItem = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (whichItem == null) {
							return itemtypeType.getNullValue();
						}
						return new HandleJassValue(itemtypeType, whichItem.getItemType().getItemClass());
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetItemDropID",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichItem = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int unitId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						if (whichItem != null) {
							whichItem.setDropId(new War3ID(unitId));
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetItemDropID",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichItem = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (whichItem == null) {
							return IntegerJassValue.of(0);
						}
						return IntegerJassValue.of(whichItem.getDropId().getValue());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetItemName",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichItem = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (whichItem == null) {
							return JassType.STRING.getNullValue();
						}
						final ItemUI itemUI = war3MapViewer.getAbilityDataUI().getItemUI(whichItem.getTypeId());
						return new StringJassValue(itemUI.getName());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetItemCharges",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichItem = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (whichItem == null) {
							return IntegerJassValue.of(0);
						}
						return IntegerJassValue.of(whichItem.getCharges());
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetItemCharges",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichItem = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int charges = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						if (whichItem != null) {
							whichItem.setCharges(charges);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetItemUserData",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichItem = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (whichItem == null) {
							return IntegerJassValue.of(0);
						}
						return IntegerJassValue.of(whichItem.getUserData());
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetItemUserData",
					(arguments, globalScope, triggerScope) -> {
						final CItem whichItem = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int value = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						if (whichItem != null) {
							whichItem.setUserData(value);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitAddItemById",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int rawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());

						final CItem newItem = CommonEnvironment.this.simulation.createItem(new War3ID(rawcode),
								unit.getX(), unit.getY());
						final CAbilityInventory inventoryData = unit.getInventoryData();
						if ((inventoryData != null) && (newItem != null)) {
							inventoryData.giveItem(CommonEnvironment.this.simulation, unit, newItem, false);
						}
						return new HandleJassValue(itemType, newItem);
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitAddItemToSlotById",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int rawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final int slot = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());

						final CItem newItem = CommonEnvironment.this.simulation.createItem(new War3ID(rawcode),
								unit.getX(), unit.getY());
						final CAbilityInventory inventoryData = unit.getInventoryData();
						if ((inventoryData != null) && (newItem != null)) {
							inventoryData.giveItem(CommonEnvironment.this.simulation, unit, newItem, slot, false);
						}

						return new HandleJassValue(itemType, newItem);
					});
			jassProgramVisitor.getJassNativeManager().createNative("ChooseRandomCreep",
					(arguments, globalScope, triggerScope) -> {
						final int level = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final War3ID creepId = CommonEnvironment.this.simulation.getUnitData().chooseRandomCreep(level,
								CommonEnvironment.this.simulation.getSeededRandom());
						return IntegerJassValue.of(creepId == null ? 0 : creepId.getValue());
					});
			jassProgramVisitor.getJassNativeManager().createNative("ChooseRandomNPBuilding",
					(arguments, globalScope, triggerScope) -> {
						final War3ID buildingId = CommonEnvironment.this.simulation.getUnitData()
								.chooseRandomNPBuilding(CommonEnvironment.this.simulation.getSeededRandom());
						return IntegerJassValue.of(buildingId == null ? 0 : buildingId.getValue());
					});
			jassProgramVisitor.getJassNativeManager().createNative("ChooseRandomItem",
					(arguments, globalScope, triggerScope) -> {
						final int level = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final War3ID randomItemId = CommonEnvironment.this.simulation.getItemData()
								.chooseRandomItem(level, CommonEnvironment.this.simulation.getSeededRandom());
						return IntegerJassValue.of(randomItemId == null ? 0 : randomItemId.getValue());
					});
			jassProgramVisitor.getJassNativeManager().createNative("ChooseRandomItemEx",
					(arguments, globalScope, triggerScope) -> {
						final CItemTypeJass whichType = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int level = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final War3ID randomItemId = CommonEnvironment.this.simulation.getItemData().chooseRandomItem(
								whichType, level, CommonEnvironment.this.simulation.getSeededRandom());
						return IntegerJassValue.of(randomItemId == null ? 0 : randomItemId.getValue());
					});
			jassProgramVisitor.getJassNativeManager().createNative("CreateDestructable",
					(arguments, globalScope, triggerScope) -> {
						final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float facing = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float scale = arguments.get(4).visit(RealJassValueVisitor.getInstance()).floatValue();
						final int variation = arguments.get(5).visit(IntegerJassValueVisitor.getInstance());
						return new HandleJassValue(destructableType, CommonEnvironment.this.simulation
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
						return new HandleJassValue(destructableType, CommonEnvironment.this.simulation
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
						return new HandleJassValue(destructableType, CommonEnvironment.this.simulation
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
							? CommonEnvironment.this.simulation.createDestructableZ(new War3ID(rawcode), x, y, z, facing,
									scale, variation)
							: CommonEnvironment.this.simulation.createDestructable(new War3ID(rawcode), x, y, facing,
									scale, variation);
					return new HandleJassValue(destructableType, destructable);
				});
			}
			jassProgramVisitor.getJassNativeManager().createNative("KillDestructable",
					(arguments, globalScope, triggerScope) -> {
						final CDestructable dest = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						dest.setLife(CommonEnvironment.this.simulation, 0f);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("RemoveDestructable",
					(arguments, globalScope, triggerScope) -> {
						final CDestructable dest = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						dest.setLife(CommonEnvironment.this.simulation, 0f);
						this.simulation.removeDestructable(dest);
						return null;
					});

			final JassFunction createUnitFxn = (arguments, globalScope, triggerScope) -> {
				final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
				final int rawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
				final double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
				final double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());
				final double facing = arguments.get(4).visit(RealJassValueVisitor.getInstance());
				if (player == null) {
					return unitType.getNullValue();
				}
				final War3ID rawcodeId = new War3ID(rawcode);
				final CUnit newUnit = CommonEnvironment.this.simulation.createUnitSimple(rawcodeId, player.getId(),
						(float) x, (float) y, (float) facing);
				return new HandleJassValue(unitType, newUnit);
			};
			jassProgramVisitor.getJassNativeManager().createNative("CreateUnit", createUnitFxn);
			unitType.setConstructorNative(new HandleJassTypeConstructor("CreateUnit"));
			jassProgramVisitor.getJassNativeManager().createNative("CreateUnitByName",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final String legacyName = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						final double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
						final double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());
						final double facing = arguments.get(4).visit(RealJassValueVisitor.getInstance());
						if (player == null) {
							return unitType.getNullValue();
						}
						final CUnitType unitTypeByJassLegacyName = this.simulation.getUnitData()
								.getUnitTypeByJassLegacyName(legacyName);
						final CUnit newUnit = CommonEnvironment.this.simulation.createUnitSimple(
								unitTypeByJassLegacyName.getTypeId(), player.getId(), (float) x, (float) y,
								(float) facing);
						return new HandleJassValue(unitType, newUnit);
					});
			jassProgramVisitor.getJassNativeManager().createNative("CreateUnitAtLoc",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int rawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final AbilityPointTarget whichLocation = nullable(arguments, 2,
								ObjectJassValueVisitor.getInstance());
						final float facing = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
						if (player == null) {
							return unitType.getNullValue();
						}
						final War3ID rawcodeId = new War3ID(rawcode);
						final float x = whichLocation == null ? 0 : (float) whichLocation.x;
						final float y = whichLocation == null ? 0 : (float) whichLocation.y;
						final CUnit newUnit = CommonEnvironment.this.simulation.createUnitSimple(rawcodeId,
								player.getId(), x, y, facing);
						return new HandleJassValue(unitType, newUnit);
					});
			jassProgramVisitor.getJassNativeManager().createNative("CreateUnitAtLocByName",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final String legacyName = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						final AbilityPointTarget whichLocation = nullable(arguments, 2,
								ObjectJassValueVisitor.getInstance());
						final float facing = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
						if (player == null) {
							return unitType.getNullValue();
						}
						final CUnitType unitTypeByJassLegacyName = this.simulation.getUnitData()
								.getUnitTypeByJassLegacyName(legacyName);
						final float x = whichLocation == null ? 0 : (float) whichLocation.x;
						final float y = whichLocation == null ? 0 : (float) whichLocation.y;
						final CUnit newUnit = CommonEnvironment.this.simulation
								.createUnitSimple(unitTypeByJassLegacyName.getTypeId(), player.getId(), x, y, facing);
						return new HandleJassValue(unitType, newUnit);
					});
			jassProgramVisitor.getJassNativeManager().createNative("CreateCorpse",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int rawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
						final double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());
						final double facing = arguments.get(4).visit(RealJassValueVisitor.getInstance());
						if (player == null) {
							return unitType.getNullValue();
						}
						final War3ID rawcodeId = new War3ID(rawcode);
						final CUnit newUnit = CommonEnvironment.this.simulation.createUnit(rawcodeId, player.getId(),
								(float) x, (float) y, (float) facing);
						if (newUnit != null) {
							newUnit.becomeCorpse(CommonEnvironment.this.simulation);
						}
						return new HandleJassValue(unitType, newUnit);
					});
			jassProgramVisitor.getJassNativeManager().createNative("BlzCreateUnitWithSkin",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						int rawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
						final double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());
						final double facing = arguments.get(4).visit(RealJassValueVisitor.getInstance());
						final int skinId = arguments.get(5).visit(IntegerJassValueVisitor.getInstance());
						final War3ID rawcodeId = new War3ID(rawcode);
						final CUnit newUnit = CommonEnvironment.this.simulation.createUnitSimple(rawcodeId,
								player.getId(), (float) x, (float) y, (float) facing);
						if (skinId != rawcode) {
							// throw new IllegalStateException("Our engine does not support UnitSkinID !=
							// UnitID (skinId=" + new War3ID(skinId) + ", unitId=" + new War3ID(rawcode) +
							// ")");
							rawcode = skinId;
						}
						return new HandleJassValue(unitType, newUnit);
					});

			jassProgramVisitor.getJassNativeManager().createNative("KillUnit",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (whichUnit != null) {
							whichUnit.kill(CommonEnvironment.this.simulation);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("RemoveUnit",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (whichUnit != null) {
							// A channel in progress ends as interrupted, as on death, so what it made
							// (locusts, a blizzard's waves) does not outlive the removed caster.
							if (!whichUnit.isDead()) {
								whichUnit.beginBehavior(CommonEnvironment.this.simulation, null, true);
							}
							CommonEnvironment.this.simulation.removeUnit(whichUnit);
							meleeUI.removedUnit(whichUnit);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("ShowUnit",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean show = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						if (whichUnit != null) {
							whichUnit.setHidden(!show);
						}
						return null;
					});

			jassProgramVisitor.getJassNativeManager().createNative("SetUnitState",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CUnitState whichUnitState = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						final float value = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						if (whichUnit != null) {
							whichUnit.setUnitState(CommonEnvironment.this.simulation, whichUnitState, value);
						}
						else {
							System.err.println("got SetUnitState(null," + whichUnitState + "," + value
									+ ")  call (skipping because unit is null)");
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetWidgetLife",
					(arguments, globalScope, triggerScope) -> {
						final CWidget whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final float value = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						if (whichUnit != null) {
							whichUnit.setLife(this.simulation, value);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetWidgetLife",
					(arguments, globalScope, triggerScope) -> {
						final CWidget whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (whichUnit != null) {
							return RealJassValue.of(whichUnit.getLife());
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitX",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final float positionX = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						if (whichUnit != null) {
							whichUnit.setX(positionX, this.simulation.getWorldCollision(),
									this.simulation.getRegionManager());
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitY",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final float positionX = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						if (whichUnit != null) {
							whichUnit.setY(positionX, this.simulation.getWorldCollision(),
									this.simulation.getRegionManager());
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitPosition",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final double positionX = arguments.get(1).visit(RealJassValueVisitor.getInstance());
						final double positionY = arguments.get(2).visit(RealJassValueVisitor.getInstance());
						if (whichUnit != null) {
							whichUnit.setPointAndCheckUnstuck((float) positionX, (float) positionY,
									CommonEnvironment.this.simulation);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitPositionLoc",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final AbilityPointTarget positionLoc = arguments.get(1)
								.visit(ObjectJassValueVisitor.getInstance());
						if (whichUnit != null) {
							whichUnit.setPointAndCheckUnstuck(positionLoc.x, positionLoc.y,
									CommonEnvironment.this.simulation);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitFacing",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final double facing = arguments.get(1).visit(RealJassValueVisitor.getInstance());
						if (whichUnit != null) {
							whichUnit.setFacing((float) facing);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitFacingTimed",
					(arguments, globalScope, triggerScope) -> {
						// TODO this needs to apply the time delay
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final double facing = arguments.get(1).visit(RealJassValueVisitor.getInstance());
						if (whichUnit != null) {
							whichUnit.setFacing((float) facing);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitMoveSpeed",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final double newSpeed = arguments.get(1).visit(RealJassValueVisitor.getInstance());
						if (whichUnit != null) {
							whichUnit.setSpeed((int) newSpeed);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitFlyHeight",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final float newHeight = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						// TODO rate
						final double rate = arguments.get(2).visit(RealJassValueVisitor.getInstance());
						if (whichUnit != null) {
							whichUnit.setFlyHeight(newHeight);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitTurnSpeed",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final float newSpeed = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						if (whichUnit != null) {
							whichUnit.setTurnRate(newSpeed);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitPropWindow",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final float newAngle = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						if (whichUnit != null) {
							whichUnit.setPropWindow(newAngle);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitAcquireRange",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final double range = arguments.get(1).visit(RealJassValueVisitor.getInstance());
						// The original game ignores a range of 50 or less (measured); the unit keeps the range it had.
						if ((unit != null) && (range > 50)) {
							unit.setAcquisitionRange((float) range);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitAcquireRange",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (unit != null) {
							return RealJassValue.of(unit.getAcquisitionRange());
						}
						return RealJassValue.ZERO;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitTurnSpeed",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (unit != null) {
							return RealJassValue.of(unit.getTurnRate());
						}
						return RealJassValue.ZERO;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitPropWindow",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (unit != null) {
							return RealJassValue.of(unit.getPropWindow());
						}
						return RealJassValue.ZERO;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitFlyHeight",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = inGame(nullable(arguments, 0, ObjectJassValueVisitor.<CUnit>getInstance()));
						if (unit != null) {
							final CBuffEnsnare snare = CBuffEnsnare.getFlyingSnare(unit);
							return RealJassValue.of(snare != null ? snare.getScriptFlyHeight() : unit.getFlyHeight());
						}
						return RealJassValue.ZERO;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitDefaultAcquireRange",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (unit != null) {
							return RealJassValue.of(unit.getUnitType().getDefaultAcquisitionRange());
						}
						return RealJassValue.ZERO;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitDefaultTurnSpeed",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (unit != null) {
							return RealJassValue.of(unit.getUnitType().getTurnRate());
						}
						return RealJassValue.ZERO;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitDefaultPropWindow",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (unit != null) {
							return RealJassValue.of(unit.getUnitType().getPropWindow());
						}
						return RealJassValue.ZERO;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitDefaultFlyHeight",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (unit != null) {
							return RealJassValue.of(unit.getUnitType().getDefaultFlyingHeight());
						}
						return RealJassValue.ZERO;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitOwner",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CPlayer whichPlayer = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						final boolean changeColor = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());

						if ((whichUnit != null) && (whichPlayer != null)) {
							whichUnit.changeOwner(CommonEnvironment.this.simulation, whichPlayer.getId(), changeColor);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitColor",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final Object whichPlayerColor = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						final RenderUnit renderPeer = war3MapViewer.getRenderPeer(whichUnit);
						if ((renderPeer != null) && (whichPlayerColor instanceof CPlayerColor)) {
							renderPeer.setPlayerColor(((CPlayerColor) whichPlayerColor).ordinal());
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitScale",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final float scaleX = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float scaleY = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float scaleZ = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
						final RenderUnit renderPeer = war3MapViewer.getRenderPeer(whichUnit);
						if (renderPeer != null) {
							renderPeer.instance.setUniformScale(scaleX);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitTimeScale",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final float timeScale = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final RenderUnit renderPeer = war3MapViewer.getRenderPeer(whichUnit);
						if (renderPeer != null) {
							renderPeer.instance.setAnimationSpeed(timeScale);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitBlendTime",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final float blendTime = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final RenderUnit renderPeer = war3MapViewer.getRenderPeer(whichUnit);
						if (renderPeer != null) {
							renderPeer.instance.setBlendTime(blendTime);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitVertexColor",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int red = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final int green = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						final int blue = arguments.get(3).visit(IntegerJassValueVisitor.getInstance());
						final int alpha = arguments.get(4).visit(IntegerJassValueVisitor.getInstance());
						final RenderUnit renderPeer = war3MapViewer.getRenderPeer(whichUnit);
						if (renderPeer != null) {
							renderPeer.setVertexColoring(war3MapViewer, red / 255f, green / 255f, blue / 255f,
									alpha / 255f);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("QueueUnitAnimation",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final String whichAnimation = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						final RenderUnit renderPeer = war3MapViewer.getRenderPeer(whichUnit);
						if (renderPeer != null) {
							renderPeer.queueAnimation(whichAnimation);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitAnimation",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final String whichAnimation = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						final RenderUnit renderPeer = war3MapViewer.getRenderPeer(whichUnit);
						if (renderPeer != null) {
							renderPeer.playAnimation(whichAnimation);
						}
						if (whichUnit != null) {
							whichUnit.holdScriptAnimation();
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("ResetUnitAnimation",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final RenderUnit renderPeer = whichUnit == null ? null : war3MapViewer.getRenderPeer(whichUnit);
						if (renderPeer != null) {
							renderPeer.resetAnimation();
						}
						if (whichUnit != null) {
							whichUnit.releaseScriptAnimation();
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitAnimationByIndex",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int whichAnimation = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final RenderUnit renderPeer = war3MapViewer.getRenderPeer(whichUnit);
						if (renderPeer != null) {
							renderPeer.playAnimation(whichAnimation);
						}
						if (whichUnit != null) {
							whichUnit.holdScriptAnimation();
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitAnimationWithRarity",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final String whichAnimation = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						final CRarityControl rarityControl = nullable(arguments, 2,
								ObjectJassValueVisitor.getInstance());
						final RenderUnit renderPeer = war3MapViewer.getRenderPeer(whichUnit);
						if (renderPeer != null) {
							renderPeer.playAnimationWithRarity(whichAnimation, rarityControl);
						}
						if (whichUnit != null) {
							whichUnit.holdScriptAnimation();
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("AddUnitAnimationProperties",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final String animProperties = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						final boolean add = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
						final RenderUnit renderPeer = war3MapViewer.getRenderPeer(whichUnit);
						if (renderPeer != null) {
							renderPeer.addAnimationProperties(animProperties, add);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitLookAt",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final String boneNameString = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						final CUnit lookAtTarget = arguments.get(2).visit(ObjectJassValueVisitor.getInstance());
						final float offsetX = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float offsetY = arguments.get(4).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float offsetZ = arguments.get(5).visit(RealJassValueVisitor.getInstance()).floatValue();
						final RenderUnit renderPeer = war3MapViewer.getRenderPeer(whichUnit);
						final RenderUnit renderPeerTarget = war3MapViewer.getRenderPeer(lookAtTarget);
						if ((renderPeer != null) && (renderPeerTarget != null)) {
							renderPeer.lockTargetFacing(boneNameString, renderPeerTarget, offsetX, offsetY, offsetZ);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("ResetUnitLookAt",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final RenderUnit renderPeer = war3MapViewer.getRenderPeer(whichUnit);
						if (renderPeer != null) {
							renderPeer.resetLookAt();
						}
						return null;
					});

			jassProgramVisitor.getJassNativeManager().createNative("CreateBlightedGoldmine",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final double x = arguments.get(1).visit(RealJassValueVisitor.getInstance());
						final double y = arguments.get(2).visit(RealJassValueVisitor.getInstance());
						final double facing = arguments.get(3).visit(RealJassValueVisitor.getInstance());
						final War3ID blightedMineRawcode = War3ID.fromString("ugol");
						final War3ID goldMineRawcode = War3ID.fromString("ngol");
						final CUnit blightedMine = CommonEnvironment.this.simulation.createUnitSimple(
								blightedMineRawcode, player.getId(), (float) x, (float) y, (float) facing);
						// Not createUnitSimple: the blighted mine already stands here, so the hidden one would be moved
						// off it and its pathing would block building around the Necropolis.
						final CUnit goldMine = CommonEnvironment.this.simulation.createUnit(goldMineRawcode,
								WarsmashConstants.MAX_PLAYERS - 1, blightedMine.getX(), blightedMine.getY(),
								(float) facing);
						goldMine.setHidden(true);
						for (final CAbility ability : blightedMine.getAbilities()) {
							if (ability instanceof CAbilityBlightedGoldMine) {
								((CAbilityBlightedGoldMine) ability).setParentMine(goldMine,
										goldMine.getGoldMineData());
							}
						}
						return new HandleJassValue(unitType, blightedMine);
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetResourceAmount",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int resourceAmount = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						whichUnit.setGold(resourceAmount);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("AddResourceAmount",
					(final List<JassValue> arguments, final GlobalScope globalScope,
							final TriggerExecutionScope triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int resourceAmount = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						whichUnit.setGold(whichUnit.getGold() + resourceAmount);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetResourceAmount",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						return IntegerJassValue.of(whichUnit.getGold());
					});
			jassProgramVisitor.getJassNativeManager().createNative("WaygateGetDestinationX",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (whichUnit != null) {
							final CAbilityWayGate wayGateAbility = whichUnit
									.getFirstAbilityOfType(CAbilityWayGate.class);
							if (wayGateAbility != null) {
								return RealJassValue.of(wayGateAbility.getDestination().x);
							}
						}
						return RealJassValue.ZERO;
					});
			jassProgramVisitor.getJassNativeManager().createNative("WaygateGetDestinationY",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (whichUnit != null) {
							final CAbilityWayGate wayGateAbility = whichUnit
									.getFirstAbilityOfType(CAbilityWayGate.class);
							if (wayGateAbility != null) {
								return RealJassValue.of(wayGateAbility.getDestination().y);
							}
						}
						return RealJassValue.ZERO;
					});
			jassProgramVisitor.getJassNativeManager().createNative("WaygateSetDestination",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						if (whichUnit != null) {
							final CAbilityWayGate wayGateAbility = whichUnit
									.getFirstAbilityOfType(CAbilityWayGate.class);
							if (wayGateAbility != null) {
								wayGateAbility.setDestination(new AbilityPointTarget(x, y));
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("WaygateActivate",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (whichUnit != null) {
							final CAbilityWayGate wayGateAbility = whichUnit
									.getFirstAbilityOfType(CAbilityWayGate.class);
							if (wayGateAbility != null) {
								wayGateAbility.setGateEnabled(true);
								whichUnit.getUnitAnimationListener().addSecondaryTag(SecondaryTag.ALTERNATE);
								whichUnit.getUnitAnimationListener().forceResetCurrentAnimation();
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("WaygateIsActive",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (whichUnit != null) {
							final CAbilityWayGate wayGateAbility = whichUnit
									.getFirstAbilityOfType(CAbilityWayGate.class);
							if (wayGateAbility != null) {
								return BooleanJassValue.of(wayGateAbility.isGateEnabled());
							}
						}
						return BooleanJassValue.FALSE;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitState",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = inGame(nullable(arguments, 0, ObjectJassValueVisitor.<CUnit>getInstance()));
						final CUnitState whichUnitState = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						if (whichUnit == null) {
							return RealJassValue.ZERO;
						}
						return RealJassValue
								.of(whichUnit.getUnitState(CommonEnvironment.this.simulation, whichUnitState));
					});
			jassProgramVisitor.getJassNativeManager().createNative("AddHeroXP",
					(arguments, globalScope, triggerScope) -> {
						// Todo add showEyeCandy boolean and update addXp fn and upstream fns to make
						// hero level up fx suppressable
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int xp = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final boolean showEyeCandy = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
						final CAbilityHero heroData = scriptHeroData(whichUnit);
						if (heroData != null) {
							heroData.addXp(CommonEnvironment.this.simulation, whichUnit, xp, showEyeCandy);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetWidgetLife",
					(arguments, globalScope, triggerScope) -> {
						final CWidget whichWidget = inGame(nullable(arguments, 0, ObjectJassValueVisitor.getInstance()));
						return new RealJassValue(whichWidget == null ? 0 : whichWidget.getLife());
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetWidgetLife",
					(arguments, globalScope, triggerScope) -> {
						final CWidget whichWidget = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (whichWidget != null) {
							whichWidget.setLife(CommonEnvironment.this.simulation,
									arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue());
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetHeroXP",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CAbilityHero heroData = whichUnit == null ? null : scriptHeroData(whichUnit);
						return IntegerJassValue.of(heroData == null ? 0 : heroData.getXp());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetHeroLevel",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = inGame(nullable(arguments, 0, ObjectJassValueVisitor.<CUnit>getInstance()));
						final CAbilityHero heroData = whichUnit == null ? null : scriptHeroData(whichUnit);
						return IntegerJassValue.of(heroData == null ? 0 : heroData.getHeroLevel());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitLevel",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = inGame(nullable(arguments, 0, ObjectJassValueVisitor.<CUnit>getInstance()));
						if (whichUnit == null) {
							return IntegerJassValue.ZERO;
						}
						final CAbilityHero heroData = scriptHeroData(whichUnit);
						return IntegerJassValue.of(
								heroData != null ? heroData.getHeroLevel() : whichUnit.getUnitType().getLevel());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetHeroSkillPoints",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CAbilityHero heroData = whichUnit == null ? null : scriptHeroData(whichUnit);
						return IntegerJassValue.of(heroData == null ? 0 : heroData.getSkillPoints());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetHeroStr",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean includeBonuses = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						final CAbilityHero heroData = whichUnit == null ? null : scriptHeroData(whichUnit);
						if (heroData == null) {
							return IntegerJassValue.ZERO;
						}
						return IntegerJassValue.of(includeBonuses ? heroData.getStrength().getCurrent()
								: heroData.getStrength().getCurrentBase());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetHeroAgi",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean includeBonuses = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						final CAbilityHero heroData = whichUnit == null ? null : scriptHeroData(whichUnit);
						if (heroData == null) {
							return IntegerJassValue.ZERO;
						}
						return IntegerJassValue.of(includeBonuses ? heroData.getAgility().getCurrent()
								: heroData.getAgility().getCurrentBase());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetHeroInt",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean includeBonuses = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						final CAbilityHero heroData = whichUnit == null ? null : scriptHeroData(whichUnit);
						if (heroData == null) {
							return IntegerJassValue.ZERO;
						}
						return IntegerJassValue.of(includeBonuses ? heroData.getIntelligence().getCurrent()
								: heroData.getIntelligence().getCurrentBase());
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetHeroXP",
					(arguments, globalScope, triggerScope) -> {
						// Todo add showEyeCandy boolean and update addXp fn and upstream fns to make
						// hero level up fx suppressable
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int xp = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final boolean showEyeCandy = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
						final CAbilityHero heroData = whichUnit.getHeroData();
						if (heroData != null) {
							heroData.setXp(CommonEnvironment.this.simulation, whichUnit, xp, showEyeCandy);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetHeroLevel",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int level = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final boolean fx = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
						final CAbilityHero heroData = whichUnit.getHeroData();
						if (heroData != null) {
							// Measured: the original treats level 1 as level 2, so a new hero set to level 1 gains a level.
							heroData.setHeroLevel(CommonEnvironment.this.simulation, whichUnit, Math.max(2, level), fx);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SelectHeroSkill",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int skill = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final CAbilityHero heroData = scriptHeroData(whichUnit);
						if (heroData != null) {
							heroData.selectHeroSkill(this.simulation, whichUnit, new War3ID(skill));
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetHeroStr",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int str = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final boolean permanent = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
						final CAbilityHero heroData = whichUnit.getHeroData();
						if (heroData != null) {
							if (permanent) {
								heroData.setStrengthBase(CommonEnvironment.this.simulation, whichUnit,
										str - (heroData.getStrength().getCurrentBase() - heroData.getStrength().getBase()));
							} // Todo add else case to handle non-permanent
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetHeroAgi",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int agi = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final boolean permanent = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
						final CAbilityHero heroData = whichUnit.getHeroData();
						if (heroData != null) {
							if (permanent) {
								heroData.setAgilityBase(CommonEnvironment.this.simulation, whichUnit,
										agi - (heroData.getAgility().getCurrentBase() - heroData.getAgility().getBase()));
							} // Todo add else case to handle non-permanent
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetHeroInt",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int intelligence = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final boolean permanent = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
						final CAbilityHero heroData = whichUnit.getHeroData();
						if (heroData != null) {
							if (permanent) {
								heroData.setIntelligenceBase(CommonEnvironment.this.simulation, whichUnit,
										intelligence - (heroData.getIntelligence().getCurrentBase() - heroData.getIntelligence().getBase()));
							} // Todo add else case to handle non-permanent
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitType",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (whichUnit != null) {
							final CUnitTypeJass whichUnitType = arguments.get(1)
									.visit(ObjectJassValueVisitor.getInstance());
							return BooleanJassValue.of(CBuffEnsnare.isScriptUnitType(whichUnit, whichUnitType));
						}
						return BooleanJassValue.FALSE;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetPlayerState",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CPlayerState whichPlayerState = arguments.get(1)
								.visit(ObjectJassValueVisitor.getInstance());
						final int value = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						if ((player != null) && (whichPlayerState != null)) {
							player.setPlayerState(CommonEnvironment.this.simulation, whichPlayerState, value);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitInTransport",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CUnit whichTransport = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichUnit != null) && (whichTransport != null)
								&& (whichTransport.getCargoData() != null)
								&& whichTransport.getCargoData().contains(whichUnit));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitLoaded",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (whichUnit != null) {
							for (final CUnit transport : CommonEnvironment.this.simulation.getUnitsIncludingNew()) {
								if ((transport.getCargoData() != null) && transport.getCargoData().contains(whichUnit)) {
									return BooleanJassValue.TRUE;
								}
							}
						}
						return BooleanJassValue.FALSE;
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitApplyTimedLife",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int buffId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final float duration = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						if ((whichUnit != null) && !whichUnit.isDead()) {
							for (final CAbility ability : new ArrayList<>(whichUnit.getAbilities())) {
								if (ability instanceof CBuffTimedLife) {
									((CBuffTimedLife) ability).markReplaced();
									whichUnit.remove(this.simulation, ability);
								}
							}
							// The original game counts these units as summoned (half experience when killed).
							whichUnit.addClassification(CUnitClassification.SUMMONED);
							whichUnit.add(this.simulation, new CBuffTimedLife(
									this.simulation.getHandleIdAllocator().createId(), new War3ID(buffId), duration,
									false, false));
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitPauseTimedLife",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						if (whichUnit != null) {
							for (final CAbility ability : whichUnit.getAbilities()) {
								if (ability instanceof CBuffTimedLife) {
									((CBuffTimedLife) ability).setTimerPaused(flag);
								}
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitPaused",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichUnit != null) && whichUnit.isPaused());
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitRemoveBuffs",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean removePositive = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						final boolean removeNegative = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
						for (final CBuff buff : matchingBuffs(whichUnit, removePositive, removeNegative, true, true,
								false, false, false)) {
							if (buff instanceof CBuffTimedLife) {
								((CBuffTimedLife) buff).markReplaced();
							}
							whichUnit.remove(this.simulation, buff);
						}
						// Measured on 1.29: stripping an illusion of its buffs ends it.
						if ((whichUnit != null) && whichUnit.isIllusion() && removePositive) {
							whichUnit.kill(this.simulation);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitRemoveBuffsEx",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						for (final CBuff buff : matchingBuffs(whichUnit, arguments, 1)) {
							if (buff instanceof CBuffTimedLife) {
								((CBuffTimedLife) buff).markReplaced();
							}
							whichUnit.remove(this.simulation, buff);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitHasBuffsEx",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of(!matchingBuffs(whichUnit, arguments, 1).isEmpty());
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitCountBuffsEx",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return IntegerJassValue.of(matchingBuffs(whichUnit, arguments, 1).size());
					});
			jassProgramVisitor.getJassNativeManager().createNative("SuspendHeroXP",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichHero = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						if ((whichHero != null) && (whichHero.getHeroData() != null)) {
							whichHero.getHeroData().setXpSuspended(flag);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsSuspendedXP",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichHero = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichHero != null) && (whichHero.getHeroData() != null)
								&& whichHero.getHeroData().isXpSuspended());
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitModifySkillPoints",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichHero = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int delta = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						if ((whichHero == null) || (whichHero.getHeroData() == null)) {
							return BooleanJassValue.FALSE;
						}
						final CAbilityHero heroData = whichHero.getHeroData();
						final int newPoints = Math.max(0, heroData.getSkillPoints() + delta);
						final boolean changed = newPoints != heroData.getSkillPoints();
						heroData.setSkillPoints(newPoints);
						whichHero.internalPublishHeroStatsChanged();
						return BooleanJassValue.of(changed);
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitUseFood",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean useFood = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						if (whichUnit != null) {
							final CPlayer player = this.simulation.getPlayer(whichUnit.getPlayerIndex());
							player.setUnitFoodUsed(whichUnit, useFood ? whichUnit.getUnitType().getFoodUsed() : 0);
							player.setUnitFoodMade(whichUnit, useFood ? whichUnit.getUnitType().getFoodMade() : 0);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetPlayerHandicapXP",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer whichPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						// 1.29 reads 1.0 (no handicap) for a null player (null-players-d320).
						return RealJassValue.of(whichPlayer == null ? 1.0 : whichPlayer.getHandicapXP());
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetPlayerHandicap",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer whichPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final float handicap = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						if (whichPlayer == null) {
							return null;
						}
						whichPlayer.setHandicap(handicap);
						for (final CUnit unit : this.simulation.getUnitsIncludingNew()) {
							if (unit.getPlayerIndex() == whichPlayer.getId()) {
								unit.setHandicap(handicap);
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitDropItemSlot",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CItem whichItem = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						final int slot = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichUnit != null) && (whichUnit.getInventoryData() != null)
								&& whichUnit.getInventoryData().moveItemToSlot(whichItem, slot));
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitRemoveItemFromSlot",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int slot = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final CAbilityInventory inventoryData = whichUnit == null ? null
								: whichUnit.getInventoryData();
						if ((inventoryData == null) || (slot < 0) || (slot >= inventoryData.getItemCapacity())
								|| (inventoryData.getItemInSlot(slot) == null)) {
							return new HandleJassValue(itemType, null);
						}
						final CItem item = inventoryData.getItemInSlot(slot);
						inventoryData.dropItem(this.simulation, whichUnit, slot, whichUnit.getX(), whichUnit.getY(),
								false);
						return new HandleJassValue(itemType, item);
					});
			jassProgramVisitor.getJassNativeManager().createNative("PauseGame",
					(arguments, globalScope, triggerScope) -> {
						final boolean flag = arguments.get(0).visit(BooleanJassValueVisitor.getInstance());
						war3MapViewer.setGamePaused(War3MapViewer.PauseSource.SCRIPT, flag);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("CripplePlayer",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer whichPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final List<CPlayerJass> toWhichPlayers = arguments.get(1)
								.visit(ObjectJassValueVisitor.<List<CPlayerJass>>getInstance());
						final boolean flag = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
						if (whichPlayer != null) {
							crippleExpose(whichPlayer.getId(), toWhichPlayers, flag);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("CachePlayerHeroData",
					(arguments, globalScope, triggerScope) -> {
						// Only feeds the campaign score screen, which this engine does not have.
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("RemovePlayer",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CPlayerGameResult result = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						if (player.getSlotState() != CPlayerSlotState.PLAYING) {
							// The melee scripts remove an observer once more when the game ends, as a neutral result.
							if (player.getRemovedResult() == null) {
								player.setRemovedResult(result);
							}
							return null;
						}
						player.setRemovedResult(result);
						player.setSlotState(CPlayerSlotState.LEFT);
						if (player.getId() != war3MapViewer.getLocalPlayerIndex()) {
							player.firePlayerEvents(CommonTriggerExecutionScope::triggerPlayerScope,
									JassGameEventsWar3.EVENT_PLAYER_LEAVE);
						}
						else if ((result == CPlayerGameResult.DEFEAT)
								&& war3MapViewer.getMapConfig().isMapFlagSet(CMapFlag.MAP_OBSERVERS_ON_DEATH)) {
							// 1.29 reports IsPlayerObserver false right after RemovePlayer and true at the next check.
							final CTimer becomeObserver = new CTimer() {
								@Override
								public void onFire(final CSimulation simulation) {
									player.setPlayerState(simulation, CPlayerState.OBSERVER, 1);
									war3MapViewer.startObserverView();
								}
							};
							becomeObserver.setTimeoutTime(0.05f);
							becomeObserver.start(CommonEnvironment.this.simulation);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetPlayerState",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CPlayerState whichPlayerState = arguments.get(1)
								.visit(ObjectJassValueVisitor.getInstance());
						if ((player == null) || (whichPlayerState == null)) {
							return IntegerJassValue.ZERO;
						}
						return IntegerJassValue
								.of(player.getPlayerState(CommonEnvironment.this.simulation, whichPlayerState));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetPlayerScore",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CPlayerScore whichScore = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						if ((player == null) || (whichScore == null)) {
							return IntegerJassValue.ZERO;
						}
						return IntegerJassValue.of(player.getScore(whichScore));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitFoodUsed",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return IntegerJassValue.of(whichUnit != null ? whichUnit.getFoodUsed() : 0);
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitFoodMade",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return IntegerJassValue.of(whichUnit != null ? whichUnit.getFoodMade() : 0);
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetFoodMade",
					(arguments, globalScope, triggerScope) -> {
						final Integer id = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final War3ID war3id = new War3ID(id);
						final CUnitType t = CommonEnvironment.this.simulation.getUnitData().getUnitType(war3id);
						return IntegerJassValue.of(t != null ? t.getFoodMade() : 0);
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetFoodUsed",
					(arguments, globalScope, triggerScope) -> {
						final Integer id = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final War3ID war3id = new War3ID(id);
						final CUnitType t = CommonEnvironment.this.simulation.getUnitData().getUnitType(war3id);
						return IntegerJassValue.of(t != null ? t.getFoodUsed() : 0);
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitFacing",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = inGame(nullable(arguments, 0, ObjectJassValueVisitor.<CUnit>getInstance()));
						return RealJassValue.of(whichUnit != null ? whichUnit.getFacing() : 0.0);
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitMoveSpeed",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = inGame(nullable(arguments, 0, ObjectJassValueVisitor.<CUnit>getInstance()));
						// A unit that cannot move, such as a building, reads 0 as in the original game.
						return RealJassValue.of((whichUnit != null) && (whichUnit.getMoveBehavior() != null)
								? whichUnit.getSpeed()
								: 0.0);
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitRace",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CRace whichRace = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());

						if (whichUnit != null) {
							final CUnitType t = whichUnit.getUnitType();

							return new BooleanJassValue(t.getRace().equals(whichRace));
						}

						return JassType.BOOLEAN.getNullValue();
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnit", (arguments, globalScope, triggerScope) -> {
				final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
				final CUnit whichSpecifiedUnit = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());

				if ((whichUnit != null) && (whichSpecifiedUnit != null)) {
					return new BooleanJassValue(whichUnit.getHandleId() == whichSpecifiedUnit.getHandleId());
				}

				return JassType.BOOLEAN.getNullValue();
			});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitInRange",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CUnit otherUnit = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						final Double distance = arguments.get(2).visit(RealJassValueVisitor.getInstance());

						if ((whichUnit != null) && (otherUnit != null) && (distance != null)
								&& (whichUnit.distance(otherUnit.getX(), otherUnit.getY()) <= distance)) {
							return new BooleanJassValue(true);
						}

						return JassType.BOOLEAN.getNullValue();
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitInRangeXY",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final Double x = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						final Double y = arguments.get(2).visit(ObjectJassValueVisitor.getInstance());
						final Double distance = arguments.get(3).visit(RealJassValueVisitor.getInstance());

						if ((whichUnit != null) && (x != null) && (y != null) && (distance != null)
								&& (whichUnit.distance(x, y) <= distance)) {
							return new BooleanJassValue(true);
						}

						return JassType.BOOLEAN.getNullValue();
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitInRangeLoc",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final AbilityPointTarget whichLocation = nullable(arguments, 1,
								ObjectJassValueVisitor.getInstance());
						final Double distance = arguments.get(2).visit(RealJassValueVisitor.getInstance());

						if ((whichUnit != null) && (whichLocation != null) && (distance != null)
								&& (whichUnit.distance(whichLocation.x, whichLocation.y) <= distance)) {
							return new BooleanJassValue(true);
						}

						return JassType.BOOLEAN.getNullValue();
					});

			// Bit Operations
			jassProgramVisitor.getJassNativeManager().createNative("BlzBitOr",
					(arguments, globalScope, triggerScope) -> {
						final Integer x = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final Integer y = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());

						return IntegerJassValue.of(x | y);
					});
			jassProgramVisitor.getJassNativeManager().createNative("BlzBitAnd",
					(arguments, globalScope, triggerScope) -> {
						final Integer x = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final Integer y = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());

						return IntegerJassValue.of(x & y);
					});
			jassProgramVisitor.getJassNativeManager().createNative("BlzBitXor",
					(arguments, globalScope, triggerScope) -> {
						final Integer x = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final Integer y = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());

						return IntegerJassValue.of(x ^ y);
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetPlayerTechResearched",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int techIdRawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final boolean specificOnly = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
						return BooleanJassValue.of((player != null) && (player.getTechtreeUnlocked(new War3ID(techIdRawcode)) > 0));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetPlayerTechCount",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int techIdRawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final boolean specificOnly = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
						if (player != null) {
							return IntegerJassValue.of(player.getTechtreeUnlocked(new War3ID(techIdRawcode)));
						}
						return IntegerJassValue.ZERO;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetPlayerTechResearched",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int techIdRawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final int setToLevel = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						final War3ID techIdRawcodeId = new War3ID(techIdRawcode);
						final CUpgradeType upgradeType = CommonEnvironment.this.simulation.getUpgradeData()
								.getType(techIdRawcodeId);
						if ((player != null) && (upgradeType != null)) {
							player.setTechResearched(CommonEnvironment.this.simulation, techIdRawcodeId, setToLevel);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("AddPlayerTechResearched",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int techIdRawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final int levels = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						final War3ID techIdRawcodeId = new War3ID(techIdRawcode);
						final CUpgradeType upgradeType = CommonEnvironment.this.simulation.getUpgradeData()
								.getType(techIdRawcodeId);
						if ((player != null) && (upgradeType != null)) {
							player.addTechResearched(CommonEnvironment.this.simulation, techIdRawcodeId, levels);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetPlayerTechMaxAllowed",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int techIdRawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final int maximum = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						if (player != null) {
							player.setTechtreeMaxAllowed(new War3ID(techIdRawcode), maximum);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetPlayerTechMaxAllowed",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int techIdRawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						if (player == null) {
							return IntegerJassValue.ZERO;
						}
						final int maximum = player.getTechtreeMaxAllowed(new War3ID(techIdRawcode));
						return IntegerJassValue.of(maximum < 0 ? Integer.MAX_VALUE : maximum);
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetPlayerAbilityAvailable",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int abilityIdRawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final boolean enabled = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
						if (player != null) {
							player.setAbilityEnabled(this.simulation, new War3ID(abilityIdRawcode), enabled);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetFogStateRect",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CFogState whichState = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						final Rectangle whichRect = arguments.get(2).visit(ObjectJassValueVisitor.getInstance());
						final boolean sharedVision = arguments.get(3).visit(BooleanJassValueVisitor.getInstance());

						if (player == null) {
							return null;
						}
						final PathingGrid pathingGrid = this.simulation.getPathingGrid();
						if (sharedVision) {
							for (int playerIndex = 0; playerIndex < WarsmashConstants.MAX_PLAYERS; playerIndex++) {
								if (player.hasAlliance(playerIndex, CAllianceType.SHARED_VISION)
										|| player.hasAlliance(playerIndex, CAllianceType.SHARED_VISION_FORCED)) {
									final CPlayer alliedPlayer = this.simulation.getPlayer(playerIndex);
									alliedPlayer.getFogOfWar().setFogStateRect(pathingGrid, whichRect, whichState);
								}
							}
						}
						else {
							player.getFogOfWar().setFogStateRect(pathingGrid, whichRect, whichState);
						}

						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetFogStateRadius",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CFogState whichState = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						final float centerX = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float centerY = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float radius = arguments.get(4).visit(RealJassValueVisitor.getInstance()).floatValue();
						final boolean sharedVision = arguments.get(5).visit(BooleanJassValueVisitor.getInstance());

						if (player == null) {
							return null;
						}
						final PathingGrid pathingGrid = this.simulation.getPathingGrid();
						if (sharedVision) {
							for (int playerIndex = 0; playerIndex < WarsmashConstants.MAX_PLAYERS; playerIndex++) {
								if (player.hasAlliance(playerIndex, CAllianceType.SHARED_VISION)
										|| player.hasAlliance(playerIndex, CAllianceType.SHARED_VISION_FORCED)) {
									final CPlayer alliedPlayer = this.simulation.getPlayer(playerIndex);
									alliedPlayer.getFogOfWar().setFogStateRadius(pathingGrid, centerX, centerY, radius,
											whichState);
								}
							}
						}
						else {
							player.getFogOfWar().setFogStateRadius(pathingGrid, centerX, centerY, radius, whichState);
						}

						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetFogStateRadiusLoc",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CFogState whichState = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						final AbilityPointTarget center = arguments.get(2).visit(ObjectJassValueVisitor.getInstance());
						final float radius = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
						final boolean sharedVision = arguments.get(4).visit(BooleanJassValueVisitor.getInstance());

						final float centerX = center.x;
						final float centerY = center.y;

						if (player == null) {
							return null;
						}
						final PathingGrid pathingGrid = this.simulation.getPathingGrid();
						if (sharedVision) {
							for (int playerIndex = 0; playerIndex < WarsmashConstants.MAX_PLAYERS; playerIndex++) {
								if (player.hasAlliance(playerIndex, CAllianceType.SHARED_VISION)
										|| player.hasAlliance(playerIndex, CAllianceType.SHARED_VISION_FORCED)) {
									final CPlayer alliedPlayer = this.simulation.getPlayer(playerIndex);
									alliedPlayer.getFogOfWar().setFogStateRadius(pathingGrid, centerX, centerY, radius,
											whichState);
								}
							}
						}
						else {
							player.getFogOfWar().setFogStateRadius(pathingGrid, centerX, centerY, radius, whichState);
						}

						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("FogMaskEnable",
					(arguments, globalScope, triggerScope) -> {
						final boolean enable = arguments.get(0).visit(BooleanJassValueVisitor.getInstance());
						this.simulation.setFogMaskEnabled(enable);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("FogEnable",
					(arguments, globalScope, triggerScope) -> {
						final boolean enable = arguments.get(0).visit(BooleanJassValueVisitor.getInstance());
						if (enable && !this.simulation.isFogEnabled()) {
							// Measured on 1.29: turning fog back on fogs what was seen until the next fog update.
							for (int i = 0; i < WarsmashConstants.MAX_PLAYERS; i++) {
								this.simulation.getPlayer(i).getFogOfWar().convertVisibleToFogged();
							}
						}
						this.simulation.setFogEnabled(enable);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsFogEnabled",
					(arguments, globalScope, triggerScope) -> {
						return BooleanJassValue.of(this.simulation.isFogEnabled());
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsFogMaskEnabled",
					(arguments, globalScope, triggerScope) -> {
						return BooleanJassValue.of(this.simulation.isFogMaskEnabled());
					});
			jassProgramVisitor.getJassNativeManager().createNative("CreateFogModifierRect",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final Object whichStateValue = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						if ((player != null) && (whichStateValue instanceof UnknownEnumHandle)) {
							// 1.29 (convert-enum-d348) makes a modifier for a fog state it has no constant for; it changes nothing.
							return newFogModifier(fogmodifierType, new CFogModifierJassMulti(new ArrayList<>()));
						}
						final CFogState whichState = (CFogState) whichStateValue;
						final Rectangle whichRect = arguments.get(2).visit(ObjectJassValueVisitor.getInstance());
						// TODO share vision thing will be busted after change of player alliance
						final boolean sharedVision = arguments.get(3).visit(BooleanJassValueVisitor.getInstance());
						final boolean afterUnits = arguments.get(4).visit(BooleanJassValueVisitor.getInstance());

						final CRectFogModifier fogModifier = new CRectFogModifier(whichState, whichRect);
						if (player == null) {
							return fogmodifierType.getNullValue();
						}
						fogModifier.setEnabled(false);
						if (sharedVision) {
							final List<CFogModifierJass> modifiers = new ArrayList<>();
							for (int playerIndex = 0; playerIndex < WarsmashConstants.MAX_PLAYERS; playerIndex++) {
								if (player.hasAlliance(playerIndex, CAllianceType.SHARED_VISION)
										|| player.hasAlliance(playerIndex, CAllianceType.SHARED_VISION_FORCED)) {
									final CPlayer alliedPlayer = this.simulation.getPlayer(playerIndex);
									alliedPlayer.addFogModifer(this.simulation, fogModifier, afterUnits);
									modifiers.add(new CFogModifierJassSingle(alliedPlayer.getId(), fogModifier));
								}
							}
							return newFogModifier(fogmodifierType, new CFogModifierJassMulti(modifiers));
						}
						else {
							player.addFogModifer(this.simulation, fogModifier, afterUnits);
							return newFogModifier(fogmodifierType,
									new CFogModifierJassSingle(player.getId(), fogModifier));
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("CreateFogModifierRadius",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final Object whichStateValue = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						if ((player != null) && (whichStateValue instanceof UnknownEnumHandle)) {
							// 1.29 (convert-enum-d348) makes a modifier for a fog state it has no constant for; it changes nothing.
							return newFogModifier(fogmodifierType, new CFogModifierJassMulti(new ArrayList<>()));
						}
						final CFogState whichState = (CFogState) whichStateValue;
						final float centerX = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float centerY = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float radius = arguments.get(4).visit(RealJassValueVisitor.getInstance()).floatValue();
						// TODO share vision thing will be busted after change of player alliance
						final boolean sharedVision = arguments.get(5).visit(BooleanJassValueVisitor.getInstance());
						final boolean afterUnits = arguments.get(6).visit(BooleanJassValueVisitor.getInstance());

						final CCircleFogModifier fogModifier = new CCircleFogModifier(whichState, radius, centerX,
								centerY);
						if (player == null) {
							return fogmodifierType.getNullValue();
						}
						fogModifier.setEnabled(false);
						if (sharedVision) {
							final List<CFogModifierJass> modifiers = new ArrayList<>();
							for (int playerIndex = 0; playerIndex < WarsmashConstants.MAX_PLAYERS; playerIndex++) {
								if (player.hasAlliance(playerIndex, CAllianceType.SHARED_VISION)
										|| player.hasAlliance(playerIndex, CAllianceType.SHARED_VISION_FORCED)) {
									final CPlayer alliedPlayer = this.simulation.getPlayer(playerIndex);
									alliedPlayer.addFogModifer(this.simulation, fogModifier, afterUnits);
									modifiers.add(new CFogModifierJassSingle(alliedPlayer.getId(), fogModifier));
								}
							}
							return newFogModifier(fogmodifierType, new CFogModifierJassMulti(modifiers));
						}
						else {
							player.addFogModifer(this.simulation, fogModifier, afterUnits);
							return newFogModifier(fogmodifierType,
									new CFogModifierJassSingle(player.getId(), fogModifier));
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("CreateFogModifierRadiusLoc",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final Object whichStateValue = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						if ((player != null) && (whichStateValue instanceof UnknownEnumHandle)) {
							// 1.29 (convert-enum-d348) makes a modifier for a fog state it has no constant for; it changes nothing.
							return newFogModifier(fogmodifierType, new CFogModifierJassMulti(new ArrayList<>()));
						}
						final CFogState whichState = (CFogState) whichStateValue;
						final AbilityPointTarget center = arguments.get(2).visit(ObjectJassValueVisitor.getInstance());
						final float radius = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
						// TODO share vision thing will be busted after change of player alliance
						final boolean sharedVision = arguments.get(4).visit(BooleanJassValueVisitor.getInstance());
						final boolean afterUnits = arguments.get(5).visit(BooleanJassValueVisitor.getInstance());
						final float centerX = center.x;
						final float centerY = center.y;

						final CCircleFogModifier fogModifier = new CCircleFogModifier(whichState, radius, centerX,
								centerY);
						if (player == null) {
							return fogmodifierType.getNullValue();
						}
						fogModifier.setEnabled(false);
						if (sharedVision) {
							final List<CFogModifierJass> modifiers = new ArrayList<>();
							for (int playerIndex = 0; playerIndex < WarsmashConstants.MAX_PLAYERS; playerIndex++) {
								if (player.hasAlliance(playerIndex, CAllianceType.SHARED_VISION)
										|| player.hasAlliance(playerIndex, CAllianceType.SHARED_VISION_FORCED)) {
									final CPlayer alliedPlayer = this.simulation.getPlayer(playerIndex);
									alliedPlayer.addFogModifer(this.simulation, fogModifier, afterUnits);
									modifiers.add(new CFogModifierJassSingle(alliedPlayer.getId(), fogModifier));
								}
							}
							return newFogModifier(fogmodifierType, new CFogModifierJassMulti(modifiers));
						}
						else {
							player.addFogModifer(this.simulation, fogModifier, afterUnits);
							return newFogModifier(fogmodifierType,
									new CFogModifierJassSingle(player.getId(), fogModifier));
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("DestroyFogModifier",
					(arguments, globalScope, triggerScope) -> {
						final CFogModifierJass modifier = nullable(arguments, 0,
								ObjectJassValueVisitor.<CFogModifierJass>getInstance());
						if (modifier != null) {
							modifier.destroy(this.simulation);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("FogModifierStart",
					(arguments, globalScope, triggerScope) -> {
						final CFogModifierJass modifier = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						modifier.setEnabled(true);
						modifier.applyNow(this.simulation);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("FogModifierStop",
					(arguments, globalScope, triggerScope) -> {
						final CFogModifierJass modifier = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						modifier.setEnabled(false);
						modifier.fogVisibleCells(this.simulation);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetTerrainCliffLevel",
					(arguments, globalScope, triggerScope) -> {
						final float x = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final int layerHeight = war3MapViewer.terrain.getCorner(x, y).getLayerHeight();
						return IntegerJassValue.of(layerHeight);
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetWaterBaseColor",
					(arguments, globalScope, triggerScope) -> {
						final int red = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final int green = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final int blue = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						final int alpha = arguments.get(3).visit(IntegerJassValueVisitor.getInstance());
						war3MapViewer.setWaterBaseColor(
								new float[] { red / 255f, green / 255f, blue / 255f, alpha / 255f });
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("CreateSoundFromLabel",
					(arguments, globalScope, triggerScope) -> {
						final String soundLabel = arguments.get(0).visit(StringJassValueVisitor.getInstance());
						final boolean looping = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						final boolean is3D = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
						final boolean stopWhenOutOfRange = arguments.get(3)
								.visit(BooleanJassValueVisitor.getInstance());
						final int fadeInRate = arguments.get(4).visit(IntegerJassValueVisitor.getInstance());
						final int fadeOutRate = arguments.get(5).visit(IntegerJassValueVisitor.getInstance());
						final UnitSound sound = war3MapViewer.getUiSounds().getSound(soundLabel);
						return new HandleJassValue(soundType,
								new CSoundFromLabel(sound,
										is3D ? war3MapViewer.worldScene.audioContext
												: meleeUI.getUiScene().audioContext,
										looping, is3D, stopWhenOutOfRange, fadeInRate, fadeOutRate));
					});
			jassProgramVisitor.getJassNativeManager().createNative("CreateSound",
					(arguments, globalScope, triggerScope) -> {
						final String fileName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
						final boolean looping = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						final boolean is3D = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
						final boolean stopWhenOutOfRange = arguments.get(3)
								.visit(BooleanJassValueVisitor.getInstance());
						final int fadeInRate = arguments.get(4).visit(IntegerJassValueVisitor.getInstance());
						final int fadeOutRate = arguments.get(5).visit(IntegerJassValueVisitor.getInstance());
						final String eaxSetting = arguments.get(6).visit(StringJassValueVisitor.getInstance());
						final Sound newSound = UnitSound.createSound(war3MapViewer.mapMpq, fileName);
						war3MapViewer.rememberFileSound(newSound, fileName);
						return new HandleJassValue(soundType,
								new CSoundFilename(newSound,
										is3D ? war3MapViewer.worldScene.audioContext
												: meleeUI.getUiScene().audioContext,
										looping, stopWhenOutOfRange, fadeInRate, fadeOutRate, eaxSetting));
					});
			jassProgramVisitor.getJassNativeManager().createNative("VersionGet",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(versionType, CVersion.VALUES[WarsmashConstants.MAP_GAME_VERSION]);
					});
			jassProgramVisitor.getJassNativeManager().createNative("VersionCompatible",
					(arguments, globalScope, triggerScope) -> {
						final CVersion version = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue
								.of((version != null) && (version.ordinal() <= WarsmashConstants.MAP_GAME_VERSION));
					});
			jassProgramVisitor.getJassNativeManager().createNative("VersionSupported",
					(arguments, globalScope, triggerScope) -> {
						final CVersion version = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of(version != null);
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetAllItemTypeSlots",
					(arguments, globalScope, triggerScope) -> {
						final int slots = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						CommonEnvironment.this.simulation.setAllItemTypeSlots(slots);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetAllUnitTypeSlots",
					(arguments, globalScope, triggerScope) -> {
						final int slots = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						CommonEnvironment.this.simulation.setAllUnitTypeSlots(slots);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetItemTypeSlots",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int slots = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						if (whichUnit != null) {
							CommonEnvironment.this.simulation.getItemStock(whichUnit, true).setItemTypeSlots(slots);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitTypeSlots",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int slots = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						if (whichUnit != null) {
							CommonEnvironment.this.simulation.getUnitStock(whichUnit, true).setUnitTypeSlots(slots);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("AddItemToStock",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int itemId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final int currentStock = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						final int stockMax = arguments.get(3).visit(IntegerJassValueVisitor.getInstance());
						final CSimulation game = CommonEnvironment.this.simulation;
						if (whichUnit != null) {
							game.getItemStock(whichUnit, true).addToStock(game, new War3ID(itemId), currentStock,
									stockMax);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("AddUnitToStock",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int unitId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final int currentStock = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						final int stockMax = arguments.get(3).visit(IntegerJassValueVisitor.getInstance());
						final CSimulation game = CommonEnvironment.this.simulation;
						if (whichUnit != null) {
							game.getUnitStock(whichUnit, true).addToStock(game, new War3ID(unitId), currentStock,
									stockMax);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("AddItemToAllStock",
					(arguments, globalScope, triggerScope) -> {
						final int itemId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final int currentStock = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final int stockMax = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						final CSimulation game = CommonEnvironment.this.simulation;
						for (final CUnit shop : game.getScriptStockedShops(SELL_ITEMS_ABILITY_ID)) {
							game.getItemStock(shop, true).addToStock(game, new War3ID(itemId), currentStock, stockMax);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("AddUnitToAllStock",
					(arguments, globalScope, triggerScope) -> {
						final int unitId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final int currentStock = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final int stockMax = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						final CSimulation game = CommonEnvironment.this.simulation;
						for (final CUnit shop : game.getScriptStockedShops(SELL_UNITS_ABILITY_ID)) {
							game.getUnitStock(shop, true).addToStock(game, new War3ID(unitId), currentStock, stockMax);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("RemoveItemFromStock",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int itemId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						if (whichUnit != null) {
							final CAbilitySellItems stock = CommonEnvironment.this.simulation.getItemStock(whichUnit,
									false);
							if (stock != null) {
								stock.removeFromStock(new War3ID(itemId));
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("RemoveUnitFromStock",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int unitId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						if (whichUnit != null) {
							final CAbilitySellUnits stock = CommonEnvironment.this.simulation.getUnitStock(whichUnit,
									false);
							if (stock != null) {
								stock.removeFromStock(new War3ID(unitId));
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("RemoveItemFromAllStock",
					(arguments, globalScope, triggerScope) -> {
						final int itemId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final CSimulation game = CommonEnvironment.this.simulation;
						for (final CUnit shop : game.getScriptStockedShops(SELL_ITEMS_ABILITY_ID)) {
							final CAbilitySellItems stock = game.getItemStock(shop, false);
							if (stock != null) {
								stock.removeFromStock(new War3ID(itemId));
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("RemoveUnitFromAllStock",
					(arguments, globalScope, triggerScope) -> {
						final int unitId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final CSimulation game = CommonEnvironment.this.simulation;
						for (final CUnit shop : game.getScriptStockedShops(SELL_UNITS_ABILITY_ID)) {
							final CAbilitySellUnits stock = game.getUnitStock(shop, false);
							if (stock != null) {
								stock.removeFromStock(new War3ID(unitId));
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterPlayerUnitEvent",
					(arguments, globalScope, triggerScope) -> {
						final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CPlayer whichPlayer = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						final JassGameEventsWar3 whichPlayerEvent = arguments.get(2)
								.visit(ObjectJassValueVisitor.getInstance());
						final TriggerBooleanExpression filter = nullable(arguments, 3,
								ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
						if (whichPlayer == null) {
							return eventType.getNullValue();
						}
						return new HandleJassValue(eventType,
								whichPlayer.addUnitEvent(globalScope, whichTrigger, whichPlayerEvent, filter));
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerEvaluate",
					(arguments, globalScope, triggerScope) -> {
						final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (whichTrigger == null) {
							return BooleanJassValue.FALSE;
						}
						return BooleanJassValue.of(whichTrigger.evaluate(globalScope,
								new CommonTriggerExecutionScope(whichTrigger, triggerScope)));
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerExecute",
					(arguments, globalScope, triggerScope) -> {
						final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (whichTrigger != null) {
							whichTrigger.execute(globalScope,
									new CommonTriggerExecutionScope(whichTrigger, triggerScope));
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerExecuteWait",
					(arguments, globalScope, triggerScope) -> {
						final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (whichTrigger != null) {
							final JassThread caller = globalScope.getCurrentThread();
							final JassThread triggerThread = whichTrigger.execute(globalScope,
									new CommonTriggerExecutionScope(whichTrigger, triggerScope));
							if ((triggerThread != null) && (caller != null) && !caller.isSleepEndsThread()) {
								Trigger.wakeWhenDone(triggerThread, caller);
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("Preload", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final String filename = nullable(arguments, 0, StringJassValueVisitor.getInstance());
					// Preload lists also name sounds and Reforged-only formats (.tif, .pkfx) that
					// have no viewer handler; sounds are loaded on demand when played.
					if ((filename != null) && (CommonEnvironment.this.preloadGenLines != null)) {
						CommonEnvironment.this.preloadGenLines.add(filename);
					}
					if ((filename != null) && hasViewerHandler(war3MapViewer, filename.trim())) {
						try {
							war3MapViewer.load(filename.trim(), war3MapViewer.mapPathSolver,
									war3MapViewer.solverParams);
						}
						catch (final Exception exc) {
							System.err.println("Preload(\"" + filename + "\") failed!");
							exc.printStackTrace();
						}
					}
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("PreloadStart",
					(arguments, globalScope, triggerScope) -> {
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("PreloadGenStart",
					(arguments, globalScope, triggerScope) -> {
						this.preloadGenLines = new ArrayList<>();
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("PreloadGenClear",
					(arguments, globalScope, triggerScope) -> {
						if (this.preloadGenLines != null) {
							this.preloadGenLines.clear();
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("PreloadGenEnd",
					(arguments, globalScope, triggerScope) -> {
						final String filename = nullable(arguments, 0, StringJassValueVisitor.getInstance());
						if ((filename != null) && (this.preloadGenLines != null)) {
							writePreloadFile(filename, this.preloadGenLines);
						}
						this.preloadGenLines = null;
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("EndGame",
					(arguments, globalScope, triggerScope) -> {
						meleeUI.endGame(arguments.get(0).visit(BooleanJassValueVisitor.getInstance()));
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("PreloadEnd", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final float timeout = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("Preloader",
					(arguments, globalScope, triggerScope) -> {
						final String filename = arguments.get(0).visit(StringJassValueVisitor.getInstance());
						// Campaign maps name .pld files the game data does not ship (NightElfX05); a preload list
						// only warms the cache.
						if ((filename != null) && dataSource.has(filename)) {
							doPreloadScript(dataSource, uiViewport, uiScene, war3MapViewer, filename, meleeUI,
									originalFiles, jassProgramVisitor, "PreloadFiles", true);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("CreateTimerDialog",
					(arguments, globalScope, triggerScope) -> {
						final CTimer timer = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return new HandleJassValue(timerdialogType, meleeUI.createTimerDialog(timer));
					});
			jassProgramVisitor.getJassNativeManager().createNative("TimerDialogSetTitle",
					(arguments, globalScope, triggerScope) -> {
						final CTimerDialog dialog = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (dialog != null) {
							dialog.setTitle(nullable(arguments, 1, StringJassValueVisitor.getInstance()));
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("TimerDialogDisplay",
					(arguments, globalScope, triggerScope) -> {
						final CTimerDialog dialog = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (dialog != null) {
							dialog.setVisible(arguments.get(1).visit(BooleanJassValueVisitor.getInstance()));
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsTimerDialogDisplayed",
					(arguments, globalScope, triggerScope) -> {
						final CTimerDialog dialog = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((dialog != null) && dialog.isVisible());
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsPlayerObserver",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (player == null) {
							return BooleanJassValue.FALSE;
						}
						return BooleanJassValue.of(player.isObserver());
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetSoundDuration",
					(arguments, globalScope, triggerScope) -> {
						final CSound sound = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int duration = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						if (sound != null) {
							sound.setDuration(duration / 1000f);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetSoundPitch",
					(arguments, globalScope, triggerScope) -> {
						final CSoundFilename sound = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final float pitch = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						// TODO NYI
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetSoundVolume",
					(arguments, globalScope, triggerScope) -> {
						final CSoundFilename sound = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int volume = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						// TODO NYI
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("AddWeatherEffect",
					(arguments, globalScope, triggerScope) -> {
						final Rectangle where = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int effectId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						if (where == null) {
							return new HandleJassValue(weathereffectType, null);
						}
						return new HandleJassValue(weathereffectType,
								war3MapViewer.createWeatherEffect(new War3ID(effectId), where.x, where.y,
										where.x + where.width, where.y + where.height));
					});
			jassProgramVisitor.getJassNativeManager().createNative("EnableWeatherEffect",
					(arguments, globalScope, triggerScope) -> {
						final SimulationRenderComponentWeather weather = nullable(arguments, 0,
								ObjectJassValueVisitor.getInstance());
						final boolean enable = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						if (weather != null) {
							weather.setEnabled(enable);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("RemoveWeatherEffect",
					(arguments, globalScope, triggerScope) -> {
						final SimulationRenderComponentWeather weather = nullable(arguments, 0,
								ObjectJassValueVisitor.getInstance());
						if (weather != null) {
							weather.remove();
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterDeathEvent",
					(arguments, globalScope, triggerScope) -> {
						final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CWidget whichWidget = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						return new HandleJassValue(eventType, whichWidget.addDeathEvent(globalScope, whichTrigger));
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterUnitEvent",
					(arguments, globalScope, triggerScope) -> {
						final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CUnit whichWidget = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						final JassGameEventsWar3 whichPlayerEvent = arguments.get(2)
								.visit(ObjectJassValueVisitor.getInstance());
						if (whichWidget == null) {
							return new HandleJassValue(eventType, RemovableTriggerEvent.doNothing(whichTrigger));
						}
						return new HandleJassValue(eventType,
								whichWidget.addEvent(globalScope, whichTrigger, whichPlayerEvent));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitHidden",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.<CUnit>getInstance());
						if (unit == null) {
							return BooleanJassValue.TRUE; // TODO this is a workaround, probably
						}
						if (unit.isRemovedFromGame()) {
							return BooleanJassValue.FALSE;
						}
						return BooleanJassValue.of(unit.isHidden());
					});
			jassProgramVisitor.getJassNativeManager().createNative("PauseUnit",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.<CUnit>getInstance());
						final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						if (unit != null) {
							unit.setPaused(flag);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetPlayerHandicapXP",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final float handicap = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						if (player != null) {
							player.setHandicapXP(handicap);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetChangingUnit",
					(arguments, globalScope, triggerScope) -> new HandleJassValue(unitType,
							((CommonTriggerExecutionScope) triggerScope).getChangingUnit()));
			jassProgramVisitor.getJassNativeManager().createNative("GetChangingUnitPrevOwner",
					(arguments, globalScope, triggerScope) -> new HandleJassValue(playerType,
							((CommonTriggerExecutionScope) triggerScope).getChangingUnitPrevOwner()));
			jassProgramVisitor.getJassNativeManager().createNative("GetWidgetX",
					(arguments, globalScope, triggerScope) -> {
						final CWidget whichWidget = inGame(nullable(arguments, 0, ObjectJassValueVisitor.getInstance()));
						return RealJassValue.of(whichWidget == null ? 0 : whichWidget.getX());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetWidgetY",
					(arguments, globalScope, triggerScope) -> {
						final CWidget whichWidget = inGame(nullable(arguments, 0, ObjectJassValueVisitor.getInstance()));
						return RealJassValue.of(whichWidget == null ? 0 : whichWidget.getY());
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
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitX",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichWidget = inGame(nullable(arguments, 0, ObjectJassValueVisitor.<CUnit>getInstance()));
						return RealJassValue.of(whichWidget == null ? 0 : whichWidget.getX());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitY",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichWidget = inGame(nullable(arguments, 0, ObjectJassValueVisitor.<CUnit>getInstance()));
						return RealJassValue.of(whichWidget == null ? 0 : whichWidget.getY());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitPointValue",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichWidget = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (whichWidget == null) {
							return IntegerJassValue.ZERO;
						}
						return IntegerJassValue.of(whichWidget.getUnitType().getPointValue());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitPointValueByType",
					(arguments, globalScope, triggerScope) -> {
						final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						return IntegerJassValue.of(CommonEnvironment.this.simulation.getUnitData()
								.getUnitType(new War3ID(rawcode)).getPointValue());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitLoc",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichWidget = inGame(nullable(arguments, 0, ObjectJassValueVisitor.<CUnit>getInstance()));
						if (whichWidget == null) {
							return new HandleJassValue(locationType,
									new LocationJass(0, 0, this.simulation.getHandleIdAllocator().createId()));
						}
						return new HandleJassValue(locationType, new LocationJass(whichWidget.getX(),
								whichWidget.getY(), this.simulation.getHandleIdAllocator().createId()));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitAbilityLevel",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichWidget = inGame(nullable(arguments, 0, ObjectJassValueVisitor.<CUnit>getInstance()));
						final int rawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						if (whichWidget == null) {
							return IntegerJassValue.ZERO;
						}
						final CAbility ability = whichWidget
								.getAbility(GetAbilityByRawcodeVisitor.getInstance().reset(new War3ID(rawcode)));
						if (ability == null) {
							final int illusionLevel = whichWidget.getIllusionAbilityLevel(rawcode);
							if (illusionLevel > 0) {
								return IntegerJassValue.of(illusionLevel);
							}
							// Measured: the original reads level 1 for a worker's build ability.
							for (final CAbility other : whichWidget.getAbilities()) {
								if ((other instanceof AbstractCAbilityBuild) && (other.getAlias().getValue() == rawcode)) {
									return IntegerJassValue.of(1);
								}
							}
							return IntegerJassValue.ZERO;
						}
						// Measured: the original reads level 1 for a buff, whatever level of spell placed it.
						if (ability instanceof CBuff) {
							return IntegerJassValue.of(1);
						}
						return IntegerJassValue.of(ability instanceof CLevelingAbility
								? Math.max(1, ((CLevelingAbility) ability).getLevel())
								: 1);
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitMakeAbilityPermanent",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final boolean permanent = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						final int rawcode = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						if (whichUnit == null) {
							return BooleanJassValue.FALSE;
						}
						final CAbility ability = whichUnit
								.getAbility(GetAbilityByRawcodeVisitor.getInstance().reset(new War3ID(rawcode)));
						if (ability == null) {
							return BooleanJassValue.FALSE;
						}
						ability.setPermanent(permanent);
						return BooleanJassValue.TRUE;
					});
			jassProgramVisitor.getJassNativeManager().createNative("IncUnitAbilityLevel",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichWidget = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int rawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final War3ID war3id = new War3ID(rawcode);
						final CLevelingAbility ability = whichWidget
								.getAbility(GetAbilityByRawcodeVisitor.getInstance().reset(war3id));
						if (ability == null) {
							whichWidget.add(CommonEnvironment.this.simulation,
									CommonEnvironment.this.simulation.getAbilityData().createAbility(war3id,
											CommonEnvironment.this.simulation.getHandleIdAllocator().createId()));
							// TODO below code is very stupid!!
							return IntegerJassValue.of(1);
						}
						else {
							final int newLevel = ability.getLevel() + 1;
							ability.setLevel(CommonEnvironment.this.simulation, whichWidget, newLevel);
							return IntegerJassValue.of(newLevel);
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitAbilityLevel",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int rawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final int newLevel = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						final War3ID war3id = new War3ID(rawcode);
						final CLevelingAbility ability = whichUnit
								.getAbility(GetAbilityByRawcodeVisitor.getInstance().reset(war3id));
						if (ability != null) {
							ability.setLevel(CommonEnvironment.this.simulation, whichUnit, newLevel);
							return IntegerJassValue.of(newLevel);
						}
						return IntegerJassValue.of(newLevel);
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetAbilityLevel",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CLevelingAbility whichAbility = arguments.get(1)
								.visit(ObjectJassValueVisitor.getInstance());
						final int newLevel = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						if (whichAbility != null) {
							whichAbility.setLevel(CommonEnvironment.this.simulation, whichUnit, newLevel);
							return IntegerJassValue.of(newLevel);
						}
						return IntegerJassValue.of(newLevel);
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetAbilityLevel",
					(arguments, globalScope, triggerScope) -> {
						final CLevelingAbility whichAbility = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						return IntegerJassValue.of(whichAbility.getLevel());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetPlayerHandicap",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer whichPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						// 1.29 reads 1.0 (no handicap) for a null player (null-players-d320).
						return RealJassValue.of(whichPlayer == null ? 1.0 : whichPlayer.getHandicap());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetHandleId",
					(arguments, globalScope, triggerScope) -> {
						final Object whichHandle = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (whichHandle == null) {
							return IntegerJassValue.ZERO;
						}
						// Effects and lightning are drawing objects with no id of their own in the simulation.
						return IntegerJassValue.of(whichHandle instanceof CHandle ? ((CHandle) whichHandle).getHandleId()
								: System.identityHashCode(whichHandle));
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerSleepAction",
					(arguments, globalScope, triggerScope) -> {
						final Trigger triggeringTrigger = triggerScope.getTriggeringTrigger();
						if ((triggeringTrigger == null) || triggeringTrigger.isWaitOnSleeps()) {
							final Double seconds = arguments.get(0).visit(RealJassValueVisitor.getInstance());
							final JassThread currentThread = globalScope.getCurrentThread();
							if ((currentThread != null) && currentThread.isSleepEndsThread()) {
								currentThread.halt();
							}
							else if (currentThread != null) {
								currentThread.setSleeping(true);
								final CTimerSleepAction timer = new CTimerSleepAction(currentThread);
								timer.setRepeats(false);
								timer.setTimeoutTime(seconds.floatValue());
								timer.start(this.simulation);
							}
							else {
								throw new JassException(globalScope,
										"Needs to sleep " + seconds + " but no thread was found", null);
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerWaitForSound",
					(arguments, globalScope, triggerScope) -> {
						final Trigger triggeringTrigger = triggerScope.getTriggeringTrigger();
						if ((triggeringTrigger == null) || triggeringTrigger.isWaitOnSleeps()) {
							final CSound sound = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
							final float offset = arguments.get(1).visit(RealJassValueVisitor.getInstance())
									.floatValue();
							float seconds = 0;
							if (sound != null) {
								seconds = sound.getRemainingTimeToPlayOnTheDesyncLocalComputer() - offset; // PRONE TO
																											// DESYNC
																											// (?)
							}
							final JassThread currentThread = globalScope.getCurrentThread();
							if ((currentThread != null) && currentThread.isSleepEndsThread()) {
								currentThread.halt();
							}
							else if (currentThread != null) {
								currentThread.setSleeping(true);
								final CTimerSleepAction timer = new CTimerSleepAction(currentThread);
								timer.setRepeats(false);
								timer.setTimeoutTime(seconds);
								timer.start(this.simulation);
							}
							else {
								throw new JassException(globalScope,
										"Needs to sleep " + seconds + " but no thread was found", null);
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("AddSpecialEffectTarget",
					(arguments, globalScope, triggerScope) -> {
						final String modelName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
						final CWidget targetWidget = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						final String attachPointName = arguments.get(2).visit(StringJassValueVisitor.getInstance());
						return new HandleJassValue(effectType,
								war3MapViewer.addSpecialEffectTarget(modelName, targetWidget, attachPointName));
					});
			jassProgramVisitor.getJassNativeManager().createNative("AddSpecialEffectLoc",
					(arguments, globalScope, triggerScope) -> {
						final String modelName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
						final AbilityPointTarget positionLoc = arguments.get(1)
								.visit(ObjectJassValueVisitor.getInstance());
						return new HandleJassValue(effectType, war3MapViewer.addSpecialEffect(modelName, positionLoc.x,
								positionLoc.y, 0 /* facing */));
					});
			jassProgramVisitor.getJassNativeManager().createNative("AddSpecialEffect",
					(arguments, globalScope, triggerScope) -> {
						final String modelName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
						final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						return new HandleJassValue(effectType,
								war3MapViewer.addSpecialEffect(modelName, x, y, 0 /* facing */));
					});
			jassProgramVisitor.getJassNativeManager().createNative("AddSpellEffectById",
					(arguments, globalScope, triggerScope) -> {
						final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final CEffectType whichEffectType = arguments.get(1)
								.visit(ObjectJassValueVisitor.getInstance());
						final double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
						final double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());
						return new HandleJassValue(effectType, war3MapViewer.spawnSpellEffectEx((float) x, (float) y,
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
							return new HandleJassValue(effectType, war3MapViewer.spawnSpellEffectOnUnitEx(target,
									new War3ID(rawcode), whichEffectType, 0, attachmentPoint));
						}
						else {
							return new HandleJassValue(effectType, war3MapViewer.spawnSpellEffectOnUnitEx(target,
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
						return new HandleJassValue(effectType, war3MapViewer.spawnSpellEffectEx(x, y, 0 /* facing */,
								new War3ID(rawcode), whichEffectType, 0));
					});
			jassProgramVisitor.getJassNativeManager().createNative("AddLightning",
					(arguments, globalScope, triggerScope) -> {
						final String rawcode = arguments.get(0).visit(StringJassValueVisitor.getInstance());
						final double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
						final double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());
						final double x2 = arguments.get(4).visit(RealJassValueVisitor.getInstance());
						final double y2 = arguments.get(5).visit(RealJassValueVisitor.getInstance());
						return new HandleJassValue(lightningType,
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
						return new HandleJassValue(lightningType,
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

			jassProgramVisitor.getJassNativeManager().createNative("BlzSetUnitFacingEx",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final double facing = arguments.get(1).visit(RealJassValueVisitor.getInstance());
						if (whichUnit == null) {
							return null;
						}
						whichUnit.setFacing((float) facing);
						final RenderUnit renderPeer = war3MapViewer.getRenderPeer(whichUnit);
						if (renderPeer != null) {
							renderPeer.setFacing((float) facing);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("DestroyEffect",
					(arguments, globalScope, triggerScope) -> {
						final ScriptEffect fx = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (fx != null) {
							fx.setKillWhenDone(true);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetDestructableInvulnerable",
					(arguments, globalScope, triggerScope) -> {
						final CDestructable whichWidget = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						whichWidget.setInvulnerable(flag);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SuspendTimeOfDay",
					(arguments, globalScope, triggerScope) -> {
						final boolean flag = arguments.get(0).visit(BooleanJassValueVisitor.getInstance());
						CommonEnvironment.this.simulation.setTimeOfDaySuspended(flag);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetTimeOfDayScale",
					(arguments, globalScope, triggerScope) -> {
						final float r = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
						CommonEnvironment.this.simulation.setTimeOfDayScale(r);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetTimeOfDayScale",
					(arguments, globalScope, triggerScope) -> {
						return RealJassValue.of(CommonEnvironment.this.simulation.getTimeOfDayScale());
					});
			jassProgramVisitor.getJassNativeManager().createNative("RemoveGuardPosition",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (whichUnit != null) {
							whichUnit.removeGuardPosition();
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("RecycleGuardPosition",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (whichUnit != null) {
							whichUnit.setGuardPosition(whichUnit.getX(), whichUnit.getY());
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitCreepGuard",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean creepGuard = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						if (whichUnit != null) {
							if (creepGuard) {
								whichUnit.setGuardPosition(whichUnit.getX(), whichUnit.getY());
							}
							else {
								whichUnit.removeGuardPosition();
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitIllusion",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichUnit != null) && whichUnit.isIllusion());
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitInventorySize",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CAbilityInventory inventoryData = whichUnit == null ? null : whichUnit.getInventoryData();
						return IntegerJassValue.of(inventoryData == null ? 0 : inventoryData.getItemCapacity());
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitAddItem",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CItem whichItem = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						final CAbilityInventory inventoryData = whichUnit == null ? null : whichUnit.getInventoryData();
						if ((inventoryData == null) || (whichItem == null)) {
							return BooleanJassValue.FALSE;
						}
						final int slot = inventoryData.giveItem(CommonEnvironment.this.simulation, whichUnit,
								whichItem, false);
						// A power up is used at once and dies instead of taking a slot.
						return BooleanJassValue.of((slot >= 0) || whichItem.isDead());
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitDropItemTarget",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CItem whichItem = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						final CWidget target = nullable(arguments, 2, ObjectJassValueVisitor.getInstance());
						final CAbilityInventory inventoryData = whichUnit == null ? null : whichUnit.getInventoryData();
						if ((inventoryData == null) || (whichItem == null) || !(target instanceof CUnit)
								|| (inventoryData.getSlot(whichItem) == -1) || whichUnit.isIllusion()) {
							return BooleanJassValue.FALSE;
						}
						CommonEnvironment.this.simulation.getDefaultPlayerUnitOrderExecutor(whichUnit.getPlayerIndex())
								.issueDropItemAtTargetOrder(whichUnit.getHandleId(), inventoryData.getHandleId(),
										OrderIds.dropitem, whichItem.getHandleId(), target.getHandleId(), false);
						return BooleanJassValue.TRUE;
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitDropItemPoint",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CItem whichItem = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						final float x = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
						final CAbilityInventory inventoryData = whichUnit == null ? null : whichUnit.getInventoryData();
						if ((inventoryData == null) || (whichItem == null) || (inventoryData.getSlot(whichItem) == -1)
								|| whichUnit.isIllusion()) {
							return BooleanJassValue.FALSE;
						}
						CommonEnvironment.this.simulation.getDefaultPlayerUnitOrderExecutor(whichUnit.getPlayerIndex())
								.issueDropItemAtPointOrder(whichUnit.getHandleId(), inventoryData.getHandleId(),
										OrderIds.dropitem, whichItem.getHandleId(), x, y, false);
						return BooleanJassValue.TRUE;
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitRemoveItem",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CItem whichItem = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						final CAbilityInventory inventoryData = whichUnit == null ? null : whichUnit.getInventoryData();
						if ((inventoryData != null) && (whichItem != null)) {
							inventoryData.dropItem(this.simulation, whichUnit, whichItem, whichUnit.getX(),
									whichUnit.getY(), true);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitAddType",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CUnitTypeJass whichClassification = arguments.get(1)
								.visit(ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of(whichUnit.addUnitType(this.simulation, whichClassification));
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitRemoveType",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CUnitTypeJass whichClassification = arguments.get(1)
								.visit(ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of(whichUnit.removeUnitType(this.simulation, whichClassification));
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitAddAbility",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int abilityId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						if (whichUnit == null) {
							return BooleanJassValue.FALSE;
						}
						final War3ID rawcode = new War3ID(abilityId);
						final CAbilityType<?> abilityTypeTmp = CommonEnvironment.this.simulation.getAbilityData()
								.getAbilityType(rawcode);
						if (abilityTypeTmp == null) {
							System.err.println(
									"UnitAddAbility: The requested ability has not been programmed yet: " + rawcode);
							return BooleanJassValue.FALSE;
						}
						final CLevelingAbility existingAbility = whichUnit
								.getAbility(GetAbilityByRawcodeVisitor.getInstance().reset(rawcode));
						if (existingAbility != null) {
							return BooleanJassValue.FALSE;
						}
						final CAbility ability = abilityTypeTmp
								.createAbility(CommonEnvironment.this.simulation.getHandleIdAllocator().createId());
						whichUnit.add(CommonEnvironment.this.simulation, ability);
						return BooleanJassValue.TRUE;
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitRemoveAbility",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int abilityId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						if (whichUnit == null) {
							return BooleanJassValue.FALSE;
						}
						final War3ID rawcode = new War3ID(abilityId);

						final CAbility abil = whichUnit
								.getAbility(GetAbilityByRawcodeVisitor.getInstance().reset(rawcode));
						if (abil != null) {
							whichUnit.remove(this.simulation, abil);
							return BooleanJassValue.TRUE;
						}
						return BooleanJassValue.FALSE;
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitItemInSlot",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int whichSlot = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						if (whichUnit == null) {
							return new HandleJassValue(itemType, null);
						}
						final CAbilityInventory inventoryData = whichUnit.getInventoryData();
						if (inventoryData != null) {
							return new HandleJassValue(itemType, inventoryData.getItemInSlot(whichSlot));
						}
						return new HandleJassValue(itemType, null);
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitHasItem",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CItem whichItem = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						final CAbilityInventory inventoryData = whichUnit == null ? null : whichUnit.getInventoryData();
						return BooleanJassValue
								.of((inventoryData != null) && (whichItem != null) && (inventoryData.getSlot(whichItem) != -1));
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitInventoryCount",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CAbilityInventory inventoryData = whichUnit == null ? null : whichUnit.getInventoryData();
						int count = 0;
						if (inventoryData != null) {
							for (int slot = 0; slot < inventoryData.getItemCapacity(); slot++) {
								if (inventoryData.getItemInSlot(slot) != null) {
									count++;
								}
							}
						}
						return IntegerJassValue.of(count);
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetCameraTargetController",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final float xoffset = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float yoffset = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						final boolean inheritOrientation = arguments.get(3)
								.visit(BooleanJassValueVisitor.getInstance());
						meleeUI.getCameraManager().setTargetController(war3MapViewer.getRenderPeer(whichUnit), xoffset,
								yoffset, inheritOrientation);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetCameraPosition",
					(arguments, globalScope, triggerScope) -> {
						final float x = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						meleeUI.getCameraManager().jumpTo(x, y);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("PanCameraTo",
					(arguments, globalScope, triggerScope) -> {
						final float x = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						meleeUI.getCameraManager().panTo(x, y);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetCameraQuickPosition",
					(arguments, globalScope, triggerScope) -> {
						final float x = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						meleeUI.getCameraManager().setQuickPosition(x, y);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("PanCameraToTimed",
					(arguments, globalScope, triggerScope) -> {
						final float x = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float duration = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						meleeUI.getCameraManager().panToTimed(x, y, duration);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("PanCameraToWithZ",
					(arguments, globalScope, triggerScope) -> {
						final float x = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float zOffsetDest = arguments.get(2).visit(RealJassValueVisitor.getInstance())
								.floatValue();
						meleeUI.getCameraManager().panTo(x, y);
						meleeUI.getCameraManager().setTargetZOffset(zOffsetDest);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("PanCameraToTimedWithZ",
					(arguments, globalScope, triggerScope) -> {
						final float x = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float zOffsetDest = arguments.get(2).visit(RealJassValueVisitor.getInstance())
								.floatValue();
						final float duration = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
						meleeUI.getCameraManager().panToTimed(x, y, duration);
						meleeUI.getCameraManager().setTargetZOffset(zOffsetDest, duration);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("CreateCameraSetup",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(camerasetupType, new CustomCameraSetup(304, 70, 90, 0, 1650, 5000, 100, 0));
					});
			jassProgramVisitor.getJassNativeManager().createNative("CameraSetupGetField",
					(arguments, globalScope, triggerScope) -> {
						final CustomCameraSetup cameraSetup = nullable(arguments, 0,
								ObjectJassValueVisitor.getInstance());
						final CCameraField cameraField = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						if ((cameraSetup != null) && (cameraField != null)) {
							return RealJassValue.of(cameraSetup.getField(cameraField));
						}
						return RealJassValue.of(0.0f);
					});
			jassProgramVisitor.getJassNativeManager().createNative("CameraSetupSetField",
					(arguments, globalScope, triggerScope) -> {
						final CustomCameraSetup cameraSetup = nullable(arguments, 0,
								ObjectJassValueVisitor.getInstance());
						final CCameraField cameraField = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						final float fieldValue = arguments.get(2).visit(RealJassValueVisitor.getInstance())
								.floatValue();
						if ((cameraSetup != null) && (cameraField != null)) {
							cameraSetup.setField(cameraField, fieldValue);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("CameraSetupSetDestPosition",
					(arguments, globalScope, triggerScope) -> {
						final CustomCameraSetup cameraSetup = nullable(arguments, 0,
								ObjectJassValueVisitor.getInstance());
						final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						if (cameraSetup != null) {
							cameraSetup.setDestPositionX(x);
							cameraSetup.setDestPositionY(y);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("CameraSetupGetDestPositionLoc",
					(arguments, globalScope, triggerScope) -> {
						final CustomCameraSetup cameraSetup = nullable(arguments, 0,
								ObjectJassValueVisitor.getInstance());
						if (cameraSetup != null) {
							return new HandleJassValue(locationType, new LocationJass(cameraSetup.getDestPositionX(),
									cameraSetup.getDestPositionY(), this.simulation.getHandleIdAllocator().createId()));
						}
						return locationType.getNullValue();
					});
			jassProgramVisitor.getJassNativeManager().createNative("CameraSetupGetDestPositionX",
					(arguments, globalScope, triggerScope) -> {
						final CustomCameraSetup cameraSetup = nullable(arguments, 0,
								ObjectJassValueVisitor.getInstance());
						if (cameraSetup != null) {
							return RealJassValue.of(cameraSetup.getDestPositionX());
						}
						return RealJassValue.ZERO;
					});
			jassProgramVisitor.getJassNativeManager().createNative("CameraSetupGetDestPositionY",
					(arguments, globalScope, triggerScope) -> {
						final CustomCameraSetup cameraSetup = nullable(arguments, 0,
								ObjectJassValueVisitor.getInstance());
						if (cameraSetup != null) {
							return RealJassValue.of(cameraSetup.getDestPositionY());
						}
						return RealJassValue.ZERO;
					});
			jassProgramVisitor.getJassNativeManager().createNative("CameraSetupApply",
					(arguments, globalScope, triggerScope) -> {
						final CustomCameraSetup cameraSetup = nullable(arguments, 0,
								ObjectJassValueVisitor.getInstance());
						final boolean doPan = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						final boolean panTimed = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
						meleeUI.getCameraManager().applyCameraSetup(cameraSetup, doPan, panTimed);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("CameraSetupApplyWithZ",
					(arguments, globalScope, triggerScope) -> {
						final CustomCameraSetup cameraSetup = nullable(arguments, 0,
								ObjectJassValueVisitor.getInstance());
						final float zOffset = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						meleeUI.getCameraManager().applyCameraSetup(cameraSetup, true, true);
						meleeUI.getCameraManager().setTargetZOffset(zOffset);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("CameraSetupApplyForceDuration",
					(arguments, globalScope, triggerScope) -> {
						final CustomCameraSetup cameraSetup = nullable(arguments, 0,
								ObjectJassValueVisitor.getInstance());
						final boolean doPan = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						final float forceDuration = arguments.get(2).visit(RealJassValueVisitor.getInstance())
								.floatValue();
						meleeUI.getCameraManager().applyCameraSetupForceDuration(cameraSetup, doPan, forceDuration);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("CameraSetupApplyForceDurationWithZ",
					(arguments, globalScope, triggerScope) -> {
						final CustomCameraSetup cameraSetup = nullable(arguments, 0,
								ObjectJassValueVisitor.getInstance());
						final float zOffset = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float forceDuration = arguments.get(2).visit(RealJassValueVisitor.getInstance())
								.floatValue();
						meleeUI.getCameraManager().applyCameraSetupForceDuration(cameraSetup, true, forceDuration);
						meleeUI.getCameraManager().setTargetZOffset(zOffset, forceDuration);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetCameraField",
					(arguments, globalScope, triggerScope) -> {
						final CCameraField field = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (field == null) {
							return RealJassValue.of(0.0f);
						}
						return RealJassValue.of(meleeUI.getCameraManager().getField(field));
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetCameraField",
					(arguments, globalScope, triggerScope) -> {
						final CCameraField field = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final float value = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float duration = arguments.get(2).visit(RealJassValueVisitor.getInstance())
								.floatValue();
						if (field != null) {
							meleeUI.getCameraManager().setField(field, value, duration);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("AdjustCameraField",
					(arguments, globalScope, triggerScope) -> {
						final CCameraField field = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final float offset = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float duration = arguments.get(2).visit(RealJassValueVisitor.getInstance())
								.floatValue();
						if (field != null) {
							meleeUI.getCameraManager().adjustField(field, offset, duration);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("StopCamera",
					(arguments, globalScope, triggerScope) -> {
						meleeUI.getCameraManager().stopPan();
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetCineFilterTexture",
					(arguments, globalScope, triggerScope) -> {
						meleeUI.getCineFilter().setTexture(nullable(arguments, 0, StringJassValueVisitor.getInstance()));
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetCineFilterBlendMode",
					(arguments, globalScope, triggerScope) -> {
						final CBlendMode mode = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						meleeUI.getCineFilter().setBlendMode(mode);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetCineFilterTexMapFlags",
					(arguments, globalScope, triggerScope) -> null);
			jassProgramVisitor.getJassNativeManager().createNative("SetCineFilterStartUV",
					(arguments, globalScope, triggerScope) -> {
						meleeUI.getCineFilter().setStartUV(realArg(arguments, 0), realArg(arguments, 1),
								realArg(arguments, 2), realArg(arguments, 3));
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetCineFilterEndUV",
					(arguments, globalScope, triggerScope) -> {
						meleeUI.getCineFilter().setEndUV(realArg(arguments, 0), realArg(arguments, 1),
								realArg(arguments, 2), realArg(arguments, 3));
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetCineFilterStartColor",
					(arguments, globalScope, triggerScope) -> {
						meleeUI.getCineFilter().setStartColor(intArg(arguments, 0), intArg(arguments, 1),
								intArg(arguments, 2), intArg(arguments, 3));
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetCineFilterEndColor",
					(arguments, globalScope, triggerScope) -> {
						meleeUI.getCineFilter().setEndColor(intArg(arguments, 0), intArg(arguments, 1),
								intArg(arguments, 2), intArg(arguments, 3));
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetCineFilterDuration",
					(arguments, globalScope, triggerScope) -> {
						meleeUI.getCineFilter().setDuration(realArg(arguments, 0));
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("DisplayCineFilter",
					(arguments, globalScope, triggerScope) -> {
						meleeUI.getCineFilter()
								.display(arguments.get(0).visit(BooleanJassValueVisitor.getInstance()));
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsCineFilterDisplayed",
					(arguments, globalScope, triggerScope) -> BooleanJassValue
							.of(meleeUI.getCineFilter().isDisplayed()));
			jassProgramVisitor.getJassNativeManager().createNative("EnableUserUI",
					(arguments, globalScope, triggerScope) -> {
						meleeUI.enableUserUI(arguments.get(0).visit(BooleanJassValueVisitor.getInstance()));
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("ResetToGameCamera",
					(arguments, globalScope, triggerScope) -> {
						final float forceDuration = arguments.get(0).visit(RealJassValueVisitor.getInstance())
								.floatValue();
						meleeUI.getCameraManager().resetToGameCamera(forceDuration);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("LeaderboardGetItemCount",
					(arguments, globalScope, triggerScope) -> {
						// TODO NYI
						return IntegerJassValue.of(0);
					});
			jassProgramVisitor.getJassNativeManager().createNative("LeaderboardGetPlayerIndex",
					(arguments, globalScope, triggerScope) -> {
						// TODO NYI
						return IntegerJassValue.of(0);
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitInvulnerable",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						if (whichUnit != null) {
							whichUnit.setInvulnerable(CommonEnvironment.this.simulation, flag);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitExploded",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						if (whichUnit != null) {
							whichUnit.setExplodesOnDeath(flag);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetTriggerWidget",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(widgetType,
								((CommonTriggerExecutionScope) triggerScope).getTriggerWidget());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetPlayerRace",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer whichPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						// 1.29 reads RACE_HUMAN for a null player (null-players-d320).
						if (whichPlayer == null) {
							return new HandleJassValue(raceType, WarsmashConstants.RACE_MANAGER.getRace(1));
						}
						return new HandleJassValue(raceType, whichPlayer.getRace());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitUserData",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (whichUnit == null) {
							return IntegerJassValue.ZERO;
						}
						return IntegerJassValue.of(whichUnit.getTriggerEditorCustomValue());
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitUserData",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int data = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						if (whichUnit != null) {
							whichUnit.setTriggerEditorCustomValue(data);
						}
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
						final String animation = CommonEnvironment.this.gameUI
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
						// TODO this "restore" function, is it a summation or assignment below???
						// Guessing assign is OK
						whichDestructable.setLife(CommonEnvironment.this.simulation, life);
						if (!birth) {
							final RenderDestructable renderPeer = war3MapViewer.getRenderPeer(whichDestructable);
							if (renderPeer != null) {
								renderPeer.notifyLifeRestored();
							}
						} // else birth plays automatically
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("DisplayTimedTextToPlayer",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer whichPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float duration = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
						final String message = CommonEnvironment.this.gameUI
								.getTrigStr(arguments.get(4).visit(StringJassValueVisitor.getInstance()));
						System.err.println("DisplayTimedTextToPlayer: " + message.replace("\\n", "\n"));
						if (whichPlayer == CommonEnvironment.this.simulation
								.getPlayer(war3MapViewer.getLocalPlayerIndex())) {
							meleeUI.displayTimedText(x, y, duration, message);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("DisplayTimedTextFromPlayer",
					(arguments, globalScope, triggerScope) -> {
						final CPlayerJass fromPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float duration = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
						final String format = CommonEnvironment.this.gameUI
								.getTrigStr(arguments.get(4).visit(StringJassValueVisitor.getInstance()));
						final String message = format.replace("%s",
								fromPlayer == null ? "" : fromPlayer.getName());
						System.err.println("DisplayTimedTextFromPlayer: " + message.replace("\\n", "\n"));
						meleeUI.displayTimedText(x, y, duration, message);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("DisplayTextToPlayer",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer whichPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						final String message = CommonEnvironment.this.gameUI
								.getTrigStr(arguments.get(3).visit(StringJassValueVisitor.getInstance()));
						if (whichPlayer == CommonEnvironment.this.simulation
								.getPlayer(war3MapViewer.getLocalPlayerIndex())) {
							meleeUI.displayTimedText(x, y, (message.length() / 6) + 5, message);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("ClearTextMessages",
					(arguments, globalScope, triggerScope) -> {
						meleeUI.clearTextMessages();
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("ShowInterface",
					(arguments, globalScope, triggerScope) -> {
						final boolean show = arguments.get(0).visit(BooleanJassValueVisitor.getInstance());
						final float fadeDuration = arguments.get(1).visit(RealJassValueVisitor.getInstance())
								.floatValue();
						meleeUI.showInterface(show, fadeDuration);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("EnableUserControl",
					(arguments, globalScope, triggerScope) -> {
						final boolean value = arguments.get(0).visit(BooleanJassValueVisitor.getInstance());
						meleeUI.enableUserControl(value);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetCinematicScene",
					(arguments, globalScope, triggerScope) -> {
						final int portraitUnitId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final Object colorValue = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						final CPlayerColor color = colorValue instanceof CPlayerColor ? (CPlayerColor) colorValue : null;
						String speakerTitle = nullable(arguments, 2, StringJassValueVisitor.getInstance());
						String text = nullable(arguments, 3, StringJassValueVisitor.getInstance());
						final float sceneDuration = arguments.get(4).visit(RealJassValueVisitor.getInstance())
								.floatValue();
						final float voiceoverDuration = arguments.get(5).visit(RealJassValueVisitor.getInstance())
								.floatValue();

						speakerTitle = this.gameUI.getTrigStr(speakerTitle);
						text = this.gameUI.getTrigStr(text);

						meleeUI.setCinematicScene(portraitUnitId, color, speakerTitle, text, sceneDuration,
								voiceoverDuration);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("EndCinematicScene",
					(arguments, globalScope, triggerScope) -> {
						meleeUI.endCinematicScene();
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("ForceCinematicSubtitles",
					(arguments, globalScope, triggerScope) -> {
						final boolean value = arguments.get(0).visit(BooleanJassValueVisitor.getInstance());
						meleeUI.forceCinematicSubtitles(value);
						return null;
					});
			// Measured: it acts as an Escape press, so it also fires EVENT_PLAYER_END_CINEMATIC (skips a cinematic).
			jassProgramVisitor.getJassNativeManager().createNative("ForceUICancel",
					(arguments, globalScope, triggerScope) -> {
						meleeUI.keyDown(com.badlogic.gdx.Input.Keys.ESCAPE);
						meleeUI.keyUp(com.badlogic.gdx.Input.Keys.ESCAPE);
						return null;
					});
			// CinematicModeBJ saves and restores this flag; the dawn and dusk sounds it switches are not played here.
			final boolean[] dawnDuskEnabled = { true };
			jassProgramVisitor.getJassNativeManager().createNative("EnableDawnDusk",
					(arguments, globalScope, triggerScope) -> {
						dawnDuskEnabled[0] = arguments.get(0).visit(BooleanJassValueVisitor.getInstance());
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsDawnDuskEnabled",
					(arguments, globalScope, triggerScope) -> BooleanJassValue.of(dawnDuskEnabled[0]));
			jassProgramVisitor.getJassNativeManager().createNative("IsPlayerInForce",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer whichPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final List<CPlayerJass> force = arguments.get(1)
								.visit(ObjectJassValueVisitor.<List<CPlayerJass>>getInstance());
						return BooleanJassValue.of((whichPlayer != null) && (force != null) && force.contains(whichPlayer));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitTypeId",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = inGame(nullable(arguments, 0, ObjectJassValueVisitor.<CUnit>getInstance()));
						if (whichUnit == null) {
							return IntegerJassValue.of(0);
						}
						return IntegerJassValue.of(whichUnit.getTypeId().getValue());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOwningPlayer",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = inGame(nullable(arguments, 0, ObjectJassValueVisitor.<CUnit>getInstance()));
						if (whichUnit == null) {
							return playerType.getNullValue();
						}
						return new HandleJassValue(playerType,
								CommonEnvironment.this.simulation.getPlayer(whichUnit.getPlayerIndex()));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetFilterPlayer",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(playerType,
								((CommonTriggerExecutionScope) triggerScope).getFilterPlayer());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetDyingUnit",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getDyingUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetAttacker",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getAttacker());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetKillingUnit",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getKillingUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetEventDamageSource",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getDamageSource());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetEventDamage",
					(arguments, globalScope, triggerScope) -> {
						return RealJassValue.of(((CommonTriggerExecutionScope) triggerScope).getDamageTaken());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetCancelledStructure",
					(arguments, globalScope, triggerScope) -> {
						final CommonTriggerExecutionScope scope = (CommonTriggerExecutionScope) triggerScope;
						return new HandleJassValue(unitType, scope.getCancelledStructure() != null
								? scope.getCancelledStructure()
								: scope.getTriggeringUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetConstructingStructure",
					(arguments, globalScope, triggerScope) -> {
						final CommonTriggerExecutionScope scope = (CommonTriggerExecutionScope) triggerScope;
						return new HandleJassValue(unitType, scope.getConstructingStructure() != null
								? scope.getConstructingStructure()
								: scope.getTriggeringUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetTriggerUnit",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getTriggeringUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetTriggerPlayer",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(playerType,
								((CommonTriggerExecutionScope) triggerScope).getTriggeringPlayer());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetTriggerDestructable",
					(arguments, globalScope, triggerScope) -> {
						final CWidget triggerWidget = ((CommonTriggerExecutionScope) triggerScope).getTriggerWidget();
						if (triggerWidget instanceof CDestructable) {
							return new HandleJassValue(destructableType, triggerWidget);
						}
						else {
							return new HandleJassValue(destructableType, null);
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderedUnit",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getOrderedUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetManipulatedItem",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(itemType,
								((CommonTriggerExecutionScope) triggerScope).getManipulatedItem());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetManipulatingUnit",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getManipulatingUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetIssuedOrderId",
					(arguments, globalScope, triggerScope) -> {
						return IntegerJassValue.of(((CommonTriggerExecutionScope) triggerScope).getIssuedOrderId());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderPointX",
					(arguments, globalScope, triggerScope) -> {
						return RealJassValue.of(((CommonTriggerExecutionScope) triggerScope).getOrderPointX());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderPointY",
					(arguments, globalScope, triggerScope) -> {
						return RealJassValue.of(((CommonTriggerExecutionScope) triggerScope).getOrderPointY());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderPointLoc",
					(arguments, globalScope, triggerScope) -> {
						final CommonTriggerExecutionScope commonTriggerExecutionScope = (CommonTriggerExecutionScope) triggerScope;
						final LocationJass jassLocation = new LocationJass(commonTriggerExecutionScope.getOrderPointX(),
								commonTriggerExecutionScope.getOrderPointY(),
								this.simulation.getHandleIdAllocator().createId());
						return new HandleJassValue(locationType, jassLocation);
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderTarget",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(widgetType,
								((CommonTriggerExecutionScope) triggerScope).getOrderTarget());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderTargetDestructable",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(destructableType,
								((CommonTriggerExecutionScope) triggerScope).getOrderTargetDestructable());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderTargetItem",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(itemType,
								((CommonTriggerExecutionScope) triggerScope).getOrderTargetItem());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderTargetUnit",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getOrderTargetUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetSpellAbilityUnit",
					(arguments, globalScope, triggerScope) -> {
						if (!(triggerScope instanceof CommonTriggerExecutionScope)) {
							return unitType.getNullValue();
						}
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getSpellAbilityUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetSpellTargetUnit",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getSpellTargetUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetSpellTargetPoint",
					(arguments, globalScope, triggerScope) -> {
						final AbilityPointTarget spellTargetPoint = ((CommonTriggerExecutionScope) triggerScope)
								.getSpellTargetPoint();
						final LocationJass jassLocation = new LocationJass(spellTargetPoint.x, spellTargetPoint.y,
								this.simulation.getHandleIdAllocator().createId());
						return new HandleJassValue(locationType, jassLocation);
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetSpellTargetX",
					(arguments, globalScope, triggerScope) -> {
						final AbilityPointTarget spellTargetPoint = ((CommonTriggerExecutionScope) triggerScope)
								.getSpellTargetPoint();
						return RealJassValue.of(spellTargetPoint.x);
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetSpellTargetY",
					(arguments, globalScope, triggerScope) -> {
						final AbilityPointTarget spellTargetPoint = ((CommonTriggerExecutionScope) triggerScope)
								.getSpellTargetPoint();
						return RealJassValue.of(spellTargetPoint.y);
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetSpellTargetLoc",
					(arguments, globalScope, triggerScope) -> {
						final AbilityPointTarget spellTargetPoint = ((CommonTriggerExecutionScope) triggerScope)
								.getSpellTargetPoint();
						return new HandleJassValue(locationType, new LocationJass(spellTargetPoint.x,
								spellTargetPoint.y, this.simulation.getHandleIdAllocator().createId()));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetSpellAbilityId",
					(arguments, globalScope, triggerScope) -> {
						final War3ID spellAbilityId = ((CommonTriggerExecutionScope) triggerScope).getSpellAbilityId();
						return IntegerJassValue.of(spellAbilityId == null ? 0 : spellAbilityId.getValue());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetEnumPlayer",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(playerType,
								((CommonTriggerExecutionScope) triggerScope).getEnumPlayer());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetConstructedStructure",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getConstructedStructure());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetResearchingUnit",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getResearchingUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetResearched",
					(arguments, globalScope, triggerScope) -> {
						return IntegerJassValue.of(((CommonTriggerExecutionScope) triggerScope).getResearched());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetSellingUnit",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getSellingUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetSoldUnit",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType, ((CommonTriggerExecutionScope) triggerScope).getSoldUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetBuyingUnit",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getBuyingUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetSoldItem",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(itemType, ((CommonTriggerExecutionScope) triggerScope).getSoldItem());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetLevelingUnit",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getTriggeringUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetLearningUnit",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getTriggeringUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetLearnedSkill",
					(arguments, globalScope, triggerScope) -> {
						return IntegerJassValue.of(((CommonTriggerExecutionScope) triggerScope).getLearnedSkill());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetLearnedSkillLevel",
					(arguments, globalScope, triggerScope) -> {
						return IntegerJassValue.of(((CommonTriggerExecutionScope) triggerScope).getLearnedSkillLevel());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetLoadedUnit",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getLoadedUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetTransportUnit",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getTransportUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetRescuer",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getRescuer());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetRevivableUnit",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getRevivableUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetTrainedUnit",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(unitType,
								((CommonTriggerExecutionScope) triggerScope).getTrainedUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetTrainedUnitType",
					(arguments, globalScope, triggerScope) -> {
						return IntegerJassValue.of(((CommonTriggerExecutionScope) triggerScope).getTrainedUnitType());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitRallyPoint",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						float x, y;
						if (whichUnit != null) {
							final AbilityTarget rallyPoint = whichUnit.getRallyPoint();
							if (rallyPoint != null) {
								x = rallyPoint.getX();
								y = rallyPoint.getY();
							}
							else {
								x = y = 0;
							}
						}
						else {
							x = y = 0;
						}
						return new HandleJassValue(locationType,
								new LocationJass(x, y, this.simulation.getHandleIdAllocator().createId()));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitRallyUnit",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						HandleJassValue rallyUnit;
						if (whichUnit != null) {
							final AbilityTarget rallyPoint = whichUnit.getRallyPoint();
							if (rallyPoint != null) {
								rallyUnit = rallyPoint.visit(new AbilityTargetVisitor<HandleJassValue>() {
									@Override
									public HandleJassValue accept(final AbilityPointTarget target) {
										return unitType.getNullValue();
									}

									@Override
									public HandleJassValue accept(final CUnit target) {
										return new HandleJassValue(unitType, target);
									}

									@Override
									public HandleJassValue accept(final CDestructable target) {
										return unitType.getNullValue();
									}

									@Override
									public HandleJassValue accept(final CItem target) {
										return unitType.getNullValue();
									}
								});
							}
							else {
								rallyUnit = unitType.getNullValue();
							}
						}
						else {
							rallyUnit = unitType.getNullValue();
						}
						return rallyUnit;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitRallyDestructable",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						HandleJassValue rallyDest;
						if (whichUnit != null) {
							final AbilityTarget rallyPoint = whichUnit.getRallyPoint();
							if (rallyPoint != null) {
								rallyDest = rallyPoint.visit(new AbilityTargetVisitor<HandleJassValue>() {
									@Override
									public HandleJassValue accept(final AbilityPointTarget target) {
										return destructableType.getNullValue();
									}

									@Override
									public HandleJassValue accept(final CUnit target) {
										return destructableType.getNullValue();
									}

									@Override
									public HandleJassValue accept(final CDestructable target) {
										return new HandleJassValue(destructableType, target);
									}

									@Override
									public HandleJassValue accept(final CItem target) {
										return destructableType.getNullValue();
									}
								});
							}
							else {
								rallyDest = destructableType.getNullValue();
							}
						}
						else {
							rallyDest = destructableType.getNullValue();
						}
						return rallyDest;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitCurrentOrder",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = inGame(nullable(arguments, 0, ObjectJassValueVisitor.<CUnit>getInstance()));
						int orderId;
						if ((whichUnit != null) && whichUnit.isPaused()) {
							// 1.29 (probe invuln-ad-cyclone-d379): a paused unit reads stunned.
							orderId = OrderIds.stunned;
						}
						else if ((whichUnit != null) && (whichUnit.getCurrentOrder() != null)) {
							orderId = whichUnit.getCurrentOrder().getOrderId();
							// 1.29 (probe order-current-d286): hold position reads 0, also while it attacks; patrol
							// reads 851991; a Peasant raising its building reads repair.
							if (orderId == OrderIds.holdposition) {
								orderId = 0;
							}
							else if (orderId == OrderIds.patrol) {
								orderId = OrderIds._instant1;
							}
							else {
								final CBehavior behavior = whichUnit.getCurrentBehavior();
								if ((behavior != null) && (behavior.getHighlightOrderId() == OrderIds.repair)) {
									orderId = OrderIds.repair;
								}
							}
						}
						else {
							orderId = 0;
						}
						return IntegerJassValue.of(orderId);
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetBlightLoc",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer whichPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final AbilityPointTarget whichLocation = arguments.get(1)
								.visit(ObjectJassValueVisitor.getInstance());
						final float radius = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						final boolean addBlight = arguments.get(3).visit(BooleanJassValueVisitor.getInstance());
						if ((whichPlayer == null) || (whichLocation == null)) {
							return null;
						}
						final float whichLocationX = whichLocation.x;
						final float whichLocationY = whichLocation.y;
						war3MapViewer.setBlight(whichLocationX, whichLocationY, radius, addBlight);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetBlight",
					(arguments, globalScope, triggerScope) -> {
						final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float radius = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
						final boolean addBlight = arguments.get(4).visit(BooleanJassValueVisitor.getInstance());
						war3MapViewer.setBlight(x, y, radius, addBlight);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetBlightPoint",
					(arguments, globalScope, triggerScope) -> {
						final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						final boolean addBlight = arguments.get(3).visit(BooleanJassValueVisitor.getInstance());
						war3MapViewer.setBlight(x, y, 1, addBlight);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetBlightRect",
					(arguments, globalScope, triggerScope) -> {
						final Rectangle rect = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						final boolean addBlight = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
						if (rect != null) {
							for (float x = rect.x; x <= (rect.x + rect.width); x += 128f) {
								for (float y = rect.y; y <= (rect.y + rect.height); y += 128f) {
									war3MapViewer.setBlight(x, y, 1, addBlight);
								}
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsPointBlighted",
					(arguments, globalScope, triggerScope) -> {
						final float x = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final PathingGrid pathingGrid = CommonEnvironment.this.simulation.getPathingGrid();
						return BooleanJassValue.of(pathingGrid.contains(x, y) && PathingGrid.PathingFlags
								.isPathingFlag(pathingGrid.getPathing(x, y), PathingGrid.PathingFlags.BLIGHTED));
					});
			// Only the map's own terrain pathing counts (not units, buildings or trees); outside the playable area
			// every type except floatability is blocked, and ANY is always blocked.
			jassProgramVisitor.getJassNativeManager().createNative("IsTerrainPathable",
					(arguments, globalScope, triggerScope) -> {
						final float x = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final CPathingTypeJass pathingType = nullable(arguments, 2,
								ObjectJassValueVisitor.<CPathingTypeJass>getInstance());
						final Rectangle entireMap = war3MapViewer.terrain.getEntireMap();
						if ((pathingType == null) || (pathingType == CPathingTypeJass.ANY) || (x < entireMap.x)
								|| (y < entireMap.y) || (x >= (entireMap.x + entireMap.width))
								|| (y >= (entireMap.y + entireMap.height))) {
							return BooleanJassValue.TRUE;
						}
						if (!war3MapViewer.terrain.getPlayableMapArea().contains(x, y)) {
							return BooleanJassValue.of(pathingType != CPathingTypeJass.FLOATABILITY);
						}
						final short pathing = CommonEnvironment.this.simulation.getPathingGrid().getTerrainPathing(x,
								y);
						switch (pathingType) {
						case WALKABILITY:
						case PEONHARVESTPATHING:
							return BooleanJassValue.of(PathingGrid.PathingFlags.isPathingFlag(pathing,
									PathingGrid.PathingFlags.UNWALKABLE));
						case FLYABILITY:
							return BooleanJassValue.of(PathingGrid.PathingFlags.isPathingFlag(pathing,
									PathingGrid.PathingFlags.UNFLYABLE));
						case BUILDABILITY:
							return BooleanJassValue.of(PathingGrid.PathingFlags.isPathingFlag(pathing,
									PathingGrid.PathingFlags.UNBUILDABLE));
						case BLIGHTPATHING:
							return BooleanJassValue.of(PathingGrid.PathingFlags.isPathingFlag(pathing,
									PathingGrid.PathingFlags.BLIGHTED));
						case FLOATABILITY:
							return BooleanJassValue.of(PathingGrid.PathingFlags.isPathingFlag(pathing,
									PathingGrid.PathingFlags.UNSWIMABLE));
						case AMPHIBIOUSPATHING:
							return BooleanJassValue.of(
									PathingGrid.PathingFlags.isPathingFlag(pathing, PathingGrid.PathingFlags.UNWALKABLE)
											&& PathingGrid.PathingFlags.isPathingFlag(pathing,
													PathingGrid.PathingFlags.UNSWIMABLE));
						default:
							return BooleanJassValue.TRUE;
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetLocalPlayer",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(playerType,
								CommonEnvironment.this.simulation.getPlayer(war3MapViewer.getLocalPlayerIndex()));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitInGroup",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.<CUnit>getInstance());
						final List<CUnit> group = nullable(arguments, 1,
								ObjectJassValueVisitor.<List<CUnit>>getInstance());
						return BooleanJassValue.of((whichUnit != null) && (group != null) && group.contains(whichUnit));
					});

			// Patch 1.23+ crap
			// jassProgramVisitor.getJassNativeManager().createNative("InitHashtable", new
			// JassFunction() {
			// @Override
			// public JassValue call(final List<JassValue> arguments, final GlobalScope
			// globalScope,
			// final TriggerExecutionScope triggerScope) {
			// return new HandleJassValue(hashtableType, new CHashtable());
			// }
			// });
			jassProgramVisitor.getJassNativeManager().createNative("InitHashtable",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(hashtableType, new CHashtable());
					});
			jassProgramVisitor.getJassNativeManager().createNative("SaveInteger", new SaveHashtableValueFunc(CHashtable.INTEGER));
			jassProgramVisitor.getJassNativeManager().createNative("SaveReal", new SaveHashtableValueFunc(CHashtable.REAL));
			jassProgramVisitor.getJassNativeManager().createNative("SaveBoolean", new SaveHashtableValueFunc(CHashtable.BOOLEAN));
			jassProgramVisitor.getJassNativeManager().createNative("SaveStr", new SaveHashtableValueFunc(CHashtable.STRING));
			jassProgramVisitor.getJassNativeManager().createNative("SavePlayerHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveWidgetHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveDestructableHandle",
					new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveItemHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveUnitHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveAbilityHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveTimerHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveTriggerHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveTriggerConditionHandle",
					new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveTriggerActionHandle",
					new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveTriggerEventHandle",
					new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveForceHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveGroupHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveLocationHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveRectHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveBooleanExprHandle",
					new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveSoundHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveEffectHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveUnitPoolHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveItemPoolHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveQuestHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveQuestItemHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveDefeatConditionHandle",
					new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveTimerDialogHandle",
					new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveLeaderboardHandle",
					new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveMultiboardHandle",
					new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveMultiboardItemHandle",
					new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveTrackableHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveDialogHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveButtonHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveTextTagHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveLightningHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveImageHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveUbersplatHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveRegionHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveFogStateHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveFogModifierHandle",
					new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveAgentHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveHashtableHandle", new SaveHashtableValueFunc());

			jassProgramVisitor.getJassNativeManager().createNative("LoadInteger",
					new LoadHashtableValueFunc(IntegerJassValue.ZERO));
			jassProgramVisitor.getJassNativeManager().createNative("LoadReal",
					new LoadHashtableValueFunc(RealJassValue.ZERO));
			jassProgramVisitor.getJassNativeManager().createNative("LoadBoolean",
					new LoadHashtableValueFunc(BooleanJassValue.FALSE));
			jassProgramVisitor.getJassNativeManager().createNative("LoadStr",
					new LoadHashtableValueFunc(StringJassValue.EMPTY_STRING));
			jassProgramVisitor.getJassNativeManager().createNative("LoadPlayerHandle",
					new LoadHashtableValueFunc(playerType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadWidgetHandle",
					new LoadHashtableValueFunc(widgetType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadDestructableHandle",
					new LoadHashtableValueFunc(destructableType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadItemHandle",
					new LoadHashtableValueFunc(itemType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadUnitHandle",
					new LoadHashtableValueFunc(unitType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadAbilityHandle",
					new LoadHashtableValueFunc(abilityType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadTimerHandle",
					new LoadHashtableValueFunc(timerType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadTriggerHandle",
					new LoadHashtableValueFunc(triggerType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadTriggerConditionHandle",
					new LoadHashtableValueFunc(triggerconditionType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadTriggerActionHandle",
					new LoadHashtableValueFunc(triggeractionType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadTriggerEventHandle",
					new LoadHashtableValueFunc(eventType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadForceHandle",
					new LoadHashtableValueFunc(forceType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadGroupHandle",
					new LoadHashtableValueFunc(groupType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadLocationHandle",
					new LoadHashtableValueFunc(locationType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadRectHandle",
					new LoadHashtableValueFunc(rectType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadBooleanExprHandle",
					new LoadHashtableValueFunc(boolexprType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadSoundHandle",
					new LoadHashtableValueFunc(soundType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadEffectHandle",
					new LoadHashtableValueFunc(effectType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadUnitPoolHandle",
					new LoadHashtableValueFunc(unitpoolType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadItemPoolHandle",
					new LoadHashtableValueFunc(itempoolType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadQuestHandle",
					new LoadHashtableValueFunc(questType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadQuestItemHandle",
					new LoadHashtableValueFunc(questitemType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadDefeatConditionHandle",
					new LoadHashtableValueFunc(defeatconditionType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadTimerDialogHandle",
					new LoadHashtableValueFunc(timerdialogType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadLeaderboardHandle",
					new LoadHashtableValueFunc(leaderboardType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadMultiboardHandle",
					new LoadHashtableValueFunc(multiboardType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadMultiboardItemHandle",
					new LoadHashtableValueFunc(multiboarditemType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadTrackableHandle",
					new LoadHashtableValueFunc(trackableType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadDialogHandle",
					new LoadHashtableValueFunc(dialogType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadButtonHandle",
					new LoadHashtableValueFunc(buttonType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadTextTagHandle",
					new LoadHashtableValueFunc(texttagType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadLightningHandle",
					new LoadHashtableValueFunc(lightningType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadImageHandle",
					new LoadHashtableValueFunc(imageType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadUbersplatHandle",
					new LoadHashtableValueFunc(ubersplatType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadRegionHandle",
					new LoadHashtableValueFunc(regionType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadFogStateHandle",
					new LoadHashtableValueFunc(fogstateType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadFogModifierHandle",
					new LoadHashtableValueFunc(fogmodifierType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadHashtableHandle",
					new LoadHashtableValueFunc(hashtableType.getNullValue()));
			jassProgramVisitor.getJassNativeManager().createNative("LoadAgentHandle",
					new LoadHashtableValueFunc(agentType.getNullValue()));

			jassProgramVisitor.getJassNativeManager().createNative("HaveSavedInteger",
					new HaveSavedHashtableValueFunc(JassType.INTEGER));
			jassProgramVisitor.getJassNativeManager().createNative("HaveSavedReal",
					new HaveSavedHashtableValueFunc(JassType.REAL));
			jassProgramVisitor.getJassNativeManager().createNative("HaveSavedBoolean",
					new HaveSavedHashtableValueFunc(JassType.BOOLEAN));
			jassProgramVisitor.getJassNativeManager().createNative("HaveSavedString",
					new HaveSavedHashtableValueFunc(JassType.STRING));
			jassProgramVisitor.getJassNativeManager().createNative("HaveSavedHandle",
					new HaveSavedHashtableValueFunc(agentType));
			jassProgramVisitor.getJassNativeManager().createNative("RemoveSavedInteger",
					new RemoveSavedHashtableValueFunc(CHashtable.INTEGER));
			jassProgramVisitor.getJassNativeManager().createNative("RemoveSavedReal",
					new RemoveSavedHashtableValueFunc(CHashtable.REAL));
			jassProgramVisitor.getJassNativeManager().createNative("RemoveSavedBoolean",
					new RemoveSavedHashtableValueFunc(CHashtable.BOOLEAN));
			jassProgramVisitor.getJassNativeManager().createNative("RemoveSavedString",
					new RemoveSavedHashtableValueFunc(CHashtable.STRING));
			jassProgramVisitor.getJassNativeManager().createNative("RemoveSavedHandle",
					new RemoveSavedHashtableValueFunc(CHashtable.HANDLE));
			jassProgramVisitor.getJassNativeManager().createNative("FlushChildHashtable",
					(arguments, globalScope, triggerScope) -> {
						final CHashtable table = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (table != null) {
							table.flushChild(arguments.get(1).visit(IntegerJassValueVisitor.getInstance()));
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("FlushParentHashtable",
					(arguments, globalScope, triggerScope) -> {
						final CHashtable table = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (table != null) {
							table.flushAll();
						}
						return null;
					});

			jassProgramVisitor.getJassNativeManager().createNative("GetExpiredTimer",
					(arguments, globalScope, triggerScope) -> {
						if (triggerScope instanceof CommonTriggerExecutionScope) {
							return new HandleJassValue(timerType,
									((CommonTriggerExecutionScope) triggerScope).getExpiringTimer());
						}
						else {
							return new HandleJassValue(timerType, null);
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetPlayerStructureCount",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer whichPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (whichPlayer == null) {
							return IntegerJassValue.ZERO;
						}
						final boolean includeIncomplete = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						final List<CUnit> units = CommonEnvironment.this.simulation.getUnitsIncludingNew();
						int count = 0;
						for (final CUnit unit : units) {
							if ((unit.getPlayerIndex() == whichPlayer.getId()) && unit.isBuilding() && !unit.isDead()) {
								if (includeIncomplete || !unit.isConstructing()) {
									count++;
								}
							}
						}
						return IntegerJassValue.of(count);
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetPlayerTypedUnitCount",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer whichPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (whichPlayer == null) {
							return IntegerJassValue.ZERO;
						}
						final String legacySystemUnitTypeName = arguments.get(1)
								.visit(StringJassValueVisitor.getInstance());
						final boolean includeIncomplete = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
						final boolean includeUpgrades = arguments.get(3).visit(BooleanJassValueVisitor.getInstance());
						final List<CUnit> units = CommonEnvironment.this.simulation.getUnitsIncludingNew();
						int count = 0;
						final Set<War3ID> countedTypes = includeUpgrades
								? typeAndUpgradesForLegacyName(legacySystemUnitTypeName)
								: null;
						for (final CUnit unit : units) {
							if ((unit.getPlayerIndex() == whichPlayer.getId()) && !unit.isDead()) {
								final boolean matches = countedTypes != null
										? countedTypes.contains(unit.getTypeId())
										: legacySystemUnitTypeName.equals(unit.getUnitType().getLegacyName());
								if (matches && (includeIncomplete || !unit.isConstructing())) {
									count++;
								}
							}
						}
						return IntegerJassValue.of(count);
					});
			jassProgramVisitor.getJassNativeManager().createNative("BlzGetPlayerTownHallCount",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer whichPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						if (whichPlayer == null) {
							return IntegerJassValue.ZERO;
						}
						int count = 0;
						for (final CUnit unit : CommonEnvironment.this.simulation.getUnitsIncludingNew()) {
							if ((unit.getPlayerIndex() == whichPlayer.getId()) && !unit.isDead()
									&& unit.isUnitType(CUnitTypeJass.TOWNHALL)) {
								count++;
							}
						}
						return IntegerJassValue.of(count);
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsVisibleToPlayer",
					(arguments, globalScope, triggerScope) -> {
						final float x = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final CPlayer whichPlayer = nullable(arguments, 2, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichPlayer != null) && (fogState(whichPlayer, x, y) == CFogState.VISIBLE));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsLocationVisibleToPlayer",
					(arguments, globalScope, triggerScope) -> {
						final AbilityPointTarget whichLocation = nullable(arguments, 0,
								ObjectJassValueVisitor.getInstance());
						final CPlayer whichPlayer = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichLocation != null) && (whichPlayer != null)
								&& (fogState(whichPlayer, whichLocation.getX(), whichLocation.getY()) == CFogState.VISIBLE));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsFoggedToPlayer",
					(arguments, globalScope, triggerScope) -> {
						final float x = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final CPlayer whichPlayer = nullable(arguments, 2, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichPlayer != null) && (fogState(whichPlayer, x, y) == CFogState.FOGGED));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsLocationFoggedToPlayer",
					(arguments, globalScope, triggerScope) -> {
						final AbilityPointTarget whichLocation = nullable(arguments, 0,
								ObjectJassValueVisitor.getInstance());
						final CPlayer whichPlayer = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichLocation != null) && (whichPlayer != null)
								&& (fogState(whichPlayer, whichLocation.getX(), whichLocation.getY()) == CFogState.FOGGED));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsMaskedToPlayer",
					(arguments, globalScope, triggerScope) -> {
						final float x = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
						final float y = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						final CPlayer whichPlayer = nullable(arguments, 2, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichPlayer != null) && (fogState(whichPlayer, x, y) == CFogState.MASKED));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsLocationMaskedToPlayer",
					(arguments, globalScope, triggerScope) -> {
						final AbilityPointTarget whichLocation = nullable(arguments, 0,
								ObjectJassValueVisitor.getInstance());
						final CPlayer whichPlayer = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichLocation != null) && (whichPlayer != null)
								&& (fogState(whichPlayer, whichLocation.getX(), whichLocation.getY()) == CFogState.MASKED));
					});
			// Measured on 1.29: the unit natives read the fog where the unit stands, even for the player's own units, and
			// IsUnitInvisible is simply "not visible", also for units in fog or black mask.
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitVisible",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CPlayer whichPlayer = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichUnit != null) && (whichPlayer != null)
								&& isUnitVisibleToPlayer(whichUnit, whichPlayer));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitInvisible",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CPlayer whichPlayer = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichUnit != null) && (whichPlayer != null)
								&& !isUnitVisibleToPlayer(whichUnit, whichPlayer));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitFogged",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CPlayer whichPlayer = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichUnit != null) && (whichPlayer != null)
								&& (unitFogState(whichUnit, whichPlayer) == CFogState.FOGGED)
								&& !this.simulation.isUnitVisionShared(whichUnit, whichPlayer));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitMasked",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CPlayer whichPlayer = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichUnit != null) && (whichPlayer != null)
								&& (unitFogState(whichUnit, whichPlayer) == CFogState.MASKED)
								&& !this.simulation.isUnitVisionShared(whichUnit, whichPlayer));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitEnemy",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CPlayer whichPlayer = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						if ((whichUnit == null) || (whichPlayer == null)) {
							return BooleanJassValue.FALSE;
						}
						return BooleanJassValue.of(!whichUnit.isUnitAlly(whichPlayer));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitAlly",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CPlayer whichPlayer = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						if ((whichUnit == null) || (whichPlayer == null)) {
							return BooleanJassValue.FALSE;
						}
						return BooleanJassValue.of(whichUnit.isUnitAlly(whichPlayer));
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsNoVictoryCheat",
					(arguments, globalScope, triggerScope) -> {
						return BooleanJassValue.FALSE;
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsNoDefeatCheat",
					(arguments, globalScope, triggerScope) -> {
						return BooleanJassValue.FALSE;
					});
			// Measured on 1.29 (cheat-native-d296): the release chat cheats work from a script and affect every
			// player; the developer cheats campaign AI scripts call (showmap, av, dg, vision N, gold N, ...) do
			// nothing.
			jassProgramVisitor.getJassNativeManager().createNative("Cheat",
					(arguments, globalScope, triggerScope) -> {
						final String cheat = nullable(arguments, 0, StringJassValueVisitor.getInstance());
						if (cheat != null) {
							applyReleaseCheat(cheat.trim().toLowerCase(java.util.Locale.US));
						}
						return null;
					});

			jassProgramVisitor.getJassNativeManager().createNative("StartMeleeAI",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final String filePath = nullable(arguments, 1, StringJassValueVisitor.getInstance());
						if (filePath != null) {
							doAIScript(dataSource, uiViewport, uiScene, war3MapViewer, player, filePath, meleeUI,
									originalFiles, jassProgramVisitor, this.simulation, "main", true);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("StartMeleeAI",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final String filePath = nullable(arguments, 1, StringJassValueVisitor.getInstance());
						if (filePath != null) {
							doAIScript(dataSource, uiViewport, uiScene, war3MapViewer, player, filePath, meleeUI,
									originalFiles, jassProgramVisitor, this.simulation, "main", true);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("StartCampaignAI",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final String filePath = nullable(arguments, 1, StringJassValueVisitor.getInstance());
						if ((player != null) && (filePath != null)) {
							doAIScript(dataSource, uiViewport, uiScene, war3MapViewer, player, filePath, meleeUI,
									originalFiles, jassProgramVisitor, this.simulation, "main", true);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("CommandAI",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if ((player != null) && (player.getAiScript() != null)) {
							player.getAiScript().command(arguments.get(1).visit(IntegerJassValueVisitor.getInstance()),
									arguments.get(2).visit(IntegerJassValueVisitor.getInstance()));
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetAIDifficulty",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final AIDifficulty difficulty = player == null ? null : player.getAIDifficulty();
						return new HandleJassValue(aidifficultyType,
								difficulty == null ? AIDifficulty.NORMAL : difficulty);
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitDetected",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CPlayer whichPlayer = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichUnit != null) && (whichPlayer != null)
								&& whichUnit.isVisible(CommonEnvironment.this.simulation, whichPlayer.getId()));
					});
			jassProgramVisitor.getJassNativeManager().createNative("PauseCompAI",
					(arguments, globalScope, triggerScope) -> {
						final CPlayer player = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean pause = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						if (player != null) {
							if (player.getAiScript() != null) {
								player.getAiScript().setPaused(pause);
							}
						}
						return null;
					});
			////// ====== Common AI functions =======
			/// These are getting declared in Jass2 here so that you can magically use them
			////// from outside
			// common ai, maybe, sometimes
			registerSharedCommonAIFunctions(jassProgramVisitor, war3MapViewer, meleeUI, this.simulation);

			// Warsmash Ability API
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitMoveFollowBehavior",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int highlightOrderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final CWidget whichFollowTarget = nullable(arguments, 2, ObjectJassValueVisitor.getInstance());
						if (unit == null) {
							return null;
						}
						return new HandleJassValue(abilitybehaviorType,
								unit.getFollowBehavior().reset(this.simulation, highlightOrderId, whichFollowTarget));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitMovePointBehavior",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int highlightOrderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final double targetX = arguments.get(2).visit(RealJassValueVisitor.getInstance());
						final double targetY = arguments.get(3).visit(RealJassValueVisitor.getInstance());
						if (unit == null) {
							return null;
						}
						return new HandleJassValue(abilitybehaviorType, unit.getMoveBehavior().reset(highlightOrderId,
								new AbilityPointTarget((float) targetX, (float) targetY)));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitMovePointBehaviorLoc",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int highlightOrderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final AbilityPointTarget target = arguments.get(2).visit(ObjectJassValueVisitor.getInstance());
						if (unit == null) {
							return null;
						}

						return new HandleJassValue(abilitybehaviorType, unit.getMoveBehavior().reset(highlightOrderId,
								new AbilityPointTarget(target.x, target.y)));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitAttackMovePointBehavior",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final double targetX = arguments.get(1).visit(RealJassValueVisitor.getInstance());
						final double targetY = arguments.get(2).visit(RealJassValueVisitor.getInstance());
						if (unit == null) {
							return null;
						}
						return new HandleJassValue(abilitybehaviorType, unit.getAttackMoveBehavior()
								.reset(new AbilityPointTarget((float) targetX, (float) targetY)));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitAttackMovePointBehaviorLoc",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final AbilityPointTarget target = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						if (unit == null) {
							return null;
						}
						return new HandleJassValue(abilitybehaviorType,
								unit.getAttackMoveBehavior().reset(new AbilityPointTarget(target.x, target.y)));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitAttackWidgetBehavior",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int highlightOrderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final int whichUnitAttackIndex = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						final CWidget target = nullable(arguments, 3, ObjectJassValueVisitor.getInstance());
						if (unit == null) {
							return null;
						}
						return new HandleJassValue(abilitybehaviorType,
								unit.getAttackBehavior().reset(this.simulation, highlightOrderId,
										unit.getCurrentAttacks().get(whichUnitAttackIndex), target, false,
										CBehaviorAttackListener.DO_NOTHING));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitAttackGroundBehavior",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int highlightOrderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final int whichUnitAttackIndex = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						final double targetX = arguments.get(3).visit(RealJassValueVisitor.getInstance());
						final double targetY = arguments.get(4).visit(RealJassValueVisitor.getInstance());
						if (unit == null) {
							return null;
						}
						return new HandleJassValue(abilitybehaviorType,
								unit.getAttackBehavior().reset(this.simulation, highlightOrderId,
										unit.getCurrentAttacks().get(whichUnitAttackIndex),
										new AbilityPointTarget((float) targetX, (float) targetY), false,
										CBehaviorAttackListener.DO_NOTHING));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitAttackGroundBehaviorLoc",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int highlightOrderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final int whichUnitAttackIndex = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						final AbilityPointTarget target = arguments.get(3).visit(ObjectJassValueVisitor.getInstance());
						if (unit == null) {
							return null;
						}
						return new HandleJassValue(abilitybehaviorType, unit.getAttackBehavior().reset(this.simulation,
								highlightOrderId, unit.getCurrentAttacks().get(whichUnitAttackIndex),
								new AbilityPointTarget(target.x, target.y), false, CBehaviorAttackListener.DO_NOTHING));
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitPollNextOrderBehavior",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						return new HandleJassValue(abilitybehaviorType,
								whichUnit.pollNextOrderBehavior(CommonEnvironment.this.simulation));
					});
			jassProgramVisitor.getJassNativeManager().createNative("CreateAbilityBehavior",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(abilitybehaviorType, new CBehaviorJass(globalScope));
					});
			jassProgramVisitor.getJassNativeManager().createNative("DestroyAbilityBehavior",
					(arguments, globalScope, triggerScope) -> {
						return null;
					});
			abilitybehaviorType.setConstructorNative(new HandleJassTypeConstructor("CreateAbilityBehavior"));
			abilitybehaviorType.setDestructorNative(new HandleJassTypeConstructor("DestroyAbilityBehavior"));
			jassProgramVisitor.getJassNativeManager().createNative("CreateRangedBehavior",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(rangedbehaviorType, new CRangedBehaviorJass(globalScope));
					});
			jassProgramVisitor.getJassNativeManager().createNative("DestroyRangedBehavior",
					(arguments, globalScope, triggerScope) -> {
						return null;
					});
			rangedbehaviorType.setConstructorNative(new HandleJassTypeConstructor("CreateRangedBehavior"));
			rangedbehaviorType.setDestructorNative(new HandleJassTypeConstructor("DestroyRangedBehavior"));

			jassProgramVisitor.getJassNativeManager().createNative("CreateAbstractRangedBehavior",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return new HandleJassValue(abstractrangedbehaviorType,
								new CAbstractRangedBehaviorJass(unit, globalScope));
					});
			jassProgramVisitor.getJassNativeManager().createNative("DestroyAbstractRangedBehavior",
					(arguments, globalScope, triggerScope) -> {
						return null;
					});
			abstractrangedbehaviorType
					.setConstructorNative(new HandleJassTypeConstructor("CreateAbstractRangedBehavior"));
			abstractrangedbehaviorType
					.setDestructorNative(new HandleJassTypeConstructor("DestroyAbstractRangedBehavior"));
			jassProgramVisitor.getJassNativeManager().createNative("AbstractRangedBehaviorResetI",
					(arguments, globalScope, triggerScope) -> {
						final CAbstractRangedBehaviorJass whichBehavior = nullable(arguments, 0,
								ObjectJassValueVisitor.getInstance());
						final AbilityTarget target = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						return new HandleJassValue(abilitybehaviorType,
								whichBehavior.resetNative(this.simulation, target));
					});
			jassProgramVisitor.getJassNativeManager().createNative("AbstractRangedBehaviorResetII",
					(arguments, globalScope, triggerScope) -> {
						final CAbstractRangedBehaviorJass whichBehavior = nullable(arguments, 0,
								ObjectJassValueVisitor.getInstance());
						final AbilityTarget target = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						final boolean disableCollision = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						return new HandleJassValue(abilitybehaviorType,
								whichBehavior.resetNative(this.simulation, target, disableCollision));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetRangedBehaviorTarget",
					(arguments, globalScope, triggerScope) -> {
						final CBehavior whichBehavior = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return new HandleJassValue(abilitytargetType,
								whichBehavior.visit(BehaviorTargetVisitor.INSTANCE));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetAbstractRangedBehaviorSourceUnit",
					(arguments, globalScope, triggerScope) -> {
						final CAbstractRangedBehaviorJass whichBehavior = nullable(arguments, 0,
								ObjectJassValueVisitor.getInstance());
						return new HandleJassValue(unitType, whichBehavior.getUnit());
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitCanReach",
					(arguments, globalScope, triggerScope) -> {
						final CUnit source = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final AbilityTarget target = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						final float radius = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						if (target == null) {
							return BooleanJassValue.FALSE;
						}
						return BooleanJassValue.of(source.canReach(target, radius));
					});

			jassProgramVisitor.getJassNativeManager().createNative("CreateBehaviorExpr",
					(arguments, globalScope, triggerScope) -> {
						final CodeJassValue func = arguments.get(0).visit(CodeJassValueVisitor.getInstance());
						return new HandleJassValue(behaviorexprType, new CodeJassValueBehaviorExpr(func));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetSpellAbilityOrderId",
					(arguments, globalScope, triggerScope) -> {
						return IntegerJassValue
								.of(((CommonTriggerExecutionScope) triggerScope).getSpellAbilityOrderId());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetSpellAbilityOrderButton",
					(arguments, globalScope, triggerScope) -> {
						return IntegerJassValue
								.of(((CommonTriggerExecutionScope) triggerScope).getSpellAbilityOrderId());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetSpellTargetType",
					(arguments, globalScope, triggerScope) -> {
						return new HandleJassValue(orderbuttontypeType,
								((CommonTriggerExecutionScope) triggerScope).getSpellAbilityTargetType());
					});
			jassProgramVisitor.getJassNativeManager().createNative("RegisterAbilityType",
					(arguments, globalScope, triggerScope) -> {
						final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final CAbilityTypeJassDefinition whichAbilityType = nullable(arguments, 1,
								ObjectJassValueVisitor.getInstance());
						CommonEnvironment.this.simulation.getAbilityData().registerJassType(new War3ID(rawcode),
								whichAbilityType);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("RegisterAbilityStructType",
					(arguments, globalScope, triggerScope) -> {
						final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final StaticStructTypeJassValue whichAbilityType = nullable(arguments, 1,
								StaticStructTypeJassValueVisitor.getInstance());
						final War3ID codeId = new War3ID(rawcode);
						CommonEnvironment.this.simulation.getAbilityData().registerJassType(codeId,
								new CAbilityTypeJassDefinition(globalScope, whichAbilityType));
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitIconUI",
					(arguments, globalScope, triggerScope) -> {
						final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						return new HandleJassValue(iconuiType,
								war3MapViewer.getAbilityDataUI().getUnitUI(new War3ID(rawcode)));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetAbilityOnIconUI",
					(arguments, globalScope, triggerScope) -> {
						final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final int level = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						return new HandleJassValue(iconuiType,
								war3MapViewer.getAbilityDataUI().getUI(new War3ID(rawcode)).getOnIconUI(level));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetAbilityOffIconUI",
					(arguments, globalScope, triggerScope) -> {
						final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final int level = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						return new HandleJassValue(iconuiType,
								war3MapViewer.getAbilityDataUI().getUI(new War3ID(rawcode)).getOffIconUI(level));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetAbilityLearnIconUI",
					(arguments, globalScope, triggerScope) -> {
						final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						return new HandleJassValue(iconuiType,
								war3MapViewer.getAbilityDataUI().getUI(new War3ID(rawcode)).getLearnIconUI());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetItemIconUI",
					(arguments, globalScope, triggerScope) -> {
						final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						return new HandleJassValue(iconuiType,
								war3MapViewer.getAbilityDataUI().getItemUI(new War3ID(rawcode)));
					});
			jassProgramVisitor.getJassNativeManager().createNative("CreateOrderButton",
					(arguments, globalScope, triggerScope) -> {
						final JassOrderButtonType type = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int orderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final CAbilityOrderButtonJass javaValue = new CAbilityOrderButtonJass(
								this.simulation.getHandleIdAllocator().createId(), orderId);
						javaValue.setType(type);
						war3MapViewer.getAbilityDataUI().createRenderPeer(javaValue);
						return new HandleJassValue(orderbuttonType, javaValue);
					});
			jassProgramVisitor.getJassNativeManager().createNative("DestroyOrderButton",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						war3MapViewer.getAbilityDataUI().removeRenderPeer(orderCommandCard);
						return null;
					});
			orderbuttonType.setConstructorNative(new HandleJassTypeConstructor("CreateOrderButton"));
			orderbuttonType.setDestructorNative(new HandleJassTypeConstructor("DestroyOrderButton"));
			jassProgramVisitor.getJassNativeManager().createNative("AbilityAddOrderButton",
					(arguments, globalScope, triggerScope) -> {
						final CAbilityJass abilityTypeTmp = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final CAbilityOrderButtonJass commandCard = arguments.get(1)
								.visit(ObjectJassValueVisitor.getInstance());
						abilityTypeTmp.addJassOrder(commandCard);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("AbilityRemoveOrderButton",
					(arguments, globalScope, triggerScope) -> {
						final CAbilityJass abilityTypeTmp = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final CAbilityOrderButtonJass commandCard = arguments.get(1)
								.visit(ObjectJassValueVisitor.getInstance());
						abilityTypeTmp.removeJassOrder(commandCard);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonAutoCastOrderId",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final int autoCastOrderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						orderCommandCard.setAutoCastOrderId(autoCastOrderId);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonUnAutoCastOrderId",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final int autoCastUnOrderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						orderCommandCard.setAutoCastUnOrderId(autoCastUnOrderId);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonContainerMenuOrderId",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final int orderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						orderCommandCard.setContainerMenuOrderId(orderId);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonDisabled",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final boolean disabled = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						orderCommandCard.setDisabled(disabled);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonManaCost",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final int manaCost = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						orderCommandCard.setManaCost(manaCost);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonGoldCost",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final int goldCost = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						orderCommandCard.setGoldCost(goldCost);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonLumberCost",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final int lumberCost = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						orderCommandCard.setLumberCost(lumberCost);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonFoodCost",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final int foodCost = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						orderCommandCard.setFoodCost(foodCost);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonCharges",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final int charges = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						orderCommandCard.setCharges(charges);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonAutoCastActive",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final boolean active = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						orderCommandCard.setAutoCastActive(active);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonHidden",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final boolean hidden = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
//						orderCommandCard.setHidden(hidden);
						throw new UnsupportedOperationException();
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonIconPath",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final String iconPath = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						final AbilityDataUI abilityDataUI = war3MapViewer.getAbilityDataUI();
						final OrderButtonUI peer = abilityDataUI.getRenderPeer(orderCommandCard);
						peer.setIcon(this.gameUI.loadTexture(iconPath));
						peer.setIconDisabled(this.gameUI
								.loadTexture(AbilityDataUI.disable(iconPath, abilityDataUI.getDisabledPrefix())));
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonButtonPositionX",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final int buttonPosX = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						war3MapViewer.getAbilityDataUI().getRenderPeer(orderCommandCard).setButtonPositionX(buttonPosX);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonButtonPositionY",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final int buttonPosY = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						war3MapViewer.getAbilityDataUI().getRenderPeer(orderCommandCard).setButtonPositionY(buttonPosY);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonToolTip",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final String tip = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						war3MapViewer.getAbilityDataUI().getRenderPeer(orderCommandCard).setTip(tip);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonUberTip",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final String uberTip = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						war3MapViewer.getAbilityDataUI().getRenderPeer(orderCommandCard).setUberTip(uberTip);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonHotKey",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final String hotkeyString = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						war3MapViewer.getAbilityDataUI().getRenderPeer(orderCommandCard)
								.setHotkey(hotkeyString.charAt(0));
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonByIconUI",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final IconUI theIconUi = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						war3MapViewer.getAbilityDataUI().getRenderPeer(orderCommandCard).setFromIconUI(theIconUi);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonPreviewBuildUnitId",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final int unitId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						war3MapViewer.getAbilityDataUI().getRenderPeer(orderCommandCard)
								.setPreviewBuildUnitId(new War3ID(unitId));
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonAOE",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final double radius = arguments.get(1).visit(RealJassValueVisitor.getInstance());
						war3MapViewer.getAbilityDataUI().getRenderPeer(orderCommandCard)
								.setMouseTargetRadius((float) radius);
						return null;
					});

			jassProgramVisitor.getJassNativeManager().createNative("GetOrderButtonOrderId",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						return IntegerJassValue.of(orderCommandCard.getOrderId());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderButtonAutoCastOrderId",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						return IntegerJassValue.of(orderCommandCard.getAutoCastOrderId());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderButtonUnAutoCastOrderId",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						return IntegerJassValue.of(orderCommandCard.getAutoCastUnOrderId());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderButtonContainerMenuOrderId",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						return IntegerJassValue.of(orderCommandCard.getContainerMenuOrderId());
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsOrderButtonDisabled",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of(orderCommandCard.isDisabled());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderButtonManaCost",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						return IntegerJassValue.of(orderCommandCard.getManaCost());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderButtonGoldCost",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						return IntegerJassValue.of(orderCommandCard.getGoldCost());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderButtonLumberCost",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						return IntegerJassValue.of(orderCommandCard.getLumberCost());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderButtonFoodCost",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						return IntegerJassValue.of(orderCommandCard.getFoodCost());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderButtonCharges",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						return IntegerJassValue.of(orderCommandCard.getCharges());
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsOrderButtonAutoCastActive",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of(orderCommandCard.isAutoCastActive());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderButtonButtonPositionX",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						return IntegerJassValue.of(
								war3MapViewer.getAbilityDataUI().getRenderPeer(orderCommandCard).getButtonPositionX());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderButtonButtonPositionY",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						return IntegerJassValue.of(
								war3MapViewer.getAbilityDataUI().getRenderPeer(orderCommandCard).getButtonPositionY());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderButtonToolTip",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						return StringJassValue
								.of(war3MapViewer.getAbilityDataUI().getRenderPeer(orderCommandCard).getTip());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderButtonUberTip",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						return StringJassValue
								.of(war3MapViewer.getAbilityDataUI().getRenderPeer(orderCommandCard).getUberTip());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderButtonHotKey",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						return StringJassValue.of(String
								.valueOf(war3MapViewer.getAbilityDataUI().getRenderPeer(orderCommandCard).getHotkey()));
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderButtonPreviewBuildUnitId",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						return IntegerJassValue.of(war3MapViewer.getAbilityDataUI().getRenderPeer(orderCommandCard)
								.getPreviewBuildUnitId().getValue());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderButtonAOE",
					(arguments, globalScope, triggerScope) -> {
						final COrderButton orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						return RealJassValue.of(war3MapViewer.getAbilityDataUI().getRenderPeer(orderCommandCard)
								.getMouseTargetRadius());
					});
			jassProgramVisitor.getJassNativeManager().createNative("FailUsableCheckOnRequirement",
					(arguments, globalScope, triggerScope) -> {
						final CAbilityOrderButtonJass orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final int techId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final int techLevel = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						orderCommandCard.getUsableReceiver().missingRequirement(new War3ID(techId), techLevel);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("FailUsableCheckOnHeroLevelRequirement",
					(arguments, globalScope, triggerScope) -> {
						final CAbilityOrderButtonJass orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final int heroLevel = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						orderCommandCard.getUsableReceiver().missingHeroLevelRequirement(heroLevel);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("FailUsableCheckOnCooldown",
					(arguments, globalScope, triggerScope) -> {
						final CAbilityOrderButtonJass orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final float cooldownRemaining = arguments.get(1).visit(RealJassValueVisitor.getInstance())
								.floatValue();
						final float cooldown = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						orderCommandCard.getUsableReceiver().cooldownNotYetReady(cooldownRemaining, cooldown);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("FailUsableCheckWithMessage",
					(arguments, globalScope, triggerScope) -> {
						final CAbilityOrderButtonJass orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final String message = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						orderCommandCard.getUsableReceiver().activationCheckFailed(message);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("FailTargetCheckWithMessage",
					(arguments, globalScope, triggerScope) -> {
						final CAbilityOrderButtonJass orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final String message = arguments.get(1).visit(StringJassValueVisitor.getInstance());
						orderCommandCard.getTargetReceiver().targetCheckFailed(message);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("PassTargetCheck",
					(arguments, globalScope, triggerScope) -> {
						final CAbilityOrderButtonJass orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final Object finalizedTarget = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						orderCommandCard.getTargetReceiver().targetOk(finalizedTarget);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("PassUsableCheck",
					(arguments, globalScope, triggerScope) -> {
						final CAbilityOrderButtonJass orderCommandCard = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						orderCommandCard.getUsableReceiver().useOk();
						return null;
					});

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
								new HandleList(this.simulation.getHandleIdAllocator().createId()));
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
								new StringList(this.simulation.getHandleIdAllocator().createId()));
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
							doPreloadScript(dataSource, uiViewport, uiScene, war3MapViewer, filePath, meleeUI,
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
										this.simulation.getHandleIdAllocator().createId());
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

			final JassFunction createAbilityFxn = (arguments, globalScope, triggerScope) -> {
				final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				final CAbility ability = this.simulation.getAbilityData().getAbilityType(new War3ID(rawcode))
						.createAbility(this.simulation.getHandleIdAllocator().createId());
				return new HandleJassValue(abilityType, ability);
			};
			jassProgramVisitor.getJassNativeManager().createNative("CreateAbility", createAbilityFxn);

			final JassFunction createJassAbilityFxn = (arguments, globalScope, triggerScope) -> {
				final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				final CAbility ability = new CAbilityJass(this.simulation.getHandleIdAllocator().createId(),
						new War3ID(rawcode), globalScope);
				return new HandleJassValue(abilityType, ability);
			};
			jassProgramVisitor.getJassNativeManager().createNative("CreateJassAbility", createJassAbilityFxn);
			abilityType.setConstructorNative(new HandleJassTypeConstructor("CreateJassAbility"));

			final JassFunction createJassBuffFxn = (arguments, globalScope, triggerScope) -> {
				final int codeId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				final int aliasId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
				final CBuffJass ability = new CBuffJass(this.simulation.getHandleIdAllocator().createId(),
						new War3ID(codeId), new War3ID(aliasId), globalScope);
				return new HandleJassValue(buffType, ability);
			};
			jassProgramVisitor.getJassNativeManager().createNative("CreateJassBuff", createJassBuffFxn);
			buffType.setConstructorNative(new HandleJassTypeConstructor("CreateJassBuff"));

			final JassFunction getUnitAbilityByIndex = (arguments, globalScope, triggerScope) -> {
				final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
				final int whichAbilityIndex = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());

				final List<CAbility> abilities = whichUnit.getAbilities();
				return new HandleJassValue(abilityType,
						(whichAbilityIndex < abilities.size()) ? abilities.get(whichAbilityIndex) : null);
			};
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitAbilityByIndex", getUnitAbilityByIndex);
			jassProgramVisitor.getJassNativeManager().createNative("BlzGetUnitAbilityByIndex", getUnitAbilityByIndex);
			final JassFunction getUnitAbilityById = (arguments, globalScope, triggerScope) -> {
				final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
				final int whichAbilityId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());

				return new HandleJassValue(abilityType, whichUnit
						.getAbility(GetAbilityByRawcodeVisitor.getInstance().reset(new War3ID(whichAbilityId))));
			};
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitAbility", getUnitAbilityById);
			jassProgramVisitor.getJassNativeManager().createNative("BlzGetUnitAbility", getUnitAbilityById);

			jassProgramVisitor.getJassNativeManager().createNative("AddUnitAbility",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CAbility ability = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						unit.add(this.simulation, ability);
						return null;
					});

			jassProgramVisitor.getJassNativeManager().createNative("RemoveUnitAbility",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CAbility ability = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						if (ability instanceof CBuff) {
							// NOTE: Retera writing this native, but I was not author of
							// the remove(CBuff) function being independent from remove(CAbility).
							// The difference is probably dumb.
							unit.remove(this.simulation, (CBuff) ability);
						}
						else {
							unit.remove(this.simulation, ability);
						}
						return null;
					});

			final JassFunction getAbilityAliasId = (arguments, globalScope, triggerScope) -> {
				final CAbility ability = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
				return IntegerJassValue.of(ability.getAlias().getValue());
			};
			jassProgramVisitor.getJassNativeManager().createNative("GetAbilityAliasId", getAbilityAliasId);
			jassProgramVisitor.getJassNativeManager().createNative("BlzGetAbilityId", getAbilityAliasId);
			jassProgramVisitor.getJassNativeManager().createNative("GetAbilityCodeId",
					(arguments, globalScope, triggerScope) -> {
						final CAbility ability = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						return IntegerJassValue.of(ability.getCode().getValue());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetAbilityLevel",
					(arguments, globalScope, triggerScope) -> {
						final CLevelingAbility ability = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						return IntegerJassValue.of(ability.getLevel());
					});

			jassProgramVisitor.getJassNativeManager().createNative("SetAbilityIconShowing",
					(arguments, globalScope, triggerScope) -> {
						final CAbility ability = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						ability.setIconShowing(flag);
						return null;
					});

			jassProgramVisitor.getJassNativeManager().createNative("SetAbilityPermanent",
					(arguments, globalScope, triggerScope) -> {
						final CAbility ability = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						ability.setPermanent(flag);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetAbilityPhysical",
					(arguments, globalScope, triggerScope) -> {
						final CAbilityJass ability = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						ability.setPhysical(flag);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetAbilityUniversal",
					(arguments, globalScope, triggerScope) -> {
						final CAbilityJass ability = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						ability.setUniversal(flag);
						return null;
					});

			jassProgramVisitor.getJassNativeManager().createNative("IsAbilityPermanent",
					(arguments, globalScope, triggerScope) -> {
						final CAbility ability = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of(ability.isPermanent());
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsAbilityPhysical",
					(arguments, globalScope, triggerScope) -> {
						final CAbilityJass ability = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of(ability.isPhysical());
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsAbilityUniversal",
					(arguments, globalScope, triggerScope) -> {
						final CAbilityJass ability = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of(ability.isUniversal());
					});

			jassProgramVisitor.getJassNativeManager().createNative("SetAbilityEnabledWhileUpgrading",
					(arguments, globalScope, triggerScope) -> {
						final CAbilityJass ability = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						ability.setEnabledWhileUpgrading(flag);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetAbilityEnabledWhileUnderConstruction",
					(arguments, globalScope, triggerScope) -> {
						final CAbilityJass ability = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						ability.setEnabledWhileUnderConstruction(flag);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterOnTick",
					(arguments, globalScope, triggerScope) -> {
						final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						this.simulation.registerOnTickEvent(trigger);
						return new HandleJassValue(eventType, new RemovableTriggerEvent(trigger) {
							@Override
							public void remove() {
								CommonEnvironment.this.simulation.unregisterOnTickEvent(trigger);
							}
						});
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterOnUnitTick",
					(arguments, globalScope, triggerScope) -> {
						final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CUnit unit = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						unit.registerOnTickEvent(trigger);
						return new HandleJassValue(eventType, new RemovableTriggerEvent(trigger) {
							@Override
							public void remove() {
								unit.unregisterOnTickEvent(trigger);
							}
						});
					});
			jassProgramVisitor.getJassNativeManager().createNative("ShowInterfaceError",
					(arguments, globalScope, triggerScope) -> {
						final CPlayerJass whichPlayer = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final String text = nullable(arguments, 1, StringJassValueVisitor.getInstance());
						if ((whichPlayer != null) && (text != null)) {
							this.simulation.getCommandErrorListener().showInterfaceError(whichPlayer.getId(), text);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("RemoveTriggerEvent",
					(arguments, globalScope, triggerScope) -> {
						final Trigger whichTrigger = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final RemovableTriggerEvent evt = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						if (whichTrigger != null) {
							whichTrigger.removeEvent(evt);
						}
						return null;
					});

			jassProgramVisitor.getJassNativeManager().createNative("ConvertAbilityDisableType",
					(arguments, globalScope, triggerScope) -> {
						final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						return enumValue(abilitydisabletypeType, CAbilityDisableType.VALUES, i);
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetAbilityDisabled",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CAbility ability = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						final boolean flag = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
						CAbilityDisableType disableType = nullable(arguments, 3, ObjectJassValueVisitor.getInstance());
						if (disableType == null) {
							disableType = CAbilityDisableType.TRIGGER;
						}
						ability.setDisabled(flag, disableType);
						unit.checkDisabledAbilities(this.simulation, flag);
						return null;
					});

			final JassFunction endUnitAbilityCooldown = (arguments, globalScope, triggerScope) -> {
				final CUnit theUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
				final int aliasId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
				final AbilityBuilderAbility abil = theUnit
						.getAbility(GetABAbilityByRawcodeVisitor.getInstance().reset(new War3ID(aliasId)));
				if (abil != null) {
					abil.resetCooldown(this.simulation, theUnit);
				}
				return null;
			};
			jassProgramVisitor.getJassNativeManager().createNative("EndUnitAbilityCooldown", endUnitAbilityCooldown);
			jassProgramVisitor.getJassNativeManager().createNative("BlzEndUnitAbilityCooldown", endUnitAbilityCooldown);

			jassProgramVisitor.getJassNativeManager().createNative("EndAbilityCooldown",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final AbilityBuilderAbility ability = arguments.get(1)
								.visit(ObjectJassValueVisitor.getInstance());
						ability.resetCooldown(this.simulation, unit);
						return null;
					});

			final JassFunction startUnitAbilityCooldown = (arguments, globalScope, triggerScope) -> {
				final CUnit theUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
				final int aliasIdRawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
				final float cooldown = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
				final War3ID aliasId = new War3ID(aliasIdRawcode);
				theUnit.beginCooldown(this.simulation, aliasId, cooldown);
				return null;
			};
			jassProgramVisitor.getJassNativeManager().createNative("StartUnitAbilityCooldown",
					startUnitAbilityCooldown);
			jassProgramVisitor.getJassNativeManager().createNative("BlzStartUnitAbilityCooldown",
					startUnitAbilityCooldown);

			jassProgramVisitor.getJassNativeManager().createNative("StartUnitAbilityDefaultCooldown",
					(arguments, globalScope, triggerScope) -> {
						final CUnit theUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int aliasIdRawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final War3ID aliasId = new War3ID(aliasIdRawcode);
						final AbilityBuilderAbility abil = theUnit
								.getAbility(GetABAbilityByRawcodeVisitor.getInstance().reset(aliasId));
						if (abil != null) {
							abil.startCooldown(this.simulation, theUnit);
						}
						return null;
					});

			jassProgramVisitor.getJassNativeManager().createNative("StartAbilityDefaultCooldown",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final CAbility ability = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						if (ability instanceof AbilityBuilderAbility) {
							final AbilityBuilderAbility abilityBuilderAbility = (AbilityBuilderAbility) ability;
							abilityBuilderAbility.startCooldown(this.simulation, unit);
						}
						return null;
					});

			final JassFunction getUnitAbilityCooldownRemaining = (arguments, globalScope, triggerScope) -> {
				final CUnit theUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
				final int aliasIdRawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
				final War3ID aliasId = new War3ID(aliasIdRawcode);
				return RealJassValue.of(theUnit.getCooldownRemainingTicks(this.simulation, aliasId)
						* WarsmashConstants.SIMULATION_STEP_TIME);
			};
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitAbilityCooldownRemaining",
					getUnitAbilityCooldownRemaining);
			jassProgramVisitor.getJassNativeManager().createNative("BlzGetUnitAbilityCooldownRemaining",
					getUnitAbilityCooldownRemaining);

			jassProgramVisitor.getJassNativeManager().createNative("GetUnitAbilityCooldownLengthDisplay",
					(arguments, globalScope, triggerScope) -> {
						final CUnit theUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final int aliasIdRawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final War3ID aliasId = new War3ID(aliasIdRawcode);
						return RealJassValue.of(theUnit.getCooldownLengthDisplayTicks(this.simulation, aliasId)
								* WarsmashConstants.SIMULATION_STEP_TIME);
					});

			jassProgramVisitor.getJassNativeManager().createNative("GetTriggerCastId",
					(arguments, globalScope, triggerScope) -> {
						return IntegerJassValue.of(((CommonTriggerExecutionScope) triggerScope).getTriggerCastId());
					});

			jassProgramVisitor.getJassNativeManager().createNative("GetAbilityItem",
					(arguments, globalScope, triggerScope) -> {
						final CAbility ability = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						return new HandleJassValue(itemType, ability.getItem());
					});

			jassProgramVisitor.getJassNativeManager().createNative("AbilityActivate",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final AbilityBuilderActiveAbility ability = arguments.get(1)
								.visit(ObjectJassValueVisitor.getInstance());
						ability.activate(this.simulation, unit);
						return null;
					});

			jassProgramVisitor.getJassNativeManager().createNative("AbilityDeactivate",
					(arguments, globalScope, triggerScope) -> {
						final CUnit unit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						final AbilityBuilderActiveAbility ability = arguments.get(1)
								.visit(ObjectJassValueVisitor.getInstance());
						ability.deactivate(this.simulation, unit);
						return null;
					});

			jassProgramVisitor.getJassNativeManager().createNative("IsToggleAbilityActive",
					(arguments, globalScope, triggerScope) -> {
						final GenericSingleIconActiveAbility ability = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of(ability.isToggleOn());
					});

			jassProgramVisitor.getJassNativeManager().createNative("SetAbilityCastRange",
					(arguments, globalScope, triggerScope) -> {
						final AbilityBuilderActiveAbility ability = arguments.get(0)
								.visit(ObjectJassValueVisitor.getInstance());
						final float range = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						ability.setCastRange(range);
						return null;
					});
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
								CommonEnvironment.this.simulation.getHandleIdAllocator().createId(),
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
								CommonEnvironment.this.simulation.getHandleIdAllocator().createId(),
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
							this.simulation.registerTimeOfDayEvent(event);
						}
						return null;
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

			// AB floating text
			jassProgramVisitor.getJassNativeManager().createNative("SetTextTagCentered",
					(arguments, globalScope, triggerScope) -> {
						final TextTag textTag = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						if (textTag != null) {
							textTag.setCentered(flag);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("ConvertTextTagConfigType",
					(arguments, globalScope, triggerScope) -> {
						final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						return enumValue(texttagconfigtypeType, TextTagConfigType.VALUES, i);
					});
			jassProgramVisitor.getJassNativeManager().createNative("CreateTextTagFromConfig",
					(arguments, globalScope, triggerScope) -> {
						final CUnit sourceUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						TextTagConfigType configType = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						String textValue = nullable(arguments, 2, StringJassValueVisitor.getInstance());

						if (sourceUnit != null) {
							if (configType == null) {
								configType = TextTagConfigType.GOLD;
							}
							if (textValue == null) {
								textValue = "(null)";
							}
							return new HandleJassValue(texttagType, CommonEnvironment.this.simulation
									.spawnTextTag(sourceUnit, sourceUnit.getPlayerIndex(), configType, textValue));
						}
						return texttagType.getNullValue();

					});
			jassProgramVisitor.getJassNativeManager().createNative("CreateIntTextTagFromConfig",
					(arguments, globalScope, triggerScope) -> {
						final CUnit sourceUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						TextTagConfigType configType = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						final int intValue = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());

						if (sourceUnit != null) {
							if (configType == null) {
								configType = TextTagConfigType.GOLD;
							}
							return new HandleJassValue(texttagType, CommonEnvironment.this.simulation
									.spawnTextTag(sourceUnit, sourceUnit.getPlayerIndex(), configType, intValue));
						}
						return texttagType.getNullValue();

					});

			jassProgramVisitor.getJassNativeManager().createNative("SetFalseTimeOfDay",
					(arguments, globalScope, triggerScope) -> {
						final int hour = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						final int minute = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						final float duration = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();

						this.simulation.addFalseTimeOfDay(hour, minute, duration);
						return null;
					});

			jassProgramVisitor.getJassNativeManager().createNative("GetGameTurnTick",
					(arguments, globalScope, triggerScope) -> {
						return IntegerJassValue.of(this.simulation.getGameTurnTick());
					});

			jassProgramVisitor.getJassNativeManager().createNative("GetSimulationStepTime",
					(arguments, globalScope, triggerScope) -> {
						return RealJassValue.of(WarsmashConstants.SIMULATION_STEP_TIME);
					});

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
			jassProgramVisitor.getJassNativeManager().createNative("GetGameObjectFieldAsStringList",
					(arguments, globalScope, triggerScope) -> {
						final GameObject gameObject = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final String key = nullable(arguments, 1, StringJassValueVisitor.getInstance());
						if (gameObject != null) {
							final List<String> javaValueList = gameObject.getFieldAsList(key);
							if (javaValueList != null) {
								final StringList stringList = new StringList(
										this.simulation.getHandleIdAllocator().createId(), javaValueList);
								return new HandleJassValue(stringlistType, stringList);
							}
						}
						return stringlistType.getNullValue();
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
			jassProgramVisitor.getJassNativeManager().createNative("UnitAddDefenseBonus",
					(arguments, globalScope, triggerScope) -> {
						final CUnit targetUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final Double defenseAmount = arguments.get(1).visit(RealJassValueVisitor.getInstance());
						if (targetUnit != null) {
							targetUnit.setTemporaryDefenseBonus(
									targetUnit.getTemporaryDefenseBonus() + defenseAmount.floatValue());
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitSetTemporaryDefenseBonus",
					(arguments, globalScope, triggerScope) -> {
						final CUnit targetUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final Double defenseAmount = arguments.get(1).visit(RealJassValueVisitor.getInstance());
						if (targetUnit != null) {
							targetUnit.setTemporaryDefenseBonus(defenseAmount.floatValue());
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitGetTemporaryDefenseBonus",
					(arguments, globalScope, triggerScope) -> {
						final CUnit targetUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return RealJassValue.of(targetUnit.getTemporaryDefenseBonus());
					});
			final JassFunction getUnitArmor = (arguments, globalScope, triggerScope) -> {
				final CUnit targetUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
				return RealJassValue.of(targetUnit.getDefense());
			};
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitDefense", getUnitArmor);
			jassProgramVisitor.getJassNativeManager().createNative("BlzGetUnitArmor", getUnitArmor);

			jassProgramVisitor.getJassNativeManager().createNative("HealUnit",
					(arguments, globalScope, triggerScope) -> {
						final CUnit targetUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final float amount = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
						if (targetUnit != null) {
							targetUnit.heal(this.simulation, amount);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("DoStopOrder",
					(arguments, globalScope, triggerScope) -> {
						final CUnit targetUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (targetUnit != null) {
							targetUnit.performDefaultBehavior(this.simulation);
						}
						return null;
					});

			jassProgramVisitor.getJassNativeManager().createNative("UnitInstantReturnResources",
					(arguments, globalScope, triggerScope) -> {
						final CUnit targetUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (targetUnit != null) {
							final CAbilityHarvest harv = targetUnit.getFirstAbilityOfType(CAbilityHarvest.class);
							if ((harv != null) && (harv.getCarriedResourceType() != null)
									&& (harv.getCarriedResourceAmount() > 0)) {
								final CPlayer pl = this.simulation.getPlayer(targetUnit.getPlayerIndex());
								switch (harv.getCarriedResourceType()) {
								case FOOD:
									// This might be a bad idea? Not sure it will ever matter
									pl.setFoodCap(Math.min(pl.getFoodCap() + harv.getCarriedResourceAmount(),
											pl.getFoodCapCeiling()));
									this.simulation.unitGainResourceEvent(targetUnit, pl.getId(),
											harv.getCarriedResourceType(), harv.getCarriedResourceAmount());
									harv.setCarriedResources(ResourceType.FOOD, 0);
									break;
								case GOLD:
									pl.addGold(harv.getCarriedResourceAmount());
									this.simulation.unitGainResourceEvent(targetUnit, pl.getId(),
											harv.getCarriedResourceType(), harv.getCarriedResourceAmount());
									harv.setCarriedResources(ResourceType.GOLD, 0);
									break;
								case LUMBER:
									pl.addLumber(harv.getCarriedResourceAmount());
									this.simulation.unitGainResourceEvent(targetUnit, pl.getId(),
											harv.getCarriedResourceType(), harv.getCarriedResourceAmount());
									harv.setCarriedResources(ResourceType.LUMBER, 0);
									break;
								case MANA:
									// ??
									break;
								default:
									break;

								}
							}
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetPreferredSelectionReplacement",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnitIsGoingToBeRemoved = nullable(arguments, 0,
								ObjectJassValueVisitor.getInstance());
						final CUnit whichUnitWeWantToSelect = nullable(arguments, 1,
								ObjectJassValueVisitor.getInstance());
						if (whichUnitIsGoingToBeRemoved != null) {
							this.simulation.unitPreferredSelectionReplacement(whichUnitIsGoingToBeRemoved,
									whichUnitWeWantToSelect);
						}
						return null;
					});

			jassProgramVisitor.getJassNativeManager().createNative("ResurrectUnit",
					(arguments, globalScope, triggerScope) -> {
						final CUnit targetUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if ((targetUnit != null) && targetUnit.isDead()) {
							targetUnit.resurrect(this.simulation);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitAlive",
					(arguments, globalScope, triggerScope) -> {
						final CUnit targetUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (targetUnit != null) {
							return BooleanJassValue.of(!targetUnit.isDead());
						}
						return BooleanJassValue.FALSE;
					});
			jassProgramVisitor.getJassNativeManager().createNative("WidgetAlive",
					(arguments, globalScope, triggerScope) -> {
						final CWidget targetUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (targetUnit != null) {
							return BooleanJassValue.of(!targetUnit.isDead());
						}
						return BooleanJassValue.FALSE;
					});
			jassProgramVisitor.getJassNativeManager().createNative("BlzSetCameraAllowsHotkeyTargetLock",
					(arguments, globalScope, triggerScope) -> {
						this.cameraAllowsHotkeyTargetLock = arguments.get(0)
								.visit(BooleanJassValueVisitor.getInstance());
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("BlzGetCameraAllowsHotkeyTargetLock",
					(arguments, globalScope, triggerScope) -> BooleanJassValue
							.of(this.cameraAllowsHotkeyTargetLock));
			jassProgramVisitor.getJassNativeManager().createNative("BlzRemoveEffect",
					(arguments, globalScope, triggerScope) -> {
						final ScriptEffect fx = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (fx != null) {
							fx.removeWithoutDeath();
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("BlzUnitIsAlive",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of((whichUnit != null) && !whichUnit.isDead());
					});
			jassProgramVisitor.getJassNativeManager().createNative("BlzUnitHeal",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final float life = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						if ((whichUnit == null) || whichUnit.isDead()) {
							return RealJassValue.ZERO;
						}
						final float before = whichUnit.getLife();
						whichUnit.heal(this.simulation, life);
						return RealJassValue.of(whichUnit.getLife() - before);
					});
			// 3.0 equipment and bag slots do not exist in this ruleset, so items are never equipped or bagged.
			for (final String name : new String[] { "BlzIsItemEquipped", "BlzIsItemInBag", "BlzUnitEquipItem",
					"BlzUnitHasItemBagged", "BlzUnitHasItemEquipped", "BlzUnitHasAnyItemEquipped",
					"BlzUnitHasItemEquipmentOfType", "BlzUnitCanEquipItemOfEquipmentType" }) {
				jassProgramVisitor.getJassNativeManager().createNative(name,
						(arguments, globalScope, triggerScope) -> BooleanJassValue.FALSE);
			}
			jassProgramVisitor.getJassNativeManager().createNative("BlzUnitHasLoadoutSlotEmpty",
					(arguments, globalScope, triggerScope) -> BooleanJassValue.TRUE);
			for (final String name : new String[] { "BlzUnitItemInBagSlot", "BlzUnitItemInEquipmentSlot",
					"BlzUnitUnequipItemFromSlot" }) {
				jassProgramVisitor.getJassNativeManager().createNative(name,
						(arguments, globalScope, triggerScope) -> new HandleJassValue(itemType, null));
			}
			jassProgramVisitor.getJassNativeManager().createNative("BlzUnitExtendedInventorySize",
					(arguments, globalScope, triggerScope) -> IntegerJassValue.of(0));
			jassProgramVisitor.getJassNativeManager().createNative("BlzGetHUDScale",
					(arguments, globalScope, triggerScope) -> RealJassValue.of(100));
			for (final String name : new String[] { "BlzUnitUnequipItem", "BlzSetItemColor", "BlzResetUnitTalents",
					"BlzSetHUDScale", "BlzLockHUDScale", "BlzUnlockHUDScale",
					"BlzSetThematicMusicAbsoluteVolume" }) {
				jassProgramVisitor.getJassNativeManager().createNative(name,
						(arguments, globalScope, triggerScope) -> null);
			}
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitGoldCost",
					(arguments, globalScope, triggerScope) -> {
						if (arguments.get(0) instanceof IntegerJassValue) {
							// Something dumb happened here, there seems to be a SmashJass version of this
							// and a CommonAI version of this both
							final Integer typeId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
							final CUnitType unitTypeData = this.simulation.getUnitData()
									.getUnitType(new War3ID(typeId));
							if (unitTypeData != null) {
								return IntegerJassValue.of(unitTypeData.getGoldCost());
							}
							return IntegerJassValue.ZERO;
						}
						final CUnit targetUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (targetUnit != null) {
							return IntegerJassValue.of(targetUnit.getUnitType().getGoldCost());
						}
						return IntegerJassValue.ZERO;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitWoodCost",
					(arguments, globalScope, triggerScope) -> {
						if (arguments.get(0) instanceof IntegerJassValue) {
							// Something dumb happened here, there seems to be a SmashJass version of this
							// and a CommonAI version of this both
							final Integer typeId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
							final CUnitType unitTypeData = this.simulation.getUnitData()
									.getUnitType(new War3ID(typeId));
							if (unitTypeData != null) {
								return IntegerJassValue.of(unitTypeData.getLumberCost());
							}
							return IntegerJassValue.ZERO;
						}
						final CUnit targetUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (targetUnit != null) {
							return IntegerJassValue.of(targetUnit.getUnitType().getLumberCost());
						}
						return IntegerJassValue.ZERO;
					});

			jassProgramVisitor.getJassNativeManager().createNative("ConvertResourceType",
					(arguments, globalScope, triggerScope) -> {
						final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						return enumValue(resourcetypeType, ResourceType.VALUES, i);
					});
			jassProgramVisitor.getJassNativeManager().createNative("SendUnitBackToWork",
					(arguments, globalScope, triggerScope) -> {
						final CUnit targetUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final ResourceType resourceType = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						if (targetUnit != null) {
							return new HandleJassValue(resourcetypeType,
									targetUnit.backToWork(this.simulation, resourceType));
						}
						return resourcetypeType.getNullValue();
					});

			jassProgramVisitor.getJassNativeManager().createNative("SetUnitMovementTypeNoCollision",
					(arguments, globalScope, triggerScope) -> {
						final CUnit targetUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						if (targetUnit != null) {
							targetUnit.setNoCollisionMovementType(flag);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitExplodeOnDeathBuffId",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int buffId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						if (whichUnit != null) {
							whichUnit.setExplodesOnDeathBuffId(new War3ID(buffId));
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnsetUnitExplodeOnDeathBuffId",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (whichUnit != null) {
							whichUnit.setExplodesOnDeathBuffId(null);
						}
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("StartSacrificingUnit",
					(arguments, globalScope, triggerScope) -> {
						final CUnit factory = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CUnit toSacrifice = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						final int resultUnitId = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
						factory.queueSacrificingUnit(this.simulation, new War3ID(resultUnitId), toSacrifice);
						factory.notifyOrdersChanged();
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("StartTrainingUnit",
					(arguments, globalScope, triggerScope) -> {
						final CUnit factory = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int resultUnitId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						factory.queueTrainingUnit(this.simulation, new War3ID(resultUnitId));
						factory.notifyOrdersChanged();
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitLoopSpellSoundEffect",
					(arguments, globalScope, triggerScope) -> {
						final CUnit onUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int abilityAliasRawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						this.simulation.unitLoopSoundEffectEvent(onUnit, new War3ID(abilityAliasRawcode));
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitSpellSoundEffect",
					(arguments, globalScope, triggerScope) -> {
						final CUnit onUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int abilityAliasRawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						this.simulation.unitSoundEffectEvent(onUnit, new War3ID(abilityAliasRawcode));
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitStopSpellSoundEffect",
					(arguments, globalScope, triggerScope) -> {
						final CUnit onUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final int abilityAliasRawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
						this.simulation.unitStopSoundEffectEvent(onUnit, new War3ID(abilityAliasRawcode));
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitMovementDisabled",
					(arguments, globalScope, triggerScope) -> {
						final CUnit targetUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						if (targetUnit != null) {
							return BooleanJassValue.of(targetUnit.isMovementDisabled());
						}
						return BooleanJassValue.FALSE;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitAnimationByTag",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean force = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						final PrimaryTag animationName = nullable(arguments, 2, ObjectJassValueVisitor.getInstance());
						final EnumSetHandle<SecondaryTag> secondaryAnimationTags = nullable(arguments, 3,
								ObjectJassValueVisitor.getInstance());
						final float speedRatio = arguments.get(4).visit(RealJassValueVisitor.getInstance())
								.floatValue();
						final boolean allowRarityVariations = arguments.get(5)
								.visit(BooleanJassValueVisitor.getInstance());
						whichUnit.getUnitAnimationListener().playAnimation(force, animationName,
								secondaryAnimationTags.getEnumSet(), speedRatio, allowRarityVariations);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitAnimationByTagWithDuration",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean force = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						final PrimaryTag animationName = nullable(arguments, 2, ObjectJassValueVisitor.getInstance());
						final EnumSetHandle<SecondaryTag> secondaryAnimationTags = nullable(arguments, 3,
								ObjectJassValueVisitor.getInstance());
						final float duration = arguments.get(4).visit(RealJassValueVisitor.getInstance()).floatValue();
						final boolean allowRarityVariations = arguments.get(5)
								.visit(BooleanJassValueVisitor.getInstance());
						whichUnit.getUnitAnimationListener().playAnimationWithDuration(force, animationName,
								secondaryAnimationTags.getEnumSet(), duration, allowRarityVariations);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitAnimationToWalk",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final boolean force = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
						final float currentMovementSpeed = arguments.get(2).visit(RealJassValueVisitor.getInstance())
								.floatValue();
						final boolean allowRarityVariations = arguments.get(3)
								.visit(BooleanJassValueVisitor.getInstance());
						whichUnit.getUnitAnimationListener().playWalkAnimation(force, currentMovementSpeed,
								allowRarityVariations);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("QueueUnitAnimationByTag",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final PrimaryTag animationName = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						final EnumSetHandle<SecondaryTag> secondaryAnimationTags = nullable(arguments, 2,
								ObjectJassValueVisitor.getInstance());
						final boolean allowRarityVariations = arguments.get(3)
								.visit(BooleanJassValueVisitor.getInstance());
						whichUnit.getUnitAnimationListener().queueAnimation(animationName,
								secondaryAnimationTags.getEnumSet(), allowRarityVariations);
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("AddUnitAnimationSecondaryTag",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final SecondaryTag whichTag = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of(whichUnit.getUnitAnimationListener().addSecondaryTag(whichTag));
					});
			jassProgramVisitor.getJassNativeManager().createNative("RemoveUnitAnimationSecondaryTag",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final SecondaryTag whichTag = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
						return BooleanJassValue.of(whichUnit.getUnitAnimationListener().removeSecondaryTag(whichTag));
					});
			jassProgramVisitor.getJassNativeManager().createNative("ForceResetUnitCurrentAnimation",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						whichUnit.getUnitAnimationListener().forceResetCurrentAnimation();
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitCastPoint",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return RealJassValue.of(whichUnit.getUnitType().getCastPoint());
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitCastBackswingPoint",
					(arguments, globalScope, triggerScope) -> {
						final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						return RealJassValue.of(whichUnit.getUnitType().getCastBackswingPoint());
					});

			jassProgramVisitor.getJassNativeManager().createNative("String2DamageType",
					(arguments, globalScope, triggerScope) -> {
						final String key = nullable(arguments, 0, StringJassValueVisitor.getInstance());
						return new HandleJassValue(damagetypeType, CDamageType.valueOf(key));
					});
			// unit group extensions
			jassProgramVisitor.getJassNativeManager().createNative("GroupEnumUnitsInRangeOfUnit",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CUnit whichUnit = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						final float x = whichUnit.getX();
						final float y = whichUnit.getY();
						final float radius = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						final TriggerBooleanExpression filter = nullable(arguments, 3,
								ObjectJassValueVisitor.getInstance());

						CommonEnvironment.this.simulation.getWorldCollision().enumUnitsOrCorpsesInRect(
								tempRect.set(x - radius, y - radius, radius * 2, radius * 2), (unit) -> {
									if (whichUnit.canReach(unit, radius)) {
										if ((filter == null) || filter.evaluate(globalScope,
												CommonTriggerExecutionScope.filterScope(triggerScope, unit))) {
											// TODO the trigger scope for evaluation here might need to be a clean one?
											group.add(unit);
										}
									}
									return false;
								});
						return null;
					});
			jassProgramVisitor.getJassNativeManager().createNative("GroupEnumUnitsInRangeOfUnitCounted",
					(arguments, globalScope, triggerScope) -> {
						final List<CUnit> group = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CUnit whichUnit = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
						final float x = whichUnit.getX();
						final float y = whichUnit.getY();
						final float radius = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
						final TriggerBooleanExpression filter = nullable(arguments, 3,
								ObjectJassValueVisitor.getInstance());
						final Integer countLimit = arguments.get(4).visit(IntegerJassValueVisitor.getInstance());

						CommonEnvironment.this.simulation.getWorldCollision().enumUnitsOrCorpsesInRect(
								tempRect.set(x - radius, y - radius, radius * 2, radius * 2), new CUnitEnumFunction() {
									int count = 0;

									@Override
									public boolean call(final CUnit unit) {
										if (whichUnit.canReach(unit, radius)) {
											if ((filter == null) || filter.evaluate(globalScope,
													CommonTriggerExecutionScope.filterScope(triggerScope, unit))) {
												// TODO the trigger scope for evaluation here might need to be a clean
												// one?
												group.add(unit);
												this.count++;
												if (this.count >= countLimit) {
													return true;
												}
											}
										}
										return false;
									}
								});
						return null;
					});

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
								this.simulation.getHandleIdAllocator().createId(), EnumSet.noneOf(PrimaryTag.class)));
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
								this.simulation.getHandleIdAllocator().createId(), EnumSet.noneOf(SecondaryTag.class)));
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
								this.simulation.getHandleIdAllocator().createId(), globalScope));
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

			jassProgramVisitor.getJassNativeManager().createNative("WarsmashGetAbilityClassName",
					(arguments, globalScope, triggerScope) -> {
						final CAbility ability = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
						return new StringJassValue(ability.getClass().getSimpleName());
					});
			jassProgramVisitor.getJassNativeManager().createNative("WarsmashGetRawcode2String",
					(arguments, globalScope, triggerScope) -> {
						final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						return new StringJassValue(new War3ID(rawcode).toString());
					});
			jassProgramVisitor.getJassNativeManager().createNative("Rawcode2String",
					(arguments, globalScope, triggerScope) -> {
						final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
						return StringJassValue.of(RawcodeUtils.toString(rawcode));
					});
			final JassFunction string2Rawcode = (arguments, globalScope, triggerScope) -> {
				final String rawcodeStr = arguments.get(0).visit(StringJassValueVisitor.getInstance());
				return IntegerJassValue.of(RawcodeUtils.toInt(rawcodeStr));
			};
			jassProgramVisitor.getJassNativeManager().createNative("String2Rawcode", string2Rawcode);
			jassProgramVisitor.getJassNativeManager().createNative("FourCC", string2Rawcode);

			registerRandomNatives(jassProgramVisitor, this.simulation);
			CampaignNatives.register(jassProgramVisitor, war3MapViewer, meleeUI);
			CustomMapNatives.register(jassProgramVisitor, war3MapViewer);
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

		public void loadSavedGame(final War3MapViewer viewer, final FileHandle file) throws IOException {
			this.simulation.setGlobalScope(this.jassProgramVisitor.getGlobals());
			SavedGame.load(viewer, file, this.aiScriptStarter);
		}

		public void main() {
			this.simulation.setGlobalScope(this.jassProgramVisitor.getGlobals());
			// The JASS ability layer is optional: abilities_main exists only when JassFileList names
			// Scripts\abilitiesDefaults.j.
			if (this.jassProgramVisitor.getGlobals().getUserFunctionInstructionPtr("abilities_main") != null) {
				try {
					final JassThread abilitiesThread = this.jassProgramVisitor.getGlobals()
							.createThread("abilities_main", Collections.emptyList(), TriggerExecutionScope.EMPTY);
					this.jassProgramVisitor.getGlobals().queueThread(abilitiesThread);
				}
				catch (final Exception exc) {
					new JassException(this.jassProgramVisitor.getGlobals(),
							"Exception on Line " + this.jassProgramVisitor.getGlobals().getLineNumber(), exc)
							.printStackTrace();
				}
			}
			try {
				final JassThread mainThread = this.jassProgramVisitor.getGlobals().createThread("main",
						Collections.emptyList(), TriggerExecutionScope.EMPTY);
				mainThread.setSleepEndsThread(true);
				this.jassProgramVisitor.getGlobals().queueThread(mainThread);
			}
			catch (final Exception exc) {
				throw new JassException(this.jassProgramVisitor.getGlobals(),
						"Exception on Line " + this.jassProgramVisitor.getGlobals().getLineNumber(), exc);
			}
			// Automated test scripts added through JassFileList define test_main; it runs after the map's main.
			if (this.jassProgramVisitor.getGlobals().getUserFunctionInstructionPtr("test_main") != null) {
				final JassThread testThread = this.jassProgramVisitor.getGlobals().createThread("test_main",
						Collections.emptyList(), TriggerExecutionScope.EMPTY);
				this.jassProgramVisitor.getGlobals().queueThread(testThread);
			}
		}

		private boolean groupPointOrder(final List<CUnit> group, final int orderId, final float x, final float y) {
			final List<CUnit> units = new ArrayList<>(group.subList(0,
					Math.min(group.size(), CGroupMove.MAX_SCRIPT_GROUP_ORDER_UNITS)));
			return CGroupMove.order(this.simulation, units, orderId, x, y,
					(unit, slotX, slotY) -> unit.order(this.simulation, orderId, new AbilityPointTarget(slotX, slotY)));
		}

		private boolean issuePointOrder(final int orderingPlayerIndex, final CUnit whichUnit, final int orderId,
				final float x, final float y) {
			final BooleanAbilityActivationReceiver activationReceiver = BooleanAbilityActivationReceiver.INSTANCE.reset();
			int abilityHandleId = 0;
			AbilityPointTarget targetAsPoint = new AbilityPointTarget(x, y);
			for (final CAbility ability : whichUnit.getAbilities()) {
				ability.checkCanUse(this.simulation, whichUnit, orderingPlayerIndex, orderId, false, activationReceiver);
				if (activationReceiver.isOk()) {
					final PointAbilityTargetCheckReceiver targetReceiver = PointAbilityTargetCheckReceiver.INSTANCE;
					ability.checkCanTarget(this.simulation, whichUnit, orderingPlayerIndex, orderId, false, targetAsPoint,
						targetReceiver.reset());
					if (targetReceiver.getTarget() != null) {
						targetAsPoint = targetReceiver.getTarget();
						abilityHandleId = ability.getHandleId();
					}
				}
			}
			if (abilityHandleId != 0) {
				this.simulation.getDefaultPlayerUnitOrderExecutor(orderingPlayerIndex).issuePointOrder(
						whichUnit.getHandleId(), abilityHandleId, orderId, targetAsPoint.x, targetAsPoint.y, false);
			}
			return abilityHandleId != 0;
		}

		private boolean issueTargetOrder(final int orderingPlayerIndex, final CUnit whichUnit, final int orderId,
				CWidget whichTarget) {
			if ((whichTarget == null) || (this.simulation.getWidget(whichTarget.getHandleId()) == null)) {
				return false;
			}
			int abilityHandleId = 0;
			// Measured: script attack orders may target a unit hidden by fog; spell orders (Hex) may not.
			this.simulation.setScriptOrder(orderId == OrderIds.attack);
			this.simulation.setScriptTargetOrder(true);
			try {
				final BooleanAbilityActivationReceiver activationReceiver = BooleanAbilityActivationReceiver.INSTANCE.reset();
				for (final CAbility ability : whichUnit.getAbilities()) {
					ability.checkCanUse(this.simulation, whichUnit, orderingPlayerIndex, orderId, false, activationReceiver);
					if (activationReceiver.isOk()) {
						final CWidgetAbilityTargetCheckReceiver targetReceiver = CWidgetAbilityTargetCheckReceiver.INSTANCE;
						ability.checkCanTarget(this.simulation, whichUnit, orderingPlayerIndex, orderId, false, whichTarget,
							targetReceiver.reset());
						if (targetReceiver.getTarget() != null) {
							whichTarget = targetReceiver.getTarget();
							abilityHandleId = ability.getHandleId();
						}
					}
				}
				if (abilityHandleId != 0) {
					this.simulation.getDefaultPlayerUnitOrderExecutor(orderingPlayerIndex).issueTargetOrder(
							whichUnit.getHandleId(), abilityHandleId, orderId, whichTarget.getHandleId(), false);
				}
				else if (whichTarget.visit(AbilityTargetVisitor.UNIT) != null) {
					return issueSpellPointOrderAtUnit(orderingPlayerIndex, whichUnit, orderId, whichTarget);
				}
			}
			finally {
				this.simulation.setScriptOrder(false);
				this.simulation.setScriptTargetOrder(false);
			}
			return abilityHandleId != 0;
		}

		// Measured (1.29, probe dispel-orders-d290): a unit-target script order of a point-target spell (Dispel Magic,
		// Disenchant, Flame Strike, Blizzard, Shockwave, Carrion Swarm) casts it at the unit.
		private boolean issueSpellPointOrderAtUnit(final int orderingPlayerIndex, final CUnit whichUnit, final int orderId,
				final CWidget whichTarget) {
			final BooleanAbilityActivationReceiver activationReceiver = BooleanAbilityActivationReceiver.INSTANCE.reset();
			int abilityHandleId = 0;
			AbilityPointTarget targetAsPoint = new AbilityPointTarget(whichTarget.getX(), whichTarget.getY());
			for (final CAbility ability : whichUnit.getAbilities()) {
				if (!(ability instanceof CAbilitySpell) && !(ability instanceof AbilityBuilderActiveAbility)
						&& !(ability instanceof CAbilityCarrionSwarmDummy)) {
					continue;
				}
				ability.checkCanUse(this.simulation, whichUnit, orderingPlayerIndex, orderId, false, activationReceiver);
				if (activationReceiver.isOk()) {
					final PointAbilityTargetCheckReceiver targetReceiver = PointAbilityTargetCheckReceiver.INSTANCE;
					ability.checkCanTarget(this.simulation, whichUnit, orderingPlayerIndex, orderId, false, targetAsPoint,
						targetReceiver.reset());
					if (targetReceiver.getTarget() != null) {
						targetAsPoint = targetReceiver.getTarget();
						abilityHandleId = ability.getHandleId();
					}
				}
			}
			if (abilityHandleId != 0) {
				this.simulation.getDefaultPlayerUnitOrderExecutor(orderingPlayerIndex).issuePointOrder(
						whichUnit.getHandleId(), abilityHandleId, orderId, targetAsPoint.x, targetAsPoint.y, false);
			}
			return abilityHandleId != 0;
		}

		private void applyReleaseCheat(final String cheat) {
			final int space = cheat.indexOf(' ');
			final String code = space < 0 ? cheat : cheat.substring(0, space);
			final String argument = space < 0 ? "" : cheat.substring(space + 1).trim();
			switch (code) {
			case "greedisgood":
			case "keysersoze":
			case "leafittome": {
				final int amount;
				try {
					amount = Integer.parseInt(argument);
				}
				catch (final NumberFormatException e) {
					return;
				}
				for (int i = 0; i < WarsmashConstants.MAX_PLAYERS; i++) {
					final CPlayer player = this.simulation.getPlayer(i);
					if (!code.equals("leafittome")) {
						player.addGold(amount);
					}
					if (!code.equals("keysersoze")) {
						player.addLumber(amount);
					}
				}
				break;
			}
			case "iseedeadpeople":
				this.simulation.toggleCheatRevealMap();
				break;
			case "warpten":
				this.simulation.toggleCheatBuildSpeed();
				break;
			default:
				break;
			}
		}

		// Measured (1.29, probe dispel-orders-d290): true when any unit of the group took the order.
		private boolean groupTargetOrder(final List<CUnit> group, final int orderId, final CWidget target) {
			boolean success = false;
			for (final CUnit unit : new ArrayList<>(group.subList(0,
					Math.min(group.size(), CGroupMove.MAX_SCRIPT_GROUP_ORDER_UNITS)))) {
				success |= issueTargetOrder(unit.getPlayerIndex(), unit, orderId, target);
			}
			return success;
		}

		private boolean issueImmediateOrder(final int orderingPlayerIndex, final CUnit whichUnit, final int orderId) {
			final BooleanAbilityActivationReceiver activationReceiver = BooleanAbilityActivationReceiver.INSTANCE.reset();
			int abilityHandleId = 0;
			for (final CAbility ability : whichUnit.getAbilities()) {
				ability.checkCanUse(this.simulation, whichUnit, orderingPlayerIndex, orderId, false, activationReceiver);
				if (activationReceiver.isOk()) {
					final BooleanAbilityTargetCheckReceiver<Void> targetReceiver = BooleanAbilityTargetCheckReceiver
							.<Void>getInstance();
					ability.checkCanTargetNoTarget(this.simulation, whichUnit, orderingPlayerIndex, orderId, false,
						targetReceiver.reset());
					if (targetReceiver.isTargetable()) {
						abilityHandleId = ability.getHandleId();
					}
				}
			}
			// Orders without an ability, such as stop, are still issued.
			this.simulation.getDefaultPlayerUnitOrderExecutor(orderingPlayerIndex).issueImmediateOrder(
					whichUnit.getHandleId(), abilityHandleId, orderId, false);
			// Measured on 1.29: stop is accepted (true) although no ability carries it.
			return (abilityHandleId != 0) || ((orderId == OrderIds.stop) && !whichUnit.isDead());
		}

		private CFogState fogState(final CPlayer player, final float x, final float y) {
			return player.getFogOfWar().getFogState(this.simulation, this.simulation.getPathingGrid(), x, y);
		}

		// A unit the player has seen reads as fogged under black mask (1.29).
		private CFogState unitFogState(final CUnit unit, final CPlayer player) {
			final CFogState state = fogState(player, unit.getX(), unit.getY());
			if ((state == CFogState.MASKED) && unit.isSeenBy(player.getId())) {
				return CFogState.FOGGED;
			}
			return state;
		}

		private boolean isUnitVisibleToPlayer(final CUnit unit, final CPlayer player) {
			if (this.simulation.isUnitVisionShared(unit, player)) {
				return true;
			}
			if (fogState(player, unit.getX(), unit.getY()) != CFogState.VISIBLE) {
				return false;
			}
			if ((unit.getInvisLevels() > 0) && (unit.getPlayerIndex() != player.getId())
					&& !this.simulation.getPlayer(unit.getPlayerIndex()).hasAlliance(player.getId(),
							CAllianceType.SHARED_VISION)) {
				return unit.isVisible(this.simulation, player.getId());
			}
			return true;
		}

		private HandleJassValue newFogModifier(final HandleJassType fogmodifierType, final CFogModifierJass modifier) {
			modifier.applyNow(this.simulation);
			return new HandleJassValue(fogmodifierType, modifier);
		}
	}

	public static final class ConfigEnvironment {

		private final GameUI gameUI;
		private Element skin;
		private final JassProgram jassProgramVisitor;

		private ConfigEnvironment(final JassProgram jassProgramVisitor, final DataSource dataSource,
				final Viewport uiViewport, final Scene uiScene, final GameUI gameUI, final War3MapConfig mapConfig) {
			this.jassProgramVisitor = jassProgramVisitor;
			this.gameUI = gameUI;
			final Rectangle tempRect = new Rectangle();
			final GlobalScope globals = jassProgramVisitor.getGlobalScope();
			final HandleJassType agentType = globals.registerHandleType("agent");
			final HandleJassType eventType = globals.registerHandleType("event");
			final HandleJassType playerType = globals.registerHandleType("player");
			final HandleJassType widgetType = globals.registerHandleType("widget");
			final HandleJassType unitType = globals.registerHandleType("unit");
			final HandleJassType destructableType = globals.registerHandleType("destructable");
			final HandleJassType itemType = globals.registerHandleType("item");
			final HandleJassType abilityType = globals.registerHandleType("ability");
			final HandleJassType buffType = globals.registerHandleType("buff");
			final HandleJassType forceType = globals.registerHandleType("force");
			final HandleJassType groupType = globals.registerHandleType("group");
			final HandleJassType triggerType = globals.registerHandleType("trigger");
			final HandleJassType triggerconditionType = globals.registerHandleType("triggercondition");
			final HandleJassType triggeractionType = globals.registerHandleType("triggeraction");
			final HandleJassType timerType = globals.registerHandleType("timer");
			final HandleJassType locationType = globals.registerHandleType("location");
			final HandleJassType regionType = globals.registerHandleType("region");
			final HandleJassType rectType = globals.registerHandleType("rect");
			final HandleJassType boolexprType = globals.registerHandleType("boolexpr");
			final HandleJassType soundType = globals.registerHandleType("sound");
			final HandleJassType conditionfuncType = globals.registerHandleType("conditionfunc");
			final HandleJassType filterfuncType = globals.registerHandleType("filterfunc");
			final HandleJassType unitpoolType = globals.registerHandleType("unitpool");
			final HandleJassType itempoolType = globals.registerHandleType("itempool");
			final HandleJassType raceType = globals.registerHandleType("race");
			final HandleJassType alliancetypeType = globals.registerHandleType("alliancetype");
			final HandleJassType racepreferenceType = globals.registerHandleType("racepreference");
			final HandleJassType gamestateType = globals.registerHandleType("gamestate");
			final HandleJassType igamestateType = globals.registerHandleType("igamestate");
			final HandleJassType fgamestateType = globals.registerHandleType("fgamestate");
			final HandleJassType playerstateType = globals.registerHandleType("playerstate");
			final HandleJassType playerscoreType = globals.registerHandleType("playerscore");
			final HandleJassType playergameresultType = globals.registerHandleType("playergameresult");
			final HandleJassType unitstateType = globals.registerHandleType("unitstate");
			final HandleJassType aidifficultyType = globals.registerHandleType("aidifficulty");
			final HandleJassType eventidType = globals.registerHandleType("eventid");
			final HandleJassType gameeventType = globals.registerHandleType("gameevent");
			final HandleJassType playereventType = globals.registerHandleType("playerevent");
			final HandleJassType playeruniteventType = globals.registerHandleType("playerunitevent");
			final HandleJassType uniteventType = globals.registerHandleType("unitevent");
			final HandleJassType limitopType = globals.registerHandleType("limitop");
			final HandleJassType widgeteventType = globals.registerHandleType("widgetevent");
			final HandleJassType dialogeventType = globals.registerHandleType("dialogevent");
			final HandleJassType unittypeType = globals.registerHandleType("unittype");
			final HandleJassType gamespeedType = globals.registerHandleType("gamespeed");
			final HandleJassType gamedifficultyType = globals.registerHandleType("gamedifficulty");
			final HandleJassType gametypeType = globals.registerHandleType("gametype");
			final HandleJassType mapflagType = globals.registerHandleType("mapflag");
			final HandleJassType mapvisibilityType = globals.registerHandleType("mapvisibility");
			final HandleJassType mapsettingType = globals.registerHandleType("mapsetting");
			final HandleJassType mapdensityType = globals.registerHandleType("mapdensity");
			final HandleJassType mapcontrolType = globals.registerHandleType("mapcontrol");
			final HandleJassType playerslotstateType = globals.registerHandleType("playerslotstate");
			final HandleJassType volumegroupType = globals.registerHandleType("volumegroup");
			final HandleJassType camerafieldType = globals.registerHandleType("camerafield");
			final HandleJassType camerasetupType = globals.registerHandleType("camerasetup");
			final HandleJassType playercolorType = globals.registerHandleType("playercolor");
			final HandleJassType placementType = globals.registerHandleType("placement");
			final HandleJassType startlocprioType = globals.registerHandleType("startlocprio");
			final HandleJassType raritycontrolType = globals.registerHandleType("raritycontrol");
			final HandleJassType blendmodeType = globals.registerHandleType("blendmode");
			final HandleJassType texmapflagsType = globals.registerHandleType("texmapflags");
			final HandleJassType effectType = globals.registerHandleType("effect");
			final HandleJassType effecttypeType = globals.registerHandleType("effecttype");
			final HandleJassType weathereffectType = globals.registerHandleType("weathereffect");
			final HandleJassType terraindeformationType = globals.registerHandleType("terraindeformation");
			final HandleJassType fogstateType = globals.registerHandleType("fogstate");
			final HandleJassType fogmodifierType = globals.registerHandleType("fogmodifier");
			final HandleJassType dialogType = globals.registerHandleType("dialog");
			final HandleJassType buttonType = globals.registerHandleType("button");
			final HandleJassType questType = globals.registerHandleType("quest");
			final HandleJassType questitemType = globals.registerHandleType("questitem");
			final HandleJassType defeatconditionType = globals.registerHandleType("defeatcondition");
			final HandleJassType timerdialogType = globals.registerHandleType("timerdialog");
			final HandleJassType leaderboardType = globals.registerHandleType("leaderboard");
			final HandleJassType multiboardType = globals.registerHandleType("multiboard");
			final HandleJassType multiboarditemType = globals.registerHandleType("multiboarditem");
			final HandleJassType trackableType = globals.registerHandleType("trackable");
			final HandleJassType gamecacheType = globals.registerHandleType("gamecache");
			final HandleJassType versionType = globals.registerHandleType("version");
			final HandleJassType itemtypeType = globals.registerHandleType("itemtype");
			final HandleJassType texttagType = globals.registerHandleType("texttag");
			final HandleJassType attacktypeType = globals.registerHandleType("attacktype");
			final HandleJassType damagetypeType = globals.registerHandleType("damagetype");
			final HandleJassType weapontypeType = globals.registerHandleType("weapontype");
			final HandleJassType soundtypeType = globals.registerHandleType("soundtype");
			final HandleJassType lightningType = globals.registerHandleType("lightning");
			final HandleJassType pathingtypeType = globals.registerHandleType("pathingtype");
			final HandleJassType imageType = globals.registerHandleType("image");
			final HandleJassType ubersplatType = globals.registerHandleType("ubersplat");
			final HandleJassType hashtableType = globals.registerHandleType("hashtable");
			final HandleJassType frameHandleType = globals.registerHandleType("framehandle");
			final HandleJassType minimapiconType = globals.registerHandleType("minimapicon");

			// Warsmash Ability API
			final HandleJassType abilitytypeType = globals.registerHandleType("abilitytype");
			final HandleJassType orderbuttonType = globals.registerHandleType("orderbutton");
			final HandleJassType orderbuttontypeType = globals.registerHandleType("orderbuttontype");
			final HandleJassType abilitybehaviorType = globals.registerHandleType("behavior");
			final HandleJassType behaviorexprType = globals.registerHandleType("behaviorexpr");
			final HandleJassType iconuiType = globals.registerHandleType("iconui");

			// Warsmash Ability API 2 "Ability Builder ported to Jass"
			final HandleJassType abilitytypeleveldataType = globals.registerHandleType("abilitytypeleveldata");
			final HandleJassType targettypeType = globals.registerHandleType("targettype");
			final HandleJassType targettypesType = globals.registerHandleType("targettypes");
			final HandleJassType texttagconfigtypeType = globals.registerHandleType("texttagconfigtype");
			final HandleJassType activeabilityType = globals.registerHandleType("activeability");
			final HandleJassType localstoreType = globals.registerHandleType("localstore");
			final HandleJassType destructablebuffType = globals.registerHandleType("destructablebuff");
			final HandleJassType abtimeofdayeventType = globals.registerHandleType("abtimeofdayevent");
			final HandleJassType worldeditordatatypeType = globals.registerHandleType("worldeditordatatype");
			final HandleJassType gameobjectType = globals.registerHandleType("gameobject");
			final HandleJassType projectileType = globals.registerHandleType("projectile");
			final HandleJassType nonstackingstatbuffType = globals.registerHandleType("nonstackingstatbuff");
			final HandleJassType nonstackingstatbufftypeType = globals.registerHandleType("nonstackingstatbufftype");
			final HandleJassType datafieldletterType = globals.registerHandleType("datafieldletter");
			final HandleJassType autocasttypeType = globals.registerHandleType("autocasttype");
			final HandleJassType abconftypeType = globals.registerHandleType("abconftype");
			final HandleJassType abilitybuilderconfigurationType = globals
					.registerHandleType("abilitybuilderconfiguration");
			final HandleJassType abtimerType = globals.registerHandleType("abtimer");
			final HandleJassType abilitydisabletypeType = globals.registerHandleType("abilitydisabletype");
			final HandleJassType resourcetypeType = globals.registerHandleType("resourcetype");
			final HandleJassType intexprType = globals.registerHandleType("intexpr");

			// Warsmash Ability API 3
			final HandleJassType handlelistType = globals.registerHandleType("handlelist");
			final HandleJassType behaviorcategoryType = globals.registerHandleType("behaviorcategory");
			final HandleJassType abilitycategoryType = globals.registerHandleType("abilitycategory");
			final HandleJassType stringlistType = globals.registerHandleType("stringlist");
			final HandleJassType abilitytargetType = globals.registerHandleType("abilitytarget");
			final HandleJassType abilitytargetvisitorType = globals.registerHandleType("abilitytargetvisitor");

			final HandleJassType primarytagType = globals.registerHandleType("primarytag");
			final HandleJassType primarytagsType = globals.registerHandleType("primarytags");
			final HandleJassType secondarytagType = globals.registerHandleType("secondarytag");
			final HandleJassType secondarytagsType = globals.registerHandleType("secondarytags");

			final HandleJassType rangedbehaviorType = globals.registerHandleType("rangedbehavior");
			final HandleJassType abstractrangedbehaviorType = globals.registerHandleType("abstractrangedbehavior");

			registerTypingNatives(jassProgramVisitor, raceType, alliancetypeType, racepreferenceType, igamestateType,
					fgamestateType, playerstateType, playerscoreType, playergameresultType, unitstateType,
					aidifficultyType, gameeventType, playereventType, playeruniteventType, uniteventType, limitopType,
					widgeteventType, dialogeventType, unittypeType, gamespeedType, gamedifficultyType, gametypeType,
					mapflagType, mapvisibilityType, mapsettingType, mapdensityType, mapcontrolType, playerslotstateType,
					volumegroupType, camerafieldType, playercolorType, placementType, startlocprioType,
					raritycontrolType, blendmodeType, texmapflagsType, effecttypeType, fogstateType, versionType,
					itemtypeType, attacktypeType, damagetypeType, weapontypeType, soundtypeType, pathingtypeType);
			registerConversionAndStringNatives(jassProgramVisitor, gameUI);
			final HandleIdAllocator configHandleIds = new HandleIdAllocator();
			registerConfigNatives(jassProgramVisitor, mapConfig, startlocprioType, gametypeType, placementType,
					gamespeedType, gamedifficultyType, mapdensityType, locationType, playerType, playercolorType,
					mapcontrolType, playerslotstateType, mapConfig, configHandleIds);
			// Blizzard.j global initializers create timers and groups while the config phase loads it.
			jassProgramVisitor.getJassNativeManager().createNative("CreateTimer",
					(arguments, globalScope, triggerScope) -> new HandleJassValue(timerType,
							new CTimerJass(configHandleIds.createId())));
			jassProgramVisitor.getJassNativeManager().createNative("CreateGroup",
					(arguments, globalScope, triggerScope) -> new HandleJassValue(groupType,
							new UnitGroup(configHandleIds.createId())));
			jassProgramVisitor.getJassNativeManager().createNative("SetPlayerState",
					(arguments, globalScope, triggerScope) -> {
						final CBasePlayer player = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
						final CPlayerState whichPlayerState = nullable(arguments, 1,
								ObjectJassValueVisitor.getInstance());
						if ((player != null) && (whichPlayerState != null)) {
							player.setConfigPlayerState(whichPlayerState,
									arguments.get(2).visit(IntegerJassValueVisitor.getInstance()));
						}
						return null;
					});

		}

		public void config() {
			try {
				final JassThread configThread = this.jassProgramVisitor.getGlobals().createThread("config",
						Collections.emptyList(), TriggerExecutionScope.EMPTY);
				this.jassProgramVisitor.getGlobals().runThreadUntilCompletion(configThread);
			}
			catch (final Exception exc) {
				throw new JassException(this.jassProgramVisitor.getGlobals(),
						"Exception on Line " + this.jassProgramVisitor.getGlobals().getLineNumber(), exc);
			}
		}

	}

	private static void registerSharedCommonAIFunctions(final JassProgram jassProgramVisitor,
			final War3MapViewer war3MapViewer, final WarsmashUI meleeUI, final CSimulation simulation) {
		jassProgramVisitor.getJassNativeManager().createNative("Sleep", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Double seconds = arguments.get(0).visit(RealJassValueVisitor.getInstance());
				final JassThread currentThread = globalScope.getCurrentThread();
				if (currentThread != null) {
					currentThread.setSleeping(true);
					final CTimerSleepAction timer = new CTimerSleepAction(currentThread);
					timer.setRepeats(false);
					timer.setTimeoutTime(seconds.floatValue());
					timer.start(simulation);
				}
				else {
					throw new JassException(globalScope, "Needs to sleep " + seconds + " but no thread was found",
							null);
				}
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("DebugS", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final String str = nullable(arguments, 0, StringJassValueVisitor.getInstance());
				final String value = "DebugS: " + str;
				meleeUI.displayTimedText(0, 0, 20, value);
				System.err.println(value);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("DebugFI", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final String str = nullable(arguments, 0, StringJassValueVisitor.getInstance());
				final int val = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());

				final String value = "DebugFI: " + String.format(str, val);
				meleeUI.displayTimedText(0, 0, 20, value);
				System.err.println(value);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("DebugUnitID", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final String str = nullable(arguments, 0, StringJassValueVisitor.getInstance());
				final int valRawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
				final War3ID valId = new War3ID(valRawcode);

				final String value = "DebugUnitID: " + String.format(str, valId);
				meleeUI.displayTimedText(0, 0, 20, value);
				System.err.println(value);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("DisplayText", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int p = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				final String str = nullable(arguments, 1, StringJassValueVisitor.getInstance());

				if (war3MapViewer.getLocalPlayerIndex() == p) {
					meleeUI.displayTimedText(0, 0, 20, str);
				}
				System.err.println(p + ": " + str);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("DisplayTextI", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int p = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				final String str = nullable(arguments, 1, StringJassValueVisitor.getInstance());
				final int v1 = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
				final String value = String.format(str, v1);
				if (war3MapViewer.getLocalPlayerIndex() == p) {
					meleeUI.displayTimedText(0, 0, 20, value);
				}
				System.err.println(p + ": " + value);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("DisplayTextII", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int p = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				final String str = nullable(arguments, 1, StringJassValueVisitor.getInstance());
				final int v1 = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
				final int v2 = arguments.get(3).visit(IntegerJassValueVisitor.getInstance());
				final String value = String.format(str, v1, v2);
				if (war3MapViewer.getLocalPlayerIndex() == p) {
					meleeUI.displayTimedText(0, 0, 20, value);
				}
				System.err.println(p + ": " + value);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("DisplayTextIII", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int p = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				final String str = nullable(arguments, 1, StringJassValueVisitor.getInstance());
				final int v1 = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
				final int v2 = arguments.get(3).visit(IntegerJassValueVisitor.getInstance());
				final int v3 = arguments.get(4).visit(IntegerJassValueVisitor.getInstance());
				final String value = String.format(str, v1, v2, v3);
				if (war3MapViewer.getLocalPlayerIndex() == p) {
					meleeUI.displayTimedText(0, 0, 20, value);
				}
				System.err.println(p + ": " + value);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetUnitBuildTime",
				(arguments, globalScope, triggerScope) -> {
					final Integer typeId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final CUnitType unitTypeData = simulation.getUnitData().getUnitType(new War3ID(typeId));
					if (unitTypeData != null) {
						return IntegerJassValue.of(unitTypeData.getBuildTime());
					}
					return IntegerJassValue.ZERO;
				});
	}

	private static void setupTriggerAPI(final JassProgram jassProgramVisitor, final HandleJassType triggerType,
			final HandleJassType triggeractionType, final HandleJassType triggerconditionType,
			final HandleJassType boolexprType, final HandleJassType conditionfuncType,
			final HandleJassType filterfuncType, final HandleJassType eventidType) {
		// ============================================================================
		// Native trigger interface
		//
		jassProgramVisitor.getJassNativeManager().createNative("CreateTrigger",
				(arguments, globalScope, triggerScope) -> {
					return new HandleJassValue(triggerType, new Trigger());
				});
		jassProgramVisitor.getJassNativeManager().createNative("DestroyTrigger",
				(arguments, globalScope, triggerScope) -> {
					final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
					trigger.destroy();
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("ResetTrigger",
				(arguments, globalScope, triggerScope) -> {
					final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
					trigger.reset();
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("EnableTrigger",
				(arguments, globalScope, triggerScope) -> {
					final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
					trigger.setEnabled(true);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("DisableTrigger",
				(arguments, globalScope, triggerScope) -> {
					final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
					trigger.setEnabled(false);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("IsTriggerEnabled",
				(arguments, globalScope, triggerScope) -> {
					final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
					return BooleanJassValue.of(trigger.isEnabled());
				});
		jassProgramVisitor.getJassNativeManager().createNative("TriggerWaitOnSleeps",
				(arguments, globalScope, triggerScope) -> {
					final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
					final Boolean value = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
					trigger.setWaitOnSleeps(value.booleanValue());
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("IsTriggerWaitOnSleeps",
				(arguments, globalScope, triggerScope) -> {
					final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
					return BooleanJassValue.of(trigger.isWaitOnSleeps());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetTriggeringTrigger",
				(arguments, globalScope, triggerScope) -> {
					return new HandleJassValue(triggerType, triggerScope.getTriggeringTrigger());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetTriggerEventId",
				(arguments, globalScope, triggerScope) -> {
					return new HandleJassValue(eventidType,
							((CommonTriggerExecutionScope) triggerScope).getTriggerEventId());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetTriggerEvalCount",
				(arguments, globalScope, triggerScope) -> {
					final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
					return IntegerJassValue.of(trigger.getEvalCount());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetTriggerExecCount",
				(arguments, globalScope, triggerScope) -> {
					final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
					return IntegerJassValue.of(trigger.getExecCount());
				});
		jassProgramVisitor.getJassNativeManager().createNative("ExecuteFunc",
				(arguments, globalScope, triggerScope) -> {
					final String funcName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					final Integer functionByName = globalScope.getUserFunctionInstructionPtr(funcName);
					System.out.println("ExecuteFunc (\"" + funcName + "\")");
					if (functionByName != null) {
						globalScope.runThreadUntilSleep(globalScope.createThread(functionByName, triggerScope));
					}
					return null;
				});

		// ============================================================================
		// Boolean Expr API ( for compositing trigger conditions and unit filter
		// funcs...)
		// ============================================================================
		jassProgramVisitor.getJassNativeManager().createNative("And", (arguments, globalScope, triggerScope) -> {
			final TriggerBooleanExpression operandA = arguments.get(0)
					.visit(ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
			final TriggerBooleanExpression operandB = arguments.get(1)
					.visit(ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
			return new HandleJassValue(boolexprType, new BoolExprAnd(operandA, operandB));
		});
		jassProgramVisitor.getJassNativeManager().createNative("Or", (arguments, globalScope, triggerScope) -> {
			final TriggerBooleanExpression operandA = arguments.get(0)
					.visit(ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
			final TriggerBooleanExpression operandB = arguments.get(1)
					.visit(ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
			return new HandleJassValue(boolexprType, new BoolExprOr(operandA, operandB));
		});
		jassProgramVisitor.getJassNativeManager().createNative("Not", (arguments, globalScope, triggerScope) -> {
			final TriggerBooleanExpression operand = arguments.get(0)
					.visit(ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
			return new HandleJassValue(boolexprType, new BoolExprNot(operand));
		});
		jassProgramVisitor.getJassNativeManager().createNative("Condition", (arguments, globalScope, triggerScope) -> {
			final CodeJassValue func = arguments.get(0).visit(CodeJassValueVisitor.getInstance());
			return new HandleJassValue(conditionfuncType, new BoolExprCondition(func));
		});
		jassProgramVisitor.getJassNativeManager().createNative("DestroyCondition",
				(arguments, globalScope, triggerScope) -> {
					final BoolExprCondition condition = arguments.get(0)
							.visit(ObjectJassValueVisitor.<BoolExprCondition>getInstance());
					reportMissingDestructorOnce("DestroyCondition");
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("Filter", (arguments, globalScope, triggerScope) -> {
			final CodeJassValue func = arguments.get(0).visit(CodeJassValueVisitor.getInstance());
			return new HandleJassValue(filterfuncType, new BoolExprFilter(func));
		});
		jassProgramVisitor.getJassNativeManager().createNative("DestroyFilter",
				(arguments, globalScope, triggerScope) -> {
					final BoolExprFilter filter = nullable(arguments, 0,
							ObjectJassValueVisitor.<BoolExprFilter>getInstance());
					reportMissingDestructorOnce("DestroyFilter");
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("DestroyBoolExpr",
				(arguments, globalScope, triggerScope) -> {
					final TriggerBooleanExpression boolexpr = nullable(arguments, 0,
							ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
					reportMissingDestructorOnce("DestroyBoolExpr");
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("TriggerAddCondition",
				(arguments, globalScope, triggerScope) -> {
					final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
					final TriggerBooleanExpression condition = arguments.get(1)
							.visit(ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
					final int index = whichTrigger.addCondition(condition);
					return new HandleJassValue(triggerconditionType,
							new TriggerCondition(condition, whichTrigger, index));
				});
		jassProgramVisitor.getJassNativeManager().createNative("TriggerRemoveCondition",
				(arguments, globalScope, triggerScope) -> {
					final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
					final TriggerCondition condition = arguments.get(1)
							.visit(ObjectJassValueVisitor.<TriggerCondition>getInstance());
					if (condition.getTrigger() != whichTrigger) {
						throw new IllegalArgumentException("Unable to remove condition, wrong trigger");
					}
					whichTrigger.removeConditionAtIndex(condition.getConditionIndex());
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("TriggerClearConditions",
				(arguments, globalScope, triggerScope) -> {
					final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
					whichTrigger.clearConditions();
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("TriggerAddAction",
				(arguments, globalScope, triggerScope) -> {
					final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
					final CodeJassValue actionFunc = arguments.get(1).visit(CodeJassValueVisitor.getInstance());
					final int actionIndex = whichTrigger.addAction(actionFunc);
					return new HandleJassValue(triggeractionType,
							new TriggerAction(whichTrigger, actionFunc, actionIndex));
				});
		jassProgramVisitor.getJassNativeManager().createNative("TriggerRemoveAction",
				(arguments, globalScope, triggerScope) -> {
					final Trigger whichTrigger = nullable(arguments, 0, ObjectJassValueVisitor.<Trigger>getInstance());
					final TriggerAction action = nullable(arguments, 1,
							ObjectJassValueVisitor.<TriggerAction>getInstance());
					if ((whichTrigger != null) && (action != null)) {
						whichTrigger.removeAction(action.getAction());
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("TriggerClearActions",
				(arguments, globalScope, triggerScope) -> {
					final Trigger whichTrigger = nullable(arguments, 0, ObjectJassValueVisitor.<Trigger>getInstance());
					if (whichTrigger != null) {
						whichTrigger.clearActions();
					}
					return null;
				});
	}

	public static void registerRandomNatives(final JassProgram jassProgramVisitor, final CSimulation simulation) {
		jassProgramVisitor.getJassNativeManager().createNative("GetRandomReal",
				(arguments, globalScope, triggerScope) -> {
					final float lowBound = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float highBound = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					return RealJassValue
							.of((simulation.getSeededRandom().nextFloat() * (highBound - lowBound)) + lowBound);
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetRandomInt",
				(arguments, globalScope, triggerScope) -> {
					int lowBound = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					int highBound = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					if (lowBound > highBound) {
						if (highBound >= 0) {
							lowBound = highBound;
						}
						else {
							highBound = lowBound;
						}
					}
					return IntegerJassValue
							.of(simulation.getSeededRandom().nextInt((highBound - lowBound) + 1) + lowBound);
				});
	}

	/**
	 * A unit removed from the game reads as no unit once the step that removed it is over, as in the original game.
	 */
	private static <T extends CWidget> T inGame(final T widget) {
		if ((widget instanceof CUnit) && ((CUnit) widget).isRemovedFromGame()) {
			return null;
		}
		if ((widget instanceof CDestructable) && ((CDestructable) widget).isRemovedFromGame()) {
			return null;
		}
		return widget;
	}

	private static void registerQuestFlagNatives(final JassProgram jassProgramVisitor, final String flag,
			final BiConsumer<CQuest, Boolean> setter, final Predicate<CQuest> getter) {
		jassProgramVisitor.getJassNativeManager().createNative("QuestSet" + flag,
				(arguments, globalScope, triggerScope) -> {
					final CQuest quest = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					if (quest != null) {
						setter.accept(quest, arguments.get(1).visit(BooleanJassValueVisitor.getInstance()));
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("IsQuest" + flag,
				(arguments, globalScope, triggerScope) -> {
					final CQuest quest = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					return BooleanJassValue.of((quest != null) && getter.test(quest));
				});
	}

	private static float realArg(final List<JassValue> arguments, final int index) {
		return arguments.get(index).visit(RealJassValueVisitor.getInstance()).floatValue();
	}

	private static int intArg(final List<JassValue> arguments, final int index) {
		return arguments.get(index).visit(IntegerJassValueVisitor.getInstance());
	}

	// Measured on 1.29: to scripts an illusion is no hero, whatever stats it copied.
	private static CAbilityHero scriptHeroData(final CUnit unit) {
		return unit.isIllusion() ? null : unit.getHeroData();
	}

	/**
	 * Measured on 1.29 (probe enum-rect-grid): rect enumerations of units, items and destructables work on the 32
	 * world-unit cells. An object is found when the rect covers the whole cell its centre is in and the rect's
	 * left and bottom edges lie strictly before that cell: cells floor(min / 32) + 1 to floor(max / 32) - 1.
	 */
	private static boolean rectCellsContain(final Rectangle rect, final float x, final float y) {
		final int cellX = (int) StrictMath.floor(x / 32f);
		final int cellY = (int) StrictMath.floor(y / 32f);
		return (cellX > (int) StrictMath.floor(rect.x / 32f))
				&& (cellX < (int) StrictMath.floor((rect.x + rect.width) / 32f))
				&& (cellY > (int) StrictMath.floor(rect.y / 32f))
				&& (cellY < (int) StrictMath.floor((rect.y + rect.height) / 32f));
	}

	private static <T> T nullable(final List<JassValue> arguments, final int index, final JassValueVisitor<T> visitor) {
		final JassValue arg = arguments.get(index);
		if (arg == null) {
			return null;
		}
		return arg.visit(visitor);
	}

	/**
	 * Same layout as the original game's preload files (CustomMapData), which maps read back as save codes.
	 */
	private static void writePreloadFile(final String filename, final List<String> lines) {
		final StringBuilder text = new StringBuilder();
		text.append("function PreloadFiles takes nothing returns nothing\r\n\r\n\tcall PreloadStart()\r\n");
		for (final String line : lines) {
			text.append("\tcall Preload( \"").append(line).append("\" )\r\n");
		}
		text.append("\tcall PreloadEnd( 0.0 )\r\n\r\nendfunction\r\n\r\n");
		try {
			Gdx.files.local("CustomMapData/" + filename.replace('\\', '/')).writeString(text.toString(), false,
					"UTF-8");
		}
		catch (final Exception exc) {
			System.err.println("PreloadGenEnd(\"" + filename + "\") failed!");
			exc.printStackTrace();
		}
	}

	private static List<CBuff> matchingBuffs(final CUnit unit, final List<JassValue> arguments, final int first) {
		final boolean[] flags = new boolean[7];
		for (int i = 0; i < flags.length; i++) {
			flags[i] = arguments.get(first + i).visit(BooleanJassValueVisitor.getInstance());
		}
		return matchingBuffs(unit, flags[0], flags[1], flags[2], flags[3], flags[4], flags[5], flags[6]);
	}

	/**
	 * Buffs flagged neither magic nor physical are matched by either flag, since most engine buffs leave both
	 * unset.
	 */
	private static List<CBuff> matchingBuffs(final CUnit unit, final boolean positive, final boolean negative,
			final boolean magic, final boolean physical, final boolean timedLife, final boolean aura,
			final boolean autoDispel) {
		final List<CBuff> buffs = new ArrayList<>();
		if (unit == null) {
			return buffs;
		}
		for (final CAbility ability : unit.getAbilities()) {
			if (!(ability instanceof CBuff)) {
				continue;
			}
			final CBuff buff = (CBuff) ability;
			final boolean polarityOk = buff.isPositive() ? positive : negative;
			final boolean typeOk = (!buff.isMagic() && !buff.isPhysical()) ? (magic || physical)
					: ((magic && buff.isMagic()) || (physical && buff.isPhysical()));
			if (polarityOk && typeOk && (timedLife || !buff.isTimedLifeBar()) && (aura || !buff.isAura())
					&& (!autoDispel || buff.isDispellable())) {
				buffs.add(buff);
			}
		}
		return buffs;
	}

	private static boolean hasViewerHandler(final War3MapViewer war3MapViewer, final String filename) {
		final int dotIndex = filename.lastIndexOf('.');
		return (dotIndex != -1)
				&& (war3MapViewer.findHandler(filename.substring(dotIndex).toLowerCase(Locale.US)) != null);
	}

	private static void doPreloadScript(final DataSource dataSource, final Viewport uiViewport, final Scene uiScene,
			final War3MapViewer war3MapViewer, final String filename, final WarsmashUI meleeUI,
			final String[] originalFiles, final JassProgram jassProgramVisitor, final String mainFunction, final boolean runMain) {
		final Integer prevPtr = jassProgramVisitor.getGlobals().getUserFunctionInstructionPtr(mainFunction);
		try {
			try (InputStreamReader reader = new InputStreamReader(dataSource.getResourceAsStream(filename))) {
				final SmashJassParser smashJassParser = new SmashJassParser(reader);
				smashJassParser.scanAndParse(filename, jassProgramVisitor);
			}
			jassProgramVisitor.initialize();
		}
		catch (final Exception e) {
			e.printStackTrace();
			JassLog.report(e);
		}
		final Integer loadedPtr = jassProgramVisitor.getGlobals().getUserFunctionInstructionPtr(mainFunction);
		war3MapViewer.addLoadedScript(new SavedGame.AiEntry(-1, filename, mainFunction));
		if (runMain && (prevPtr != loadedPtr)) {
			// else we didn't actually load a new fxn
			try {
				final JassThread preloadThread = jassProgramVisitor.getGlobals().createThread(mainFunction,
						Collections.emptyList(), TriggerExecutionScope.EMPTY);
				jassProgramVisitor.getGlobals().queueThread(preloadThread);
			}
			catch (final Exception e) {
				throw new JassException(jassProgramVisitor.getGlobals(), "Unable to run: " + filename, e);
			}
		}
	}

	private static void doAIScript(final DataSource dataSource, final Viewport uiViewport, final Scene uiScene,
			final War3MapViewer war3MapViewer, final CPlayer player, final String filename, final WarsmashUI meleeUI,
			final String[] originalFiles, final JassProgram jassProgramVisitor, final CSimulation game,
			final String mainFunction, final boolean runMain) {
		final JassProgram jassProgram = new JassProgram();
		jassProgram.inheritFrom(jassProgramVisitor);
		final JassAIEnvironment jassAIEnvironment = new JassAIEnvironment(jassProgram, dataSource, uiViewport, uiScene,
				game, player.getId(), meleeUI);
		// common.ai declares these again, so the AI program needs its own implementations.
		registerSharedCommonAIFunctions(jassProgram, war3MapViewer, meleeUI, game);
		final Integer prevPtr = jassProgram.getGlobals().getUserFunctionInstructionPtr(mainFunction);
		// Blizzard.j names the melee scripts without their folder; every AI script runs on top of common.ai.
		final String scriptPath = dataSource.has(filename) ? filename : "Scripts\\" + filename;
		if (!dataSource.has(scriptPath)) {
			// Measured: the original ignores an AI script that is not there.
			return;
		}
		try {
			for (final String aiFile : new String[] { "Scripts\\common.ai", scriptPath }) {
				try (InputStreamReader reader = new InputStreamReader(dataSource.getResourceAsStream(aiFile))) {
					final SmashJassParser smashJassParser = new SmashJassParser(reader);
					smashJassParser.scanAndParse(aiFile, jassProgram);
				}
			}
			jassProgram.initialize();
		}
		catch (final Exception e) {
			e.printStackTrace();
			JassLog.report(e);
		}
		final Integer loadedPtr = jassProgram.getGlobals().getUserFunctionInstructionPtr(mainFunction);
		if (runMain && (prevPtr != loadedPtr)) {
			// else we didn't actually load a new fxn
			try {
				final JassThread preloadThread = jassProgram.getGlobals().createThread(mainFunction,
						Collections.emptyList(), TriggerExecutionScope.EMPTY);
				jassProgram.getGlobals().queueThread(preloadThread);
			}
			catch (final Exception e) {
				throw new JassException(jassProgram.getGlobals(), "Unable to run: " + filename, e);
			}
		}
		jassAIEnvironment.setScript(filename, mainFunction);
		war3MapViewer.addLoadedScript(new SavedGame.AiEntry(player.getId(), filename, mainFunction));
		player.setAiScript(jassAIEnvironment);
	}

	private static boolean isNullValue(final JassValue value) {
		return (value == null) || ((value instanceof HandleJassValue) && (((HandleJassValue) value).getJavaValue() == null));
	}

	private static int hashtableSlot(final JassType type) {
		if (type == JassType.INTEGER) {
			return CHashtable.INTEGER;
		}
		if (type == JassType.REAL) {
			return CHashtable.REAL;
		}
		if (type == JassType.BOOLEAN) {
			return CHashtable.BOOLEAN;
		}
		if (type == JassType.STRING) {
			return CHashtable.STRING;
		}
		return CHashtable.HANDLE;
	}

	private static final class SaveHashtableValueFunc implements JassFunction {
		private final int slot;

		public SaveHashtableValueFunc() {
			this(CHashtable.HANDLE);
		}

		public SaveHashtableValueFunc(final int slot) {
			this.slot = slot;
		}

		@Override
		public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
				final TriggerExecutionScope triggerScope) {
			final CHashtable table = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
			final Integer parentKey = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
			final Integer childKey = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
			if (table == null) {
				return BooleanJassValue.FALSE;
			}
			final JassValue value = arguments.get(3);
			// Measured on 1.29: a null handle is not saved (false, an older value stays); a null string is saved.
			if ((this.slot == CHashtable.HANDLE) && isNullValue(value)) {
				return BooleanJassValue.FALSE;
			}
			table.save(this.slot, parentKey, childKey,
					value == null ? StringJassValue.of(null) : value);
			return BooleanJassValue.TRUE;
		}
	}

	private static final class LoadHashtableValueFunc implements JassFunction {
		private final JassValue nullValue;
		private final JassType type;
		private final int slot;

		public LoadHashtableValueFunc(final JassValue nullValue) {
			this.nullValue = nullValue;
			this.type = nullValue.visit(JassTypeGettingValueVisitor.getInstance());
			this.slot = hashtableSlot(this.type);
		}

		@Override
		public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
				final TriggerExecutionScope triggerScope) {
			final CHashtable table = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
			final Integer parentKey = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
			final Integer childKey = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
			if (table == null) {
				return this.nullValue;
			}
			final Object loadedValue = table.load(this.slot, parentKey, childKey);
			if (loadedValue == null) {
				return this.nullValue;
			}
			final JassValue loadedJassValue = (JassValue) loadedValue;
			if ((this.slot == CHashtable.HANDLE) && !this.type
					.isAssignableFrom(loadedJassValue.visit(JassTypeGettingValueVisitor.getInstance()))) {
				return this.nullValue;
			}
			return loadedJassValue;
		}
	}

	private static final class HaveSavedHashtableValueFunc implements JassFunction {
		private final int slot;

		public HaveSavedHashtableValueFunc(final JassType jassType) {
			this.slot = hashtableSlot(jassType);
		}

		@Override
		public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
				final TriggerExecutionScope triggerScope) {
			final CHashtable table = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
			if (table == null) {
				return BooleanJassValue.FALSE;
			}
			final Integer parentKey = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
			final Integer childKey = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
			return BooleanJassValue.of(table.load(this.slot, parentKey, childKey) != null);
		}
	}

	private static final class RemoveSavedHashtableValueFunc implements JassFunction {
		private final int slot;

		public RemoveSavedHashtableValueFunc(final int slot) {
			this.slot = slot;
		}

		@Override
		public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
				final TriggerExecutionScope triggerScope) {
			final CHashtable table = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
			if (table != null) {
				table.remove(this.slot, arguments.get(1).visit(IntegerJassValueVisitor.getInstance()),
						arguments.get(2).visit(IntegerJassValueVisitor.getInstance()));
			}
			return null;
		}
	}

	public static void registerConfigNatives(final JassProgram jassProgramVisitor, final War3MapConfig mapConfig,
			final HandleJassType startlocprioType, final HandleJassType gametypeType,
			final HandleJassType placementType, final HandleJassType gamespeedType,
			final HandleJassType gamedifficultyType, final HandleJassType mapdensityType,
			final HandleJassType locationType, final HandleJassType playerType, final HandleJassType playercolorType,
			final HandleJassType mapcontrolType, final HandleJassType playerslotstateType, final CPlayerAPI playerAPI,
			final HandleIdAllocator handleIdAllocator) {
		// Colors the engine cannot draw: kept for GetPlayerColor only, the units keep their last color.
		final Map<Integer, Integer> unknownPlayerColors = new HashMap<>();
		jassProgramVisitor.getJassNativeManager().createNative("SetMapName", (arguments, globalScope, triggerScope) -> {
			final String name = arguments.get(0).visit(StringJassValueVisitor.getInstance());
			mapConfig.setMapName(name);
			return null;
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetMapDescription",
				(arguments, globalScope, triggerScope) -> {
					final String name = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					mapConfig.setMapDescription(name);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetTeams", (arguments, globalScope, triggerScope) -> {
			final Integer teamCount = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
			mapConfig.setTeamCount(teamCount);
			return null;
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetPlayers", (arguments, globalScope, triggerScope) -> {
			final Integer playerCount = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
			mapConfig.setPlayerCount(playerCount);
			return null;
		});
		jassProgramVisitor.getJassNativeManager().createNative("DefineStartLocation",
				(arguments, globalScope, triggerScope) -> {
					final Integer whichStartLoc = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final Double x = arguments.get(1).visit(RealJassValueVisitor.getInstance());
					final Double y = arguments.get(2).visit(RealJassValueVisitor.getInstance());
					mapConfig.getStartLoc(whichStartLoc).setX(x.floatValue());
					mapConfig.getStartLoc(whichStartLoc).setY(y.floatValue());
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("DefineStartLocationLoc",
				(arguments, globalScope, triggerScope) -> {
					final Integer whichStartLoc = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final AbilityPointTarget whichLocation = arguments.get(1)
							.visit(ObjectJassValueVisitor.<AbilityPointTarget>getInstance());
					mapConfig.getStartLoc(whichStartLoc).setX(whichLocation.x);
					mapConfig.getStartLoc(whichStartLoc).setY(whichLocation.y);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetStartLocPrioCount",
				(arguments, globalScope, triggerScope) -> {
					final Integer whichStartLoc = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final Integer prioSlotCount = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					mapConfig.getStartLoc(whichStartLoc).setStartLocPrioCount(prioSlotCount);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetStartLocPrio",
				(arguments, globalScope, triggerScope) -> {
					final Integer whichStartLoc = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final Integer prioSlotIndex = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final Integer otherStartLocIndex = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					final CStartLocPrio priority = arguments.get(1)
							.visit(ObjectJassValueVisitor.<CStartLocPrio>getInstance());
					mapConfig.getStartLoc(whichStartLoc).setStartLocPrio(prioSlotIndex, otherStartLocIndex, priority);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetStartLocPrioSlot",
				(arguments, globalScope, triggerScope) -> {
					final Integer whichStartLoc = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final Integer prioSlotIndex = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					return IntegerJassValue
							.of(mapConfig.getStartLoc(whichStartLoc).getOtherStartIndices()[prioSlotIndex]);
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetStartLocPrio",
				(arguments, globalScope, triggerScope) -> {
					final Integer whichStartLoc = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final Integer prioSlotIndex = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					return new HandleJassValue(startlocprioType,
							mapConfig.getStartLoc(whichStartLoc).getOtherStartLocPriorities()[prioSlotIndex]);
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetGameTypeSupported",
				(arguments, globalScope, triggerScope) -> {
					final CGameType gameType = arguments.get(0).visit(ObjectJassValueVisitor.<CGameType>getInstance());
					final Boolean value = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
					mapConfig.setGameTypeSupported(gameType, value);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetMapFlag", (arguments, globalScope, triggerScope) -> {
			final CMapFlag mapFlag = arguments.get(0).visit(ObjectJassValueVisitor.<CMapFlag>getInstance());
			final Boolean value = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
			mapConfig.setMapFlag(mapFlag, value);
			return null;
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetGamePlacement",
				(arguments, globalScope, triggerScope) -> {
					final CMapPlacement placement = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CMapPlacement>getInstance());
					mapConfig.setPlacement(placement);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetGameSpeed",
				(arguments, globalScope, triggerScope) -> {
					final CGameSpeed gameSpeed = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CGameSpeed>getInstance());
					mapConfig.setGameSpeed(gameSpeed);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetGameDifficulty",
				(arguments, globalScope, triggerScope) -> {
					final CMapDifficulty gameDifficulty = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CMapDifficulty>getInstance());
					mapConfig.setGameDifficulty(gameDifficulty);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetResourceDensity",
				(arguments, globalScope, triggerScope) -> {
					final CMapDensity resourceDensity = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CMapDensity>getInstance());
					mapConfig.setResourceDensity(resourceDensity);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetCreatureDensity",
				(arguments, globalScope, triggerScope) -> {
					final CMapDensity creatureDensity = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CMapDensity>getInstance());
					mapConfig.setCreatureDensity(creatureDensity);
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetTeams", (arguments, globalScope, triggerScope) -> {
			return IntegerJassValue.of(mapConfig.getTeamCount());
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayers", (arguments, globalScope, triggerScope) -> {
			return IntegerJassValue.of(mapConfig.getPlayerCount());
		});
		jassProgramVisitor.getJassNativeManager().createNative("IsGameTypeSupported",
				(arguments, globalScope, triggerScope) -> {
					final Object gameType = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					if (gameType instanceof UnknownEnumHandle) {
						// Game types are bits; 1.29 answers true for a mixed value (99) holding a supported bit.
						final int bits = ((UnknownEnumHandle) gameType).getHandleId();
						for (final CGameType type : CGameType.VALUES) {
							if (((bits & type.getId()) != 0) && mapConfig.isGameTypeSupported(type)) {
								return BooleanJassValue.TRUE;
							}
						}
						return BooleanJassValue.FALSE;
					}
					return BooleanJassValue.of(mapConfig.isGameTypeSupported((CGameType) gameType));
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetGameTypeSelected",
				(arguments, globalScope, triggerScope) -> {
					return new HandleJassValue(gametypeType, mapConfig.getGameTypeSelected());
				});
		jassProgramVisitor.getJassNativeManager().createNative("IsMapFlagSet",
				(arguments, globalScope, triggerScope) -> {
					final CMapFlag mapFlag = arguments.get(0).visit(ObjectJassValueVisitor.<CMapFlag>getInstance());
					return BooleanJassValue.of(mapConfig.isMapFlagSet(mapFlag));
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetGamePlacement",
				(arguments, globalScope, triggerScope) -> {
					return new HandleJassValue(placementType, mapConfig.getPlacement());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetGameSpeed",
				(arguments, globalScope, triggerScope) -> {
					return new HandleJassValue(gamespeedType, mapConfig.getGameSpeed());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetGameDifficulty",
				(arguments, globalScope, triggerScope) -> {
					return new HandleJassValue(gamedifficultyType, mapConfig.getGameDifficulty());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetDefaultDifficulty",
				(arguments, globalScope, triggerScope) -> {
					return new HandleJassValue(gamedifficultyType, mapConfig.getGameDifficulty());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetResourceDensity",
				(arguments, globalScope, triggerScope) -> {
					return new HandleJassValue(mapdensityType, mapConfig.getResourceDensity());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetCreatureDensity",
				(arguments, globalScope, triggerScope) -> {
					return new HandleJassValue(mapdensityType, mapConfig.getCreatureDensity());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetStartLocationX",
				(arguments, globalScope, triggerScope) -> {
					final Integer whichStartLoc = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return RealJassValue.of(mapConfig.getStartLoc(whichStartLoc).getX());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetStartLocationY",
				(arguments, globalScope, triggerScope) -> {
					final Integer whichStartLoc = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return RealJassValue.of(mapConfig.getStartLoc(whichStartLoc).getY());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetStartLocationLoc",
				(arguments, globalScope, triggerScope) -> {
					final Integer whichStartLoc = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return new HandleJassValue(locationType,
							new LocationJass(mapConfig.getStartLoc(whichStartLoc).getX(),
									mapConfig.getStartLoc(whichStartLoc).getY(), handleIdAllocator.createId()));
				});
		// PlayerAPI: with a null player the setters do nothing and the getters read what 1.29 reads
		// (null-players-d320).

		jassProgramVisitor.getJassNativeManager().createNative("SetPlayerTeam",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass player = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final Integer whichTeam = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					if (player != null) {
						player.setTeam(whichTeam);
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetPlayerStartLocation",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass player = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final Integer startLocIndex = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					if (player != null) {
						player.setStartLocationIndex(startLocIndex);
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("ForcePlayerStartLocation",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass player = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final Integer startLocIndex = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					if (player != null) {
						player.forceStartLocation(startLocIndex);
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetPlayerColor",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass player = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final Object playerColor = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					if (player != null) {
						if (playerColor instanceof CPlayerColor) {
							unknownPlayerColors.remove(player.getId());
							playerAPI.setColor(player, (CPlayerColor) playerColor);
						}
						else if (playerColor instanceof UnknownEnumHandle) {
							unknownPlayerColors.put(player.getId(), ((UnknownEnumHandle) playerColor).getHandleId());
						}
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetPlayerAlliance",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass player = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final CPlayerJass otherPlayer = arguments.get(1)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final CAllianceType whichAllianceSetting = arguments.get(2)
							.visit(ObjectJassValueVisitor.<CAllianceType>getInstance());
					final Boolean value = arguments.get(3).visit(BooleanJassValueVisitor.getInstance());
					// Measured (1.29, null-handles-d311): a null player on either side changes nothing.
					if ((player != null) && (otherPlayer != null) && (whichAllianceSetting != null)) {
						player.setAlliance(otherPlayer.getId(), whichAllianceSetting, value);
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerAlliance",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass player = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final CPlayerJass otherPlayer = arguments.get(1)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final CAllianceType whichAllianceSetting = arguments.get(2)
							.visit(ObjectJassValueVisitor.<CAllianceType>getInstance());
					// 1.29 reads false for a null player; a null other player crashes the game there.
					if ((player == null) || (otherPlayer == null) || (whichAllianceSetting == null)) {
						return BooleanJassValue.FALSE;
					}
					return BooleanJassValue.of(player.hasAlliance(otherPlayer.getId(), whichAllianceSetting));
				});
		// Measured: a player is another's ally exactly when it is passive toward it, and its enemy otherwise; with a null
		// player on either side 1.29 reads both as false (null-players-d320).
		jassProgramVisitor.getJassNativeManager().createNative("IsPlayerAlly",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass player = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final CPlayerJass otherPlayer = arguments.get(1)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					if ((player == null) || (otherPlayer == null)) {
						return BooleanJassValue.FALSE;
					}
					return BooleanJassValue.of(player.hasAlliance(otherPlayer.getId(), CAllianceType.PASSIVE));
				});
		jassProgramVisitor.getJassNativeManager().createNative("IsPlayerEnemy",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass player = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final CPlayerJass otherPlayer = arguments.get(1)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					if ((player == null) || (otherPlayer == null)) {
						return BooleanJassValue.FALSE;
					}
					return BooleanJassValue.of(!player.hasAlliance(otherPlayer.getId(), CAllianceType.PASSIVE));
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetPlayerTaxRate",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass player = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final CPlayerJass otherPlayer = arguments.get(1)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final CPlayerState whichResource = arguments.get(2)
							.visit(ObjectJassValueVisitor.<CPlayerState>getInstance());
					final int taxRate = arguments.get(3).visit(IntegerJassValueVisitor.getInstance());
					if ((player != null) && (otherPlayer != null) && (whichResource != null)) {
						player.setTaxRate(otherPlayer.getId(), whichResource, taxRate);
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetPlayerRacePreference",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass player = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final CRacePreference whichRacePreference = arguments.get(1)
							.visit(ObjectJassValueVisitor.<CRacePreference>getInstance());
					if (player != null) {
						player.setRacePref(whichRacePreference);
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetPlayerRaceSelectable",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass player = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final Boolean value = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
					if (player != null) {
						player.setRaceSelectable(value);
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetPlayerController",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass player = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final CMapControl controlType = arguments.get(1)
							.visit(ObjectJassValueVisitor.<CMapControl>getInstance());
					if (player != null) {
						player.setController(controlType);
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetPlayerName",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass player = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final String name = arguments.get(1).visit(StringJassValueVisitor.getInstance());
					if (player != null) {
						player.setName(name);
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("SetPlayerOnScoreScreen",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass player = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final Boolean value = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
					if (player != null) {
						player.setOnScoreScreen(value);
					}
					return null;
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerTeam",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass player = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					return IntegerJassValue.of(player == null ? 0 : player.getTeam());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerStartLocation",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass player = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					return IntegerJassValue.of(player == null ? 0 : player.getStartLocationIndex());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerColor",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass player = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					if ((player != null) && unknownPlayerColors.containsKey(player.getId())) {
						return playercolorType.getUnknownEnumValue(unknownPlayerColors.get(player.getId()));
					}
					// 1.29 reads color 0 (red) for a null player.
					return new HandleJassValue(playercolorType,
							CPlayerColor.getColorByIndex(player == null ? 0 : player.getColor()));
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerSelectable",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass player = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					return BooleanJassValue.of((player != null) && player.isRaceSelectable());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerController",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass player = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					// 1.29 reads MAP_CONTROL_NONE for a null player (null-players-d320).
					if (player == null) {
						return new HandleJassValue(mapcontrolType, CMapControl.NONE);
					}
					return new HandleJassValue(mapcontrolType, player.getController());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerSlotState",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass player = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					return new HandleJassValue(playerslotstateType,
							player == null ? CPlayerSlotState.EMPTY : player.getSlotState());
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerTaxRate",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass player = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final CPlayerJass otherPlayer = arguments.get(1)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final CPlayerState whichResource = arguments.get(2)
							.visit(ObjectJassValueVisitor.<CPlayerState>getInstance());
					if ((player == null) || (otherPlayer == null) || (whichResource == null)) {
						return IntegerJassValue.ZERO;
					}
					return IntegerJassValue.of(player.getTaxRate(otherPlayer.getId(), whichResource));
				});
		jassProgramVisitor.getJassNativeManager().createNative("IsPlayerRacePrefSet",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass player = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final CRacePreference racePref = arguments.get(1)
							.visit(ObjectJassValueVisitor.<CRacePreference>getInstance());
					return BooleanJassValue.of((player != null) && player.isRacePrefSet(racePref));
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerName",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass player = arguments.get(0)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					if (player == null) {
						return JassType.STRING.getNullValue();
					}
					return new StringJassValue(player.getName());
				});

		jassProgramVisitor.getJassNativeManager().createNative("Player", (arguments, globalScope, triggerScope) -> {
			final int playerIndex = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
			if ((playerIndex < 0) || (playerIndex >= WarsmashConstants.MAX_PLAYERS)) {
				return playerType.getNullValue();
			}
			return new HandleJassValue(playerType, playerAPI.getPlayer(playerIndex));
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerId",
				(arguments, globalScope, triggerScope) -> {
					final CPlayerJass whichPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					return (whichPlayer == null) ? IntegerJassValue.of(0) : IntegerJassValue.of(whichPlayer.getId());
				});

		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerNeutralAggressive",
				(arguments, globalScope, triggerScope) -> {
					return IntegerJassValue.of(WarsmashConstants.MAX_PLAYERS - 4);
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetBJPlayerNeutralVictim",
				(arguments, globalScope, triggerScope) -> {
					return IntegerJassValue.of(WarsmashConstants.MAX_PLAYERS - 3);
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetBJPlayerNeutralExtra",
				(arguments, globalScope, triggerScope) -> {
					return IntegerJassValue.of(WarsmashConstants.MAX_PLAYERS - 2);
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerNeutralPassive",
				(arguments, globalScope, triggerScope) -> {
					return IntegerJassValue.of(WarsmashConstants.MAX_PLAYERS - 1);
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetBJMaxPlayers",
				(arguments, globalScope, triggerScope) -> {
					return IntegerJassValue.of(WarsmashConstants.MAX_PLAYERS - 4);
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetBJMaxPlayerSlots",
				(arguments, globalScope, triggerScope) -> {
					return IntegerJassValue.of(WarsmashConstants.MAX_PLAYERS);
				});
	}

	private static int unitEventId(final boolean mouseEventsBeforeUnitEvents, final int scriptEventId) {
		if (mouseEventsBeforeUnitEvents && (scriptEventId >= 272) && (scriptEventId <= 304)) {
			return scriptEventId - 3;
		}
		return scriptEventId;
	}

	// 1.29 keeps a value it has no constant for as an enum handle of that value instead of failing.
	private static HandleJassValue enumValue(final HandleJassType type, final Object[] values, final int i) {
		if ((i >= 0) && (i < values.length)) {
			return new HandleJassValue(type, values[i]);
		}
		return type.getUnknownEnumValue(i);
	}

	private static HandleJassValue enumValue(final HandleJassType type, final Object value, final int i) {
		return value == null ? type.getUnknownEnumValue(i) : new HandleJassValue(type, value);
	}

	public static void registerTypingNatives(final JassProgram jassProgramVisitor, final HandleJassType raceType,
			final HandleJassType alliancetypeType, final HandleJassType racepreferenceType,
			final HandleJassType igamestateType, final HandleJassType fgamestateType,
			final HandleJassType playerstateType, final HandleJassType playerscoreType,
			final HandleJassType playergameresultType, final HandleJassType unitstateType,
			final HandleJassType aidifficultyType, final HandleJassType gameeventType,
			final HandleJassType playereventType, final HandleJassType playeruniteventType,
			final HandleJassType uniteventType, final HandleJassType limitopType, final HandleJassType widgeteventType,
			final HandleJassType dialogeventType, final HandleJassType unittypeType, final HandleJassType gamespeedType,
			final HandleJassType gamedifficultyType, final HandleJassType gametypeType,
			final HandleJassType mapflagType, final HandleJassType mapvisibilityType,
			final HandleJassType mapsettingType, final HandleJassType mapdensityType,
			final HandleJassType mapcontrolType, final HandleJassType playerslotstateType,
			final HandleJassType volumegroupType, final HandleJassType camerafieldType,
			final HandleJassType playercolorType, final HandleJassType placementType,
			final HandleJassType startlocprioType, final HandleJassType raritycontrolType,
			final HandleJassType blendmodeType, final HandleJassType texmapflagsType,
			final HandleJassType effecttypeType, final HandleJassType fogstateType, final HandleJassType versionType,
			final HandleJassType itemtypeType, final HandleJassType attacktypeType, final HandleJassType damagetypeType,
			final HandleJassType weapontypeType, final HandleJassType soundtypeType,
			final HandleJassType pathingtypeType) {
		final ToIntFunction<Object> enumHandleValue = (value) -> (value instanceof CHandle)
				? ((CHandle) value).getHandleId()
				: -1;
		for (final HandleJassType enumType : new HandleJassType[] { raceType, alliancetypeType, racepreferenceType,
				igamestateType, fgamestateType, playerstateType, playerscoreType, playergameresultType, unitstateType,
				aidifficultyType, gameeventType, playereventType, playeruniteventType, uniteventType, limitopType,
				widgeteventType, dialogeventType, unittypeType, gamespeedType, gamedifficultyType, gametypeType,
				mapflagType, mapvisibilityType, mapsettingType, mapdensityType, mapcontrolType, playerslotstateType,
				volumegroupType, camerafieldType, playercolorType, placementType, startlocprioType,
				raritycontrolType, blendmodeType, texmapflagsType, effecttypeType, fogstateType, versionType,
				itemtypeType, weapontypeType, soundtypeType, pathingtypeType, damagetypeType }) {
			enumType.setEnumValue(enumHandleValue);
		}
		attacktypeType.setEnumValue((value) -> (value instanceof UnknownEnumHandle)
				? ((UnknownEnumHandle) value).getHandleId()
				: Arrays.asList(CAttackTypeJass.VALUES).indexOf(value));
		jassProgramVisitor.getJassNativeManager().createNative("ConvertRace",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(raceType, WarsmashConstants.RACE_MANAGER.getRace(i), i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertAllianceType",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(alliancetypeType, CAllianceType.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertRacePref",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(racepreferenceType, WarsmashConstants.RACE_MANAGER.getRacePreference(i), i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertIGameState",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(igamestateType, CGameState.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertFGameState",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(fgamestateType, CGameState.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertPlayerState",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(playerstateType, CPlayerState.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertPlayerScore",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(playerscoreType, CPlayerScore.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertPlayerGameResult",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(playergameresultType, CPlayerGameResult.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertUnitState",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(unitstateType, CUnitState.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertAIDifficulty",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(aidifficultyType, AIDifficulty.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertGameEvent",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(gameeventType, JassGameEventsWar3.VALUES, i);
				});
		// The 1.29 and 1.30 common.j number the mouse events 269-271, so the Frozen Throne unit events after them sit
		// three higher than in 1.31+ (where the mouse events are 305-307). common.j declares the mouse events before
		// those unit events, which tells the two numberings apart.
		final boolean[] mouseEventsBeforeUnitEvents = { false };
		jassProgramVisitor.getJassNativeManager().createNative("ConvertPlayerEvent",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					if ((i >= 269) && (i <= 271)) {
						mouseEventsBeforeUnitEvents[0] = true;
						return new HandleJassValue(playereventType,
								JassGameEventsWar3.VALUES[JassGameEventsWar3.EVENT_PLAYER_MOUSE_DOWN.getEventId() + (i - 269)]);
					}
					return enumValue(playereventType, JassGameEventsWar3.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertPlayerUnitEvent",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(playeruniteventType, JassGameEventsWar3.VALUES,
							unitEventId(mouseEventsBeforeUnitEvents[0], i));
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertWidgetEvent",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(widgeteventType, JassGameEventsWar3.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertDialogEvent",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(dialogeventType, JassGameEventsWar3.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertUnitEvent",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(uniteventType, JassGameEventsWar3.VALUES,
							unitEventId(mouseEventsBeforeUnitEvents[0], i));
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertLimitOp",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(limitopType, CLimitOp.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertUnitType",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(unittypeType, CUnitTypeJass.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertGameSpeed",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(gamespeedType, CGameSpeed.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertPlacement",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(placementType, CMapPlacement.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertStartLocPrio",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(startlocprioType, CStartLocPrio.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertGameDifficulty",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(gamedifficultyType, CMapDifficulty.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertGameType",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(gametypeType, CGameType.getById(i), i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertMapFlag",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(mapflagType, CMapFlag.getById(i), i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertMapVisibility",
				(arguments, globalScope, triggerScope) -> {
					return mapvisibilityType
							.getUnknownEnumValue(arguments.get(0).visit(IntegerJassValueVisitor.getInstance()));
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertMapSetting",
				(arguments, globalScope, triggerScope) -> {
					return mapsettingType.getUnknownEnumValue(arguments.get(0).visit(IntegerJassValueVisitor.getInstance()));
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertMapDensity",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(mapdensityType, CMapDensity.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertMapControl",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(mapcontrolType, CMapControl.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertPlayerColor",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(playercolorType, CPlayerColor.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertPlayerSlotState",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(playerslotstateType, CPlayerSlotState.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertVolumeGroup",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(volumegroupType, CSoundVolumeGroup.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertCameraField",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(camerafieldType, CCameraField.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertBlendMode",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(blendmodeType, CBlendMode.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertRarityControl",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(raritycontrolType, CRarityControl.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertTexMapFlags",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(texmapflagsType, CTexMapFlags.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertFogState",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(fogstateType, CFogState.getById(i), i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertEffectType",
				(arguments, globalScope, triggerType) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(effecttypeType, CEffectType.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertVersion",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(versionType, CVersion.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertItemType",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(itemtypeType, CItemTypeJass.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertAttackType",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(attacktypeType, CAttackTypeJass.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertDamageType",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(damagetypeType, CDamageType.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertWeaponType",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(weapontypeType, CWeaponSoundTypeJass.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertSoundType",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(soundtypeType, CSoundType.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertPathingType",
				(arguments, globalScope, triggerScope) -> {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return enumValue(pathingtypeType, CPathingTypeJass.VALUES, i);
				});
		jassProgramVisitor.getJassNativeManager().createNative("OrderId", (arguments, globalScope, triggerScope) -> {
			final String idString = arguments.get(0).visit(StringJassValueVisitor.getInstance());
			final int orderId = OrderIdUtils.getOrderId(idString);
			return IntegerJassValue.of(orderId);
		});
		jassProgramVisitor.getJassNativeManager().createNative("OrderId2String",
				(arguments, globalScope, triggerScope) -> {
					final Integer id = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return new StringJassValue(OrderIdUtils.getStringFromOrderId(id));
				});
	}

	public static void registerConversionAndStringNatives(final JassProgram jassProgramVisitor, final GameUI gameUI) {
		jassProgramVisitor.getJassNativeManager().createNative("Deg2Rad", (arguments, globalScope, triggerScope) -> {
			final Double value = arguments.get(0).visit(RealJassValueVisitor.getInstance());
			return RealJassValue.of(StrictMath.toRadians(value));
		});
		jassProgramVisitor.getJassNativeManager().createNative("Rad2Deg", (arguments, globalScope, triggerScope) -> {
			final Double value = arguments.get(0).visit(RealJassValueVisitor.getInstance());
			return RealJassValue.of(StrictMath.toDegrees(value));
		});
		jassProgramVisitor.getJassNativeManager().createNative("Sin", (arguments, globalScope, triggerScope) -> {
			final Double value = arguments.get(0).visit(RealJassValueVisitor.getInstance());
			return RealJassValue.of(StrictMath.sin(value));
		});
		jassProgramVisitor.getJassNativeManager().createNative("Cos", (arguments, globalScope, triggerScope) -> {
			final Double value = arguments.get(0).visit(RealJassValueVisitor.getInstance());
			return RealJassValue.of(StrictMath.cos(value));
		});
		jassProgramVisitor.getJassNativeManager().createNative("Tan", (arguments, globalScope, triggerScope) -> {
			final Double value = arguments.get(0).visit(RealJassValueVisitor.getInstance());
			return RealJassValue.of(StrictMath.tan(value));
		});
		jassProgramVisitor.getJassNativeManager().createNative("Asin", (arguments, globalScope, triggerScope) -> {
			final Double value = arguments.get(0).visit(RealJassValueVisitor.getInstance());
			final double result = StrictMath.asin(value);
			if (Double.isNaN(result)) {
				return RealJassValue.of(0);
			}
			return RealJassValue.of(result);
		});
		jassProgramVisitor.getJassNativeManager().createNative("Acos", (arguments, globalScope, triggerScope) -> {
			final Double value = arguments.get(0).visit(RealJassValueVisitor.getInstance());
			final double result = StrictMath.acos(value);
			if (Double.isNaN(result)) {
				return RealJassValue.of(0);
			}
			return RealJassValue.of(result);
		});
		jassProgramVisitor.getJassNativeManager().createNative("Atan", (arguments, globalScope, triggerScope) -> {
			final Double value = arguments.get(0).visit(RealJassValueVisitor.getInstance());
			final double result = StrictMath.atan(value);
			if (Double.isNaN(result)) {
				return RealJassValue.of(0);
			}
			return RealJassValue.of(result);
		});
		jassProgramVisitor.getJassNativeManager().createNative("Atan2", (arguments, globalScope, triggerScope) -> {
			final Double y = arguments.get(0).visit(RealJassValueVisitor.getInstance());
			final Double x = arguments.get(1).visit(RealJassValueVisitor.getInstance());
			final double result = StrictMath.atan2(y, x);
			if (Double.isNaN(result)) {
				return RealJassValue.of(0);
			}
			return RealJassValue.of(result);
		});
		jassProgramVisitor.getJassNativeManager().createNative("SquareRoot", (arguments, globalScope, triggerScope) -> {
			final Double value = arguments.get(0).visit(RealJassValueVisitor.getInstance());
			final double result = StrictMath.sqrt(value);
			return RealJassValue.of(result);
		});
		jassProgramVisitor.getJassNativeManager().createNative("Pow", (arguments, globalScope, triggerScope) -> {
			final Double y = arguments.get(0).visit(RealJassValueVisitor.getInstance());
			final Double x = arguments.get(1).visit(RealJassValueVisitor.getInstance());
			final double result = StrictMath.pow(y, x);
			if (Double.isNaN(result)) {
				return RealJassValue.of(0);
			}
			return RealJassValue.of(result);
		});
		jassProgramVisitor.getJassNativeManager().createNative("I2R", (arguments, globalScope, triggerScope) -> {
			final Integer i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
			return RealJassValue.of(i.doubleValue());
		});
		jassProgramVisitor.getJassNativeManager().createNative("R2I", (arguments, globalScope, triggerScope) -> {
			final Double r = arguments.get(0).visit(RealJassValueVisitor.getInstance());
			return IntegerJassValue.of(r.intValue());
		});
		jassProgramVisitor.getJassNativeManager().createNative("I2S", (arguments, globalScope, triggerScope) -> {
			final Integer i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
			return new StringJassValue(i.toString());
		});
		jassProgramVisitor.getJassNativeManager().createNative("R2S", (arguments, globalScope, triggerScope) -> {
			final Double r = arguments.get(0).visit(RealJassValueVisitor.getInstance());
			return new StringJassValue(r.toString());
		});
		jassProgramVisitor.getJassNativeManager().createNative("R2SW", (arguments, globalScope, triggerScope) -> {
			final Double r = arguments.get(0).visit(RealJassValueVisitor.getInstance());
			final int width = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
			final int precision = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
			final String format = "%" + (width > 0 ? Integer.toString(width) : "") + "." + Math.max(0, precision) + "f";
			return new StringJassValue(String.format(Locale.US, format, r));
		});
		jassProgramVisitor.getJassNativeManager().createNative("S2I", (arguments, globalScope, triggerScope) -> {
			final String s = arguments.get(0).visit(StringJassValueVisitor.getInstance());
			try {
				final int intValue = Integer.parseInt(s);
				return IntegerJassValue.of(intValue);
			}
			catch (final Exception exc) {
				return IntegerJassValue.of(0);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("S2R", (arguments, globalScope, triggerScope) -> {
			final String s = arguments.get(0).visit(StringJassValueVisitor.getInstance());
			try {
				final double parsedValue = Double.parseDouble(s);
				return RealJassValue.of(parsedValue);
			}
			catch (final Exception exc) {
				return RealJassValue.of(0);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SubString", (arguments, globalScope, triggerScope) -> {
			final String s = arguments.get(0).visit(StringJassValueVisitor.getInstance());
			int start = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
			int end = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
			if (start > s.length()) {
				start = s.length();
			}
			if (start < 0) {
				start = 0;
			}
			if (end > s.length()) {
				end = s.length();
			}
			if (end < start) {
				end = start;
			}
			return new StringJassValue(s.substring(start, end));
		});
		jassProgramVisitor.getJassNativeManager().createNative("StringLength",
				(arguments, globalScope, triggerScope) -> {
					final String s = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					return IntegerJassValue.of(s == null ? 0 : s.length());
				});
		jassProgramVisitor.getJassNativeManager().createNative("StringCase", (arguments, globalScope, triggerScope) -> {
			final String s = arguments.get(0).visit(StringJassValueVisitor.getInstance());
			final boolean upper = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
			return new StringJassValue(upper ? s.toUpperCase(Locale.US) : s.toLowerCase(Locale.US));
		});
		jassProgramVisitor.getJassNativeManager().createNative("StringHash", (arguments, globalScope, triggerScope) -> {
			final String s = arguments.get(0).visit(StringJassValueVisitor.getInstance());
			return IntegerJassValue.of(s.hashCode());
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetLocalizedString",
				(arguments, globalScope, triggerScope) -> {
					final String key = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					if (key == null) {
						return new StringJassValue("");
					}
					// TODO this might be wrong, or a subset of the needed return values
					final String decoratedString = gameUI.getTemplates().getDecoratedString(key);
					if (key.equals(decoratedString)) {
						System.err.println("GetLocalizedString: NOT FOUND: " + key);
					}
					return new StringJassValue(decoratedString);
				});
		jassProgramVisitor.getJassNativeManager().createNative("GetLocalizedHotkey",
				(arguments, globalScope, triggerScope) -> {
					final String key = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					return IntegerJassValue.of(key == null ? 0 : gameUI.getLocalizedHotkey(key));
				});
	}
}
