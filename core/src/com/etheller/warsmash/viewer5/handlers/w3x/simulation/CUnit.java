package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.IntIntMap;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.scope.trigger.RemovableTriggerEvent;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.scope.variableevent.CLimitOp;
import com.etheller.warsmash.parsers.jass.scope.CommonTriggerExecutionScope;
import com.etheller.warsmash.util.ArgbImage;
import com.etheller.warsmash.util.Log;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilitySpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.ability.AbilityBuilderAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens.PrimaryTag;
import com.etheller.warsmash.viewer5.handlers.w3x.SequenceUtils;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid.MovementType;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid.PathingFlags;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid.RemovablePathingMapInstance;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.OrientationInterpolation;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.save.SavedComponent;
import com.etheller.warsmash.viewer5.handlers.w3x.save.SavedDefault;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitStateListener.CUnitStateNotifier;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilitySpell;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.GenericSingleIconActiveAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityAttack;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityDisableType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityMove;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.GetAbilityByRawcodeVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.AutocastType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.CAutocastAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.creeps.CCreepCastAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.build.CAbilityBuildInProgress;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold.CAbilityCargoHold;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.AbilityGenericSingleIconPassiveAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.CBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.CLevelingAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.harvest.CAbilityHarvest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.hero.CAbilityHero;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.inventory.CAbilityInventory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.shop.CAbilityNeutralBuilding;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.listeners.CUnitAbilityEffectReactionListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.mine.CAbilityGoldMinable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.mine.CAbilityOverlayedMine;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.nightelf.root.CAbilityRoot;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.queue.CAbilityReviveHero;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.units.CBuffDisease;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.CAbilityType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.CBehaviorAbilityBuilderNoTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff.ABGenericTimedBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff.CPausedTickingBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.BehaviorAbilityVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.BehaviorTargetUnitVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorAttack;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorAttackListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorAttackMove;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorBoardTransport;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorFollow;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorHoldPosition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorMove;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorPatrol;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorStop;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorStun;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CRangedBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.build.AbilityDisableWhileUpgradingVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.harvest.CBehaviorReturnResources;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.skills.CBehaviorNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.skills.CBehaviorTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CDamageFlags;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CDefenseType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CRegenType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CWeaponType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttack;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackDamageTakenListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackDamageTakenModificationListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackDamageTakenModificationListenerDamageModResult;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackEvasionListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackFinalDamageTakenModificationListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackPostDamageListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackPreDamageListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackPreDamageListenerPriority;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitDeathReplacementEffect;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitDeathReplacementEffectPriority;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitDeathReplacementResult;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitDeathReplacementStacking;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitDefaultAccuracyCheckListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitDefaultEtherealDamageModListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitDefaultLifestealListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitDefaultMagicImmuneDamageModListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitDefaultSleepListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitDefaultThornsListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.replacement.CUnitAttackModifier;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.replacement.CUnitAttackSettings;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.replacement.CUnitPriorityLoopData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAttackProjectile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CProjectile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.listeners.CUnitAbilityProjReactionListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.listeners.CUnitAttackProjReactionListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitRace;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrder;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrderNoTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrderTargetPoint;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrderTargetWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CMapControl;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision.CPlayerFogOfWar;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision.CUnitAttackVisionFogModifier;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision.CUnitDeathVisionFogModifier;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.region.CRegion;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.region.CRegionEnumFunction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.region.CRegionManager;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.state.CUnitState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.JassGameEventsWar3;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CFogState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.CUnitAppliedUpgrade;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.CUnitBehaviorChangeListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.CUnitStateEvent;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.CUnitTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.CWidgetEvent;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingFx;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffs;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.upgrade.CUpgradeEffect;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.ResourceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponent;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentModel;

public class CUnit extends CWidget {
	private static final int CHEESY_UNIQUE_NUMBER = 1337;
	private static final double DRAWN_FACING_TICK = 0.03;
	private static final double DRAWN_FACING_EASE = 0.7;
	private static final float DRAWN_FACING_REST = 0.8f;

	private static RegionCheckerImpl regionCheckerImpl = new RegionCheckerImpl();

	private static final StateModBuff pauseBuff = new StateModBuff(StateModBuffType.PAUSED, 1);

	private War3ID typeId;
	private float facing; // degrees
	/**
	 * What GetUnitFacing reads in 1.29: the drawn facing, which follows the heading on a clock of its own, a turn every
	 * 0.03 s that speeds up to the model's top turn speed (orientInterp), so after a quick turn of the heading it lags
	 * behind it for a while. NaN in a save from before it was kept: the heading then.
	 */
	@SavedDefault("NaN")
	private float drawnFacing = Float.NaN;
	/** Radians per drawn facing tick, signed. */
	private float drawnTurnVelocity;
	private int drawnFacingTick;
	private float mana;
	/**
	 * What the last unit update regenerated and the values right after it: 1.29 regenerates continuously, so a script
	 * reading inside the step sees only the part up to its time.
	 */
	private int regenTick = -1;
	private float lifeRegenGiven;
	private float lifeAfterRegen;
	private float manaRegenGiven;
	private float manaAfterRegen;
	private int baseMaximumLife;
	private float handicap = 1f;
	/** Vertex color {r, g, b, a} the game gave the unit (abilities, scripts), or null for its type's tint. */
	private float[] vertexColor;
	private int maximumLife;
	private float lifeRegen;
	private float lifeRegenStrengthBonus;
	private float lifeRegenBonus;
	private float manaRegen;
	private float manaRegenIntelligenceBonus;
	private float manaRegenBonus;
	private int baseMaximumMana;
	private int maximumMana;
	private int maxSpeed;
	private int minSpeed;
	private int baseSpeed;
	private int finalSpeed;
	private float agilityDefensePermanentBonus;
	private float agilityDefenseTemporaryBonus;
	private int permanentDefenseBonus;
	private float temporaryDefenseBonus;
	private float scriptDefenseBonus;
	private float totalTemporaryDefenseBonus;

	private int speedBonus;

	private CUnitDefaultThornsListener flatThornsListener = null;
	private CUnitDefaultThornsListener percentThornsListener = null;
	private CUnitDefaultLifestealListener lifestealListener = null;

	private final List<StateModBuff> stateModBuffs = new ArrayList<>();
	@SavedComponent
	private final NonStackingStatBuffs nonStackingStatBuffs = new NonStackingStatBuffs();
	private final Map<String, List<NonStackingFx>> nonStackingFx = new HashMap<>();
	private final Map<String, List<CBuff>> nonStackingDisplayBuffs = new HashMap<>();

	private int currentDefenseDisplay;
	private float baseDefense;
	private float currentDefense;
	private float baseLifeRegenPerTick;
	private float currentLifeRegenPerTick;
	private float currentManaRegenPerTick;
	private CDefenseType defenseType;

	private int cooldownEndTime = 0;
	private float flyHeight;
	private float turnRate;
	private float propWindow;
	private int lastWalkTick = Integer.MIN_VALUE / 2;
	private int playerIndex;
	private int fakePlayerIndex;

	private final List<CAbility> abilities = new ArrayList<>();
	private final List<CAbility> disabledAbilities = new ArrayList<>();
	private final Set<CUnitAppliedUpgrade> appliedUpgrades = new LinkedHashSet<>();
	private final List<Trigger> onTickTriggers = new ArrayList<>();

	private CBehavior currentBehavior;
	private final Queue<COrder> orderQueue = new LinkedList<>();
	private CUnitType unitType;

	private Rectangle collisionRectangle;
	private RemovablePathingMapInstance pathingInstance;

	private final EnumSet<CUnitClassification> classifications = EnumSet.noneOf(CUnitClassification.class);
	private final EnumSet<CTargetType> targetedAs = EnumSet.noneOf(CTargetType.class);

	private int deathTurnTick;
	private boolean raisable;
	private boolean decays;
	private boolean corpse;
	private boolean boneCorpse;
	private boolean freshCorpse;
	private boolean falseDeath;

	private transient CUnitAnimationListener unitAnimationListener;

	// if you use triggers for this then the transient tag here becomes really
	// questionable -- it already was -- but I meant for those to inform us
	// which fields shouldn't be persisted if we do game state save later
	private transient CUnitStateNotifier stateNotifier = new CUnitStateNotifier();
	private transient List<StateListenerUpdate> stateListenersUpdates = new ArrayList<>();
	private float acquisitionRange;
	private boolean illusion;
	private float illusionDamageDealtFactor = 1;
	private float illusionDamageTakenFactor = 1;
	private final Map<Integer, Integer> illusionAbilityLevels = new HashMap<>();
	private transient static AutoAttackTargetFinderEnum autoAttackTargetFinderEnum = new AutoAttackTargetFinderEnum();
	private transient static AutocastTargetFinderEnum autocastTargetFinderEnum = new AutocastTargetFinderEnum();
	private transient CAutocastAbility autocastAbility = null;
	private EnumSet<CTargetType> priorityAttackTargets = null;

	private transient CBehaviorMove moveBehavior;
	private transient CBehaviorAttack attackBehavior;
	private transient CBehaviorAttackMove attackMoveBehavior;
	private transient CBehaviorFollow followBehavior;
	private transient CBehaviorPatrol patrolBehavior;
	private transient CBehaviorStop stopBehavior;
	private transient CBehaviorHoldPosition holdPositionBehavior;
	private transient CBehaviorBoardTransport boardTransportBehavior;
	private boolean constructing = false;
	private boolean constructingPaused = false;
	private boolean structure;
	private boolean uprooted;
	private War3ID upgradeIdType = null;
	private final List<War3ID> upgradedFromTypeIds = new ArrayList<>();
	private float constructionProgress;
	private boolean hidden = false;
	private boolean removedFromGame;
	private boolean paused = false;
	private boolean acceptingOrders = true;
	private static final War3ID INVULNERABLE_ABILITY_ID = War3ID.fromString("Avul");
	/** A worker inside a gold mine or the Orc building it builds. */
	public static final StateModBuff INSIDE_INVULNERABLE = new StateModBuff(StateModBuffType.INVULNERABLE, 1);
	/** A unit raised by Animate Dead, for its whole life. */
	public static final StateModBuff ANIMATED_INVULNERABLE = new StateModBuff(StateModBuffType.INVULNERABLE, 1);
	private boolean invulnerable = false;
	private boolean scriptInvulnerable = false;
	private boolean buffInvulnerable = false;
	private boolean magicImmune = false;
	private boolean limitedMagicImmune = false;
	private boolean morphImmune = false;
	private boolean resistant = false;
	private boolean autoAttack = true;
	private boolean autoCast = true;
	private static final int AUTOCAST_COMBAT_SCAN_TICKS = CTimer.wholeSteps(0.5f);
	// Measured (1.29): autocast looks this far for targets whatever the acquisition range (Web, Heal).
	private static final float AUTOCAST_SEARCH_RANGE = 600f;
	// Measured (1.29): an idle caster looks for an autocast target when it is made, then every 3 s, and again as
	// soon as the spell's cooldown ends (Spell Steal, Web, Heal).
	private static final int AUTOCAST_IDLE_RESCAN_TICKS = CTimer.wholeSteps(3.0f);
	private int nextIdleAutocastScanTick;
	// Measured (1.29): an idle unit looks for something to attack every half second, not every step. A new unit
	// first looks up to half a second after it is made, at its own phase; an order, or the end of one, makes it look
	// at once. Changing the acquisition range does not.
	private static final int IDLE_ACQUIRE_SCAN_TICKS = CTimer.wholeSteps(0.5f);
	private int nextIdleAcquireScanTick;
	// Measured (1.29): a creep thinks of its spells when it starts a fight, then once a second while it lasts
	// (Rain of Fire at once, Animate Dead and Devour about a second later once the attackers reach it).
	private static final int CREEP_CAST_INTERVAL_TICKS = CTimer.wholeSteps(1.0f);
	private int nextCreepCastTick;
	private boolean creepCastFighting;
	private int creepCastThinks;
	private boolean autoTargetable = true;
	private boolean assistAlly = true;
	private boolean moveDisabled = false;
	private boolean noUnitCollision = false;
	private boolean noBuildingCollision = false;
	private final boolean selectable = false;
	private CBehavior defaultBehavior;
	private static final Rectangle callForHelpRect = new Rectangle();
	private int nextCallForHelpTick;
	private CUnit pendingAttackBackSource;
	private CUnit pendingSpellAnswerSource;
	private CUnit spellMissileSource;
	private boolean pendingAttackBackWasHit;
	private boolean pendingAttackBackHelp;
	private int fleeMoveResetCount = -1;
	private static final float CREEP_ALERT_RADIUS = 800f;
	private CreepCamp creepCamp;
	private boolean guarding;
	private float guardX;
	private float guardY;
	private int lastCombatTick;
	private int lastHurtTick = Integer.MIN_VALUE / 2;
	private static final float GUARD_LOST_STOP = 48f;
	private static final float GUARD_GIVE_UP_EXTRA_SECONDS = 1.5f;
	private static final int GUARD_RETURN_NONE = 0;
	private static final int GUARD_RETURN_LOST = 1;
	private static final int GUARD_RETURN_GIVE_UP = 2;
	private int guardReturn = GUARD_RETURN_NONE;
	private boolean guardEngaged;
	private int guardAwayTick = -1;
	private static final War3ID CREEP_SLEEP = War3ID.fromString("Asla");
	private StateModBuff naturalSleep;
	private CAbility finishedSpell;
	private SimulationRenderComponentModel naturalSleepEffect;
	private static final float SLEEP_AFTER_WAKE_SECONDS = 2.0f;
	private int lastWakeTick = Integer.MIN_VALUE / 2;
	private Boolean canSleepOverride;
	private String nameOverride;
	private CBehavior interruptedDefaultBehavior;
	private CBehavior interruptedBehavior;
	private COrder lastStartedOrder = null;
	private static final float DEFAULT_RESCUE_RANGE = 414.5f;
	private static final int RESCUE_REPEAT_TICKS = CTimer.wholeSteps(1);
	private float rescueRange = DEFAULT_RESCUE_RANGE;
	private int rescuableByPlayers;
	private int unrescuableByPlayers;
	private int lastRescueTick = Integer.MIN_VALUE / 2;
	private COrder scriptAnimationOrder = null;
	private CUnit worker;
	private boolean workerInside;
	private int powerWorkers;
	private final War3ID[] buildQueue = new War3ID[WarsmashConstants.BUILD_QUEUE_SIZE];
	private final QueueItemType[] buildQueueTypes = new QueueItemType[WarsmashConstants.BUILD_QUEUE_SIZE];
	private final boolean[] buildQueueHeroToken = new boolean[WarsmashConstants.BUILD_QUEUE_SIZE];
	private boolean queuedUnitFoodPaid = false;
	private boolean animatedCorpse = false;
	private AbilityTarget rallyPoint;

	private int foodMade;
	private int foodUsed;

	private int triggerEditorCustomValue;

	private List<CUnitAttack> unitSpecificAttacks;
	private List<CUnitAttack> unitSpecificCurrentAttacks;
	private boolean disableAttacks;
	private final CUnitAttackVisionFogModifier attackFogMod;

	private final Map<CUnitAttackPreDamageListenerPriority, List<CUnitAttackPreDamageListener>> preDamageListeners = new HashMap<>();
	private final List<CUnitAttackPostDamageListener> postDamageListeners = new ArrayList<>();
	private final List<CUnitAttackDamageTakenModificationListener> damageTakenModificationListeners = new ArrayList<>();
	private final List<CUnitAttackFinalDamageTakenModificationListener> finalDamageTakenModificationListeners = new ArrayList<>();
	private final List<CUnitAttackDamageTakenListener> damageTakenListeners = new ArrayList<>();
	private float healingReceivedFactor = 1f;
	private final Map<CUnitDeathReplacementEffectPriority, List<CUnitDeathReplacementEffect>> deathReplacementEffects = new HashMap<>();
	private final List<CUnitAttackEvasionListener> evasionListeners = new ArrayList<>();
	private final List<CUnitAttackProjReactionListener> attackProjReactionListeners = new ArrayList<>();
	private final List<CUnitAbilityProjReactionListener> abilityProjReactionListeners = new ArrayList<>();
	private final List<CUnitAbilityEffectReactionListener> abilityEffectReactionListeners = new ArrayList<>();
	private final List<CUnitAttackModifier> attackModifiers = new ArrayList<>();

	private final List<CUnitBehaviorChangeListener> behaviorChangeListeners = new ArrayList<>();

	private transient Set<CRegion> containingRegions = new LinkedHashSet<>();
	private transient Set<CRegion> priorContainingRegions = new LinkedHashSet<>();

	private boolean constructionConsumesWorker;
	private boolean explodesOnDeath;
	private War3ID explodesOnDeathBuffId;
	private final IntIntMap rawcodeToCooldownExpireTime = new IntIntMap();
	private final IntIntMap rawcodeToCooldownStartTime = new IntIntMap();

	private byte invisLevels = 0;
	private CTimer fadeTimer;
	private final List<Integer> detectorLevels = new ArrayList<>(1);
	private long detections = 0;
	// Players whose fog has shown this unit at least once: 1.29 reports such a unit fogged, not masked, even under
	// black mask set later.
	private long seenByPlayers = 0;
	private int ultravisionSources = 0;

	private boolean hideMinimapIcon;
	private boolean usesAltMinimapIcon;

	private long unselectablePlayers = 0;
	private long untargetablePlayers = 0;

	private boolean unenumerable = false;

	private MovementType movementOverride = null;
	private boolean pathingDisabled;
	private MovementType nextMovementOverride = null;

	private boolean repairBlocked;
	private boolean trainingBlocked;
	private boolean constructionBlocked;

	private List<String> uniqueFlags = null;


	public CUnit(final int handleId, final int playerIndex, final float x, final float y, final float life,
			final War3ID typeId, final float facing, final float mana, final int maximumLife, final float lifeRegen,
			final int maximumMana, final int speed, final int maxSpeed, final int minSpeed, final CUnitType unitType) {
		super(handleId, x, y, life);
		this.playerIndex = playerIndex;
		this.fakePlayerIndex = playerIndex;
		this.typeId = typeId;
		this.facing = facing;
		this.drawnFacing = this.facing;
		this.drawnFacingTick = Integer.MIN_VALUE;
		this.mana = mana;
		this.baseMaximumLife = maximumLife;
		this.maximumLife = maximumLife;
		this.lifeRegen = lifeRegen;
		this.manaRegen = unitType.getManaRegen();
		this.baseMaximumMana = maximumMana;
		this.maximumMana = maximumMana;
		this.maxSpeed = maxSpeed;
		this.minSpeed = minSpeed;
		this.baseSpeed = speed;
		this.flyHeight = unitType.getDefaultFlyingHeight();
		this.turnRate = unitType.getTurnRate();
		this.propWindow = unitType.getPropWindow();
		this.unitType = unitType;
		this.defenseType = unitType.getDefenseType();
		this.classifications.addAll(unitType.getClassifications());
		this.targetedAs.addAll(unitType.getTargetedAs());
		this.acquisitionRange = unitType.getDefaultAcquisitionRange();
		this.structure = unitType.isBuilding();
		this.stopBehavior = new CBehaviorStop(this);
		this.defaultBehavior = this.stopBehavior;
		this.raisable = unitType.isRaise();
		this.decays = unitType.isDecay();
		initializeListenerLists();
		addPreDamageListener(CUnitAttackPreDamageListenerPriority.ACCURACY, new CUnitDefaultAccuracyCheckListener());
		this.attackFogMod = new CUnitAttackVisionFogModifier(this, playerIndex);
		computeAllDerivedFields();
	}

	public void beginBehavior(final CSimulation game, final CBehavior behavior) {
		this.beginBehavior(game, behavior, false);
	}

	public void beginBehavior(final CSimulation game, final CBehavior behavior, boolean interrupted) {
		if (this.currentBehavior != behavior) {
			final int lastBehaviorHighlightOrderId = this.currentBehavior != null
					? this.currentBehavior.getHighlightOrderId()
					: -1;
			this.nextIdleAcquireScanTick = game.getGameTurnTick()
					+ ((this.currentBehavior == null) ? firstIdleAcquireScanDelay() : 0);
			if (this.currentBehavior != null) {
				this.currentBehavior.end(game, interrupted);
			}

			fireBehaviorChangeEvent(game, behavior, false);

			this.currentBehavior = behavior;

			if (this.currentBehavior != null) {
				this.currentBehavior.begin(game);

				if (this.currentBehavior.getHighlightOrderId() != lastBehaviorHighlightOrderId) {
					this.stateNotifier.ordersChanged();
				}
			}
		}
	}

	public CBehavior finishMoveBehavior(final CSimulation game, final CBehavior behavior) {
		fireBehaviorChangeEvent(game, behavior, false);

		return behavior.update(game);
	}

	public void fireBehaviorChangeEvent(final CSimulation game, final CBehavior behavior, final boolean ongoing) {
		for (final CUnitBehaviorChangeListener listener : new ArrayList<>(this.behaviorChangeListeners)) {
			listener.onChange(game, this, this.currentBehavior, behavior, ongoing);
		}
	}

	public void performDefaultBehavior(final CSimulation game) {
		beginBehavior(game, this.defaultBehavior, true);
	}

	public void regeneratePathingInstance(final CSimulation game, final ArgbImage buildingPathingPixelMap) {
		float unitX = getX();
		float unitY = getY();
		unitX = (float) Math.floor(unitX / 64f) * 64f;
		unitY = (float) Math.floor(unitY / 64f) * 64f;
		if (((buildingPathingPixelMap.getWidth() / 2) % 2) == 1) {
			unitX += 32f;
		}
		if (((buildingPathingPixelMap.getHeight() / 2) % 2) == 1) {
			unitY += 32f;
		}
		this.pathingInstance = game.getPathingGrid().blitRemovablePathingOverlayTexture(unitX, unitY,
				270 /* no rotation, face forward */, buildingPathingPixelMap);
		setX(unitX);
		setY(unitY);
	}

	public float getLifeRegenBonus() {
		return this.lifeRegenBonus;
	}

	public void setLifeRegenStrengthBonus(final float lifeRegenStrengthBonus) {
		this.lifeRegenStrengthBonus = lifeRegenStrengthBonus;
		computeDerivedFields(NonStackingStatBuffType.HPGEN);
	}

	public void startInvisibilityTimer(final float durationSeconds, final StateModBuff stateModBuff) {
		this.fadeTimer = new CTimerUnitFade(stateModBuff);
		this.fadeTimer.setRepeats(false);
	}

	public void setFadeTimer(final CTimer whichTimer) {
		this.fadeTimer = whichTimer;
	}

	public CTimer getFadeTimer() {
		return this.fadeTimer;
	}

	public byte getInvisLevels() {
		return this.invisLevels;
	}

	public void addNonStackingStatBuff(final CSimulation game, final NonStackingStatBuff buff) {
		this.nonStackingStatBuffs.add(buff);
		if (buff.getBuffType().isHeroStat()) {
			computeDerivedHeroFields(game, buff.getBuffType());
		} else {
			computeDerivedFields(buff.getBuffType());
		}
	}

	public void removeNonStackingStatBuff(final CSimulation game, final NonStackingStatBuff buff) {
		this.nonStackingStatBuffs.remove(buff, getTypeId().asStringValue());
		if (buff.getBuffType().isHeroStat()) {
			computeDerivedHeroFields(game, buff.getBuffType());
		} else {
			computeDerivedFields(buff.getBuffType());
		}
	}

	public void addStateModBuff(final StateModBuff listener) {
		if (!this.stateModBuffs.contains(listener)) {
			this.stateModBuffs.add(0, listener);
		}
	}

	public void removeStateModBuff(final StateModBuff listener) {
		this.stateModBuffs.remove(listener);
	}

	public void removeAllStateModBuffs(final StateModBuffType type) {
		for (int i = this.stateModBuffs.size() - 1; i >= 0; i--) {
			if (this.stateModBuffs.get(i).getBuffType() == type) {
				this.stateModBuffs.remove(i);
			}
		}
	}

	public void computeUnitState(final CSimulation game, final StateModBuffType type) {
		switch (type) {
		case ENABLE_ATTACK:
		case DISABLE_ATTACK_INDEX:
		case DISABLE_ALL:
		case DISABLE_MOVE:
		case DISABLE_ATTACK:
		case DISABLE_MELEE_ATTACK:
		case DISABLE_RANGED_ATTACK:
		case DISABLE_SPECIAL_ATTACK:
		case DISABLE_SPELLS:
		case ETHEREAL:
		case POLYMORPHED:
			byte extraEnabledAttacks = 0;
			byte disabledAttacks = 0;
			boolean isDisableAll = false;
			boolean isDisableMove = false;
			boolean isDisableAttack = false;
			boolean isDisableMeleeAttack = false;
			boolean isDisableRangedAttack = false;
			boolean isDisableSpecialAttack = false;
			boolean isDisableSpells = false;
			boolean isEthereal = false;
			boolean isPolymorphed = false;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.ENABLE_ATTACK) {
					if (buff.getValue() != 0) {
						extraEnabledAttacks |= buff.getValue();
					}
				}
				if (buff.getBuffType() == StateModBuffType.DISABLE_ATTACK_INDEX) {
					disabledAttacks |= buff.getValue();
				}
				if (buff.getBuffType() == StateModBuffType.DISABLE_ALL) {
					if (buff.getValue() != 0) {
						isDisableAll = true;
					}
				}
				if (buff.getBuffType() == StateModBuffType.DISABLE_MOVE) {
					if (buff.getValue() != 0) {
						isDisableMove = true;
					}
				}
				if (buff.getBuffType() == StateModBuffType.DISABLE_ATTACK) {
					if (buff.getValue() != 0) {
						isDisableAttack = true;
					}
				}
				if (buff.getBuffType() == StateModBuffType.DISABLE_MELEE_ATTACK) {
					if (buff.getValue() != 0) {
						isDisableMeleeAttack = true;
					}
				}
				if (buff.getBuffType() == StateModBuffType.DISABLE_RANGED_ATTACK) {
					if (buff.getValue() != 0) {
						isDisableRangedAttack = true;
					}
				}
				if (buff.getBuffType() == StateModBuffType.DISABLE_SPECIAL_ATTACK) {
					if (buff.getValue() != 0) {
						isDisableSpecialAttack = true;
					}
				}
				if (buff.getBuffType() == StateModBuffType.DISABLE_SPELLS) {
					if (buff.getValue() != 0) {
						isDisableSpells = true;
					}
				}
				if (buff.getBuffType() == StateModBuffType.ETHEREAL) {
					if (buff.getValue() != 0) {
						isEthereal = true;
					}
				}
				if (buff.getBuffType() == StateModBuffType.POLYMORPHED) {
					if (buff.getValue() != 0) {
						isPolymorphed = true;
					}
				}
			}
			if (isDisableAll) {
				isDisableMove = true;
				isDisableAttack = true;
				isDisableSpells = true;
			}
//			CAbility attack = this.getFirstAbilityOfType(CAbilityAttack.class);
//			if (attack != null) {
//				attack.setDisabled(isDisableAttack, CAbilityDisableType.ATTACKDISABLED);
//			}
			for (final CAbility ability : this.abilities) {
				if (((isDisableAttack || isEthereal) && (ability.getAbilityCategory() == CAbilityCategory.ATTACK))
						|| (isDisableMove && (ability.getAbilityCategory() == CAbilityCategory.MOVEMENT))
						|| (isDisableSpells && (ability.getAbilityCategory() == CAbilityCategory.SPELL)
								&& ability.isMagic())
						|| (isEthereal && ability.isPhysical()
								&& ((ability.getAbilityCategory() == CAbilityCategory.SPELL)
										|| (ability.getAbilityCategory() == CAbilityCategory.CORE)))
						|| (isPolymorphed && ((ability.getAbilityCategory() == CAbilityCategory.SPELL)
								|| (ability.getAbilityCategory() == CAbilityCategory.PASSIVE
										|| ability.getAbilityCategory() == CAbilityCategory.ATTACK
										|| (ability.getAbilityCategory() == CAbilityCategory.CORE
												&& ability.isPhysical()))))) {
					ability.setDisabled(true, CAbilityDisableType.DISABLED_EFFECT);
				} else {
					ability.setDisabled(false, CAbilityDisableType.DISABLED_EFFECT);
				}
			}
			final List<CUnitAttack> newAttackList = new ArrayList<CUnitAttack>();
			for (int i = 0; i < this.unitSpecificAttacks.size(); i++) {
				final CUnitAttack attack = this.unitSpecificAttacks.get(i);
				if ((((getUnitType().getAttacksEnabled() & (1 << i)) != 0) || ((extraEnabledAttacks & (1 << i)) != 0))
						&& ((disabledAttacks & (1 << i)) == 0) && !isDisableAttack
						&& (!isDisableMeleeAttack || !attack.getWeaponType().equals(CWeaponType.NORMAL))
						&& (!isDisableRangedAttack || attack.getWeaponType().equals(CWeaponType.NORMAL))
						&& (!isDisableSpecialAttack || !((attack.getTargetsAllowed().size() == 1)
								&& attack.getTargetsAllowed().equals(EnumSet.of(CTargetType.TREE))))) {
					newAttackList.add(attack);
				}

			}
			setUnitSpecificCurrentAttacks(newAttackList);
			notifyAttacksChanged();
			if ((this.currentBehavior instanceof CBehaviorAttack)
					&& !newAttackList.contains(((CBehaviorAttack) this.currentBehavior).getUnitAttack())) {
				performDefaultBehavior(game);
			}
			switch (type) {
			case ENABLE_ATTACK:
			case DISABLE_ATTACK_INDEX:
				checkDisabledAbilities(game, isDisableAttack);
				break;
			case DISABLE_ALL:
				checkDisabledAbilities(game, isDisableAll);
				break;
			case DISABLE_MOVE:
				checkDisabledAbilities(game, isDisableMove);
				break;
			case DISABLE_ATTACK:
				checkDisabledAbilities(game, isDisableAttack);
				break;
			case DISABLE_MELEE_ATTACK:
				checkDisabledAbilities(game, isDisableMeleeAttack);
				break;
			case DISABLE_RANGED_ATTACK:
				checkDisabledAbilities(game, isDisableRangedAttack);
				break;
			case DISABLE_SPECIAL_ATTACK:
				checkDisabledAbilities(game, isDisableSpecialAttack);
				break;
			case DISABLE_SPELLS:
				checkDisabledAbilities(game, isDisableSpells);
				break;
			case ETHEREAL:
				checkDisabledAbilities(game, isEthereal);
				break;
			case POLYMORPHED:
				checkDisabledAbilities(game, isPolymorphed);
				break;
			default:
				break;
			}

			if (isEthereal) {
				if (!this.damageTakenModificationListeners.contains(CUnitDefaultEtherealDamageModListener.INSTANCE)) {
					addDamageTakenModificationListener(CUnitDefaultEtherealDamageModListener.INSTANCE);
				}
				game.changeUnitVertexColor(this, RenderUnit.ETHEREAL);
			} else {
				if (this.damageTakenModificationListeners.contains(CUnitDefaultEtherealDamageModListener.INSTANCE)) {
					removeDamageTakenModificationListener(CUnitDefaultEtherealDamageModListener.INSTANCE);
					game.changeUnitVertexColor(this, RenderUnit.DEFAULT);
				}
			}
			break;
		case DISABLE_AUTO_ATTACK:
			boolean isDisableAutoAttack = false;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.DISABLE_AUTO_ATTACK) {
					if (buff.getValue() != 0) {
						isDisableAutoAttack = true;
					}
				}
			}
			this.autoAttack = !isDisableAutoAttack;
			break;
		case DISABLE_AUTO_CAST:
			boolean isDisableAutoCast = false;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.DISABLE_AUTO_CAST) {
					if (buff.getValue() != 0) {
						isDisableAutoCast = true;
					}
				}
			}
			this.autoCast = !isDisableAutoCast;
			break;
		case DISABLE_ASSIST_ALLY:
			boolean isDisableAssistAlly = false;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.DISABLE_ASSIST_ALLY) {
					if (buff.getValue() != 0) {
						isDisableAssistAlly = true;
					}
				}
			}
			this.assistAlly = !isDisableAssistAlly;
			break;
		case MAGIC_IMMUNE:
			boolean isMagicImmune = false;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.MAGIC_IMMUNE) {
					if (buff.getValue() != 0) {
						isMagicImmune = true;
					}
				}
			}
			setMagicImmune(isMagicImmune);
			if (isMagicImmune) {
				if (!this.finalDamageTakenModificationListeners
						.contains(CUnitDefaultMagicImmuneDamageModListener.INSTANCE)) {
					addFinalDamageTakenModificationListener(CUnitDefaultMagicImmuneDamageModListener.INSTANCE);
				}
			} else {
				if (this.finalDamageTakenModificationListeners
						.contains(CUnitDefaultMagicImmuneDamageModListener.INSTANCE)) {
					removeFinalDamageTakenModificationListener(CUnitDefaultMagicImmuneDamageModListener.INSTANCE);
				}
			}
			break;
		case MORPH_IMMUNE:
			boolean isMorphImmune = false;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.MORPH_IMMUNE) {
					if (buff.getValue() != 0) {
						isMorphImmune = true;
					}
				}
			}
			this.morphImmune = isMorphImmune;
			break;
		case RESISTANT:
			boolean isResistant = false;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.RESISTANT) {
					if (buff.getValue() != 0) {
						isResistant = true;
					}
				}
			}
			this.resistant = isResistant;
			break;
		case SLEEPING:
		case STUN:
			boolean isSleeping = false;
			boolean isStun = false;
			boolean updateAnim = false;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.SLEEPING) {
					if (buff.getValue() != 0) {
						isSleeping = true;
					}
					if (buff.getValue() == 1) {
						updateAnim = true;
					}
				}
				if (buff.getBuffType() == StateModBuffType.STUN) {
					if (buff.getValue() != 0) {
						isStun = true;
					}
					if (buff.getValue() == 1) {
						updateAnim = true;
					}
				}
			}
			if (isSleeping || isStun) {
				if ((this.currentBehavior == null)
						|| (this.currentBehavior.getHighlightOrderId() != OrderIds.stunned)) {
					if (this.currentBehavior != null) {
						this.interruptedBehavior = this.currentBehavior;
						this.interruptedDefaultBehavior = this.defaultBehavior;
					}
					this.currentBehavior = new CBehaviorStun(this, updateAnim);
					this.currentBehavior.begin(game);
					setDefaultBehavior(this.currentBehavior);
					this.stateNotifier.ordersChanged();
				}
			} else {
				if ((this.currentBehavior != null)
						&& (this.currentBehavior.getHighlightOrderId() == OrderIds.stunned)) {
//					this.setAcceptingOrders(true);
					setDefaultBehavior(this.interruptedDefaultBehavior);
					this.currentBehavior = pollNextOrderBehavior(game);
					this.interruptedBehavior = null;
					this.interruptedDefaultBehavior = null;
					this.stateNotifier.ordersChanged();
				}
			}

			if (isSleeping) {
				if (!this.damageTakenListeners.contains(CUnitDefaultSleepListener.INSTANCE)) {
					addDamageTakenListener(CUnitDefaultSleepListener.INSTANCE);
				}
			} else {
				if (this.damageTakenListeners.contains(CUnitDefaultSleepListener.INSTANCE)) {
					removeDamageTakenListener(CUnitDefaultSleepListener.INSTANCE);
				}
			}
			break;
		case SNARED:
			boolean isSnared = false;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.SNARED) {
					if (buff.getValue() != 0) {
						isSnared = true;
					}
				}
			}
			if (isSnared) {
				setFlyHeight(0);
				this.moveDisabled = true;
			} else {
				if (this.moveDisabled) {
					setFlyHeight(this.unitType.getDefaultFlyingHeight());
					this.moveDisabled = false;
				}
			}
			break;
		case PAUSED:
			boolean isPaused = false;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.PAUSED) {
					if (buff.getValue() != 0) {
						isPaused = true;
					}
				}
			}
			this.paused = isPaused;

			break;
		case DISABLE_UNIT_COLLISION:
			boolean isDisableCollision = false;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.DISABLE_UNIT_COLLISION) {
					if (buff.getValue() != 0) {
						isDisableCollision = true;
					}
				}
			}
			if (isDisableCollision) {
				if (!this.noUnitCollision) {
					this.noUnitCollision = true;
				}
			} else {
				if (this.noUnitCollision) {
					this.noUnitCollision = false;
				}
			}
			break;
		case DISABLE_BUILDING_COLLISION:
			boolean isDisableBuildCollision = false;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.DISABLE_BUILDING_COLLISION) {
					if (buff.getValue() != 0) {
						isDisableBuildCollision = true;
					}
				}
			}
			if (isDisableBuildCollision) {
				if (!this.noBuildingCollision) {
					this.noBuildingCollision = true;
				}
			} else {
				if (this.noBuildingCollision) {
					this.noBuildingCollision = false;
				}
			}
			break;
		case INVULNERABLE:
			boolean isInvuln = false;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.INVULNERABLE) {
					if (buff.getValue() != 0) {
						isInvuln = true;
					}
				}
			}
			this.buffInvulnerable = isInvuln;
			this.invulnerable = this.scriptInvulnerable || this.buffInvulnerable;
			this.stateNotifier.abilitiesChanged();
			break;
		case HIDE_MINIMAP_ICON:
			boolean hideMini = false;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.HIDE_MINIMAP_ICON) {
					if (buff.getValue() != 0) {
						hideMini = true;
					}
				}
			}
			this.hideMinimapIcon = hideMini;
			break;
		case FALSE_OWNERSHIP:
			int fakePlayerId = -1;
			int prio = -1;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.FALSE_OWNERSHIP) {
					if (buff.getValue() != 0) {
						int pr = (int) (buff.getValue() & 0b1111);
						if (pr > prio) {
							prio = pr;
							fakePlayerId = (int) (buff.getValue() >> 4);
						}
					}
				}
			}
			if (fakePlayerId >= 0) {
				this.fakePlayerIndex = fakePlayerId;
			} else {
				this.fakePlayerIndex = this.playerIndex;
			}
			break;
		case DETECTOR:
			this.detectorLevels.clear();
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.DETECTOR) {
					if (buff.getValue() != 0) {
						this.detectorLevels.add((int) buff.getValue());
					}
				}
			}
			break;
		case INVISIBLE:
		case DETECTED:
			byte invisLevels = 0;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.INVISIBLE) {
					if (buff.getValue() != 0) {
						invisLevels = (byte) (invisLevels | buff.getValue());
					}
				}
			}
			this.invisLevels = invisLevels;

			long detections = 0;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.DETECTED) {
					if (buff.getValue() != 0) {
						if ((((byte) buff.getValue()) & this.invisLevels) == this.invisLevels) {
							detections = (detections | (buff.getValue() >> 8));
						}
					}
				}
			}
			this.detections = detections;
			break;
		case UNSELECTABLE:
			long unselectablePlayers = 0;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.UNSELECTABLE) {
					if (buff.getValue() != 0) {
						unselectablePlayers = (unselectablePlayers | buff.getValue());
					}
				}
			}
			this.unselectablePlayers = unselectablePlayers;
			break;
		case UNTARGETABLE:
			long untargetablePlayers = 0;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.UNTARGETABLE) {
					if (buff.getValue() != 0) {
						untargetablePlayers = (untargetablePlayers | buff.getValue());
					}
				}
			}
			this.untargetablePlayers = untargetablePlayers;
			break;
		case UNENUMERABLE:
			boolean isUnenumerable = false;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.UNENUMERABLE) {
					if (buff.getValue() != 0) {
						isUnenumerable = true;
					}
				}
			}
			this.unenumerable = isUnenumerable;
			break;
		case UNAUTOATTACKABLE:
			boolean isUnAutoAttackable = false;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.UNAUTOATTACKABLE) {
					if (buff.getValue() != 0) {
						isUnAutoAttackable = true;
					}
				}
			}
			this.autoTargetable = !isUnAutoAttackable;
			break;
		case BLOCK_REPAIR:
			boolean isRepairBlocked = false;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.BLOCK_REPAIR) {
					if (buff.getValue() != 0) {
						isRepairBlocked = true;
					}
				}
			}
			this.repairBlocked = isRepairBlocked;
			break;
		case BLOCK_TRAINING:
			boolean isTrainingBlocked = false;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.BLOCK_TRAINING) {
					if (buff.getValue() != 0) {
						isTrainingBlocked = true;
					}
				}
			}
			this.trainingBlocked = isTrainingBlocked;
			break;
		case BLOCK_CONSTRUCTION:
			boolean isConstructionBlocked = false;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.BLOCK_CONSTRUCTION) {
					if (buff.getValue() != 0) {
						isConstructionBlocked = true;
					}
				}
			}
			this.constructionBlocked = isConstructionBlocked;
			break;
		default:
			break;
		}
	}

	public void computeDerivedFields(final NonStackingStatBuffType type) {
		switch (type) {
		case DEF:
		case DEFPCT:
			this.baseDefense = this.unitType.getDefense() + this.agilityDefensePermanentBonus
					+ this.permanentDefenseBonus;
			this.currentDefenseDisplay = Math.round(this.baseDefense);

			float totalNSDefBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.DEF, 0);
			totalNSDefBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.DEFPCT, totalNSDefBuff,
					this.baseDefense);

			this.totalTemporaryDefenseBonus = this.temporaryDefenseBonus + this.agilityDefenseTemporaryBonus
					+ totalNSDefBuff;
			this.currentDefense = this.baseDefense + this.totalTemporaryDefenseBonus + this.scriptDefenseBonus;
			break;
		case HPGEN:
		case HPGENPCT:
		case MAXHPGENPCT:
			float totalNSHPGenBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.HPGEN, 0);
			totalNSHPGenBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.HPGENPCT, totalNSHPGenBuff,
					this.lifeRegen + this.lifeRegenBonus + this.lifeRegenStrengthBonus);
			totalNSHPGenBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.MAXHPGENPCT, totalNSHPGenBuff,
					this.maximumLife);
			this.currentLifeRegenPerTick = (this.lifeRegen + this.lifeRegenBonus + this.lifeRegenStrengthBonus
					+ totalNSHPGenBuff) * WarsmashConstants.SIMULATION_STEP_TIME;
			break;
		case MPGEN:
		case MPGENPCT:
		case MAXMPGENPCT:
			float totalNSMPGenBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.MPGEN, 0);
			totalNSMPGenBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.MPGENPCT, totalNSMPGenBuff,
					this.manaRegen + this.manaRegenBonus + this.manaRegenIntelligenceBonus);
			totalNSMPGenBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.MAXMPGENPCT, totalNSMPGenBuff,
					this.maximumMana);
			this.currentManaRegenPerTick = (this.manaRegen + this.manaRegenBonus + this.manaRegenIntelligenceBonus
					+ totalNSMPGenBuff) * WarsmashConstants.SIMULATION_STEP_TIME;
			break;
		case MVSPDPCT:
		case MVSPD:
			float totalNSMvSpdBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.MVSPD, 0);
			totalNSMvSpdBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.MVSPDPCT, totalNSMvSpdBuff,
					this.baseSpeed);
			this.speedBonus = Math.round(totalNSMvSpdBuff);
			this.computeFinalSpeed();
			break;
		case MELEEATK:
		case MELEEATKPCT:
			for (final CUnitAttack attack : getUnitSpecificAttacks()) {
				if ((attack.getWeaponType() != null) && (attack.getWeaponType() == CWeaponType.NORMAL)) {
					attack.setNonStackingFlatBuffs(this.nonStackingStatBuffs.get(NonStackingStatBuffType.MELEEATK));
					attack.setNonStackingPctBuffs(this.nonStackingStatBuffs.get(NonStackingStatBuffType.MELEEATKPCT));
					attack.computeDerivedFields();
				}
			}
			notifyAttacksChanged();
			break;
		case RNGDATK:
		case RNGDATKPCT:
			for (final CUnitAttack attack : getUnitSpecificAttacks()) {
				if ((attack.getWeaponType() != null) && (attack.getWeaponType() != CWeaponType.NORMAL)) {
					attack.setNonStackingFlatBuffs(this.nonStackingStatBuffs.get(NonStackingStatBuffType.RNGDATK));
					attack.setNonStackingPctBuffs(this.nonStackingStatBuffs.get(NonStackingStatBuffType.RNGDATKPCT));
					attack.computeDerivedFields();
				}
			}
			notifyAttacksChanged();
			break;
		case ALLATK:
		case ALLATKPCT:
			for (final CUnitAttack attack : getUnitSpecificAttacks()) {
				if ((attack.getWeaponType() != null) && (attack.getWeaponType() == CWeaponType.NORMAL)) {
					attack.setNonStackingFlatBuffs(this.nonStackingStatBuffs.get(NonStackingStatBuffType.MELEEATK));
					attack.setNonStackingPctBuffs(this.nonStackingStatBuffs.get(NonStackingStatBuffType.MELEEATKPCT));
					attack.computeDerivedFields();
				}
			}
			for (final CUnitAttack attack : getUnitSpecificAttacks()) {
				if ((attack.getWeaponType() != null) && (attack.getWeaponType() != CWeaponType.NORMAL)) {
					attack.setNonStackingFlatBuffs(this.nonStackingStatBuffs.get(NonStackingStatBuffType.RNGDATK));
					attack.setNonStackingPctBuffs(this.nonStackingStatBuffs.get(NonStackingStatBuffType.RNGDATKPCT));
					attack.computeDerivedFields();
				}
			}
			notifyAttacksChanged();
			break;
		case ATKSPD:
			float totalNSAtkSpdBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.ATKSPD, 0);
			for (final CUnitAttack attack : getUnitSpecificAttacks()) {
				attack.setAttackSpeedModifier(totalNSAtkSpdBuff);
			}
			notifyAttacksChanged();
			break;
		case HPSTEAL:
			float totalNSVampBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.HPSTEAL, 0);
			if (this.lifestealListener != null) {
				this.lifestealListener.setAmount(totalNSVampBuff);
			} else {
				this.lifestealListener = new CUnitDefaultLifestealListener(totalNSVampBuff);
				addPostDamageListener(this.lifestealListener);
			}
			break;
		case THORNS:
			float totalNSThornsBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.THORNS, 0);
			if (this.flatThornsListener != null) {
				this.flatThornsListener.setAmount(totalNSThornsBuff);
			} else {
				this.flatThornsListener = new CUnitDefaultThornsListener(false, totalNSThornsBuff);
				addDamageTakenListener(this.flatThornsListener);
			}
			break;
		case THORNSPCT:
			float totalNSThornsPctBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.THORNSPCT, 0);
			if (this.percentThornsListener != null) {
				this.percentThornsListener.setAmount(totalNSThornsPctBuff);
			} else {
				this.percentThornsListener = new CUnitDefaultThornsListener(true, totalNSThornsPctBuff);
				addDamageTakenListener(this.percentThornsListener);
			}
			break;
		case MAXHPPCT:
		case MAXHP:
			float totalNSMaxHPBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.MAXHP, 0);
			totalNSMaxHPBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.MAXHPPCT, totalNSMaxHPBuff,
					this.baseMaximumLife);
			final int newMaxLife = Math.round((this.baseMaximumLife + totalNSMaxHPBuff) * this.handicap);
			if (newMaxLife > this.maximumLife) {
				this.life += newMaxLife - this.maximumLife;
				this.maximumLife = newMaxLife;
			} else {
				this.maximumLife = newMaxLife;
				this.life = Math.min(this.life, this.maximumLife);
			}
			break;
		case MAXMPPCT:
		case MAXMP:
			float totalNSMaxMPBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.MAXMP, 0);
			totalNSMaxMPBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.MAXMPPCT, totalNSMaxMPBuff,
					this.baseMaximumMana);
			final int newMaxMana = this.baseMaximumMana + Math.round(totalNSMaxMPBuff);
			if (newMaxMana > this.maximumMana) {
				this.mana += newMaxMana - this.maximumMana;
				this.maximumMana = newMaxMana;
			} else {
				this.maximumMana = newMaxMana;
				this.mana = Math.min(this.mana, this.maximumMana);
			}
			break;
		case STR:
		case STRPCT:
		case AGI:
		case AGIPCT:
		case INT:
		case INTPCT:
			throw new RuntimeException("Cannot run method with hero stat type");
		default:
			break;
		}
	}

	public void computeDerivedHeroFields(final CSimulation game, final NonStackingStatBuffType type) {
		switch (type) {
		case STRPCT:
		case STR:
			if (this.isHero()) {
				float totalStrBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.STR, 0);
				totalStrBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.STRPCT, totalStrBuff,
						this.getHeroData().getStrength().getCurrentBase());
				this.getHeroData().setStrengthBuffBonus(game, this, Math.round(totalStrBuff));
			}
			break;
		case AGIPCT:
		case AGI:
			if (this.isHero()) {
				float totalAgiBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.AGI, 0);
				totalAgiBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.AGIPCT, totalAgiBuff,
						this.getHeroData().getAgility().getCurrentBase());
				this.getHeroData().setAgilityBuffBonus(game, this, Math.round(totalAgiBuff));
			}
			break;
		case INTPCT:
		case INT:
			if (this.isHero()) {
				float totalIntBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.INT, 0);
				totalIntBuff = this.nonStackingStatBuffs.sum(NonStackingStatBuffType.INTPCT, totalIntBuff,
						this.getHeroData().getIntelligence().getCurrentBase());
				this.getHeroData().setIntelligenceBuffBonus(game, this, Math.round(totalIntBuff));
			}
			break;
		default:
			throw new RuntimeException("Cannot run method with non-hero stat type");
		}
	}

	public NonStackingFx addNonStackingFx(final CSimulation game, final String stackingKey, final War3ID id,
			final CEffectType target) {
		List<NonStackingFx> existingArts = this.nonStackingFx.get(stackingKey);
		final NonStackingFx newFx = new NonStackingFx(stackingKey, id);
		if (existingArts == null) {
			existingArts = new ArrayList<>();
			this.nonStackingFx.put(stackingKey, existingArts);
		}
		if (existingArts.isEmpty()) {
			final SimulationRenderComponent fx = game.createPersistentSpellEffectOnUnit(this, id, target);
			newFx.setArt(fx);
		} else {
			newFx.setArt(existingArts.iterator().next().getArt());
		}
		existingArts.add(newFx);
		return newFx;
	}

	public void removeNonStackingFx(final CSimulation game, final NonStackingFx fx) {
		final List<NonStackingFx> existingArts = this.nonStackingFx.get(fx.getStackingKey());
		if (existingArts != null) {
			existingArts.remove(fx);
			if (existingArts.isEmpty()) {
				fx.getArt().remove();
			}
		}
	}

	public void addNonStackingDisplayBuff(final CSimulation game, final String stackingKey, final CBuff buff) {
		List<CBuff> existingBuffs = this.nonStackingDisplayBuffs.get(stackingKey);
		if (existingBuffs == null) {
			existingBuffs = new ArrayList<>();
			this.nonStackingDisplayBuffs.put(stackingKey, existingBuffs);
		}
		if (existingBuffs.isEmpty()) {
			this.add(game, buff);
		} else {
			final CBuff currentBuff = this.getFirstAbilityOfType(buff.getClass());
			if (currentBuff == null) {
				existingBuffs.clear();
				this.add(game, buff);
			} else {
				if (buff.getLevel() >= currentBuff.getLevel()) {
					this.remove(game, currentBuff);
					this.add(game, buff);
				}

			}
		}
		existingBuffs.add(buff);
	}

	public void removeNonStackingDisplayBuff(final CSimulation game, final String stackingKey, final CBuff buff) {
		final List<CBuff> existingBuffs = this.nonStackingDisplayBuffs.get(stackingKey);
		if (existingBuffs != null) {
			existingBuffs.remove(buff);
			CBuff currentBuff = this.getFirstAbilityOfType(buff.getClass());
			if (currentBuff == buff) {
				this.remove(game, currentBuff);
				// Auras leave a removed unit one by one; the next one's buff would go on a unit no longer drawn.
				if (!existingBuffs.isEmpty() && !this.removedFromGame) {
					currentBuff = null;
					for (final CBuff iterBuff : existingBuffs) {
						if ((currentBuff == null) || (currentBuff.getLevel() < iterBuff.getLevel())) {
							currentBuff = iterBuff;
						}
					}
					this.add(game, currentBuff);
				}
			}
		}
	}

	private void initializeListenerLists() {
		for (final CUnitAttackPreDamageListenerPriority priority : CUnitAttackPreDamageListenerPriority.values()) {
			this.preDamageListeners.put(priority, new ArrayList<>());
		}
		for (final CUnitDeathReplacementEffectPriority priority : CUnitDeathReplacementEffectPriority.values()) {
			this.deathReplacementEffects.put(priority, new ArrayList<>());
		}
	}

	private void computeAllDerivedFields() {
		this.baseLifeRegenPerTick = this.lifeRegen * WarsmashConstants.SIMULATION_STEP_TIME;
		computeDerivedFields(NonStackingStatBuffType.DEF);
		computeDerivedFields(NonStackingStatBuffType.HPGEN);
		computeDerivedFields(NonStackingStatBuffType.MPGEN);
		if (getUnitSpecificAttacks() != null) {
			computeDerivedFields(NonStackingStatBuffType.ALLATK);
			computeDerivedFields(NonStackingStatBuffType.ATKSPD);
		}
		computeDerivedFields(NonStackingStatBuffType.HPSTEAL);
		computeDerivedFields(NonStackingStatBuffType.MVSPD);
		computeDerivedFields(NonStackingStatBuffType.THORNS);
		computeDerivedFields(NonStackingStatBuffType.THORNSPCT);
		computeDerivedFields(NonStackingStatBuffType.MAXHP);
		computeDerivedFields(NonStackingStatBuffType.MAXMP);
	}

	private void computeAllUnitStates(final CSimulation game) {
		computeUnitState(game, StateModBuffType.FALSE_OWNERSHIP);
		computeUnitState(game, StateModBuffType.HIDE_MINIMAP_ICON);
		computeUnitState(game, StateModBuffType.DETECTOR);
		computeUnitState(game, StateModBuffType.INVISIBLE);
		computeUnitState(game, StateModBuffType.INVULNERABLE);
		computeUnitState(game, StateModBuffType.ENABLE_ATTACK);
		computeUnitState(game, StateModBuffType.DISABLE_AUTO_ATTACK);
		computeUnitState(game, StateModBuffType.DISABLE_AUTO_CAST);
		computeUnitState(game, StateModBuffType.DISABLE_ASSIST_ALLY);
		computeUnitState(game, StateModBuffType.DISABLE_UNIT_COLLISION);
		computeUnitState(game, StateModBuffType.DISABLE_BUILDING_COLLISION);
		computeUnitState(game, StateModBuffType.MAGIC_IMMUNE);
		computeUnitState(game, StateModBuffType.MORPH_IMMUNE);
		computeUnitState(game, StateModBuffType.RESISTANT);
		computeUnitState(game, StateModBuffType.STUN);
		computeUnitState(game, StateModBuffType.PAUSED);
		computeUnitState(game, StateModBuffType.UNSELECTABLE);
		computeUnitState(game, StateModBuffType.UNTARGETABLE);
		computeUnitState(game, StateModBuffType.UNENUMERABLE);
		computeUnitState(game, StateModBuffType.UNAUTOATTACKABLE);
		computeUnitState(game, StateModBuffType.BLOCK_REPAIR);
		computeUnitState(game, StateModBuffType.BLOCK_TRAINING);
		computeUnitState(game, StateModBuffType.BLOCK_CONSTRUCTION);
	}

	public void setManaRegenIntelligenceBonus(final float manaRegenIntelligenceBonus) {
		this.manaRegenIntelligenceBonus = manaRegenIntelligenceBonus;
		computeDerivedFields(NonStackingStatBuffType.MPGEN);
	}

	public void setLifeRegenBonus(final float lifeRegenBonus) {
		this.lifeRegenBonus = lifeRegenBonus;
		computeDerivedFields(NonStackingStatBuffType.HPGEN);
	}

	public void setManaRegenBonus(final float manaRegenBonus) {
		this.manaRegenBonus = manaRegenBonus;
		computeDerivedFields(NonStackingStatBuffType.MPGEN);
	}

	public void setManaRegen(final float manaRegen) {
		this.manaRegen = manaRegen;
		computeDerivedFields(NonStackingStatBuffType.MPGEN);
	}

	public float getManaRegenBonus() {
		return this.manaRegenBonus;
	}

	public float getManaRegen() {
		return this.manaRegen;
	}

	public void setAgilityDefensePermanentBonus(final float agilityDefensePermanentBonus) {
		this.agilityDefensePermanentBonus = agilityDefensePermanentBonus;
		computeDerivedFields(NonStackingStatBuffType.DEF);
	}

	public void setAgilityDefenseTemporaryBonus(final float agilityDefenseTemporaryBonus) {
		this.agilityDefenseTemporaryBonus = agilityDefenseTemporaryBonus;
		computeDerivedFields(NonStackingStatBuffType.DEF);
	}

	public void setPermanentDefenseBonus(final int permanentDefenseBonus) {
		this.permanentDefenseBonus = permanentDefenseBonus;
		computeDerivedFields(NonStackingStatBuffType.DEF);
	}

	public int getPermanentDefenseBonus() {
		return this.permanentDefenseBonus;
	}

	/** BlzSetUnitArmor: the base armor changes so the total is the given value; bonuses keep adding on top. */
	public void setTotalDefense(final float totalDefense) {
		this.scriptDefenseBonus += totalDefense - this.currentDefense;
		computeDerivedFields(NonStackingStatBuffType.DEF);
	}

	public void setTemporaryDefenseBonus(final float temporaryDefenseBonus) {
		this.temporaryDefenseBonus = temporaryDefenseBonus;
		computeDerivedFields(NonStackingStatBuffType.DEF);
	}

	public float getTemporaryDefenseBonus() {
		return this.temporaryDefenseBonus;
	}

	public float getTotalTemporaryDefenseBonus() {
		return this.totalTemporaryDefenseBonus;
	}

	public int getCurrentDefenseDisplay() {
		return this.currentDefenseDisplay;
	}

	public void setUnitAnimationListener(final CUnitAnimationListener unitAnimationListener) {
		this.unitAnimationListener = unitAnimationListener;
		this.unitAnimationListener.playAnimation(true, PrimaryTag.STAND, SequenceUtils.EMPTY, 1.0f, true);
	}

	public CUnitAnimationListener getUnitAnimationListener() {
		return this.unitAnimationListener;
	}

	public void registerOnTickEvent(final Trigger trigger) {
		this.onTickTriggers.add(trigger);
		this.stateNotifier.abilitiesChanged();
	}

	public void unregisterOnTickEvent(final Trigger trigger) {
		this.onTickTriggers.remove(trigger);
		this.stateNotifier.abilitiesChanged();
	}

	public void add(final CSimulation simulation, final CAbility ability) {
		this.abilities.add(ability);
		ability.onAddDisabled(simulation, this);
		simulation.onAbilityAddedToUnit(this, ability);
		if (!ability.isRequirementsMet(simulation, this)) {
			ability.setDisabled(true, CAbilityDisableType.REQUIREMENTS);
		}
		if (simulation.getPlayer(this.playerIndex).isAbilityDisabled(ability.getAlias())) {
			ability.setDisabled(true, CAbilityDisableType.PLAYER);
		}
		if (ability.isDisabled()) {
			this.disabledAbilities.add(ability);
		} else {
			ability.onAdd(simulation, this);
		}
		this.stateNotifier.abilitiesChanged();
	}

	public void add(final CSimulation simulation, final CBuff ability) {
		int index = this.abilities.indexOf(ability);
		if (index >= 0 && (ability instanceof ABGenericTimedBuff)) {
			((ABGenericTimedBuff) this.abilities.get(index)).updateExpiration(simulation, this);
		} else {
			this.abilities.add(ability);
			simulation.onAbilityAddedToUnit(this, ability);
			ability.onAdd(simulation, this);
			this.stateNotifier.abilitiesChanged();
		}
	}

	public void remove(final CSimulation simulation, final CAbility ability) {
		if (this.disabledAbilities.contains(ability)) {
			this.abilities.remove(ability);
			this.disabledAbilities.remove(ability);
			simulation.onAbilityRemovedFromUnit(this, ability);
			ability.onRemoveDisabled(simulation, this);
			this.stateNotifier.abilitiesChanged();
		} else {
			this.abilities.remove(ability);
			simulation.onAbilityRemovedFromUnit(this, ability);
			ability.onRemove(simulation, this);
			ability.onRemoveDisabled(simulation, this);
			this.stateNotifier.abilitiesChanged();
		}
	}

	public void remove(final CSimulation simulation, final CBuff ability) {
		this.abilities.remove(ability);
		simulation.onAbilityRemovedFromUnit(this, ability);
		ability.onRemove(simulation, this);
		this.stateNotifier.abilitiesChanged();
	}

	public void checkDisabledAbilities(final CSimulation simulation, final boolean disable) {
		if (disable) {
			for (final CAbility ability : new ArrayList<>(this.abilities)) {
				if (!ability.isRequirementsMet(simulation, this)) {
					ability.setDisabled(true, CAbilityDisableType.REQUIREMENTS);
				}
				if (simulation.getPlayer(this.playerIndex).isAbilityDisabled(ability.getAlias())) {
					ability.setDisabled(true, CAbilityDisableType.PLAYER);
				}
				if (ability.isDisabled() && !this.disabledAbilities.contains(ability)) {
//					System.err.println("Disabling ability: " + ability.getAlias().asStringValue());
					this.disabledAbilities.add(ability);
					ability.onRemove(simulation, this);
					if ((this.currentBehavior != null)
							&& ability.equals(this.currentBehavior.visit(BehaviorAbilityVisitor.INSTANCE))) {
						performDefaultBehavior(simulation);
					}
				}
			}
		} else {
			for (final CAbility ability : new ArrayList<>(this.disabledAbilities)) {
				if (ability.isRequirementsMet(simulation, this)) {
					ability.setDisabled(false, CAbilityDisableType.REQUIREMENTS);
				}
				if (!simulation.getPlayer(this.playerIndex).isAbilityDisabled(ability.getAlias())) {
					ability.setDisabled(false, CAbilityDisableType.PLAYER);
				}
				if (!ability.isDisabled()) {
					this.disabledAbilities.remove(ability);
					ability.onAdd(simulation, this);
				}
			}
		}
		fireCooldownsChangedEvent();
	}

	/** The vertex color the game gave the unit, or null while it has its type's tint. */
	public float[] getVertexColor() {
		return this.vertexColor;
	}

	public void setVertexColor(final float[] vertexColor) {
		this.vertexColor = vertexColor;
	}

	public War3ID getTypeId() {
		return this.typeId;
	}

	/**
	 * @return facing in DEGREES
	 */
	public float getFacing() {
		return this.facing;
	}

	/** The facing GetUnitFacing reads, at the game's time now; see {@link #drawnFacing}. */
	public float getDrawnFacing(final CSimulation game) {
		updateDrawnFacing(CTimer.currentTime(game));
		return this.drawnFacing;
	}

	/** Turns the drawn facing at once too, as BlzSetUnitFacingEx does. */
	public void setFacingAndDrawnFacing(final float facing) {
		setFacing(facing);
		this.drawnFacing = this.facing;
		this.drawnTurnVelocity = 0;
	}

	private void updateDrawnFacing(final double time) {
		final int tick = (int) StrictMath.floor(((time - WarsmashConstants.SIMULATION_STEP_TIME) / DRAWN_FACING_TICK)
				+ 1e-6);
		if (Float.isNaN(this.drawnFacing) || (this.drawnFacingTick == Integer.MIN_VALUE)) {
			if (Float.isNaN(this.drawnFacing)) {
				this.drawnFacing = this.facing;
			}
			this.drawnFacingTick = tick;
			return;
		}
		if (tick <= this.drawnFacingTick) {
			return;
		}
		int ticks = tick - this.drawnFacingTick;
		this.drawnFacingTick = tick;
		final int row = this.unitType.getOrientationInterpolation();
		final OrientationInterpolation turn = OrientationInterpolation.VALUES[(row < 0)
				|| (row >= OrientationInterpolation.VALUES.length) ? 0 : row];
		while ((ticks-- > 0) && ((this.drawnFacing != this.facing) || (this.drawnTurnVelocity != 0))) {
			float delta = this.facing - this.drawnFacing;
			if (delta < -180) {
				delta += 360;
			}
			if (delta > 180) {
				delta -= 360;
			}
			final float sign = Math.signum(delta);
			final float remaining = (float) StrictMath.toRadians(Math.abs(delta));
			// Measured in 1.29: the turn eases off as a power of the angle left rather than in a straight line, and it
			// stops a degree or two short once the step would be smaller than DRAWN_FACING_REST.
			final float slowdown = (float) StrictMath.pow(Math.min(1, remaining / turn.getEndingAccelCutoff()),
					DRAWN_FACING_EASE);
			final float speed = Math.max(0, this.drawnTurnVelocity * sign) + turn.getStartingAcceleration();
			this.drawnTurnVelocity = Math.min(speed, turn.getMaxVelocity() * slowdown) * sign;
			final float step = (float) StrictMath.toDegrees(this.drawnTurnVelocity);
			if (Math.abs(delta) <= Math.abs(step)) {
				this.drawnFacing = this.facing;
				this.drawnTurnVelocity = 0;
			}
			else if (Math.abs(step) < DRAWN_FACING_REST) {
				this.drawnTurnVelocity = 0;
			}
			else {
				this.drawnFacing = (((this.drawnFacing + step) % 360) + 360) % 360;
			}
		}
	}

	public float getMana() {
		return this.mana;
	}

	public int getMaximumMana() {
		return this.maximumMana;
	}

	public int getMaximumLife() {
		return this.maximumLife;
	}

	public void setTypeId(final CSimulation game, final War3ID typeId, final boolean keepRatios) {
		setTypeId(game, typeId, keepRatios, true);
	}

	public void setTypeId(final CSimulation game, final War3ID typeId, final boolean keepRatios,
			final boolean updateArt) {
		game.getWorldCollision().removeUnit(this);
		final CPlayer player = game.getPlayer(this.playerIndex);
		player.removeTechtreeUnlocked(game, this.typeId);
		game.getUnitData().unapplyPlayerUpgradesToUnit(game, playerIndex, unitType, this);
		this.typeId = typeId;
		player.addTechtreeUnlocked(game, this.typeId);
		final float lifeRatio = this.maximumLife == 0 ? 1 : this.life / this.maximumLife;
		final float manaRatio = this.maximumMana == 0 ? Float.NaN : this.mana / this.maximumMana;
		final CUnitType previousUnitType = getUnitType();
		this.unitType = game.getUnitData().getUnitType(typeId);
		this.baseMaximumMana += this.unitType.getManaMaximum() - previousUnitType.getManaMaximum();
		this.baseMaximumLife += this.unitType.getMaxLife() - previousUnitType.getMaxLife();
		this.maximumMana += this.unitType.getManaMaximum() - previousUnitType.getManaMaximum();
		this.maximumLife += this.unitType.getMaxLife() - previousUnitType.getMaxLife();
		this.lifeRegen = this.unitType.getLifeRegen();
		this.manaRegen = this.unitType.getManaRegen();
		computeAllDerivedFields();

		if (updateArt) {
			this.flyHeight = this.unitType.getDefaultFlyingHeight();
		}
		this.turnRate = this.unitType.getTurnRate();
		this.propWindow = this.unitType.getPropWindow();
		this.maxSpeed = this.unitType.getMaxSpeed();
		if (maxSpeed == 0) {
			this.maxSpeed = (int) game.getGameplayConstants().getMaxUnitSpeed();
		}
		this.minSpeed = this.unitType.getMinSpeed();
		if (minSpeed == 0) {
			this.minSpeed = (int) game.getGameplayConstants().getMinUnitSpeed();
		}
		this.baseSpeed = this.unitType.getSpeed();
		this.classifications.removeAll(previousUnitType.getClassifications());
		this.classifications.addAll(this.unitType.getClassifications());
		this.targetedAs.removeAll(previousUnitType.getTargetedAs());
		this.targetedAs.addAll(this.unitType.getTargetedAs());
		this.defenseType = this.unitType.getDefenseType();
		this.acquisitionRange = this.unitType.getDefaultAcquisitionRange();
		this.structure = this.unitType.isBuilding();
		this.raisable = this.unitType.isRaise();
		this.decays = this.unitType.isDecay();
		final List<War3ID> sharedAbilities = new ArrayList<War3ID>(previousUnitType.getAbilityList());
		// sharedAbilities.addAll(previousUnitType.getHeroAbilityList());
		final List<War3ID> newIds = new ArrayList<War3ID>(this.unitType.getAbilityList());
		// newIds.addAll(this.unitType.getHeroAbilityList());
		sharedAbilities.retainAll(newIds); // TODO Seems wasteful, but need to avoid messing up heros on transform
		final List<CAbility> persistedAbilities = new ArrayList<>();
		final List<CAbility> removedAbilities = new ArrayList<>();
		final List<CAbility> startingAbilities = new ArrayList<>(this.abilities);
		for (final CAbility ability : startingAbilities) {
			if (!ability.isPermanent() && !sharedAbilities.contains(ability.getAlias())
					&& (!ability.isHero() || !this.unitType.isHero())
					&& !(ability.getAbilityCategory() == CAbilityCategory.BUFF)) {
				ability.onRemove(game, this);
				game.onAbilityRemovedFromUnit(this, ability);
				removedAbilities.add(ability);
			} else {
				persistedAbilities.add(ability);
			}
		}
		for (final CAbility removed : removedAbilities) {
			this.abilities.remove(removed); // TODO remove inefficient O(N) search
		}
		game.unitUpdatedType(this, typeId, true);
		game.getUnitData().addMissingDefaultAbilitiesToUnit(game, game.getHandleIdAllocator(), this.unitType, false, -1,
				this.baseSpeed, this);
		{
			// Remove and add the persisted abilities, so that some stuff like move and
			// attack are "first" in the end resulting list. This is "dumb" and a better
			// design later might be worth considering; it fixes a bug where otherwise
			// `Chaos` used on a worker into another near identical worker was changing the
			// ability order used by "smart" such that the unit would prefer Attack over
			// Harvest
			for (final CAbility persisted : persistedAbilities) {
				this.abilities.remove(persisted);
			}
			for (final CAbility persisted : persistedAbilities) {
				this.abilities.add(persisted);
			}
		}
		game.getWorldCollision().addUnit(this);
		for (final CAbility ability : persistedAbilities) {
			ability.onSetUnitType(game, this);
			game.onAbilityAddedToUnit(this, ability);
		}
		game.getUnitData().applyPlayerUpgradesToUnit(game, playerIndex, unitType, this);
		computeAllDerivedFields();
		computeAllUnitStates(game);
		if (keepRatios) {
			this.life = lifeRatio * this.maximumLife;
			if (Float.isNaN(manaRatio)) {
				this.mana = this.unitType.getManaInitial();
			} else {
				this.mana = manaRatio * this.maximumMana;
			}
		} else {
			this.life += this.unitType.getMaxLife() - previousUnitType.getMaxLife();
			if (Float.isNaN(manaRatio)) {
				this.mana = this.unitType.getManaInitial();
			} else {
				this.mana += this.unitType.getManaMaximum() - previousUnitType.getManaMaximum();
			}
		}
	}

	public void setFacing(final float facing) {
		// java modulo output can be negative, but not if we
		// force positive and modulo again
		this.facing = ((facing % 360) + 360) % 360;
	}

	public void setMana(final float mana) {
		this.mana = mana;
		this.stateNotifier.manaChanged();
		checkStateEvents(false);
	}

	public void setMaximumLife(final int maximumLife) {
		this.baseMaximumLife = maximumLife;
		computeDerivedFields(NonStackingStatBuffType.MAXHPPCT);
		computeDerivedFields(NonStackingStatBuffType.MAXHPGENPCT);
	}

	public void addMaxLifeRelative(final CSimulation game, final int hitPointBonus) {
		final int oldMaximumLife = getMaximumLife();
		final float oldLife = getLife();
		setMaximumLife(this.baseMaximumLife + hitPointBonus);
		setLife(game, (oldLife * getMaximumLife()) / oldMaximumLife);
	}

	/**
	 * The owner's handicap scales maximum life at once, as in the original game; life keeps its share of the maximum.
	 */
	public void setHandicap(final float handicap) {
		final float lifeShare = this.maximumLife > 0 ? this.life / this.maximumLife : 1;
		this.handicap = handicap;
		computeDerivedFields(NonStackingStatBuffType.MAXHP);
		this.life = lifeShare * this.maximumLife;
		this.stateNotifier.lifeChanged();
	}

	public void setMaximumMana(final int maximumMana) {
		this.baseMaximumMana = maximumMana;
		computeDerivedFields(NonStackingStatBuffType.MAXMPPCT);
		computeDerivedFields(NonStackingStatBuffType.MAXMPGENPCT);
	}

	public void setSpeed(final int speed) {
		this.baseSpeed = speed;
		computeDerivedFields(NonStackingStatBuffType.MVSPD);
	}

	private void computeFinalSpeed() {
		this.finalSpeed = Math.max(Math.min(this.baseSpeed + this.speedBonus, this.maxSpeed), this.minSpeed);
	}

	public int getSpeed() {
		return this.moveDisabled ? 0 : this.finalSpeed;
	}

	/** What buffs and items add to the move speed, or take from it. */
	public int getSpeedBonus() {
		return this.speedBonus;
	}

	/**
	 * Updates one tick of simulation logic and return true if it's time to remove
	 * this unit from the game.
	 */
	public boolean update(final CSimulation game) {
		if (!this.stateListenersUpdates.isEmpty()) {
			for (final StateListenerUpdate update : this.stateListenersUpdates) {
				switch (update.getUpdateType()) {
				case ADD:
					this.stateNotifier.subscribe(update.listener);
					break;
				case REMOVE:
					this.stateNotifier.unsubscribe(update.listener);
					break;
				}
			}
			this.stateListenersUpdates.clear();
		}
		updateDrawnFacing(CTimer.currentTime(game));
		// Mana regeneration and stat changes write the fields directly; they reach the state events here. Measured: the
		// original writes a dying unit's life once more a moment after its death.
		checkStateEvents(false);
		if (isDead() && !this.corpse && (game.getGameTurnTick() == (this.deathTurnTick + 1))) {
			checkStateEvents(true);
		}
		this.spellMissileSource = null;
		if ((this.pendingSpellAnswerSource != null) && !isDead()) {
			resolveSpellAnswer(game);
		}
		if ((this.pendingAttackBackSource != null) && !isDead()) {
			resolveAttackBack(game);
		}
		if (this.guarding && !isDead()) {
			updateGuard(game);
			updateNightSleep(game);
		}
		if (isDead()) {
			if (!this.falseDeath) {
				final int gameTurnTick = game.getGameTurnTick();
				if (!this.corpse) {
					if ((this.collisionRectangle != null) && !this.freshCorpse) {
						// Moved this here because doing it on "kill" was able to happen in some cases
						// while also iterating over the units that are in the collision system, and
						// then it hit the "writing while iterating" problem.
						game.getWorldCollision().removeUnit(this);
						// Measured: the original raises or resurrects a unit one second after it dies, during
						// its death animation, so the corpse can be used from the moment of death.
						if (isRaisable() && isDecays() && !this.unitType.isHero() && !isHidden()) {
							this.freshCorpse = true;
							game.getWorldCollision().addUnit(this);
						}
					}
					if (gameTurnTick > (this.deathTurnTick
							+ CTimer.wholeSteps(this.unitType.getDeathTime()))) {
						this.corpse = true;
						if (!isRaisable()) {
							this.boneCorpse = true;
							// start final phase immediately for "cant raise" case
						}
						if (!this.unitType.isHero()) {
							if (!isDecays()) {
								// if we dont raise AND dont decay, then now that death anim is over
								// we just delete the unit
								return true;
							}
						} else {
							game.heroDeathEvent(this);
						}
						this.deathTurnTick = gameTurnTick;
					}
				} else if (!this.boneCorpse) {
					if (game.getGameTurnTick() > (this.deathTurnTick
							+ CTimer.wholeSteps(game.getGameplayConstants().getDecayTime()))) {
						this.boneCorpse = true;
						this.deathTurnTick = gameTurnTick;

						if (isRaisable() && !this.freshCorpse) {
							game.getWorldCollision().addUnit(this);
						}
					}
				} else if (game.getGameTurnTick() > (this.deathTurnTick
						+ CTimer.wholeSteps(getEndingDecayTime(game)))) {
					if (this.unitType.isHero()) {
						if (!getHeroData().isAwaitingRevive()) {
							setHidden(true);
							getHeroData().setAwaitingRevive(true);
							game.heroDissipateEvent(this);
						}
						return false;
					}
					return true;
				}
			}
		}
		else {
			innerUpdateNoCollisionMovementType(game.getWorldCollision());
			if (!this.paused) {
				if ((this.rallyPoint != this) && (this.rallyPoint instanceof CUnit)
						&& ((CUnit) this.rallyPoint).isDead()) {
					setRallyPoint(this);
				}
				if (this.constructing) {
					if (!(this.constructingPaused || this.constructionBlocked)) {
						this.constructionProgress += WarsmashConstants.SIMULATION_STEP_TIME * game.getCheatBuildSpeed();
					}
					final int buildTime;
					final boolean upgrading = isUpgrading();
					if (!upgrading) {
						buildTime = this.unitType.getBuildTime();
						if (!(this.constructingPaused || this.constructionBlocked)) {
							final float healthGain = ((WarsmashConstants.SIMULATION_STEP_TIME * game.getCheatBuildSpeed()) / buildTime)
									* (this.maximumLife * (1.0f - WarsmashConstants.BUILDING_CONSTRUCT_START_LIFE));
							setLife(game, Math.min(this.life + healthGain, this.maximumLife));
						}
					} else {
						buildTime = game.getUnitData().getUnitType(this.upgradeIdType).getBuildTime();
					}
					if (this.constructionProgress >= buildTime) {
						this.constructing = false;
						this.constructingPaused = false;
						this.constructionProgress = 0;
						if (this.constructionConsumesWorker) {
							if (this.worker != null) {
								game.removeUnit(this.worker);
								this.worker = null;
							}
						} else {
							popoutWorker(game);
						}
						final Iterator<CAbility> abilityIterator = this.abilities.iterator();
						while (abilityIterator.hasNext()) {
							final CAbility ability = abilityIterator.next();
							if (ability instanceof CAbilityBuildInProgress) {
								abilityIterator.remove();
							} else {
								ability.setDisabled(false, CAbilityDisableType.CONSTRUCTION);
								ability.setIconShowing(true);
							}
						}
						checkDisabledAbilities(game, false);
						final CPlayer player = game.getPlayer(this.playerIndex);
						if (upgrading) {
							if (this.unitType.getFoodMade() != 0) {
								player.setFoodCap(player.getFoodCap() - this.unitType.getFoodMade());
							}
							final War3ID previousTypeId = this.typeId;
							setTypeId(game, this.upgradeIdType, true);
							this.upgradeIdType = null;
							// Measured: an upgraded building still meets requirements for the types it was upgraded
							// from, so a Castle unlocks what a Keep unlocks.
							this.upgradedFromTypeIds.add(previousTypeId);
							player.addTechtreeUnlocked(game, previousTypeId);
						}
						if (this.unitType.getFoodMade() != 0) {
							player.setFoodCap(player.getFoodCap() + this.unitType.getFoodMade());
						}
						player.removeTechtreeInProgress(game, this.unitType.getTypeId());
						if (!upgrading) {
							player.addTechtreeUnlocked(game, this.unitType.getTypeId());
							player.scoreUnitGained(this.unitType);
							game.unitConstructFinishEvent(this);
							fireConstructFinishEvents(game);
						} else {
							game.unitUpgradeFinishEvent(this); 
							firePlayerThenUnitEvents(game, JassGameEventsWar3.EVENT_UNIT_UPGRADE_FINISH,
									JassGameEventsWar3.EVENT_PLAYER_UNIT_UPGRADE_FINISH);
						}
						if (upgrading || true) {
							// TODO shouldnt need to play stand here, probably
							getUnitAnimationListener().playAnimation(false, PrimaryTag.STAND, SequenceUtils.EMPTY, 1.0f,
									true);
						}
						this.stateNotifier.ordersChanged();
					}
					for (int i = this.abilities.size() - 1; i >= 0; i--) {
						// okay if it removes self from this during onTick() because of reverse
						// iteration order
						CAbility abil = this.abilities.get(i);
						if (abil.getAbilityCategory() == CAbilityCategory.BUFF) {
							abil.onTick(game, this);
						}
					}
				} else {
					final War3ID queuedRawcode = this.buildQueue[0];
					if (queuedRawcode != null) {
						// queue step forward
						if (this.queuedUnitFoodPaid) {
							if (!this.trainingBlocked) {
								this.constructionProgress += WarsmashConstants.SIMULATION_STEP_TIME * game.getCheatBuildSpeed();
							}
						} else {
							if ((this.buildQueueTypes[0] == QueueItemType.UNIT)
									|| (this.buildQueueTypes[0] == QueueItemType.HERO_REVIVE)) {
								// waits for the player's food to change, see retryQueuedFood
							} else if (this.buildQueueTypes[0] == QueueItemType.SACRIFICE) {
								this.queuedUnitFoodPaid = true;
							} else {
								this.queuedUnitFoodPaid = true;
								Log.warn(
										"Unpaid food for non unit queue item ???? Attempting to correct this by setting paid=true");
							}
						}
						if (this.buildQueueTypes[0] == QueueItemType.UNIT) {
							final CUnitType trainedUnitType = game.getUnitData().getUnitType(queuedRawcode);
							if (this.constructionProgress >= trainedUnitType.getBuildTime()) {
								this.constructionProgress = 0;
								final CUnit trainedUnit = game.createUnit(queuedRawcode, this.playerIndex, getX(),
										getY(), game.getGameplayConstants().getBuildingAngle());
								// dont add food cost to player 2x
								trainedUnit.setFoodUsed(trainedUnitType.getFoodUsed());
								final CPlayer player = game.getPlayer(this.playerIndex);
								player.setUnitFoodMade(trainedUnit, trainedUnitType.getFoodMade());
								player.removeTechtreeInProgress(game, queuedRawcode);
								player.addTechtreeUnlocked(game, queuedRawcode);
								player.scoreUnitGained(trainedUnitType);
								trainedUnit.placeTrainedUnit(game, this, this.rallyPoint);
								fireTrainFinishEvents(game, trainedUnit);
								game.unitTrainedEvent(this, trainedUnit);
								if (this.rallyPoint != null) {
									final int rallyOrderId = OrderIds.smart;
									this.rallyPoint.visit(UseAbilityOnTargetByIdVisitor.INSTANCE.reset(game,
											trainedUnit, trainedUnit.playerIndex, rallyOrderId));
								}
								shiftBuildQueue(game, 0);
								fireBuildQueueStartEvents(game);
								this.stateNotifier.queueChanged();
							}
						} else if (this.buildQueueTypes[0] == QueueItemType.SACRIFICE) {
							final CUnitType trainedUnitType = game.getUnitData().getUnitType(queuedRawcode);
							if (this.constructionProgress >= trainedUnitType.getBuildTime()) {
								this.constructionProgress = 0;
								final CUnit trainedUnit = game.createUnit(queuedRawcode, this.playerIndex, getX(),
										getY(), game.getGameplayConstants().getBuildingAngle());

								game.removeUnit(getWorker());

								// dont add food cost to player 2x
								trainedUnit.setFoodUsed(trainedUnitType.getFoodUsed());
								final CPlayer player = game.getPlayer(this.playerIndex);
								player.setUnitFoodMade(trainedUnit, trainedUnitType.getFoodMade());
								player.removeTechtreeInProgress(game, queuedRawcode);
								player.addTechtreeUnlocked(game, queuedRawcode);
								player.scoreUnitGained(trainedUnitType);
								trainedUnit.placeTrainedUnit(game, this, this.rallyPoint);
								fireTrainFinishEvents(game, trainedUnit);
								game.unitTrainedEvent(this, trainedUnit);
								if (this.rallyPoint != null) {
									final int rallyOrderId = OrderIds.smart;
									this.rallyPoint.visit(UseAbilityOnTargetByIdVisitor.INSTANCE.reset(game,
											trainedUnit, trainedUnit.playerIndex, rallyOrderId));
								}
								shiftBuildQueue(game, 0);
								fireBuildQueueStartEvents(game);
								this.stateNotifier.queueChanged();
							}
						} else if (this.buildQueueTypes[0] == QueueItemType.HERO_REVIVE) {
							final CUnit revivingHero = game.getUnit(queuedRawcode.getValue());
							final CUnitType trainedUnitType = revivingHero.getUnitType();
							final CGameplayConstants gameplayConstants = game.getGameplayConstants();
							if (this.constructionProgress >= heroReviveTime(game, revivingHero)) {
								this.constructionProgress = 0;
								revivingHero.reviveHeroAt(game, this, gameplayConstants.getHeroReviveLifeFactor(),
										gameplayConstants.getHeroReviveManaFactor(),
										gameplayConstants.getHeroReviveManaStart());
								// dont add food cost to player 2x
								revivingHero.setFoodUsed(trainedUnitType.getFoodUsed());
								final CPlayer player = game.getPlayer(this.playerIndex);
								player.setUnitFoodMade(revivingHero, trainedUnitType.getFoodMade());
								// NOTE: Dont "add techtree unlocked" here, because hero doesn't lose that
								// status upon death
								game.heroReviveEvent(this, revivingHero);
								if (this.rallyPoint != null) {
									final int rallyOrderId = OrderIds.smart;
									this.rallyPoint.visit(UseAbilityOnTargetByIdVisitor.INSTANCE.reset(game,
											revivingHero, revivingHero.playerIndex, rallyOrderId));
								}
								shiftBuildQueue(game, 0);
								fireBuildQueueStartEvents(game);
								this.stateNotifier.queueChanged();
							}
						} else if (this.buildQueueTypes[0] == QueueItemType.RESEARCH) {
							final CUpgradeType trainedUnitType = game.getUpgradeData().getType(queuedRawcode);
							// TODO the "getBuildTime" math below probably would be better served to have
							// been cached, for performance, since we are in the update method. But maybe it
							// doens't matter.
							final CPlayer player = game.getPlayer(this.playerIndex);
							final int techtreeUnlocked = player.getTechtreeUnlocked(queuedRawcode);
							if (this.constructionProgress >= trainedUnitType.getBuildTime(techtreeUnlocked)) {
								this.constructionProgress = 0;
								// off the queue first: a finished research takes back the queued items of its kind
								shiftBuildQueue(game, 0);
								player.removeTechtreeInProgress(game, queuedRawcode);
								player.addTechResearched(game, queuedRawcode, 1);
								fireResearchFinishEvents(game, queuedRawcode);
								game.researchFinishEvent(this, queuedRawcode,
										player.getTechtreeUnlocked(queuedRawcode));
								fireBuildQueueStartEvents(game);
								this.stateNotifier.queueChanged();
							}
						}
					}
					final float lifeBeforeRegen = this.life;
					final float manaBeforeRegen = this.mana;
					if ((this.life < this.maximumLife) || (this.currentLifeRegenPerTick < 0)) {
						final CRegenType lifeRegenType = getUnitType().getLifeRegenType();
						boolean active = false;
						switch (lifeRegenType) {
						case ALWAYS:
							active = true;
							break;
						case DAY:
							active = game.isDay();
							break;
						case NIGHT:
							active = game.isNight();
							break;
						case BLIGHT:
							active = PathingFlags.isPathingFlag(game.getPathingGrid().getPathing(getX(), getY()),
									PathingFlags.BLIGHTED);
							break;
						default:
							active = false;
						}
						if (active) {
							float lifePlusRegen = this.life + this.currentLifeRegenPerTick;
							if (lifePlusRegen > this.maximumLife) {
								lifePlusRegen = this.maximumLife;
							}
							setLife(game, lifePlusRegen);
						} else {
							float lifePlusRegen = (this.life + this.currentLifeRegenPerTick)
									- this.baseLifeRegenPerTick;
							if (lifePlusRegen > this.maximumLife) {
								lifePlusRegen = this.maximumLife;
							}
							setLife(game, lifePlusRegen);
						}
					}
					if ((this.mana < this.maximumMana) || (this.currentManaRegenPerTick < 0)) {
						float manaPlusRegen = this.mana + this.currentManaRegenPerTick;
						if (manaPlusRegen > this.maximumMana) {
							manaPlusRegen = this.maximumMana;
						}
						this.mana = Math.max(manaPlusRegen, 0);
						this.stateNotifier.manaChanged();
					}
					this.regenTick = game.getGameTurnTick();
					this.lifeRegenGiven = this.life - lifeBeforeRegen;
					this.lifeAfterRegen = this.life;
					this.manaRegenGiven = this.mana - manaBeforeRegen;
					this.manaAfterRegen = this.mana;
					if (!this.hidden && game.isNeutralAggressive(this.playerIndex)) {
						tryCreepCast(game);
					}
					// Autocast spells (Heal, Slow, Spell Steal) are also cast in the middle of a fight, not only
					// by idle units.
					if ((this.currentBehavior instanceof CBehaviorAttack) && this.autoCast
							&& (this.autocastAbility != null)
							&& ((game.getGameTurnTick() % AUTOCAST_COMBAT_SCAN_TICKS) == 0)) {
						autoAcquireAutocastTargets(game, this.moveDisabled);
					}
					if (this.currentBehavior != null) {
						final CUnit target = this.currentBehavior.visit(BehaviorTargetUnitVisitor.INSTANCE);
						if ((target != null) && !game.getPlayer(this.playerIndex).hasAlliance(target.getPlayerIndex(),
								CAllianceType.SHARED_VISION)) {
							if (this.attackFogMod.getPlayerIndex() != target.getPlayerIndex()) {
								game.getPlayer(this.attackFogMod.getPlayerIndex()).removeFogModifer(game,
										this.attackFogMod);
								this.attackFogMod.setPlayerIndex(target.getPlayerIndex());
								game.getPlayer(target.getPlayerIndex()).addFogModifer(game, this.attackFogMod, false);
							}
						} else {
							if (this.attackFogMod.getPlayerIndex() != this.playerIndex) {
								game.getPlayer(this.attackFogMod.getPlayerIndex()).removeFogModifer(game,
										this.attackFogMod);
								this.attackFogMod.setPlayerIndex(this.playerIndex);
							}
						}

						beginBehavior(game, this.currentBehavior.update(game));
					} else {
						// check to auto acquire targets
						autoAcquireTargets(game, this.moveDisabled);
					}
//					for (CAbility ability : new ArrayList<>(this.abilities)) {
//						ability.onTick(game, this);
//					}
					for (int i = this.abilities.size() - 1; i >= 0; i--) {
						// okay if it removes self from this during onTick() because of reverse
						// iteration order; a timed life running out kills the unit, which can take several
						if (i < this.abilities.size()) {
							this.abilities.get(i).onTick(game, this);
						}
					}
					final GlobalScope globalScope = game.getGlobalScope();
					for (int i = this.onTickTriggers.size() - 1; i >= 0; i--) {
						final Trigger trigger = this.onTickTriggers.get(i);
						final TriggerExecutionScope triggerExecutionScope = CommonTriggerExecutionScope
								.unitTriggerScope(null, trigger, this);
						trigger.fire(globalScope, triggerExecutionScope, triggerExecutionScope);
					}
				}
			} else if (!this.constructing) {
				// Paused units only allow passives to function. Buffs don't tick (except a few)
				// Base and bonus life/mana regen function, but regen from Str/Int doesn't
				if ((this.life < this.maximumLife)
						|| ((this.currentLifeRegenPerTick - this.lifeRegenStrengthBonus) < 0)) {
					final CRegenType lifeRegenType = getUnitType().getLifeRegenType();
					boolean active = false;
					switch (lifeRegenType) {
					case ALWAYS:
						active = true;
						break;
					case DAY:
						active = game.isDay();
						break;
					case NIGHT:
						active = game.isNight();
						break;
					case BLIGHT:
						active = PathingFlags.isPathingFlag(game.getPathingGrid().getPathing(getX(), getY()),
								PathingFlags.BLIGHTED);
						break;
					default:
						active = false;
					}
					if (active) {
						float lifePlusRegen = (this.life + this.currentLifeRegenPerTick)
								- (this.lifeRegenStrengthBonus * WarsmashConstants.SIMULATION_STEP_TIME);
						if (lifePlusRegen > this.maximumLife) {
							lifePlusRegen = this.maximumLife;
						}
						setLife(game, lifePlusRegen);
					}
				}
				if ((this.mana < this.maximumMana)
						|| ((this.currentManaRegenPerTick - this.manaRegenIntelligenceBonus) < 0)) {
					float manaPlusRegen = (this.mana + this.currentManaRegenPerTick)
							- (this.manaRegenIntelligenceBonus * WarsmashConstants.SIMULATION_STEP_TIME);
					if (manaPlusRegen > this.maximumMana) {
						manaPlusRegen = this.maximumMana;
					}
					this.mana = Math.max(manaPlusRegen, 0);
					this.stateNotifier.manaChanged();
				}

				// try to remove the below line of code, once all the abilities are on tick
				// triggers and none of them just magically assume tick to be called on all
				// abilities regardless of what they are (just add this stuff as global,
				// non-unit tick triggers)
				for (int i = this.abilities.size() - 1; i >= 0; i--) {
					// okay if it removes self from this during onTick() because of reverse
					// iteration order
					if ((i < this.abilities.size())
							&& ((this.abilities.get(i) instanceof AbilityGenericSingleIconPassiveAbility)
									|| (this.abilities.get(i) instanceof CPausedTickingBuff))) {
						this.abilities.get(i).onTick(game, this);
					}
				}
			}
		}
		return false;

	}

	private void popoutWorker(final CSimulation game) {
		if ((this.worker != null) && this.workerInside) {
			this.worker.endInsideInvulnerable(game);
			this.worker.setHidden(false);
			this.worker.setAcceptingOrders(true);
			this.worker.nudgeAround(game, this);
			if (this.constructionConsumesWorker) {
				game.getPlayer(this.worker.getPlayerIndex()).setUnitFoodUsed(this.worker,
						this.worker.getUnitType().getFoodUsed());
			}
		}
		this.worker = null;
	}

	public boolean autoAcquireTargets(final CSimulation game, final boolean disableMove) {
		return autoAcquireTargets(game, disableMove, true);
	}

	private boolean autoAcquireTargets(final CSimulation game, final boolean disableMove, final boolean attack) {
		// Cinematic maps park hostile units hidden among the player's (ShowUnit false); hidden units carry out
		// script orders but never pick targets themselves.
		if (this.hidden) {
			return false;
		}
		boolean autocast = false;
		final int tick = game.getGameTurnTick();
		if (this.autoCast && (this.autocastAbility != null) && (tick >= this.nextIdleAutocastScanTick)) {
			autocast = autoAcquireAutocastTargets(game, disableMove);
			if (!autocast) {
				final int cooldownTicks = autocastCooldownTicks(game);
				this.nextIdleAutocastScanTick = tick
						+ ((cooldownTicks > 0) ? Math.min(cooldownTicks, AUTOCAST_IDLE_RESCAN_TICKS) : AUTOCAST_IDLE_RESCAN_TICKS);
			}
		}
		if (!autocast) {
			if (attack && this.autoAttack) {
				return autoAcquireAttackTargets(game, disableMove);
			}
		}
		return autocast;
	}

	/**
	 * {@link #autoAcquireTargets} for a unit standing idle or holding its position: it looks for something to attack
	 * only when its scan is due.
	 */
	public boolean autoAcquireIdleTargets(final CSimulation game, final boolean disableMove) {
		final int tick = game.getGameTurnTick();
		final boolean due = tick >= this.nextIdleAcquireScanTick;
		if (due) {
			this.nextIdleAcquireScanTick = tick + IDLE_ACQUIRE_SCAN_TICKS;
		}
		return autoAcquireTargets(game, disableMove, due);
	}

	/**
	 * Measured (1.29): a creep with spells starts its fight, and so thinks of its spells, as soon as an enemy is sent to
	 * attack it; without spells, or when an enemy only comes near, it looks at its own phase.
	 */
	private void onAttackOrdered(final CSimulation game, final CUnit attacker) {
		if ((attacker != this) && game.isNeutralAggressive(this.playerIndex) && !isDead()
				&& !game.getPlayer(this.playerIndex).hasAlliance(attacker.playerIndex, CAllianceType.PASSIVE)) {
			for (final CAbility ability : this.abilities) {
				if (!ability.isDisabled() && CCreepCastAbility.isCreepSpell(ability)) {
					this.nextIdleAcquireScanTick = Math.min(this.nextIdleAcquireScanTick, game.getGameTurnTick());
					return;
				}
			}
		}
	}

	/** A new unit's first scan, 1 step to a whole scan period away, spread over units by their handle. */
	private int firstIdleAcquireScanDelay() {
		int hash = getHandleId() * 0x9E3779B1;
		hash ^= hash >>> 16;
		return 1 + Math.floorMod(hash, IDLE_ACQUIRE_SCAN_TICKS);
	}

	/**
	 * Creeps cast their spells by themselves while they fight (Devour, Animate Dead, War Stomp, Bloodlust, Slow...).
	 */
	private void tryCreepCast(final CSimulation game) {
		CBehavior behavior = this.currentBehavior;
		// The walk towards the unit it attacks counts too: a Doom Guard rains fire before the fight starts.
		if (behavior instanceof CBehaviorMove) {
			behavior = ((CBehaviorMove) behavior).getRangedBehavior();
		}
		if (!(behavior instanceof CBehaviorAttack)) {
			this.creepCastFighting = false;
			if (this.currentBehavior == this.defaultBehavior) {
				for (final CAbility ability : this.abilities) {
					if (!ability.isDisabled() && CCreepCastAbility.tryIdleCreepCast(game, this, ability)) {
						return;
					}
				}
			}
			return;
		}
		final CUnit target = behavior.visit(BehaviorTargetUnitVisitor.INSTANCE);
		if ((target == null) || target.isDead()) {
			return;
		}
		final int tick = game.getGameTurnTick();
		if (!this.creepCastFighting) {
			this.creepCastThinks = -1;
		}
		final boolean think = !this.creepCastFighting || (tick >= this.nextCreepCastTick);
		if (think) {
			this.creepCastFighting = true;
			this.nextCreepCastTick = tick + CREEP_CAST_INTERVAL_TICKS;
			this.creepCastThinks++;
		}
		final boolean inAttackRange = ((CBehaviorAttack) behavior).isWithinRange(game);
		for (final CAbility ability : this.abilities) {
			if (ability.isDisabled()) {
				continue;
			}
			if (think && (ability instanceof CCreepCastAbility)
					&& ((CCreepCastAbility) ability).tryCreepCast(game, this, target, inAttackRange)) {
				return;
			}
			if (CCreepCastAbility.tryCreepCastRule(game, this, ability, target, think, this.creepCastThinks)) {
				return;
			}
		}
	}

	private int autocastCooldownTicks(final CSimulation game) {
		if (this.autocastAbility instanceof AbilityBuilderAbility) {
			return (int) ((AbilityBuilderAbility) this.autocastAbility).getCooldownRemainingTicks(game, this);
		}
		if (this.autocastAbility instanceof CAbilitySpellBase) {
			return getCooldownRemainingTicks(game, ((CAbilitySpellBase) this.autocastAbility).getCode());
		}
		return 0;
	}

	public boolean autoAcquireAutocastTargets(final CSimulation game, final boolean disableMove) {
		if ((this.autocastAbility != null) && !this.autocastAbility.isDisabled()
				&& this.autocastAbility.getAutocastType() != AutocastType.NOAUTOCASTBEHAVIOR) {
			if (this.autocastAbility.getAutocastType() == AutocastType.NOTARGET) {
				final BooleanAbilityTargetCheckReceiver<Void> booleanTargetReceiver = BooleanAbilityTargetCheckReceiver
						.<Void>getInstance().reset();
				this.autocastAbility.checkCanAutoTargetNoTarget(game, this, this.playerIndex,
						this.autocastAbility.getBaseOrderId(), booleanTargetReceiver);
				if (booleanTargetReceiver.isTargetable()) {
					return this.order(game, this.autocastAbility.getBaseOrderId(), null, true);
				}
			} else if (this.autocastAbility.getAutocastType() != AutocastType.NONE) {
				if (this.collisionRectangle != null) {
					tempRect.set(this.collisionRectangle);
				} else {
					tempRect.set(getX(), getY(), 0, 0);
				}
				final float halfSize = AUTOCAST_SEARCH_RANGE;
				tempRect.x -= halfSize;
				tempRect.y -= halfSize;
				tempRect.width += halfSize * 2;
				tempRect.height += halfSize * 2;
				game.getWorldCollision().enumUnitsInRect(tempRect,
						autocastTargetFinderEnum.reset(game, this, this.autocastAbility, disableMove));
				if (autocastTargetFinderEnum.currentUnitTarget != null) {
					this.order(game, this.autocastAbility.getBaseOrderId(), autocastTargetFinderEnum.currentUnitTarget,
							true);
					return true;
				}
			}
		}
		return false;
	}

	public boolean autoAcquireAttackTargets(final CSimulation game, final boolean disableMove) {
		if (!getCurrentAttacks().isEmpty() && !this.classifications.contains(CUnitClassification.PEON)) {
			if (this.collisionRectangle != null) {
				tempRect.set(this.collisionRectangle);
			} else {
				tempRect.set(getX(), getY(), 0, 0);
			}
			final float halfSize = this.acquisitionRange;
			tempRect.x -= halfSize;
			tempRect.y -= halfSize;
			tempRect.width += halfSize * 2;
			tempRect.height += halfSize * 2;
			if (this.priorityAttackTargets != null) {
				game.getWorldCollision().enumUnitsInRect(tempRect,
						autoAttackTargetFinderEnum.reset(game, this, disableMove, this.priorityAttackTargets));
				if (autoAttackTargetFinderEnum.attackNearest()) {
					return true;
				}
			}
			game.getWorldCollision().enumUnitsInRect(tempRect,
					autoAttackTargetFinderEnum.reset(game, this, disableMove, null));
			return autoAttackTargetFinderEnum.attackNearest();
		}
		return false;
	}

	/**
	 * Turns to an enemy of the kinds this unit prefers when one is in acquisition range; false if there is none.
	 */
	public boolean acquirePriorityAttackTarget(final CSimulation game, final boolean disableMove) {
		if ((this.priorityAttackTargets == null) || getCurrentAttacks().isEmpty()) {
			return false;
		}
		if (this.collisionRectangle != null) {
			tempRect.set(this.collisionRectangle);
		} else {
			tempRect.set(getX(), getY(), 0, 0);
		}
		final float halfSize = this.acquisitionRange;
		tempRect.x -= halfSize;
		tempRect.y -= halfSize;
		tempRect.width += halfSize * 2;
		tempRect.height += halfSize * 2;
		game.getWorldCollision().enumUnitsInRect(tempRect,
				autoAttackTargetFinderEnum.reset(game, this, disableMove, this.priorityAttackTargets));
		return autoAttackTargetFinderEnum.attackNearest();
	}

	/**
	 * Kinds of enemies (air, ground, structure) this unit acquires first when it looks for something to attack;
	 * null for no preference (Prioritize).
	 */
	public void setPriorityAttackTargets(final EnumSet<CTargetType> priorityAttackTargets) {
		this.priorityAttackTargets = priorityAttackTargets;
	}

	public EnumSet<CTargetType> getPriorityAttackTargets() {
		return this.priorityAttackTargets;
	}

	public static boolean isPriorityAttackTarget(final CUnit unit, final EnumSet<CTargetType> priorityTargets) {
		return (priorityTargets.contains(CTargetType.AIR) && unit.isUnitType(CUnitTypeJass.FLYING))
				|| (priorityTargets.contains(CTargetType.GROUND) && !unit.isBuilding()
						&& unit.isUnitType(CUnitTypeJass.GROUND))
				|| (priorityTargets.contains(CTargetType.STRUCTURE) && unit.isBuilding());
	}

	public float getEndingDecayTime(final CSimulation game) {
		if (isBuilding()) {
			return game.getGameplayConstants().getStructureDecayTime();
		}
		if (this.unitType.isHero()) {
			return game.getGameplayConstants().getDissipateTime();
		}
		return game.getGameplayConstants().getBoneDecayTime();
	}

	public void order(final CSimulation game, final COrder order, final boolean queue) {
		this.order(game, order, queue, false);
	}

	public void order(final CSimulation game, final COrder order, final boolean queue, final boolean autoOrder) {
		if (isDead()) {
			return;
		}

		if (order != null) {
			final CAbility ability = game.getAbility(order.getAbilityHandleId());

			if (ability != null) {
				if (!getAbilities().contains(ability)) {
					// not allowed to use ability of other unit...
					return;
				}
				// Allow the ability to response to the order without actually placing itself in
				// the queue, nor modifying (interrupting) the queue.
				if (!ability.checkBeforeQueue(game, this, order.getPlayerIndex(), order.getOrderId(), autoOrder, order.getTarget(game))) {
					// TODO is this a possible bug vector that the network request doesn't
					// checkCanUse like the UI before checkBeforeQueue is called??
					order.fireEvents(game, this);
					this.stateNotifier.ordersChanged();
					return;
				}
			}
		}

		if ((this.lastStartedOrder != null) && this.lastStartedOrder.equals(order)
				&& (this.lastStartedOrder.getOrderId() == OrderIds.smart)) {
			// I skip your spammed move orders, TODO this will probably break some repeat
			// attack order or something later
			return;
		}
		if (!queue && (!this.acceptingOrders
				|| ((this.currentBehavior != null) && !this.currentBehavior.interruptable()))) {
			for (final COrder queuedOrder : this.orderQueue) {
				if (queuedOrder != null) {
					final int abilityHandleId = queuedOrder.getAbilityHandleId();
					final CAbility ability = game.getAbility(abilityHandleId);
					if (ability != null) {
						ability.onCancelFromQueue(game, this, queuedOrder.getPlayerIndex(), queuedOrder.getOrderId());
					}
				}
			}
			this.orderQueue.clear();
			this.orderQueue.add(order);
			this.stateNotifier.ordersChanged();
			this.stateNotifier.waypointsChanged();
		} else if (queue && (this.currentBehavior != this.stopBehavior)
				&& (this.currentBehavior != this.holdPositionBehavior)) {
			if (order.getOrderId() == OrderIds.patrol) {
				if (this.defaultBehavior == this.patrolBehavior) {
					this.patrolBehavior.addPatrolPoint(order.getTarget(game));
				} else {
					this.orderQueue.add(order);
				}
			} else {
				this.orderQueue.add(order);
			}
			this.stateNotifier.waypointsChanged();
		} else {
			this.nextIdleAcquireScanTick = game.getGameTurnTick();
			if ((order != null)
					&& ((order.getOrderId() == OrderIds.attack) || (order.getOrderId() == OrderIds.smart))) {
				final AbilityTarget orderTarget = order.getTarget(game);
				final CUnit orderTargetUnit = (orderTarget != null) ? orderTarget.visit(AbilityTargetVisitor.UNIT)
						: null;
				if (orderTargetUnit != null) {
					orderTargetUnit.onAttackOrdered(game, this);
				}
			}
			setDefaultBehavior(this.stopBehavior);
			final boolean walking = (this.moveBehavior != null) && (this.currentBehavior == this.moveBehavior);
			final CRangedBehavior walkingFor = walking ? this.moveBehavior.getRangedBehavior() : null;
			final int moveResets = walking ? this.moveBehavior.getResetCount() : 0;
			final CBehavior nextBehavior = beginOrder(game, order);
			if (walking && (this.moveBehavior.getResetCount() != moveResets)) {
				// The new order took over the move the unit was on (one move behaviour per unit), so it is never
				// ended; the behaviour it was walking for still has to learn it was interrupted (a build pays back).
				if ((walkingFor != null) && (this.moveBehavior.getRangedBehavior() != walkingFor)) {
					walkingFor.endMove(game, true);
				}
				// Ending that move below must not tell the new order it was interrupted: a build order whose site is
				// already in reach would pay its cost back before it starts.
				if (nextBehavior != this.moveBehavior) {
					this.moveBehavior.takenOver();
				}
			}
			beginBehavior(game, nextBehavior, true);
			for (final COrder queuedOrder : this.orderQueue) {
				if (queuedOrder != null) {
					final int abilityHandleId = queuedOrder.getAbilityHandleId();
					final CAbility ability = game.getAbility(abilityHandleId);
					ability.onCancelFromQueue(game, this, queuedOrder.getPlayerIndex(), queuedOrder.getOrderId());
				}
			}
			this.orderQueue.clear();
			this.stateNotifier.ordersChanged();
			this.stateNotifier.waypointsChanged();
			startCastInOrder(game, nextBehavior);
		}
	}

	/**
	 * Measured on 1.29: a cast that needs no turn starts inside the order call (its channel event fires before the
	 * order returns), and one that turns counts its turn ticks from the order's own time, not from the next step.
	 */
	private void startCastInOrder(final CSimulation game, final CBehavior behavior) {
		if (isDead() || this.paused || this.constructing || (this.currentBehavior != behavior)
				|| !((behavior instanceof CBehaviorTargetSpellBase) || (behavior instanceof CBehaviorNoTargetSpellBase)
						|| (behavior instanceof CBehaviorAbilityBuilderNoTarget))) {
			return;
		}
		final CBehavior next = behavior.update(game);
		if ((next != behavior) && (this.currentBehavior == behavior)) {
			beginBehavior(game, next);
		}
	}

	public boolean order(final CSimulation simulation, final int orderId, final AbilityTarget target) {
		return this.order(simulation, orderId, target, false);
	}

	public boolean order(final CSimulation simulation, final int orderId, final AbilityTarget target,
			final boolean autoOrder) {
		if (orderId == OrderIds.stop) {
			order(simulation, new COrderNoTarget(this.playerIndex, 0, orderId, false), false, autoOrder);
			return true;
		}
		if (this.removedFromGame) {
			return false;
		}
		for (final CAbility ability : this.abilities) {
			// An order built on an ability the simulation no longer knows comes straight back here (COrder*.begin).
			if (simulation.getAbility(ability.getHandleId()) != ability) {
				continue;
			}
			final BooleanAbilityActivationReceiver activationReceiver = BooleanAbilityActivationReceiver.INSTANCE.reset();
			ability.checkCanUse(simulation, this, this.playerIndex, orderId, false, activationReceiver);
			if (activationReceiver.isOk()) {
				if (target == null) {
					final BooleanAbilityTargetCheckReceiver<Void> booleanTargetReceiver = BooleanAbilityTargetCheckReceiver
							.<Void>getInstance().reset();
					ability.checkCanTargetNoTarget(simulation, this, this.playerIndex, orderId, false, booleanTargetReceiver);
					if (booleanTargetReceiver.isTargetable()) {
						order(simulation, new COrderNoTarget(this.playerIndex, ability.getHandleId(), orderId, false, autoOrder), false,
								autoOrder);
						return true;
					}
				} else {
					final boolean targetable = target.visit(new AbilityTargetVisitor<Boolean>() {
						@Override
						public Boolean accept(final AbilityPointTarget target) {
							final BooleanAbilityTargetCheckReceiver<AbilityPointTarget> booleanTargetReceiver = BooleanAbilityTargetCheckReceiver
									.<AbilityPointTarget>getInstance().reset();
							ability.checkCanTarget(simulation, CUnit.this, CUnit.this.playerIndex, orderId, false, target,
									booleanTargetReceiver);
							final boolean pointTargetable = booleanTargetReceiver.isTargetable();
							if (pointTargetable) {
								order(simulation,
										new COrderTargetPoint(CUnit.this.playerIndex, ability.getHandleId(), orderId, target, false, autoOrder),
										false, autoOrder);
							}
							return pointTargetable;
						}

						public Boolean acceptWidget(final CWidget target) {
							final BooleanAbilityTargetCheckReceiver<CWidget> booleanTargetReceiver = BooleanAbilityTargetCheckReceiver
									.<CWidget>getInstance().reset();
							ability.checkCanTarget(simulation, CUnit.this, CUnit.this.playerIndex, orderId, autoOrder, target,
									booleanTargetReceiver);
							final boolean widgetTargetable = booleanTargetReceiver.isTargetable();
							if (widgetTargetable) {
								order(simulation, new COrderTargetWidget(CUnit.this.playerIndex, ability.getHandleId(), orderId,
										target.getHandleId(), false, autoOrder), false, autoOrder);
							}
							return widgetTargetable;
						}

						@Override
						public Boolean accept(final CUnit target) {
							return acceptWidget(target);
						}

						@Override
						public Boolean accept(final CDestructable target) {
							return acceptWidget(target);
						}

						@Override
						public Boolean accept(final CItem target) {
							return acceptWidget(target);
						}
					});
					if (targetable) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private CBehavior beginOrder(final CSimulation game, final COrder order) {
		this.lastStartedOrder = order;
		CBehavior nextBehavior;
		if (order != null) {
			nextBehavior = order.begin(game, this);
		} else {
			nextBehavior = this.defaultBehavior;
		}
		return nextBehavior;
	}

	public CBehavior getCurrentBehavior() {
		return this.currentBehavior;
	}

	public List<CAbility> getAbilities() {
		return this.abilities;
	}

	public <T> T getAbility(final CAbilityVisitor<T> visitor) {
		for (int i = 0, n = this.abilities.size(); i < n; i++) {
			final CAbility ability = this.abilities.get(i);
			final T visited = ability.visit(visitor);
			if (visited != null) {
				return visited;
			}
		}
		return null;
	}

	public <T extends CAbility> T getFirstAbilityOfType(final Class<T> cAbilityClass) {
		for (int i = 0, n = this.abilities.size(); i < n; i++) {
			final CAbility ability = this.abilities.get(i);
			if (cAbilityClass.isAssignableFrom(ability.getClass())) {
				return (T) ability;
			}
		}
		return null;
	}

	public void setCooldownEndTime(final int cooldownEndTime) {
		this.cooldownEndTime = cooldownEndTime;
	}

	public int getCooldownEndTime() {
		return this.cooldownEndTime;
	}

	@Override
	public float getFlyHeight() {
		return this.flyHeight;
	}

	public void setFlyHeight(final float flyHeight) {
		this.flyHeight = flyHeight;
	}

	public float getTurnRate() {
		return this.turnRate;
	}

	public void setTurnRate(final float turnRate) {
		this.turnRate = turnRate;
	}

	public float getPropWindow() {
		return this.propWindow;
	}

	public void setPropWindow(final float propWindow) {
		this.propWindow = propWindow;
	}

	public void markWalked(final int gameTurnTick) {
		this.lastWalkTick = gameTurnTick;
	}

	/**
	 * Whether the unit stepped forward along a move in the last ticks. Pathing goes through walking units (they will be
	 * gone by the time it gets there); a unit that waited a few ticks counts as standing.
	 */
	public boolean isWalking(final int gameTurnTick) {
		return (gameTurnTick - this.lastWalkTick) <= 2;
	}

	public int getPlayerIndex() {
		return this.playerIndex;
	}

	public void setPlayerIndex(final CSimulation simulation, final int playerIndex, final boolean changeColor) {
		this.playerIndex = playerIndex;
		if (changeColor) {
			simulation.changeUnitColor(this, playerIndex);
		}
		this.computeUnitState(simulation, StateModBuffType.FALSE_OWNERSHIP);
	}

	/**
	 * Gives the unit to another player as SetUnitOwner, Charm, Possession and Control Magic do. Measured on 1.29
	 * (unit-change-owner): food used and food made move with the unit, the order is dropped, and the change fires
	 * EVENT_UNIT_CHANGE_OWNER and the new owner's EVENT_PLAYER_UNIT_CHANGE_OWNER at once; giving a unit to its own
	 * player fires nothing.
	 */
	public void changeOwner(final CSimulation simulation, final int newPlayerIndex, final boolean changeColor) {
		final int previousPlayerIndex = this.playerIndex;
		if (newPlayerIndex == previousPlayerIndex) {
			if (changeColor) {
				simulation.changeUnitColor(this, newPlayerIndex);
			}
			return;
		}
		final CPlayer previousPlayer = simulation.getPlayer(previousPlayerIndex);
		final CPlayer newPlayer = simulation.getPlayer(newPlayerIndex);
		final boolean alive = !isDead();
		if (alive) {
			for (final War3ID upgradeUsed : this.unitType.getUpgradesUsed()) {
				final CUpgradeType upgradeType = simulation.getUpgradeData().getType(upgradeUsed);
				if ((upgradeType != null) && upgradeType.isTransferWithUnitOwnership()) {
					final int previousLevel = previousPlayer.getTechtreeUnlocked(upgradeUsed);
					if (previousLevel > newPlayer.getTechtreeUnlocked(upgradeUsed)) {
						newPlayer.setTechResearched(simulation, upgradeUsed, previousLevel);
					}
				}
			}
		}
		simulation.getUnitData().unapplyPlayerUpgradesToUnit(simulation, previousPlayerIndex, this.unitType, this);
		final int foodUsed = this.foodUsed;
		final int foodMade = this.foodMade;
		previousPlayer.setUnitFoodUsed(this, 0);
		previousPlayer.setUnitFoodMade(this, 0);
		final boolean hero = simulation.getPlayerHeroes(previousPlayerIndex).remove(this);
		if (alive) {
			if (this.constructing) {
				previousPlayer.removeTechtreeInProgress(simulation, this.unitType.getTypeId());
			}
			else {
				previousPlayer.removeTechtreeUnlocked(simulation, this.unitType.getTypeId());
				for (final War3ID upgradedFromTypeId : this.upgradedFromTypeIds) {
					previousPlayer.removeTechtreeUnlocked(simulation, upgradedFromTypeId);
				}
			}
		}
		setPlayerIndex(simulation, newPlayerIndex, changeColor);
		if (alive) {
			if (this.constructing) {
				newPlayer.addTechtreeInProgress(simulation, this.unitType.getTypeId());
			}
			else {
				newPlayer.addTechtreeUnlocked(simulation, this.unitType.getTypeId());
				for (final War3ID upgradedFromTypeId : this.upgradedFromTypeIds) {
					newPlayer.addTechtreeUnlocked(simulation, upgradedFromTypeId);
				}
			}
		}
		if (hero) {
			simulation.getPlayerHeroes(newPlayerIndex).add(this);
		}
		simulation.getUnitData().applyPlayerUpgradesToUnit(simulation, newPlayerIndex, this.unitType, this);
		newPlayer.setUnitFoodUsed(this, foodUsed);
		newPlayer.setUnitFoodMade(this, foodMade);
		if (alive) {
			// 1.29 (unit-change-owner): the unit is idle at once, GetUnitCurrentOrder reads 0 and not stop.
			order(simulation, (COrder) null, false);
		}
		fireChangeOwnerEvents(simulation, previousPlayer, newPlayer);
	}

	private void fireChangeOwnerEvents(final CSimulation simulation, final CPlayer previousPlayer,
			final CPlayer newPlayer) {
		final List<CWidgetEvent> eventList = getEventList(JassGameEventsWar3.EVENT_UNIT_CHANGE_OWNER);
		if (eventList != null) {
			for (int i = eventList.size() - 1; i >= 0; i--) {
				final CWidgetEvent event = eventList.get(i);
				event.fire(this, CommonTriggerExecutionScope.unitChangeOwnerScope(
						JassGameEventsWar3.EVENT_UNIT_CHANGE_OWNER, event.getTrigger(), this, newPlayer,
						previousPlayer));
			}
		}
		newPlayer.fireUnitChangeOwnerEvents(this, previousPlayer);
	}

	public void setRescuable(final int byPlayerIndex, final boolean rescuable) {
		final int bit = 1 << byPlayerIndex;
		if (rescuable) {
			this.rescuableByPlayers |= bit;
			this.unrescuableByPlayers &= ~bit;
		}
		else {
			this.unrescuableByPlayers |= bit;
			this.rescuableByPlayers &= ~bit;
		}
	}

	public void setRescueRange(final float rescueRange) {
		this.rescueRange = rescueRange;
	}

	/**
	 * A unit of a rescuable-controlled player is rescuable by the players it has ALLIANCE_RESCUABLE towards;
	 * SetUnitRescuable turns that on or off for one unit and one player, also for units of other players.
	 */
	public boolean isRescuableBy(final CSimulation simulation, final int byPlayerIndex) {
		final int bit = 1 << byPlayerIndex;
		if ((this.unrescuableByPlayers & bit) != 0) {
			return false;
		}
		if ((this.rescuableByPlayers & bit) != 0) {
			return true;
		}
		final CPlayer owner = simulation.getPlayer(this.playerIndex);
		return (owner.getController() == CMapControl.RESCUABLE)
				&& owner.hasAlliance(byPlayerIndex, CAllianceType.RESCUABLE);
	}

	/**
	 * Measured on 1.29 (unit-rescue): the first tick a ground unit of a rescuing player stands within the rescue range
	 * (between 414 and 415 apart by default, also for larger units; flying units rescue nothing) the unit fires
	 * EVENT_UNIT_RESCUED and its owner's EVENT_PLAYER_UNIT_RESCUED. The ownership change itself is Blizzard.j's
	 * (RescueUnitBJ), and a rescued unit rescues the ones near it in turn.
	 */
	void checkRescue(final CSimulation simulation) {
		if ((this.rescuableByPlayers == 0)
				&& (simulation.getPlayer(this.playerIndex).getController() != CMapControl.RESCUABLE)) {
			return;
		}
		if (isDead() || this.hidden || isRemovedFromGame()
				|| ((simulation.getGameTurnTick() - this.lastRescueTick) < RESCUE_REPEAT_TICKS)) {
			return;
		}
		final float range = this.rescueRange;
		final float x = getX();
		final float y = getY();
		final CUnit[] rescuer = { null };
		simulation.getWorldCollision().enumUnitsInRect(new Rectangle(x - range, y - range, range * 2, range * 2),
				(enumUnit) -> {
					if ((enumUnit.playerIndex != this.playerIndex) && !enumUnit.isDead() && !enumUnit.hidden
							&& !enumUnit.isRemovedFromGame()
							&& !enumUnit.isBuilding() && !enumUnit.isUnitType(CUnitTypeJass.FLYING)
							&& (StrictMath.hypot(enumUnit.getX() - x, enumUnit.getY() - y) <= range) && isRescuableBy(simulation, enumUnit.playerIndex)) {
						rescuer[0] = enumUnit;
						return true;
					}
					return false;
				});
		if (rescuer[0] == null) {
			return;
		}
		this.lastRescueTick = simulation.getGameTurnTick();
		final CPlayer owner = simulation.getPlayer(this.playerIndex);
		final List<CWidgetEvent> eventList = getEventList(JassGameEventsWar3.EVENT_UNIT_RESCUED);
		if (eventList != null) {
			for (int i = eventList.size() - 1; i >= 0; i--) {
				final CWidgetEvent event = eventList.get(i);
				event.fire(this, CommonTriggerExecutionScope.unitRescuedScope(JassGameEventsWar3.EVENT_UNIT_RESCUED,
						event.getTrigger(), this, rescuer[0], owner));
			}
		}
		owner.fireUnitRescuedEvents(this, rescuer[0]);
	}

	public int getFakePlayerIndex() {
		return this.fakePlayerIndex;
	}

	public CUnitType getUnitType() {
		return this.unitType;
	}

	public void setCollisionRectangle(final Rectangle collisionRectangle) {
		this.collisionRectangle = collisionRectangle;
	}

	public Rectangle getCollisionRectangle() {
		return this.collisionRectangle;
	}

	public void setX(final float newX, final CWorldCollision collision, final CRegionManager regionManager) {
		final float prevX = getX();
		setX(newX);
		collision.translate(this, newX - prevX, 0);
		checkRegionEvents(regionManager);
	}

	public void setY(final float newY, final CWorldCollision collision, final CRegionManager regionManager) {
		final float prevY = getY();
		setY(newY);
		collision.translate(this, 0, newY - prevY);
		checkRegionEvents(regionManager);
	}

	public void setPointAndCheckUnstuck(final float newX, final float newY, final CSimulation game) {
		if (this.removedFromGame) {
			// Measured (1.29): moving a removed unit does nothing.
			return;
		}
		final CWorldCollision collision = game.getWorldCollision();
		final PathingGrid pathingGrid = game.getPathingGrid();
		;
		if (this.pathingDisabled) {
			setPoint(newX, newY, collision, game.getRegionManager());
			game.unitRepositioned(this);
			return;
		}
		float outputX = newX, outputY = newY;
		int checkX = 0;
		int checkY = 0;
		float collisionSize;
		if (isBuilding() && (this.unitType.getBuildingPathingPixelMap() != null)) {
			tempRect.setSize(this.unitType.getBuildingPathingPixelMap().getWidth() * 32,
					this.unitType.getBuildingPathingPixelMap().getHeight() * 32);
			collisionSize = tempRect.getWidth() / 2;
		} else if (this.collisionRectangle != null) {
			tempRect.set(this.collisionRectangle);
			collisionSize = this.unitType.getCollisionSize();
		} else {
			tempRect.setSize(16, 16);
			collisionSize = this.unitType.getCollisionSize();
		}
		if (!isBuilding()) {
			final float[] free = findNearestFreeCell(newX, newY, collision, pathingGrid, collisionSize, getMovementType());
			setPoint(free[0], free[1], collision, game.getRegionManager());
			game.unitRepositioned(this);
			return;
		}
		for (int i = 0; i < 300; i++) {
			final float centerX = newX + (checkX * 64);
			final float centerY = newY + (checkY * 64);
			tempRect.setCenter(centerX, centerY);
			if (!collision.intersectsAnythingOtherThan(tempRect, this, getMovementType(), false)
					&& pathingGrid.isPathable(centerX, centerY, getMovementType(), collisionSize)) {
				outputX = centerX;
				outputY = centerY;
				break;
			}
			final double angle = (((int) Math.floor(Math.sqrt((4 * i) + 1)) % 4) * Math.PI) / 2;
			checkX -= (int) StrictMath.cos(angle);
			checkY -= (int) StrictMath.sin(angle);
		}
		setPoint(outputX, outputY, collision, game.getRegionManager());
		game.unitRepositioned(this);
	}

	/**
	 * Puts a flyer forced to the ground (Web) where a land unit of its size could stand: it stays when its own place
	 * fits one, otherwise it goes to the first free cell of the spiral from fromX, fromY.
	 */
	public void placeOnGround(final CSimulation game, final float fromX, final float fromY) {
		final CWorldCollision collision = game.getWorldCollision();
		final PathingGrid pathingGrid = game.getPathingGrid();
		final float collisionSize = this.unitType.getCollisionSize();
		// Measured with a Gryphon Rider (collision 8) next to a Crypt Fiend (32): a cell 48 from the Fiend on an axis
		// is refused, 68 and 73 are taken, so the flyer counts as 32 here, not 8.
		final float size = Math.max(64f, collisionSize * 2);
		tempRect.setSize(size, size);
		if (fitsAt(getX(), getY(), collision, pathingGrid, collisionSize, MovementType.FOOT)) {
			return;
		}
		final float[] free = findNearestFreeCell(fromX, fromY, collision, pathingGrid, collisionSize,
				MovementType.FOOT);
		setPoint(free[0], free[1], collision, game.getRegionManager());
		game.unitRepositioned(this);
	}

	/**
	 * Where this (non-building) unit would stand if put at the point, without moving it.
	 */
	public float[] findFreePoint(final float x, final float y, final CSimulation game) {
		if (this.pathingDisabled || isBuilding()) {
			return new float[] { x, y };
		}
		if (this.collisionRectangle != null) {
			tempRect.set(this.collisionRectangle);
		}
		else {
			tempRect.setSize(16, 16);
		}
		return findNearestFreeCell(x, y, game.getWorldCollision(), game.getPathingGrid(),
				this.unitType.getCollisionSize(), getMovementType());
	}

	private boolean fitsAt(final float x, final float y, final CWorldCollision collision,
			final PathingGrid pathingGrid, final float collisionSize, final MovementType movementType) {
		tempRect.setCenter(x, y);
		return !collision.intersectsAnythingOtherThan(tempRect, this, movementType, false)
				&& pathingGrid.isPathable(x, y, movementType, collisionSize);
	}

	/**
	 * A unit that cannot stand where it is put keeps that point when it is free; otherwise it goes to the centre of the
	 * first free 32 pathing cell along the square spiral the original game uses (from the point's own cell: down 1,
	 * right 1, up 2, left 2, down 3, ...), which is not always the nearest one. Measured: the original tests the
	 * pathing map at the cell's lower left corner but other units at the cell centre.
	 */
	private float[] findNearestFreeCell(final float x, final float y, final CWorldCollision collision,
			final PathingGrid pathingGrid, final float collisionSize, final MovementType movementType) {
		if (fitsAt(x, y, collision, pathingGrid, collisionSize, movementType)) {
			return new float[] { x, y };
		}
		final float baseX = (float) Math.floor(x / 32f) * 32f;
		final float baseY = (float) Math.floor(y / 32f) * 32f;
		final int[] stepX = { 0, 1, 0, -1 };
		final int[] stepY = { -1, 0, 1, 0 };
		int cellX = 0;
		int cellY = 0;
		int direction = 0;
		for (int legLength = 1; legLength <= 128; legLength++) {
			for (int leg = 0; leg < 2; leg++) {
				for (int step = 0; step < legLength; step++) {
					final float cx = baseX + (cellX * 32f);
					final float cy = baseY + (cellY * 32f);
					tempRect.setCenter(cx + 16f, cy + 16f);
					if (!collision.intersectsAnythingOtherThan(tempRect, this, movementType, false)
							&& pathingGrid.isPathable(cx, cy, movementType, collisionSize)) {
						return new float[] { cx + 16f, cy + 16f };
					}
					cellX += stepX[direction];
					cellY += stepY[direction];
				}
				direction = (direction + 1) % 4;
			}
		}
		return new float[] { x, y };
	}

	public void setPoint(final float newX, final float newY, final CWorldCollision collision,
			final CRegionManager regionManager) {
		final float prevX = getX();
		final float prevY = getY();
		setX(newX);
		setY(newY);
		collision.translate(this, newX - prevX, newY - prevY);
		checkRegionEvents(regionManager);
	}

	private void checkRegionEvents(final CRegionManager regionManager) {
		final Set<CRegion> temp = this.containingRegions;
		this.containingRegions = this.priorContainingRegions;
		this.priorContainingRegions = temp;
		this.containingRegions.clear();
		regionManager.checkRegions(
				this.collisionRectangle == null ? tempRect.set(getX(), getY(), 0, 0) : this.collisionRectangle,
				regionCheckerImpl.reset(this, regionManager));
		for (final CRegion region : this.priorContainingRegions) {
			if (!this.containingRegions.contains(region)) {
				regionManager.onUnitLeaveRegion(this, region);
			}
		}
	}

	public EnumSet<CUnitClassification> getClassifications() {
		return this.classifications;
	}

	public boolean addClassification(final CUnitClassification unitClassification) {
		return this.classifications.add(unitClassification);
	}

	public boolean removeClassification(final CUnitClassification unitClassification) {
		return this.classifications.remove(unitClassification);
	}

	public float getDefense() {
		return this.currentDefense;
	}

	@Override
	public float getImpactZ() {
		return this.unitType.getImpactZ();
	}

	public double angleTo(final AbilityTarget target) {
		final double dx = target.getX() - getX();
		final double dy = target.getY() - getY();
		return StrictMath.atan2(dy, dx);
	}

	public double distance(final AbilityTarget target) {
		double dx = StrictMath.abs(target.getX() - getX());
		double dy = StrictMath.abs(target.getY() - getY());
		final float thisCollisionSize = this.unitType.getCollisionSize();
		float targetCollisionSize;
		if (target instanceof CUnit) {
			final CUnitType targetUnitType = ((CUnit) target).getUnitType();
			targetCollisionSize = targetUnitType.getCollisionSize();
		} else {
			targetCollisionSize = 0; // TODO destructable collision size here
		}
		if (dx < 0) {
			dx = 0;
		}
		if (dy < 0) {
			dy = 0;
		}

		double groundDistance = StrictMath.sqrt((dx * dx) + (dy * dy)) - thisCollisionSize - targetCollisionSize;
		if (groundDistance < 0) {
			groundDistance = 0;
		}
		return groundDistance;
	}

	@Override
	public double distance(final float x, final float y) {
		return distance((double) x, (double) y);
	}

	public double distance(final double x, final double y) {
		double dx = Math.abs(x - getX());
		double dy = Math.abs(y - getY());
		final float thisCollisionSize = this.unitType.getCollisionSize();
		if (dx < 0) {
			dx = 0;
		}
		if (dy < 0) {
			dy = 0;
		}

		double groundDistance = StrictMath.sqrt((dx * dx) + (dy * dy)) - thisCollisionSize;
		if (groundDistance < 0) {
			groundDistance = 0;
		}
		return groundDistance;
	}

	public boolean checkForMiss(final CSimulation simulation, final CUnit source, final boolean isAttack,
			final boolean isRanged, final CAttackType attackType, final CDamageType damageType, final float damage,
			final float bonusDamage) {
		boolean miss = false;
		// Evasion stays on an illusion but never makes it dodge in the original game.
		if (isAttack && !this.illusion) {
			for (final CUnitAttackEvasionListener listener : new ArrayList<>(this.evasionListeners)) {
				miss = miss || listener.onAttack(simulation, source, this, isAttack, isRanged, damageType);
			}
		}
		return miss;
	}

	public boolean checkForAttackProjReaction(final CSimulation simulation, final CUnit source,
			final CAttackProjectile projectile) {
		boolean allow = true;
		for (final CUnitAttackProjReactionListener listener : new ArrayList<>(this.attackProjReactionListeners)) {
			if (allow) {
				allow = allow && listener.onHit(simulation, source, this, projectile);
			}
		}
		return allow;
	}

	public boolean checkForAbilityProjReaction(final CSimulation simulation, final CUnit source,
			final CProjectile projectile) {
		boolean allow = true;
		for (final CUnitAbilityProjReactionListener listener : new ArrayList<>(this.abilityProjReactionListeners)) {
			if (allow) {
				allow = allow && listener.onHit(simulation, source, this, projectile);
			}
		}
		return allow;
	}

	public boolean checkForAbilityEffectReaction(final CSimulation simulation, final CUnit source,
			final CAbility ability) {
		boolean allow = true;
		for (final CUnitAbilityEffectReactionListener listener : new ArrayList<>(this.abilityEffectReactionListeners)) {
			if (allow) {
				allow = allow && listener.onHit(simulation, source, this, ability);
			}
		}
		return allow;
	}

	public CUnitAttackSettings checkForAttackPreLaunchModification(final CSimulation simulation,
			final AbilityTarget target, final CUnitAttack attack, final CUnitAttackSettings settings) {
		if (!this.attackModifiers.isEmpty()) {
			int maxPriority = 0;
			int i = 0;
			final CUnitPriorityLoopData attackModLoop = new CUnitPriorityLoopData();
			while (i <= maxPriority) {
				attackModLoop.startLoop(i);
				for (final CUnitAttackModifier modification : new ArrayList<>(this.attackModifiers)) {
					int prio = modification.getPriority(simulation, this, target, attack, settings);
					if (prio > maxPriority) {
						maxPriority = prio;
					}
					if (prio == i && !attackModLoop.skipCurrentLevel()) {
						if (modification.checkPreLaunchApplication(simulation, this, target, attack, settings)) {
							modification.applyPreLaunchModification(simulation, this, target, attack, settings,
									attackModLoop);
						}
					}
				}
				if (attackModLoop.end()) {
					break;
				}
				i++;
			}
		}
		return settings;
	}

	public CUnitAttackSettings checkForAttackModification(final CSimulation simulation, final AbilityTarget target,
			final CUnitAttack attack, final CUnitAttackSettings settings) {
		if (!this.attackModifiers.isEmpty()) {
			int maxPriority = 0;
			int i = 0;
			final CUnitPriorityLoopData attackModLoop = new CUnitPriorityLoopData();
			while (i <= maxPriority) {
				attackModLoop.startLoop(i);
				for (final CUnitAttackModifier modification : new ArrayList<>(this.attackModifiers)) {
					int prio = modification.getPriority(simulation, this, target, attack, settings);
					if (prio > maxPriority) {
						maxPriority = prio;
					}
					if (prio == i && !attackModLoop.skipCurrentLevel()) {
						if (modification.checkApplication(simulation, this, target, attack, settings)) {
							modification.applyModification(simulation, this, target, attack, settings, attackModLoop);
						}
					}
				}
				if (attackModLoop.end()) {
					break;
				}
				i++;
			}
		}
		return settings;
	}

	public boolean isImmuneToDamage(final CSimulation simulation, final CDamageFlags flags,
			final CAttackType attackType, final CDamageType damageType) {
		if (flags != null && flags.isOnlyDamageSummons() && !this.illusion
				&& !this.isUnitType(CUnitTypeJass.SUMMONED)) {
			return true;
		}
		if (simulation.getGameplayConstants().isMagicImmuneResistsDamage()) {
			if (attackType.isMagic() || damageType.isMagic()) {
				if (this.isMagicImmune() || (this.isLimitedMagicImmune() && !flags.isPassLimitedMagicImmune())) {
					return true;
				}
			} else if (attackType.isPhysical() || damageType.isPhysical()) {
				if (this.isUnitType(CUnitTypeJass.ETHEREAL)) {
					return true;
				}
			}
		} else {
			if (damageType.isOldMagic()
					&& (this.isMagicImmune() || (this.isLimitedMagicImmune() && !flags.isPassLimitedMagicImmune()))) {
				return true;
			}
		}
		return false;
	}

	public void fireAcquiredTargetEvents(final CSimulation simulation, final CUnit target) {
		final List<CWidgetEvent> eventList = getEventList(JassGameEventsWar3.EVENT_UNIT_ACQUIRED_TARGET);
		if (eventList != null) {
			for (final CWidgetEvent event : eventList) {
				event.fire(this, CommonTriggerExecutionScope.unitAcquiredTargetScope(
						JassGameEventsWar3.EVENT_UNIT_ACQUIRED_TARGET, event.getTrigger(), this, target,
						simulation.getPlayer(this.playerIndex)));
			}
		}
	}

	public void fireAttackedEvents(final CSimulation simulation, final CUnit attacker) {
		final List<CWidgetEvent> eventList = getEventList(JassGameEventsWar3.EVENT_UNIT_ATTACKED);
		if (eventList != null) {
			for (final CWidgetEvent event : eventList) {
				event.fire(this, CommonTriggerExecutionScope.unitAttackedScope(JassGameEventsWar3.EVENT_UNIT_ATTACKED,
						event.getTrigger(), this, attacker, simulation.getPlayer(this.playerIndex)));
			}
		}
		simulation.getPlayer(this.playerIndex).fireUnitAttackedEvents(this, attacker);
	}

	@Override
	public float damage(final CSimulation simulation, final CUnit source, final CDamageFlags flags,
			final CAttackType attackType, final CDamageType damageType, final String weaponSoundType,
			final float damage) {
		return this.damage(simulation, source, flags, attackType, damageType, weaponSoundType, damage, 0);
	}

	@Override
	public float damage(final CSimulation simulation, final CUnit source, final CDamageFlags flags,
			final CAttackType attackType, final CDamageType damageType, final String weaponSoundType,
			final float damage, final float bonusDamage) {
		final boolean wasDead = isDead();
		if (wasDead) {
			return 0;
		}
		if (this.isImmuneToDamage(simulation, flags, attackType, damageType)) {
			return 0;
		}
		float trueDamage = 0;
		if (!this.invulnerable || (flags != null && (flags.isIgnoreInvulnerable() || (flags.isOnlyDamageSummons()
				&& simulation.getGameplayConstants().isInvulnerableSummonsTakeDispelDamage()
				&& this.isUnitType(CUnitTypeJass.SUMMONED))))) {

			final CUnitAttackDamageTakenModificationListenerDamageModResult result = new CUnitAttackDamageTakenModificationListenerDamageModResult(
					damage, bonusDamage);
			for (final CUnitAttackDamageTakenModificationListener listener : this.damageTakenModificationListeners) {
				listener.onDamage(simulation, source, this, flags, attackType, damageType, result);
			}

			final float damageRatioFromArmorClass = simulation.getGameplayConstants().getDamageRatioAgainst(attackType,
					(this.isBuilding() && this.isConstructing()) ? CDefenseType.NORMAL : getDefenseType());
			final float damageRatioFromDefense;
			final float defense = this.currentDefense;
			if (damageType != CDamageType.NORMAL || (this.isBuilding() && this.isConstructing())) {
				damageRatioFromDefense = 1.0f;
			} else if (defense >= 0) {
				damageRatioFromDefense = 1f - ((defense * simulation.getGameplayConstants().getDefenseArmor())
						/ (1 + (simulation.getGameplayConstants().getDefenseArmor() * defense)));
			} else {
				damageRatioFromDefense = 2f
						- (float) StrictMath.pow(1f - simulation.getGameplayConstants().getDefenseArmor(), -defense);
			}
			trueDamage = damageRatioFromArmorClass * damageRatioFromDefense * result.computeFinalDamage();
			if ((source != null) && (trueDamage > 0)) {
				trueDamage *= simulation.getPlayer(source.getPlayerIndex()).getHandicapDamage();
			}

			for (final CUnitAttackFinalDamageTakenModificationListener listener : new ArrayList<>(
					this.finalDamageTakenModificationListeners)) {
				trueDamage = listener.onDamage(simulation, source, this, flags, attackType, damageType, trueDamage);
			}
			if (this.illusion) {
				trueDamage *= this.illusionDamageTakenFactor;
			}

			acquireAttackerOnHit(simulation, source, damage + bonusDamage);
			final List<CWidgetEvent> eventList = getEventList(JassGameEventsWar3.EVENT_UNIT_DAMAGED);
			if (eventList != null) {
				for (final CWidgetEvent event : eventList) {
					final CommonTriggerExecutionScope damageScope = CommonTriggerExecutionScope.unitDamageTakenScope(
							JassGameEventsWar3.EVENT_UNIT_DAMAGED, event.getTrigger(), this, source, trueDamage);
					event.fire(this, damageScope);
					// BlzSetEventDamage in a damage trigger changes the damage this hit deals.
					trueDamage = damageScope.getDamageTaken();
				}
			}

			final boolean wasAboveMax = this.life > this.maximumLife;
			this.life -= trueDamage;
			if ((result.computeFinalDamage() < 0) && !wasAboveMax && (this.life > this.maximumLife)) {
				// NOTE wasAboveMax is for that weird life drain power to drain above max... to
				// be honest that's a crazy mechanic anyway so I didn't test whether it works
				// yet
				this.life = this.maximumLife;
			}
			if (flags.isNonlethal() && this.life < 1) {
				this.life = 1;
			} else if (this.life < 0) {
				this.life = 0;
			}
			this.stateNotifier.lifeChanged();
			checkStateEvents(false);
		}
		for (final CUnitAttackDamageTakenListener listener : new ArrayList<>(this.damageTakenListeners)) {
			listener.onDamage(simulation, source, this, flags, damageType, damage, bonusDamage, trueDamage);
		}
		simulation.unitDamageEvent(this, weaponSoundType, this.unitType.getArmorType());
		if ((source != null) && (trueDamage > 0)) {
			simulation.unitAttackedEvent(this, source);
		}
		if (isDead()) {
			if (!wasDead) {
				// A hero killed by artillery keeps its body, so it can still be revived.
				if (flags.isExplode() && !this.unitType.isHero()) {
					this.setExplodesOnDeath(true);
				}
				kill(simulation, source);
			}
		} else {
			// 1.29: a hit for 0 (Life Drain's first) draws no answer or run.
			if (((damage + bonusDamage) > 0) && (isIdleForAttack() || isReturningFromLostTarget()
					|| (isFleeing() && isCritter()))) {
				queueAttackBack(simulation, source, true, false);
			}
			this.lastCombatTick = simulation.getGameTurnTick();
			this.lastHurtTick = this.lastCombatTick;
			wakeByHit(simulation);
			if (source != null) {
				source.lastCombatTick = this.lastCombatTick;
			}
			callForHelp(simulation, source);
		}
		return trueDamage;
	}

	/**
	 * Measured on 1.29: a guarding creep that can sleep is asleep the moment it is idle at night (made at night, at
	 * nightfall, when UnitAddSleep allows it). Called for help or UnitWakeUp, it stays awake at least 2 s; a hit gives
	 * no such delay and ends one already running. A creep that chased and lost its target
	 * sleeps once its walk back ends; otherwise it falls asleep where it stands. It snores under the creep sleep
	 * effect and wakes at daybreak.
	 */
	public void updateNightSleep(final CSimulation game) {
		if (!this.guarding || isDead()) {
			return;
		}
		if (!canSleep() || ((this.naturalSleep == null)
				&& (game.getPlayer(this.playerIndex).getPlayerState(game, CPlayerState.NO_CREEP_SLEEP) != 0))) {
			wakeFromNaturalSleep(game);
			return;
		}
		if (this.naturalSleep != null) {
			if (game.isDay() || !this.stateModBuffs.contains(this.naturalSleep)) {
				wakeFromNaturalSleep(game);
			}
			return;
		}
		final int tick = game.getGameTurnTick();
		if (game.isNight() && isIdleForAttack()
				&& ((tick - this.lastWakeTick) >= CTimer.wholeSteps(SLEEP_AFTER_WAKE_SECONDS))) {
			this.naturalSleep = new StateModBuff(StateModBuffType.SLEEPING, 1);
			addStateModBuff(this.naturalSleep);
			computeUnitState(game, StateModBuffType.SLEEPING);
			this.naturalSleepEffect = game.createPersistentSpellEffectOnUnit(this, CREEP_SLEEP, CEffectType.CASTER);
		}
	}

	public boolean isNaturallyAsleep() {
		return this.naturalSleep != null;
	}

	/**
	 * Stunned or put to sleep by a spell. 1.29: such a unit's current
	 * order reads stunned, a creep in its night sleep reads 0.
	 */
	public boolean isStunnedBySpell() {
		for (final StateModBuff buff : this.stateModBuffs) {
			if ((buff != this.naturalSleep) && (buff.getValue() != 0)
					&& ((buff.getBuffType() == StateModBuffType.STUN)
							|| (buff.getBuffType() == StateModBuffType.SLEEPING))) {
				return true;
			}
		}
		return false;
	}

	/** Whether the unit may fall asleep at night: its type's setting until a trigger sets it (UnitAddSleep). */
	public boolean canSleep() {
		return this.canSleepOverride == null ? this.unitType.isCanSleep() : this.canSleepOverride;
	}

	public void setCanSleep(final boolean canSleep) {
		this.canSleepOverride = canSleep;
	}

	public void wakeFromNaturalSleep(final CSimulation game) {
		if (this.naturalSleep != null) {
			removeStateModBuff(this.naturalSleep);
			computeUnitState(game, StateModBuffType.SLEEPING);
			this.naturalSleep = null;
		}
		if (this.naturalSleepEffect != null) {
			this.naturalSleepEffect.remove();
			this.naturalSleepEffect = null;
		}
	}

	/** Called for help or UnitWakeUp: wakes the creep and keeps it awake for a while even if it already was. */
	public void wakeUp(final CSimulation game) {
		this.lastWakeTick = game.getGameTurnTick();
		wakeFromNaturalSleep(game);
	}

	private void wakeByHit(final CSimulation game) {
		this.lastWakeTick = Integer.MIN_VALUE / 2;
		wakeFromNaturalSleep(game);
	}

	/**
	 * Measured on 1.29: a creep made at night is asleep at once, unless an enemy it would attack is already in its
	 * acquisition range; then it fights instead.
	 */
	public void onCreatedNightSleep(final CSimulation game) {
		if (this.guarding && canSleep() && game.isNight() && isIdleForAttack()
				&& !autoAcquireAttackTargets(game, this.moveDisabled)) {
			updateNightSleep(game);
		}
	}

	/** A guarding unit (a creep at its camp) goes back to its spot like the original game's creeps do. */
	public void setGuardPosition(final float x, final float y) {
		this.guarding = true;
		this.guardX = x;
		this.guardY = y;
	}

	public void removeGuardPosition() {
		this.guarding = false;
	}

	public boolean isGuarding() {
		return this.guarding;
	}

	/**
	 * Measured on 1.29: a guard that fought and lost its target (removed, dead) walks back
	 * a moment later and stops 48 short of its spot, still fighting back when hit on the way. A guard that has been
	 * fighting beyond GuardDistance gives up about GuardReturnTime + 1.5 s after crossing it, unless it was hit within
	 * the last GuardReturnTime while not beyond MaxGuardDistance; it then walks to the spot itself, ignoring attacks.
	 * MaxGuardDistance does not cut a chase short. Guards that never fought (woken, moved) stay where they are.
	 */
	private void updateGuard(final CSimulation game) {
		if ((this.moveBehavior == null) || isMovementDisabled()) {
			return;
		}
		final CBehavior behavior = this.currentBehavior;
		if (this.guardReturn != GUARD_RETURN_NONE) {
			if ((behavior == this.moveBehavior) && (this.moveBehavior.getRangedBehavior() == null)) {
				if (this.guardReturn == GUARD_RETURN_LOST) {
					final double fromHome = distanceFromGuardSpot();
					if (fromHome <= GUARD_LOST_STOP) {
						// The last step ends GUARD_LOST_STOP out, as in the original; the quarter keeps float rounding
						// from landing just inside it.
						final float keep = (float) ((GUARD_LOST_STOP + 0.25f) / Math.max(fromHome, 1.0));
						setPoint(this.guardX + ((getX() - this.guardX) * keep),
								this.guardY + ((getY() - this.guardY) * keep), game.getWorldCollision(),
								game.getRegionManager());
						this.guardReturn = GUARD_RETURN_NONE;
						performDefaultBehavior(game);
					}
				}
				return;
			}
			this.guardReturn = GUARD_RETURN_NONE;
		}
		final int tick = game.getGameTurnTick();
		final CGameplayConstants constants = game.getGameplayConstants();
		final boolean fighting = (behavior instanceof CBehaviorAttack) || ((behavior == this.moveBehavior)
				&& (this.moveBehavior.getRangedBehavior() instanceof CBehaviorAttack));
		if (fighting) {
			this.guardEngaged = true;
			final double fromHome = distanceFromGuardSpot();
			if (this.guardAwayTick < 0) {
				if (fromHome > constants.getGuardDistance()) {
					this.guardAwayTick = tick;
				}
			}
			else if (((tick - this.guardAwayTick) >= secondsToTicks(
					constants.getGuardReturnTime() + GUARD_GIVE_UP_EXTRA_SECONDS))
					&& ((fromHome > constants.getMaxGuardDistance())
							|| ((tick - this.lastHurtTick) >= secondsToTicks(constants.getGuardReturnTime())))) {
				walkToGuardSpot(game, GUARD_RETURN_GIVE_UP, this.guardX, this.guardY);
			}
			return;
		}
		this.guardAwayTick = -1;
		if (!this.guardEngaged || !isIdleForAttack()) {
			return;
		}
		this.guardEngaged = false;
		if (distanceFromGuardSpot() > GUARD_LOST_STOP) {
			walkToGuardSpot(game, GUARD_RETURN_LOST, this.guardX, this.guardY);
		}
	}

	private double distanceFromGuardSpot() {
		return StrictMath.sqrt(distanceSquaredNoCollision(this.guardX, this.guardY));
	}

	private static int secondsToTicks(final float seconds) {
		return CTimer.wholeSteps(seconds);
	}

	private void walkToGuardSpot(final CSimulation game, final int kind, final float x, final float y) {
		this.guardEngaged = false;
		this.guardAwayTick = -1;
		beginBehavior(game, this.moveBehavior.reset(OrderIds.move, new AbilityPointTarget(x, y)), true);
		this.guardReturn = kind;
	}

	private boolean isReturningFromLostTarget() {
		return (this.guardReturn == GUARD_RETURN_LOST) && (this.currentBehavior == this.moveBehavior)
				&& (this.moveBehavior.getRangedBehavior() == null);
	}

	private boolean isIdleForAttack() {
		return (this.currentBehavior == null)
				|| ((this.currentBehavior == this.defaultBehavior) && this.currentBehavior.interruptable());
	}

	/**
	 * Measured on 1.29: a unit hit by a source that the same script call removes or kills
	 * never answers it; a sleeping creep woken by the hit is asleep again at once and does not move. So the answer
	 * (attack back, alert, flee) waits for the unit's own update and is dropped if the source is gone by then.
	 */
	private static final float FLEE_DISTANCE = 300f;
	private static final float CRITTER_FLEE_DISTANCE = 500f;
	private static final float FLEE_STOP_SHORT = 15f;

	private void queueAttackBack(final CSimulation simulation, final CUnit source, final boolean wasHit,
			final boolean help) {
		// 1.29: a unit hit after a neighbour called it for help answers its own hit (runs). A critter also leaves
		// its own hit for a call that comes after it, but keeps the first of two calls.
		if ((source != null) && ((this.pendingAttackBackSource == null)
				|| (wasHit && !this.pendingAttackBackWasHit && this.pendingAttackBackHelp)
				|| (isCritter() && (wasHit || !this.pendingAttackBackHelp)))) {
			this.pendingAttackBackSource = source;
			this.pendingAttackBackWasHit = wasHit;
			this.pendingAttackBackHelp = help;
		}
	}

	/**
	 * Measured on 1.29 (creeps and player units): a unit that will strike back acquires its attacker as the hit lands,
	 * before the damage triggers run, while the answer itself still waits for the unit's update.
	 */
	private void acquireAttackerOnHit(final CSimulation simulation, final CUnit source, final float damage) {
		if ((source != null) && (source != this) && (damage > 0)
				&& !((this.pendingAttackBackSource != null) && this.pendingAttackBackWasHit)
				&& (isIdleForAttack() || isReturningFromLostTarget())
				&& (attackBackAttack(simulation, source) != null)) {
			fireAcquiredTargetEvents(simulation, source);
		}
	}

	private void resolveAttackBack(final CSimulation simulation) {
		CUnit source = this.pendingAttackBackSource;
		final boolean wasHit = this.pendingAttackBackWasHit;
		this.pendingAttackBackSource = null;
		final boolean critter = isCritter();
		if (source.isDead() || source.isHidden() || source.isRemovedFromGame()
				|| !(isIdleForAttack() || isReturningFromLostTarget() || (critter && isFleeing()))) {
			return;
		}
		final CUnit attacker = source;
		final boolean help = this.pendingAttackBackHelp;
		if (help) {
			source = nearestHelpTarget(simulation, source);
		}
		final boolean foundMatchingReturnFireAttack = attackBack(simulation, source);
		if (wasHit && foundMatchingReturnFireAttack) {
			alertCreeps(simulation, source);
		}
		// Measured on 1.29 (creeps and player units): a unit called for help acquires its target like one that found
		// it itself.
		else if (help && foundMatchingReturnFireAttack) {
			fireAcquiredTargetEvents(simulation, source);
		}
		// Measured on 1.29: a critter called for help by another runs too, away from the caller's attacker.
		if (!foundMatchingReturnFireAttack && (wasHit || critter)) {
			flee(simulation, attacker, critter ? CRITTER_FLEE_DISTANCE : FLEE_DISTANCE);
		}
	}

	private void flee(final CSimulation simulation, final CUnit source, final float distance) {
		if (this.unitType.isCanFlee() && !isMovementDisabled()
				&& (this.moveBehavior != null) && (this.playerIndex != source.getPlayerIndex())
				&& !holdsBackOnlyForPassive(simulation, source)
				// Measured on 1.29: a unit holding position stays when it cannot hit back.
				&& (this.defaultBehavior != this.holdPositionBehavior)) {
			// Measured on 1.29: the run is a real move order (order events fire, the current order is move) to a
			// point 300 (a critter's 500) straight away from the source whatever the unit's speed, and the unit stops
			// 15 short of it.
			final double angleTo = source.angleTo(this);
			final double cos = StrictMath.cos(angleTo);
			final double sin = StrictMath.sin(angleTo);
			final AbilityPointTarget fleePoint = new AbilityPointTarget((float) (getX() + (distance * cos)),
					(float) (getY() + (distance * sin)));
			final AbilityPointTarget stopPoint = new AbilityPointTarget(
					(float) (getX() + ((distance - FLEE_STOP_SHORT) * cos)),
					(float) (getY() + ((distance - FLEE_STOP_SHORT) * sin)));
			if (!order(simulation, OrderIds.move, fleePoint)) {
				beginBehavior(simulation, this.moveBehavior.reset(OrderIds.move, stopPoint), true);
			} else if (this.currentBehavior == this.moveBehavior) {
				this.moveBehavior.reset(OrderIds.move, stopPoint);
			}
			if (this.currentBehavior == this.moveBehavior) {
				this.fleeMoveResetCount = this.moveBehavior.getResetCount();
			}
		}
	}

	private boolean isFleeing() {
		return (this.currentBehavior == this.moveBehavior) && (this.moveBehavior.getRangedBehavior() == null)
				&& (this.moveBehavior.getResetCount() == this.fleeMoveResetCount);
	}

	/**
	 * Measured on 1.29: a critter (a Sheep, of any owner) runs 500 instead of 300, runs when it hears a call for
	 * help, and is the only unit that runs from a source its owner is passive toward.
	 */
	private boolean isCritter() {
		return this.unitType.getRace() == CUnitRace.CRITTERS;
	}

	/**
	 * 1.29: a unit whose owner is passive toward the source (a Neutral Passive Footman, Peasant or Wisp) stands
	 * still; only a critter (a Sheep) runs.
	 */
	private boolean holdsBackOnlyForPassive(final CSimulation simulation, final CUnit source) {
		return !isCritter()
				&& simulation.getPlayer(getPlayerIndex()).hasAlliance(source.getPlayerIndex(), CAllianceType.PASSIVE);
	}

	/**
	 * Measured on 1.29: an idle unit that an enemy's unit-target spell takes effect on (Slow,
	 * Curse, Faerie Fire) answers the caster as if hit, and calls its allies for help; the caster itself does not
	 * attack after its cast. Point and no-target spells draw no answer.
	 */
	public void onTargetedBySpell(final CSimulation simulation, final CUnit caster) {
		if ((caster == null) || (caster == this) || isDead() || simulation.getPlayer(this.playerIndex)
				.hasAlliance(caster.getPlayerIndex(), CAllianceType.PASSIVE)) {
			return;
		}
		this.pendingSpellAnswerSource = caster;
	}

	/**
	 * A spell that flies to its target is answered when it lands (1.29: an Ensnared Footman never turns to the
	 * Raider). Hardcoded spells launch their missile after the effect event and builder spells before it, so the
	 * launch both withdraws an answer already asked for and blocks the effect event that follows it before the
	 * target's next update.
	 */
	public void onSpellMissileLaunched(final CUnit caster) {
		if (this.pendingSpellAnswerSource == caster) {
			this.pendingSpellAnswerSource = null;
		}
		this.spellMissileSource = caster;
	}

	private void onSpellEffect(final CSimulation simulation, final CUnit caster) {
		if (this.spellMissileSource == caster) {
			return;
		}
		onTargetedBySpell(simulation, caster);
	}

	private void resolveSpellAnswer(final CSimulation simulation) {
		final CUnit caster = this.pendingSpellAnswerSource;
		this.pendingSpellAnswerSource = null;
		if (caster.isDead() || caster.isHidden() || caster.isRemovedFromGame()) {
			return;
		}
		if (isIdleForAttack() || isReturningFromLostTarget()) {
			queueAttackBack(simulation, caster, false, false);
		}
		callForHelp(simulation, caster);
	}

	private boolean attackBack(final CSimulation simulation, final CUnit source) {
		final CUnitAttack attack = attackBackAttack(simulation, source);
		if (attack == null) {
			return false;
		}
		// 1.29: a unit on hold position answers only an attacker it reaches from where it stands.
		final boolean disableMove = this.moveDisabled || (this.defaultBehavior == this.holdPositionBehavior);
		final CBehaviorAttack attackBehaviorForAtk = getAttackBehavior();
		final CBehavior chase = attackBehaviorForAtk.reset(simulation, OrderIds.attack, attack, source, disableMove,
				CBehaviorAttackListener.DO_NOTHING);
		attackBehaviorForAtk.setThroughFog(true);
		beginBehavior(simulation, chase, true);
		return true;
	}

	private CUnitAttack attackBackAttack(final CSimulation simulation, final CUnit source) {
		if ((source == null)
				|| simulation.getPlayer(getPlayerIndex()).hasAlliance(source.getPlayerIndex(), CAllianceType.PASSIVE)
				|| this.classifications.contains(CUnitClassification.PEON)) {
			return null;
		}
		final CAbilityAttack attackAbility = getFirstAbilityOfType(CAbilityAttack.class);
		if ((getAttackBehavior() == null) || ((attackAbility != null) && attackAbility.isDisabled())) {
			return null;
		}
		for (final CUnitAttack attack : getCurrentAttacks()) {
			// 1.29 (creep-wake-sleep r1): an attacker out of sight in the fog is still chased.
			if (source.canBeTargetedBy(simulation, this, true, attack.getTargetsAllowed(),
					BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset(), true)) {
				return attack;
			}
		}
		return null;
	}

	/**
	 * Measured on 1.29: a unit called for help does not go for the attacker that called it but
	 * for the enemy nearest to itself, even one that has not attacked; the attacker when nothing is nearer.
	 */
	private CUnit nearestHelpTarget(final CSimulation simulation, final CUnit attacker) {
		final float reach = (float) distance(attacker);
		final CPlayer owner = simulation.getPlayer(this.playerIndex);
		final CUnit[] nearest = { attacker };
		final double[] nearestDistance = { reach };
		callForHelpRect.set(getX() - reach, getY() - reach, reach * 2, reach * 2);
		simulation.getWorldCollision().enumUnitsInRect(callForHelpRect, (unit) -> {
			if ((unit == this) || (unit == attacker) || unit.isDead() || unit.isHidden() || !unit.isAutoTargetable()
					|| unit.isInvulnerable() || unit.isUnitType(CUnitTypeJass.SLEEPING)
					|| owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)) {
				return false;
			}
			final double unitDistance = distance(unit);
			if (unitDistance >= nearestDistance[0]) {
				return false;
			}
			for (final CUnitAttack attack : getCurrentAttacks()) {
				if (unit.canBeTargetedBy(simulation, this, attack.getTargetsAllowed())) {
					nearest[0] = unit;
					nearestDistance[0] = unitDistance;
					break;
				}
			}
			return false;
		});
		return nearest[0];
	}

	/**
	 * An attacked unit calls idle helpers within the CallForHelp range (CreepCallForHelp for creeps) to
	 * attack the attacker, at most once a second so a long fight does not search every hit.
	 */
	private void callForHelp(final CSimulation simulation, final CUnit attacker) {
		// Measured on 1.29: a critter calls the others even when its owner is passive toward the attacker.
		if ((attacker == null) || (simulation.getGameTurnTick() < this.nextCallForHelpTick)
				|| (!isCritter() && simulation.getPlayer(this.playerIndex).hasAlliance(attacker.getPlayerIndex(),
						CAllianceType.PASSIVE))) {
			return;
		}
		this.nextCallForHelpTick = simulation.getGameTurnTick()
				+ CTimer.wholeSteps(1.0f);
		final CGameplayConstants constants = simulation.getGameplayConstants();
		float radius;
		if (simulation.isNeutralAggressive(this.playerIndex)) {
			radius = constants.getCreepCallForHelp();
		}
		else if (simulation.getPlayer(this.playerIndex).getController() == CMapControl.COMPUTER) {
			radius = constants.getAICallForHelp();
		}
		else {
			radius = constants.getCallForHelp();
		}
		if (radius <= 0) {
			// 1.29 data has no AICallForHelp entry; its computer players are helped like users.
			radius = constants.getCallForHelp();
		}
		summonHelpers(simulation, attacker, radius);
	}

	/**
	 * Measured on 1.29: a creep that starts attacking (before any damage) brings its camp along at the same instant,
	 * each one acquiring the same target; every creep so brought in alerts in turn the idle creeps within
	 * {@link #CREEP_ALERT_RADIUS} of itself, and those call nobody. The caller alerts no creep outside its camp: of two
	 * idle creeps 600 to either side of it only the camp mate joins.
	 */
	private void alertCreeps(final CSimulation simulation, final CUnit target) {
		if ((target == null) || !simulation.isNeutralAggressive(this.playerIndex)) {
			return;
		}
		if (this.creepCamp == null) {
			// Given to Neutral Hostile after it was made.
			joinCreepCamp(simulation);
		}
		final List<CUnit> joined = new ArrayList<>();
		final List<CUnit> members = this.creepCamp.members;
		for (int i = 0; i < members.size(); i++) {
			final CUnit mate = members.get(i);
			if (mate.isDead() || mate.isRemovedFromGame() || (mate.creepCamp != this.creepCamp)) {
				members.remove(i--);
			}
			else if ((mate != this) && (mate.playerIndex == this.playerIndex)) {
				mate.wakeUp(simulation);
				if (mate.answerAlert(simulation, target)) {
					joined.add(mate);
				}
			}
		}
		for (final CUnit mate : joined) {
			for (final CUnit neighbour : mate.helpersInRange(simulation, CREEP_ALERT_RADIUS)) {
				neighbour.wakeUp(simulation);
				neighbour.answerAlert(simulation, target);
			}
		}
	}

	private boolean answerAlert(final CSimulation simulation, final CUnit target) {
		if (isDead() || isHidden() || target.isDead() || target.isHidden() || target.isRemovedFromGame()
				|| !(isIdleForAttack() || isReturningFromLostTarget())) {
			return false;
		}
		if (attackBack(simulation, target)) {
			fireAcquiredTargetEvents(simulation, target);
			return true;
		}
		return false;
	}

	/**
	 * Measured on 1.29: a new creep joins the camp of the nearest camp founder within {@link #CREEP_ALERT_RADIUS}
	 * (counted to its own edge, as for an alert), not of the nearest member, so of three creeps made in a row 600
	 * apart the third starts a camp of its own. A founder that dies or is removed hands the camp to its oldest
	 * living member, and the camp still answers together.
	 */
	public void joinCreepCamp(final CSimulation simulation) {
		final float collisionSize = this.unitType.getCollisionSize();
		final float reach = CREEP_ALERT_RADIUS + collisionSize;
		final CUnit[] founder = { null };
		final double[] founderDistance = { Double.MAX_VALUE };
		callForHelpRect.set(getX() - reach, getY() - reach, reach * 2, reach * 2);
		simulation.getWorldCollision().enumUnitsInRect(callForHelpRect, (unit) -> {
			if ((unit != this) && (unit.playerIndex == this.playerIndex) && (unit.creepCamp != null)
					&& (unit.creepCamp.founder() == unit)) {
				final double unitDistance = distance(unit.getX(), unit.getY());
				if ((unitDistance <= CREEP_ALERT_RADIUS) && ((unitDistance < founderDistance[0])
						|| ((unitDistance == founderDistance[0]) && (unit.getHandleId() < founder[0].getHandleId())))) {
					founder[0] = unit;
					founderDistance[0] = unitDistance;
				}
			}
			return false;
		});
		this.creepCamp = (founder[0] != null) ? founder[0].creepCamp : new CreepCamp();
		this.creepCamp.members.add(this);
	}

	private static final class CreepCamp {
		private final List<CUnit> members = new ArrayList<>();

		private CUnit founder() {
			while (!this.members.isEmpty()) {
				final CUnit first = this.members.get(0);
				if (!first.isDead() && !first.isRemovedFromGame() && (first.creepCamp == this)) {
					return first;
				}
				this.members.remove(0);
			}
			return null;
		}
	}

	/**
	 * Idle units of this owner, and of players this owner asks for help who answer it, join against the attacker.
	 */
	private void summonHelpers(final CSimulation simulation, final CUnit attacker, final float radius) {
		for (final CUnit helper : helpersInRange(simulation, radius)) {
			helper.wakeUp(simulation);
			if (helper.isIdleForAttack() || (helper.isFleeing() && helper.isCritter())) {
				helper.queueAttackBack(simulation, attacker, false, true);
			}
		}
	}

	/**
	 * Living, shown units of this owner, and of players this owner asks for help who answer it, within the radius. The
	 * reach counts to the helper's edge, as the original's thresholds sit one Footman radius past the constant.
	 */
	private List<CUnit> helpersInRange(final CSimulation simulation, final float radius) {
		final CPlayer owner = simulation.getPlayer(this.playerIndex);
		final List<CUnit> helpers = new ArrayList<>();
		callForHelpRect.set(getX() - radius, getY() - radius, radius * 2, radius * 2);
		simulation.getWorldCollision().enumUnitsInRect(callForHelpRect, (helper) -> {
			if ((helper != this) && !helper.isDead() && !helper.isHidden()
					&& ((helper.playerIndex == this.playerIndex)
							|| (owner.hasAlliance(helper.playerIndex, CAllianceType.HELP_REQUEST)
									&& simulation.getPlayer(helper.playerIndex).hasAlliance(this.playerIndex,
											CAllianceType.HELP_RESPONSE)))
					&& ((helper.distance(getX(), getY()) - helper.unitType.getCollisionSize()) <= radius)) {
				helpers.add(helper);
			}
			return false;
		});
		return helpers;
	}

	private void kill(final CSimulation simulation, final CUnit source) {
		beginBehavior(simulation, null, true);

		final CUnitDeathReplacementResult result = new CUnitDeathReplacementResult();
		CUnitDeathReplacementStacking allowContinue = new CUnitDeathReplacementStacking();
		for (final CUnitDeathReplacementEffectPriority priority : CUnitDeathReplacementEffectPriority.values()) {
			if (allowContinue.isAllowStacking()) {
				for (final CUnitDeathReplacementEffect effect : new ArrayList<>(
						deathReplacementEffects.get(priority))) {
					if (allowContinue.isAllowSamePriorityStacking()) {
						allowContinue = effect.onDeath(simulation, this, source, result);
					}
				}
			}
		}
		if (result.isReviving()) {
			return;
		}

		this.orderQueue.clear();
		// Measured on 1.29: a unit killed while walking under an order reads current order 0.
		this.lastStartedOrder = null;
		killPathingInstance();
		freeShadowPoolIds(simulation.getHandleIdAllocator());
		popoutWorker(simulation);
		for (int i = this.abilities.size() - 1; i >= 0; i--) {
			// okay if it removes self from this during onDeath() because of reverse
			// iteration order
			if (i < this.abilities.size()) {
				this.abilities.get(i).onDeath(simulation, this);
			}
		}
		simulation.getPlayer(this.playerIndex).addFogModifer(simulation, new CUnitDeathVisionFogModifier(this), false);
		if (source != null) {
			simulation.getPlayer(source.getPlayerIndex()).addFogModifer(simulation,
					new CUnitDeathVisionFogModifier(this), false);
		}
		if (this.attackFogMod.getPlayerIndex() != this.playerIndex) {
			simulation.getPlayer(this.attackFogMod.getPlayerIndex()).removeFogModifer(simulation, this.attackFogMod);
			this.attackFogMod.setPlayerIndex(this.playerIndex);
		}

		if (result.isReincarnating()) {
			return;
		}

		if (this.constructing) {
			simulation.createDeathExplodeEffect(this, this.explodesOnDeathBuffId);
		} else {
			this.deathTurnTick = simulation.getGameTurnTick();
		}

		final CPlayer player = simulation.getPlayer(this.playerIndex);
		if (this.foodMade != 0) {
			player.setUnitFoodMade(this, 0);
		}
		if (this.foodUsed != 0) {
			player.setUnitFoodUsed(this, 0);
		}
		// A dead hero awaiting revive no longer counts as unlocked (requirements, GetPlayerTechCount); the
		// per-type limit still counts it, see CPlayer.isTechtreeAllowedByMax.
		if (this.constructing) {
			player.removeTechtreeInProgress(simulation, this.unitType.getTypeId());
		} else if (!this.animatedCorpse) {
			player.removeTechtreeUnlocked(simulation, this.unitType.getTypeId());
			for (final War3ID upgradedFromTypeId : this.upgradedFromTypeIds) {
				player.removeTechtreeUnlocked(simulation, upgradedFromTypeId);
			}
		}

		// Award hero experience; illusions give neither experience nor bounty
		if ((source != null) && !this.illusion) {
			final CPlayer sourcePlayer = simulation.getPlayer(source.getPlayerIndex());
			if (!sourcePlayer.hasAlliance(this.playerIndex, CAllianceType.PASSIVE)) {
				sourcePlayer.scoreKill(this.unitType);
				if (player.getPlayerState(simulation, CPlayerState.GIVES_BOUNTY) > 0) {
					int goldBountyAwarded = this.unitType.getGoldBountyAwardedBase();
					final int goldBountyAwardedDice = this.unitType.getGoldBountyAwardedDice();
					final int goldBountyAwardedSides = this.unitType.getGoldBountyAwardedSides();
					for (int i = 0; i < goldBountyAwardedDice; i++) {
						final int singleRoll = goldBountyAwardedSides == 0 ? 0
								: simulation.getSeededRandom().nextInt(goldBountyAwardedSides);
						goldBountyAwarded += singleRoll + 1;
					}
					if (goldBountyAwarded > 0) {
						sourcePlayer.addGold(goldBountyAwarded);
						simulation.unitGainResourceEvent(this, sourcePlayer.getId(), ResourceType.GOLD,
								goldBountyAwarded);
					}
					int lumberBountyAwarded = this.unitType.getLumberBountyAwardedBase();
					final int lumberBountyAwardedDice = this.unitType.getLumberBountyAwardedDice();
					final int lumberBountyAwardedSides = this.unitType.getLumberBountyAwardedSides();
					for (int i = 0; i < lumberBountyAwardedDice; i++) {
						lumberBountyAwarded += simulation.getSeededRandom().nextInt(lumberBountyAwardedSides) + 1;
					}
					if (lumberBountyAwarded > 0) {
						sourcePlayer.addLumber(lumberBountyAwarded);
						simulation.unitGainResourceEvent(this, sourcePlayer.getId(), ResourceType.LUMBER,
								lumberBountyAwarded);
					}
				}
				final CGameplayConstants gameplayConstants = simulation.getGameplayConstants();
				if (gameplayConstants.isBuildingKillsGiveExp() || !source.isBuilding()) {
					final CUnit killedUnit = this;
					final CAbilityHero killedUnitHeroData = getHeroData();
					final boolean killedUnitIsAHero = killedUnitHeroData != null;
					int availableAwardXp;
					if (killedUnitIsAHero) {
						availableAwardXp = gameplayConstants.getGrantHeroXP(killedUnitHeroData.getHeroLevel());
					} else {
						availableAwardXp = gameplayConstants.getGrantNormalXP(this.unitType.getLevel());
						if (isUnitType(CUnitTypeJass.SUMMONED)) {
							availableAwardXp = (int) (availableAwardXp * gameplayConstants.getSummonedKillFactor());
						}
					}
					final List<CUnit> xpReceivingHeroes = new ArrayList<>();
					final int heroExpRange = gameplayConstants.getHeroExpRange();
					simulation.getWorldCollision().enumUnitsInRect(new Rectangle(getX() - heroExpRange,
							getY() - heroExpRange, heroExpRange * 2, heroExpRange * 2), new CUnitEnumFunction() {
								@Override
								public boolean call(final CUnit unit) {
									if ((unit.distance(killedUnit) <= heroExpRange)
											&& sourcePlayer.hasAlliance(unit.getPlayerIndex(), CAllianceType.SHARED_XP)
											&& unit.isHero() && !unit.isDead() && !unit.illusion) {
										xpReceivingHeroes.add(unit);
									}
									return false;
								}
							});
					if (xpReceivingHeroes.isEmpty()) {
						if (gameplayConstants.isGlobalExperience()) {
							for (int i = 0; i < WarsmashConstants.MAX_PLAYERS; i++) {
								if (sourcePlayer.hasAlliance(i, CAllianceType.SHARED_XP)) {
									xpReceivingHeroes.addAll(simulation.getPlayerHeroes(i));
								}
							}
						}
					}
					xpReceivingHeroes.removeIf((hero) -> hero.getHeroData().isXpSuspended());
					// HeroFactorXP is the creep reduction table: it only scales the experience from creeps.
					final boolean creepKill = simulation.isNeutralAggressive(this.playerIndex);
					for (final CUnit receivingHero : xpReceivingHeroes) {
						final CAbilityHero heroData = receivingHero.getHeroData();
						final float share = availableAwardXp * (1f / xpReceivingHeroes.size());
						final int xp = creepKill ? creepXp(gameplayConstants, heroData, share) : (int) share;
						heroData.addXp(simulation, receivingHero, xp, true);
					}
				}
			}
		}
		if (this.unitType.isHero()) {
			simulation.heroKilledEvent(this);
		}
		fireDeathEvents(simulation);
		final List<CWidgetEvent> eventList = getEventList(JassGameEventsWar3.EVENT_UNIT_DEATH);
		if (eventList != null) {
			// A death trigger may register or destroy events on this unit while it runs.
			for (final CWidgetEvent event : new ArrayList<>(eventList)) {
				event.fire(this, CommonTriggerExecutionScope.unitDeathScope(JassGameEventsWar3.EVENT_UNIT_DEATH,
						event.getTrigger(), this, source, simulation.getPlayer(this.playerIndex)));
			}
		}
		simulation.getPlayer(this.playerIndex).fireUnitDeathEvents(this, source);
		if (isExplodesOnDeath()) {
			setHidden(true);
			simulation.createDeathExplodeEffect(this, this.explodesOnDeathBuffId);
			simulation.removeUnit(this);
		}
	}

	public void killPathingInstance() {
		if (this.pathingInstance != null) {
			this.pathingInstance.remove();
			this.pathingInstance = null;
		}
	}

	public void kill(final CSimulation simulation) {
		if (!isDead()) {
			setLife(simulation, 0f);
		}
	}

	public boolean canReach(final AbilityTarget target, final float range) {
		final double distance = distance(target);
		if (target instanceof CUnit) {
			final CUnit targetUnit = (CUnit) target;
			final CUnitType targetUnitType = targetUnit.getUnitType();
			if (targetUnit.isBuilding() && (targetUnitType.getBuildingPathingPixelMap() != null)) {
				final ArgbImage buildingPathingPixelMap = targetUnitType.getBuildingPathingPixelMap();
				final float targetX = target.getX();
				final float targetY = target.getY();
				if (canReachToPathing(range, targetUnit.getFacing(), buildingPathingPixelMap, targetX, targetY)) {
					return true;
				}
			}
		} else if (target instanceof CDestructable) {
			final CDestructable targetDest = (CDestructable) target;
			final CDestructableType targetDestType = targetDest.getDestType();
			final ArgbImage pathingPixelMap = targetDest.isDead() ? targetDestType.getPathingDeathPixelMap()
					: targetDestType.getPathingPixelMap();
			final float targetX = target.getX();
			final float targetY = target.getY();
			if ((pathingPixelMap != null) && canReachToPathing(range, 270, pathingPixelMap, targetX, targetY)) {
				return true;
			}
		}
		return distance <= range;
	}

	public boolean canReach(final float x, final float y, final float range) {
		return distance(x, y) <= range; // TODO use dist squared for performance
	}

	public boolean canReachToPathing(final float range, final float rotationForPathing,
			final ArgbImage buildingPathingPixelMap, final float targetX, final float targetY) {
		if (buildingPathingPixelMap == null) {
			return canReach(targetX, targetY, range);
		}
		final int rotation = ((int) rotationForPathing + 450) % 360;
		final float relativeOffsetX = getX() - targetX;
		final float relativeOffsetY = getY() - targetY;
		final int gridWidth = (rotation % 180) != 0 ? buildingPathingPixelMap.getHeight()
				: buildingPathingPixelMap.getWidth();
		final int gridHeight = (rotation % 180) != 0 ? buildingPathingPixelMap.getWidth()
				: buildingPathingPixelMap.getHeight();
		final int relativeGridX = (int) Math.floor(relativeOffsetX / 32f) + (gridWidth / 2);
		final int relativeGridY = (int) Math.floor(relativeOffsetY / 32f) + (gridHeight / 2);
		final int rangeInCells = (int) Math.floor(range / 32f) + 1;
		final int rangeInCellsSquare = rangeInCells * rangeInCells;
		int minCheckX = relativeGridX - rangeInCells;
		int minCheckY = relativeGridY - rangeInCells;
		int maxCheckX = relativeGridX + rangeInCells;
		int maxCheckY = relativeGridY + rangeInCells;
		if ((minCheckX < gridWidth) && (maxCheckX >= 0)) {
			if ((minCheckY < gridHeight) && (maxCheckY >= 0)) {
				if (minCheckX < 0) {
					minCheckX = 0;
				}
				if (minCheckY < 0) {
					minCheckY = 0;
				}
				if (maxCheckX > (gridWidth - 1)) {
					maxCheckX = gridWidth - 1;
				}
				if (maxCheckY > (gridHeight - 1)) {
					maxCheckY = gridHeight - 1;
				}
				for (int checkX = minCheckX; checkX <= maxCheckX; checkX++) {
					for (int checkY = minCheckY; checkY <= maxCheckY; checkY++) {
						final int dx = relativeGridX - checkX;
						final int dy = relativeGridY - checkY;
						if (((dx * dx) + (dy * dy)) <= rangeInCellsSquare) {
							if (((getRGBFromPixelData(buildingPathingPixelMap, checkX, checkY, rotation)
									& 0xFF0000) >>> 16) > 127) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	private int getRGBFromPixelData(final ArgbImage buildingPathingPixelMap, final int checkX, final int checkY,
			final int rotation) {

		// Below: y is downwards (:()
		int x;
		int y;
		switch (rotation) {
		case 90:
			x = checkY;
			y = buildingPathingPixelMap.getWidth() - 1 - checkX;
			break;
		case 180:
			x = buildingPathingPixelMap.getWidth() - 1 - checkX;
			y = buildingPathingPixelMap.getHeight() - 1 - checkY;
			break;
		case 270:
			x = buildingPathingPixelMap.getHeight() - 1 - checkY;
			y = checkX;
			break;
		default:
		case 0:
			x = checkX;
			y = checkY;
		}
		return buildingPathingPixelMap.getRGB(x, buildingPathingPixelMap.getHeight() - 1 - y);
	}

	public void addStateListener(final CUnitStateListener listener) {
		this.stateListenersUpdates.add(new StateListenerUpdate(listener, StateListenerUpdateType.ADD));
	}

	public void removeStateListener(final CUnitStateListener listener) {
		this.stateListenersUpdates.add(new StateListenerUpdate(listener, StateListenerUpdateType.REMOVE));
	}

	public boolean isCorpse() {
		return this.corpse;
	}

	public boolean isIllusion() {
		return this.illusion;
	}

	public float getIllusionDamageDealtFactor() {
		return this.illusionDamageDealtFactor;
	}

	/**
	 * The level scripts read for an ability the illusion lost, 0 if it never had it. Measured on 1.29: such abilities
	 * do nothing and refuse their orders but still read as present.
	 */
	public int getIllusionAbilityLevel(final int rawcode) {
		final Integer level = this.illusionAbilityLevels.get(rawcode);
		return level == null ? 0 : level;
	}

	/**
	 * Turns this freshly created unit into an illusion: it keeps moving, attacking, its hero attributes and its
	 * items (whose stat bonuses still apply) but loses every other ability, the hero skill menu, food use and its
	 * place in the player's hero list, and on death it vanishes with the buff's special effect instead of a corpse.
	 * A hero's illusion still reads its learned skills to scripts, a plain unit's its abilities that are not spells.
	 */
	public void makeIllusion(final CSimulation simulation, final float damageDealtFactor,
			final float damageTakenFactor, final War3ID buffId) {
		this.illusion = true;
		this.illusionDamageDealtFactor = damageDealtFactor;
		this.illusionDamageTakenFactor = damageTakenFactor;
		setExplodesOnDeath(true);
		setExplodesOnDeathBuffId(buffId);
		final CPlayer player = simulation.getPlayer(this.playerIndex);
		player.setUnitFoodUsed(this, 0);
		player.setUnitFoodMade(this, 0);
		simulation.getPlayerHeroes(this.playerIndex).remove(this);
		for (final CAbility ability : new ArrayList<>(getAbilities())) {
			if (ability instanceof CAbilityHero) {
				ability.setIconShowing(false);
			}
			else if (!(ability instanceof CAbilityMove) && !(ability instanceof CAbilityAttack)
					&& !(ability instanceof CAbilityInventory) && (ability.getItem() == null)
					&& !isPassiveKeptByIllusion(ability)) {
				if ((getHeroData() != null) || !isSpellLostByIllusion(ability)) {
					this.illusionAbilityLevels.put(ability.getAlias().getValue(),
							ability instanceof CLevelingAbility ? Math.max(1, ((CLevelingAbility) ability).getLevel())
									: 1);
				}
				remove(simulation, ability);
			}
		}
	}

	private static boolean isSpellLostByIllusion(final CAbility ability) {
		return (ability instanceof CAbilitySpell) || ((ability instanceof GenericSingleIconActiveAbility)
				&& (((GenericSingleIconActiveAbility) ability).getUIManaCost() > 0));
	}

	/**
	 * The original game keeps passives on an illusion (Bash still stuns); auras keep their icon but give nothing.
	 */
	private static boolean isPassiveKeptByIllusion(final CAbility ability) {
		return ability instanceof AbilityGenericSingleIconPassiveAbility;
	}

	public boolean isBoneCorpse() {
		return this.boneCorpse;
	}

	public boolean isUsableCorpse() {
		return this.boneCorpse || this.freshCorpse;
	}

	@Override
	public boolean canBeTargetedBy(final CSimulation simulation, final CUnit source,
			final EnumSet<CTargetType> targetsAllowed, final AbilityTargetCheckReceiver<CWidget> receiver) {
		return this.canBeTargetedBy(simulation, source, true, targetsAllowed, receiver);
	}

	public boolean canBeTargetedBy(final CSimulation simulation, final CUnit source, final boolean targeted,
			final EnumSet<CTargetType> targetsAllowed, final AbilityTargetCheckReceiver<CWidget> receiver) {
		return canBeTargetedBy(simulation, source, targeted, targetsAllowed, receiver, simulation.isScriptOrder());
	}

	public boolean canBeTargetedBy(final CSimulation simulation, final CUnit source, final boolean targeted,
			final EnumSet<CTargetType> targetsAllowed, final AbilityTargetCheckReceiver<CWidget> receiver,
			final boolean throughFog) {
		if ((this == source) && targetsAllowed.contains(CTargetType.NOTSELF)
				&& !targetsAllowed.contains(CTargetType.SELF)) {
			receiver.targetCheckFailed(CommandStringErrorKeys.UNABLE_TO_TARGET_SELF);
			return false;
		}
		// "player" alone means own units only; with "ally", "neutral" or "enemy" (Mass Teleport) it is one of the
		// alternatives that the alliance checks below weigh.
		if (targetsAllowed.contains(CTargetType.PLAYERUNITS) && (source.getPlayerIndex() != getPlayerIndex())
				&& !targetsAllowed.contains(CTargetType.ALLIES) && !targetsAllowed.contains(CTargetType.FRIEND)
				&& !targetsAllowed.contains(CTargetType.NEUTRAL) && !targetsAllowed.contains(CTargetType.ENEMIES)) {
			receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_ONE_OF_YOUR_OWN_UNITS);
			return false;
		}
		if (targetsAllowed.contains(CTargetType.NON_MAGIC_IMMUNE) && isMagicImmune()) {
			receiver.targetCheckFailed(CommandStringErrorKeys.THAT_UNIT_IS_IMMUNE_TO_MAGIC);
			return false;
		}
		if (isLimitedMagicImmune() && targetsAllowed.contains(CTargetType.NON_MAGIC_IMMUNE)
				&& !targetsAllowed.contains(CTargetType.LIMITED_MAGIC_IMMUNE)) {
			receiver.targetCheckFailed(CommandStringErrorKeys.THAT_UNIT_IS_IMMUNE_TO_MAGIC);
			return false;
		}
		// Measured: script attack orders may target a unit hidden only by the fog of war.
		final boolean fogOnly = throughFog && (this.invisLevels == 0);
		if (targeted && ((!fogOnly && !isVisible(simulation, source.getPlayerIndex()))
				|| (this.untargetablePlayers & (1 << source.getPlayerIndex())) != 0)) {
			receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_UNIT_WITH_THIS_ACTION);
			return false;
		}
		if (this.repairBlocked && targetsAllowed.contains(CTargetType.REPAIRABLE)) {
			if (this.hasUniqueFlag("b_Frozen")) {
				receiver.targetCheckFailed(CommandStringErrorKeys.TARGET_BUILDING_HAS_BEEN_FROZEN);
			} else if (this.hasUniqueFlag("b_LFire")) {
				receiver.targetCheckFailed(CommandStringErrorKeys.TARGET_BUILDING_HAS_LIQUID_FIRE);
			} else {
				receiver.targetCheckFailed(CommandStringErrorKeys.UNABLE_TO_TARGET_THIS_UNIT);
			}
			return false;
		}
		if (targetsAllowed.containsAll(this.targetedAs) || (!targetsAllowed.contains(CTargetType.GROUND)
				&& !targetsAllowed.contains(CTargetType.STRUCTURE) && !targetsAllowed.contains(CTargetType.AIR))) {
			final int sourcePlayerIndex = source.getPlayerIndex();
			final CPlayer sourcePlayer = simulation.getPlayer(sourcePlayerIndex);
			// "ally" and "player" name friendly targets too; the flags are alternatives, as in "friend".
			final boolean friendAllowed = targetsAllowed.contains(CTargetType.FRIEND)
					|| targetsAllowed.contains(CTargetType.ALLIES) || targetsAllowed.contains(CTargetType.PLAYERUNITS);
			if (!targetsAllowed.contains(CTargetType.ENEMIES)
					|| !sourcePlayer.hasAlliance(this.fakePlayerIndex, CAllianceType.PASSIVE)
					|| (friendAllowed
							&& sourcePlayer.hasAlliance(this.fakePlayerIndex, CAllianceType.SHARED_SPELLS))
					|| (targetsAllowed.contains(CTargetType.NEUTRAL)
							&& !sourcePlayer.hasAlliance(this.fakePlayerIndex, CAllianceType.SHARED_SPELLS))) {
				if (!friendAllowed
						|| sourcePlayer.hasAlliance(this.fakePlayerIndex, CAllianceType.SHARED_SPELLS)
						|| (targetsAllowed.contains(CTargetType.ENEMIES)
								&& !sourcePlayer.hasAlliance(this.fakePlayerIndex, CAllianceType.PASSIVE))
						|| (targetsAllowed.contains(CTargetType.NEUTRAL)
								&& sourcePlayer.hasAlliance(this.fakePlayerIndex, CAllianceType.PASSIVE))) {
					if (!targetsAllowed.contains(CTargetType.NEUTRAL)
							|| (sourcePlayer.hasAlliance(this.fakePlayerIndex, CAllianceType.PASSIVE)
									&& !sourcePlayer.hasAlliance(this.fakePlayerIndex, CAllianceType.SHARED_SPELLS))
							|| (targetsAllowed.contains(CTargetType.ENEMIES)
									&& !sourcePlayer.hasAlliance(this.fakePlayerIndex, CAllianceType.PASSIVE))
							|| (friendAllowed
									&& sourcePlayer.hasAlliance(this.fakePlayerIndex, CAllianceType.SHARED_SPELLS))) {
						if (!targetsAllowed.contains(CTargetType.MECHANICAL)
								|| this.classifications.contains(CUnitClassification.MECHANICAL)) {
							if (!targetsAllowed.contains(CTargetType.ORGANIC)
									|| !this.classifications.contains(CUnitClassification.MECHANICAL)) {
								// "ancient,nonancient" (Finger of Death) allows both, like the alliance flags.
								final boolean anyAncientness = targetsAllowed.contains(CTargetType.ANCIENT)
										&& targetsAllowed.contains(CTargetType.NONANCIENT);
								if (anyAncientness || !targetsAllowed.contains(CTargetType.ANCIENT)
										|| this.classifications.contains(CUnitClassification.ANCIENT)) {
									if (anyAncientness || !targetsAllowed.contains(CTargetType.NONANCIENT)
											|| !this.classifications.contains(CUnitClassification.ANCIENT)) {
										final boolean invulnerable = isInvulnerable();
										if ((!invulnerable && (targetsAllowed.contains(CTargetType.VULNERABLE)
												|| !targetsAllowed.contains(CTargetType.INVULNERABLE)))
												|| (invulnerable
														&& targetsAllowed.contains(CTargetType.INVULNERABLE))) {
											if (!targetsAllowed.contains(CTargetType.HERO) || (getHeroData() != null)) {
												if (!targetsAllowed.contains(CTargetType.NONHERO)
														|| (getHeroData() == null)) {
													if (isDead()) {
														if (isRaisable() && isDecays() && isUsableCorpse()) {
															if (targetsAllowed.contains(CTargetType.DEAD)) {
																return true;
															} else {
																receiver.targetCheckFailed(
																		CommandStringErrorKeys.TARGET_MUST_BE_LIVING);
															}
														} else {
															receiver.targetCheckFailed(
																	CommandStringErrorKeys.MUST_TARGET_A_UNIT_WITH_THIS_ACTION);
														}
													} else {
														if (!targetsAllowed.contains(CTargetType.DEAD)
																|| targetsAllowed.contains(CTargetType.ALIVE)) {
															return true;
														} else {
															receiver.targetCheckFailed(
																	CommandStringErrorKeys.MUST_TARGET_A_CORPSE);
														}
													}
												} else {
													receiver.targetCheckFailed(
															CommandStringErrorKeys.UNABLE_TO_TARGET_HEROES);
												}
											} else {
												receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_HERO);
											}
										} else {
											if (invulnerable) {
												receiver.targetCheckFailed(
														CommandStringErrorKeys.THAT_TARGET_IS_INVULNERABLE);
											} else {
												receiver.targetCheckFailed(
														CommandStringErrorKeys.UNABLE_TO_TARGET_THIS_UNIT);
											}
										}
									} else {
										receiver.targetCheckFailed(CommandStringErrorKeys.UNABLE_TO_TARGET_ANCIENTS);
									}
								} else {
									receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_AN_ANCIENT);
								}
							} else {
								receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_ORGANIC_UNITS);
							}
						} else {
							receiver.targetCheckFailed(CommandStringErrorKeys.UNABLE_TO_TARGET_ORGANIC_UNITS);
						}
					} else {
						receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_NEUTRAL_UNIT);
					}
				} else {
					receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_FRIENDLY_UNIT);
				}
			} else {
				receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_AN_ENEMY_UNIT);
			}
		} else {
			if (this.targetedAs.contains(CTargetType.GROUND) && !targetsAllowed.contains(CTargetType.GROUND)) {
				receiver.targetCheckFailed(CommandStringErrorKeys.UNABLE_TO_TARGET_GROUND_UNITS);
			} else if (this.targetedAs.contains(CTargetType.STRUCTURE)
					&& !targetsAllowed.contains(CTargetType.STRUCTURE)) {
				receiver.targetCheckFailed(CommandStringErrorKeys.UNABLE_TO_TARGET_BUILDINGS);
			} else if (this.targetedAs.contains(CTargetType.AIR) && !targetsAllowed.contains(CTargetType.AIR)) {
				receiver.targetCheckFailed(CommandStringErrorKeys.UNABLE_TO_TARGET_AIR_UNITS);
			} else if (this.targetedAs.contains(CTargetType.WARD) && !targetsAllowed.contains(CTargetType.WARD)) {
				receiver.targetCheckFailed(CommandStringErrorKeys.UNABLE_TO_TARGET_WARDS);
			} else if (targetsAllowed.contains(CTargetType.GROUND)) {
				receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_GROUND_UNIT);
			} else if (targetsAllowed.contains(CTargetType.STRUCTURE)) {
				receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_BUILDING);
			} else if (targetsAllowed.contains(CTargetType.AIR)) {
				receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_AN_AIR_UNIT);
			} else if (targetsAllowed.contains(CTargetType.WARD)) {
				receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_WARD);
			} else {
				receiver.targetCheckFailed(CommandStringErrorKeys.UNABLE_TO_TARGET_THIS_UNIT);
			}
		}
		return false;
	}

	public boolean isMovementDisabled() {
		return (this.moveBehavior == null) || this.moveDisabled;
		// TODO this used to directly return the state of whether our unit was a
		// building. Will it be a problem that I changed it?
		// I was trying to fix attack move on stationary units which was crashing
	}

	public boolean isMovementOnWaterAllowed() {
		return !isMovementDisabled() && getMovementType().isPathable((short) ~PathingFlags.UNSWIMABLE);
	}

	public MovementType getMovementType() {
		if (isMovementDisabled()) {
			return MovementType.DISABLED;
		}
		return this.movementOverride != null ? this.movementOverride : getUnitType().getMovementType();
		// later maybe it has unit instance override for windwalk, so this wrapper
		// exists to later mod
	}

	public void setNoCollisionMovementType(final boolean active) {
		if (active) {
			this.nextMovementOverride = MovementType.FOOT_NO_COLLISION;
		}
		else {
			this.nextMovementOverride = null;
		}
	}

	private void innerUpdateNoCollisionMovementType(final CWorldCollision worldCollision) {
		// NOTE: changing movement type will crash the game unless we also remove/add
		// the unit from world collision
		if (this.movementOverride != this.nextMovementOverride) {
			final boolean hasCollision = getCollisionRectangle() != null;
			if (hasCollision) {
				worldCollision.removeUnit(this);
			}
			this.movementOverride = this.nextMovementOverride;
			if (hasCollision) {
				worldCollision.addUnit(this);
			}
		}
	}

	public float getAcquisitionRange() {
		return this.acquisitionRange;
	}

	public void setAcquisitionRange(final float acquisitionRange) {
		this.acquisitionRange = acquisitionRange;
	}

	public void setCurrentHp(final CSimulation game, final float hpValue) {
		setLife(game, Math.min(hpValue, getMaximumLife()));
	}

	public void heal(final CSimulation game, final float lifeToRegain) {
		setLife(game, Math.min(getLife() + (lifeToRegain * this.healingReceivedFactor), getMaximumLife()));
	}

	public void multiplyHealingReceived(final float factor) {
		this.healingReceivedFactor *= factor;
	}

	public void restoreMana(final CSimulation game, final float manaToRegain) {
		setMana(Math.min(getMana() + manaToRegain, getMaximumMana()));
	}

	/**
	 * Brings a dead unit back. Measured on 1.29 (Resurrection): it uses its food again and counts again in its player's
	 * techtree (requirements, GetPlayerTechCount).
	 */
	public void resurrect(final CSimulation simulation) {
		resurrect(simulation, false);
	}

	/**
	 * Raises a corpse as Animate Dead does. Measured on 1.29: the raised unit uses no food and is left out of
	 * GetPlayerTechCount, GetPlayerUnitCount and GetPlayerTypedUnitCount.
	 */
	public void resurrectAsAnimatedCorpse(final CSimulation simulation) {
		resurrect(simulation, true);
	}

	private void resurrect(final CSimulation simulation, final boolean animatedCorpse) {
		final boolean wasCounted = !isDead() || this.animatedCorpse;
		this.animatedCorpse = animatedCorpse;
		simulation.getWorldCollision().removeUnit(this);
		this.corpse = false;
		this.boneCorpse = false;
		this.freshCorpse = false;
		this.deathTurnTick = 0;
		this.explodesOnDeath = false;
		this.explodesOnDeathBuffId = null;
		setLife(simulation, getMaximumLife());
		simulation.takeShadowPoolIds(this);
		if (!wasCounted && !animatedCorpse) {
			final CPlayer player = simulation.getPlayer(this.playerIndex);
			player.setUnitFoodUsed(this, this.unitType.getFoodUsed());
			player.setUnitFoodMade(this, this.unitType.getFoodMade());
			unlockTechtreeOnRevive(simulation);
		}
		simulation.getWorldCollision().addUnit(this);
		simulation.unitUpdatedType(this, this.typeId, true); // clear out some state
		this.unitAnimationListener.playAnimation(true, PrimaryTag.STAND, SequenceUtils.EMPTY, 0.0f, true);
	}

	/**
	 * Turns a unit just made by a script or an ability into a corpse that can be used at once, without a death:
	 * no death events, bounty or experience (CreateCorpse, Create Corpse, Exhume Corpses).
	 */
	public void becomeCorpse(final CSimulation game) {
		beginBehavior(game, null, true);
		this.orderQueue.clear();
		killPathingInstance();
		game.getWorldCollision().removeUnit(this);
		final CPlayer player = game.getPlayer(this.playerIndex);
		if (this.foodMade != 0) {
			player.setUnitFoodMade(this, 0);
		}
		if (this.foodUsed != 0) {
			player.setUnitFoodUsed(this, 0);
		}
		this.life = 0;
		this.corpse = true;
		this.boneCorpse = true;
		this.deathTurnTick = game.getGameTurnTick();
		game.getWorldCollision().addUnit(this);
		this.stateNotifier.lifeChanged();
	}

	/**
	 * A corpse picked up by a Meat Wagon: hidden, out of the world and not decaying while it is carried.
	 */
	public void holdCorpse(final CSimulation game) {
		game.getWorldCollision().removeUnit(this);
		this.collisionRectangle = null;
		setHidden(true);
		resetCorpseDecay(game);
	}

	public void resetCorpseDecay(final CSimulation game) {
		this.deathTurnTick = game.getGameTurnTick();
	}

	/**
	 * Puts a carried corpse back on the ground as a fresh corpse.
	 */
	public void releaseCorpse(final CSimulation game, final float x, final float y) {
		setX(x);
		setY(y);
		resetCorpseDecay(game);
		game.getWorldCollision().addUnit(this);
		setHidden(false);
		game.unitRepositioned(this);
	}

	public static void releaseStaticReferences() {
		autoAttackTargetFinderEnum = new AutoAttackTargetFinderEnum();
		autocastTargetFinderEnum = new AutocastTargetFinderEnum();
		regionCheckerImpl = new RegionCheckerImpl();
		UseAbilityOnTargetByIdVisitor.INSTANCE.reset(null, null, 0, 0);
	}

	private static final class AutocastTargetFinderEnum implements CUnitEnumFunction {
		private final static BooleanAbilityTargetCheckReceiver<CWidget> targetCheckReceiver = BooleanAbilityTargetCheckReceiver
				.<CWidget>getInstance();
		private CSimulation game;
		private CUnit source;
		private CAutocastAbility ability;
		private boolean disableMove;
		private AutocastType type;

		private CUnit currentUnitTarget;
		private double comparisonValue;

		private AutocastTargetFinderEnum reset(final CSimulation game, final CUnit source,
				final CAutocastAbility ability, final boolean disableMove) {
			this.game = game;
			this.source = source;
			this.ability = ability;
			this.disableMove = disableMove;
			this.type = ability.getAutocastType();

			this.currentUnitTarget = null;
			this.comparisonValue = Double.NaN;
			return this;
		}

		@Override
		public boolean call(final CUnit unit) {
			if (unit.isAutoTargetable() && !unit.isHidden() && this.type != AutocastType.NONE
					&& this.source.canReach(unit, AUTOCAST_SEARCH_RANGE)) {
				switch (this.type) {
				case ATTACKINGALLY:
				case ATTACKINGENEMY:
					if ((unit.getCurrentBehavior() != null) && (unit.getCurrentBehavior() instanceof CRangedBehavior)) {
						final CRangedBehavior rbeh = (CRangedBehavior) unit.getCurrentBehavior();
						if (rbeh.getHighlightOrderId() == OrderIds.attack) {
							final AbilityTarget target = rbeh.getTarget();
							final CUnit targetUnit = target.visit(AbilityTargetVisitor.UNIT);
							if (targetUnit != null) {
								if (((this.type == AutocastType.ATTACKINGALLY)
										&& this.game.getPlayer(this.source.getPlayerIndex())
												.hasAlliance(targetUnit.getPlayerIndex(), CAllianceType.PASSIVE))
										|| ((this.type == AutocastType.ATTACKINGENEMY)
												&& !this.game.getPlayer(this.source.getPlayerIndex()).hasAlliance(
														targetUnit.getPlayerIndex(), CAllianceType.PASSIVE))) {
									targetCheckReceiver.reset();
									this.ability.checkCanAutoTarget(this.game, this.source, this.source.playerIndex,
											this.ability.getBaseOrderId(), unit, targetCheckReceiver);
									if (targetCheckReceiver.isTargetable()) {
										if (this.currentUnitTarget == null) {
											this.currentUnitTarget = unit;
											this.comparisonValue = this.source.distance(unit);
										} else {
											final double dist = this.source.distance(unit);
											if (dist < this.comparisonValue) {
												this.currentUnitTarget = unit;
												this.comparisonValue = dist;
											}
										}
									}
								}
							}
						}
					}
					break;
				case ATTACKTARGETING:
					if (!this.game.getPlayer(this.source.getPlayerIndex()).hasAlliance(unit.getPlayerIndex(),
							CAllianceType.PASSIVE) && !unit.isDead() && !unit.isInvulnerable()
							&& !unit.isUnitType(CUnitTypeJass.SLEEPING)) {
						for (final CUnitAttack attack : this.source.getCurrentAttacks()) {
							if (this.source.canReach(unit, this.source.acquisitionRange)
									&& unit.canBeTargetedBy(this.game, this.source, attack.getTargetsAllowed())
									&& (this.source.distance(unit) >= this.source.getUnitType()
											.getMinimumAttackRange())) {
								targetCheckReceiver.reset();
								this.ability.checkCanAutoTarget(this.game, this.source, this.source.playerIndex,
										this.ability.getBaseOrderId(), unit, targetCheckReceiver);
								if (targetCheckReceiver.isTargetable()) {
									if (this.currentUnitTarget == null) {
										this.currentUnitTarget = unit;
										this.comparisonValue = this.source.distance(unit);
									} else {
										final double dist = this.source.distance(unit);
										if (dist < this.comparisonValue) {
											this.currentUnitTarget = unit;
											this.comparisonValue = dist;
										}
									}
								}
							}
						}
					}

					break;
				case HIGESTHP:
					targetCheckReceiver.reset();
					this.ability.checkCanAutoTarget(this.game, this.source, this.source.playerIndex,
							this.ability.getBaseOrderId(), unit, targetCheckReceiver);
					if (targetCheckReceiver.isTargetable()) {
						if (this.currentUnitTarget == null) {
							this.currentUnitTarget = unit;
							this.comparisonValue = unit.getLife() / unit.getMaximumLife();
						} else {
							final double ratio = unit.getLife() / unit.getMaximumLife();
							if (ratio > this.comparisonValue) {
								this.currentUnitTarget = unit;
								this.comparisonValue = ratio;
							}
						}
					}
					break;
				case LOWESTHP:
					targetCheckReceiver.reset();
					this.ability.checkCanAutoTarget(this.game, this.source, this.source.playerIndex,
							this.ability.getBaseOrderId(), unit, targetCheckReceiver);
					if (targetCheckReceiver.isTargetable()) {
						if (unit.getLife() < unit.getMaximumLife()) {
							if (this.currentUnitTarget == null) {
								this.currentUnitTarget = unit;
								this.comparisonValue = unit.getLife() / unit.getMaximumLife();
							} else {
								final double ratio = unit.getLife() / unit.getMaximumLife();
								if (ratio < this.comparisonValue) {
									this.currentUnitTarget = unit;
									this.comparisonValue = ratio;
								}
							}
						}
					}
					break;
				case FARTHESTVALID:
					targetCheckReceiver.reset();
					this.ability.checkCanAutoTarget(this.game, this.source, this.source.getPlayerIndex(), this.ability.getBaseOrderId(), unit,
							targetCheckReceiver);
					if (targetCheckReceiver.isTargetable()) {
						if (this.currentUnitTarget == null) {
							this.currentUnitTarget = unit;
							this.comparisonValue = this.source.distance(unit);
						} else {
							final double dist = this.source.distance(unit);
							if (dist > this.comparisonValue) {
								this.currentUnitTarget = unit;
								this.comparisonValue = dist;
							}
						}
					}
					break;
				case NEARESTVALID:
					targetCheckReceiver.reset();
					this.ability.checkCanAutoTarget(this.game, this.source, this.source.playerIndex,
							this.ability.getBaseOrderId(), unit, targetCheckReceiver);
					if (targetCheckReceiver.isTargetable()) {
						if (this.currentUnitTarget == null) {
							this.currentUnitTarget = unit;
							this.comparisonValue = this.source.distance(unit);
						} else {
							final double dist = this.source.distance(unit);
							if (dist < this.comparisonValue) {
								this.currentUnitTarget = unit;
								this.comparisonValue = dist;
							}
						}
					}
					break;
				case NEARESTENEMY:
					targetCheckReceiver.reset();
					this.ability.checkCanAutoTarget(this.game, this.source, this.source.playerIndex,
							this.ability.getBaseOrderId(), unit, targetCheckReceiver);
					if (targetCheckReceiver.isTargetable() && !this.game.getPlayer(this.source.getPlayerIndex())
							.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)) {
						if (this.currentUnitTarget == null) {
							this.currentUnitTarget = unit;
							this.comparisonValue = this.source.distance(unit);
						} else {
							final double dist = this.source.distance(unit);
							if (dist < this.comparisonValue) {
								this.currentUnitTarget = unit;
								this.comparisonValue = dist;
							}
						}
					}
					break;
				case NONE:
				case NOTARGET:
				default:
					break;

				}
			}

			return false;
		}
	}

	private static final class AutoAttackTargetFinderEnum implements CUnitEnumFunction {
		private CSimulation game;
		private CUnit source;
		private boolean disableMove;
		private EnumSet<CTargetType> priorityTargets;
		private boolean foundAnyTarget;
		private CUnit nearestTarget;
		private CUnitAttack nearestTargetAttack;
		private double nearestTargetDistance;
		private int nearestTargetRank;

		private AutoAttackTargetFinderEnum reset(final CSimulation game, final CUnit source,
				final boolean disableMove, final EnumSet<CTargetType> priorityTargets) {
			this.game = game;
			this.source = source;
			this.disableMove = disableMove;
			this.priorityTargets = priorityTargets;
			this.foundAnyTarget = false;
			this.nearestTarget = null;
			this.nearestTargetAttack = null;
			this.nearestTargetDistance = Double.MAX_VALUE;
			this.nearestTargetRank = Integer.MIN_VALUE;
			return this;
		}

		/**
		 * Attacks the target the enumeration found, as the original game picks it: the best ranked, then the
		 * nearest.
		 */
		private boolean attackNearest() {
			if (this.nearestTarget != null) {
				// The events below run triggers that may look for targets with this same finder.
				final CSimulation game = this.game;
				final CUnit source = this.source;
				final CUnit target = this.nearestTarget;
				final CBehavior attack = source.getAttackBehavior().reset(game, OrderIds.attack,
						this.nearestTargetAttack, target, this.disableMove, CBehaviorAttackListener.DO_NOTHING);
				source.getAttackBehavior().delayFirstSwing(game);
				source.beginBehavior(game, attack, true);
				this.foundAnyTarget = true;
				source.fireAcquiredTargetEvents(game, target);
				source.alertCreeps(game, target);
				return true;
			}
			return this.foundAnyTarget;
		}

		@Override
		public boolean call(final CUnit unit) {
			if ((this.priorityTargets != null) && !isPriorityAttackTarget(unit, this.priorityTargets)) {
				return false;
			}
			if (unit.isAutoTargetable() && !unit.isHidden() && !this.source.isHidden()
					&& (this.source.getAttackBehavior() != null)
					&& !this.source.getFirstAbilityOfType(CAbilityAttack.class).isDisabled()) {
				// TODO this "attack behavior null" check was added for some weird Root edge
				// case with NE, maybe
				// refactor it later
				if (!this.game.getPlayer(this.source.getPlayerIndex()).hasAlliance(unit.getPlayerIndex(),
						CAllianceType.PASSIVE) && !unit.isDead() && !unit.isInvulnerable()
						&& !unit.isUnitType(CUnitTypeJass.SLEEPING)) {
					for (final CUnitAttack attack : this.source.getCurrentAttacks()) {
						if (this.source.canReach(unit, this.source.acquisitionRange)
								&& unit.canBeTargetedBy(this.game, this.source, attack.getTargetsAllowed())
								&& (this.source.distance(unit) >= this.source.getUnitType().getMinimumAttackRange())) {
							if (!(unit.isUnitType(CUnitTypeJass.ETHEREAL)
									&& (attack.getAttackType() != CAttackType.MAGIC)
									&& (attack.getAttackType() != CAttackType.SPELLS))
									&& !(this.game.getGameplayConstants().isMagicImmuneResistsDamage()
											&& unit.isUnitType(CUnitTypeJass.MAGIC_IMMUNE)
											&& (attack.getAttackType() == CAttackType.MAGIC))) {
								final double distance = this.source.distance(unit);
								final int rank = acquireRank(this.game, this.source, unit);
								if ((rank > this.nearestTargetRank)
										|| ((rank == this.nearestTargetRank) && (distance < this.nearestTargetDistance))) {
									this.nearestTarget = unit;
									this.nearestTargetAttack = attack;
									this.nearestTargetDistance = distance;
									this.nearestTargetRank = rank;
								}
								return false;
							}
						}
					}
				}
			}
			return false;
		}
	}

	/**
	 * Measured on 1.29: an idle unit picking a target in its acquisition range takes, before any nearer one, a unit it
	 * can hit from where it stands, then a hero, then a unit that can hit it from where that one stands (a Rifleman in
	 * range), then other units; workers and units that cannot fight (paused) come after all of these, whatever their
	 * reach, and buildings last, a tower that can hit it before other buildings. A hero under an order (moving,
	 * attacking, holding position) has no lead over other units: a nearer Footman is taken first.
	 */
	private static int acquireRank(final CSimulation game, final CUnit source, final CUnit target) {
		if (target.isBuilding()) {
			return target.canAttackFromHere(game, source) ? 1 : 0;
		}
		if (target.isPaused() || target.getCurrentAttacks().isEmpty() || target.isDisableAttacks()
				|| target.getClassifications().contains(CUnitClassification.PEON)) {
			return 2;
		}
		if (source.canAttackFromHere(game, target)) {
			return 6;
		}
		if (target.isHero() && (target.lastStartedOrder == null)) {
			return 5;
		}
		if (target.canAttackFromHere(game, source)) {
			return 4;
		}
		return 3;
	}

	private boolean canAttackFromHere(final CSimulation game, final CUnit target) {
		if (this.paused || this.isDisableAttacks() || (this.attackBehavior == null)) {
			return false;
		}
		for (final CUnitAttack attack : getCurrentAttacks()) {
			if (canReach(target, attack.getRange()) && target.canBeTargetedBy(game, this, attack.getTargetsAllowed())) {
				return true;
			}
		}
		return false;
	}

	public CBehaviorMove getMoveBehavior() {
		return this.moveBehavior;
	}

	public void setMoveBehavior(final CBehaviorMove moveBehavior) {
		this.moveBehavior = moveBehavior;
	}

	public CBehaviorAttack getAttackBehavior() {
		return this.attackBehavior;
	}

	public void setAttackBehavior(final CBehaviorAttack attackBehavior) {
		this.attackBehavior = attackBehavior;
	}

	public void setAttackMoveBehavior(final CBehaviorAttackMove attackMoveBehavior) {
		this.attackMoveBehavior = attackMoveBehavior;
	}

	public CBehaviorAttackMove getAttackMoveBehavior() {
		return this.attackMoveBehavior;
	}

	public CBehaviorStop getStopBehavior() {
		return this.stopBehavior;
	}

	public void setFollowBehavior(final CBehaviorFollow followBehavior) {
		this.followBehavior = followBehavior;
	}

	public void setPatrolBehavior(final CBehaviorPatrol patrolBehavior) {
		this.patrolBehavior = patrolBehavior;
	}

	public void setHoldPositionBehavior(final CBehaviorHoldPosition holdPositionBehavior) {
		this.holdPositionBehavior = holdPositionBehavior;
	}

	public void setBoardTransportBehavior(final CBehaviorBoardTransport boardTransportBehavior) {
		this.boardTransportBehavior = boardTransportBehavior;
	}

	public CBehaviorFollow getFollowBehavior() {
		return this.followBehavior;
	}

	public CBehaviorPatrol getPatrolBehavior() {
		return this.patrolBehavior;
	}

	public CBehaviorHoldPosition getHoldPositionBehavior() {
		return this.holdPositionBehavior;
	}

	public CBehaviorBoardTransport getBoardTransportBehavior() {
		return this.boardTransportBehavior;
	}

	public CBehavior pollNextOrderBehavior(final CSimulation game) {
		if (this.defaultBehavior != this.stopBehavior) {
			// kind of a stupid hack, meant to align in feel with some behaviors that were
			// observed on War3
			return this.defaultBehavior;
		}
		if (this.interruptedBehavior != null) {
			return this.interruptedBehavior;
		}
		final COrder order = this.orderQueue.poll();
		final CBehavior nextOrderBehavior = beginOrder(game, order);
		this.stateNotifier.waypointsChanged();
		return nextOrderBehavior;
	}

	public boolean isMoving() {
		return getCurrentBehavior() instanceof CBehaviorMove;
	}

	public void setConstructing(final boolean constructing) {
		this.constructing = constructing;
		if (constructing) {
			this.unitAnimationListener.playAnimation(true, PrimaryTag.BIRTH, SequenceUtils.EMPTY, 0.0f, true);
		}
	}

	public void setConstructingPaused(final boolean constructingPaused) {
		this.constructingPaused = constructingPaused;
	}

	public void setConstructionProgress(final float constructionProgress) {
		this.constructionProgress = constructionProgress;
	}

	public boolean isConstructing() {
		return this.constructing && (this.upgradeIdType == null);
	}

	public boolean isConstructingPaused() {
		return this.constructingPaused;
	}

	public boolean isUpgrading() {
		return this.constructing && (this.upgradeIdType != null);
	}

	public War3ID getUpgradeIdType() {
		return this.upgradeIdType;
	}

	public boolean isConstructingOrUpgrading() {
		return this.constructing;
	}

	public float getConstructionProgress() {
		return this.constructionProgress;
	}

	public void setHidden(final boolean hidden) {
		this.hidden = hidden;
		this.stateNotifier.hideStateChanged();
	}

	public void setPaused(final boolean paused) {
		if (paused) {
			this.addStateModBuff(pauseBuff);
		} else {
			this.removeStateModBuff(pauseBuff);
		}
		this.computeUnitState(null, StateModBuffType.PAUSED);
	}

	public boolean isPaused() {
		return this.paused;
	}

	public void setAcceptingOrders(final boolean acceptingOrders) {
		this.acceptingOrders = acceptingOrders;
	}

	public boolean isHidden() {
		return this.hidden;
	}

	/**
	 * SetUnitInvulnerable. Measured on 1.29: it adds or removes the Invulnerable ability ('Avul'), so it is one flag
	 * however often it is set, UnitRemoveAbility 'Avul' clears it, and it neither ends nor is ended by spell sources
	 * (Divine Shield, Big Bad Voodoo, a gold mine visit), which are INVULNERABLE state mod buffs of their own.
	 */
	public void setInvulnerable(final CSimulation simulation, final boolean invulnerable) {
		// Saves from before 'Avul' kept the flag in scriptInvulnerable.
		this.scriptInvulnerable = false;
		final CLevelingAbility existing = getAbility(
				GetAbilityByRawcodeVisitor.getInstance().reset(INVULNERABLE_ABILITY_ID));
		if (invulnerable && (existing == null)) {
			final CAbilityType<?> type = simulation.getAbilityData().getAbilityType(INVULNERABLE_ABILITY_ID);
			if (type != null) {
				add(simulation, type.createAbility(simulation.getHandleIdAllocator().createId()));
			}
			else {
				this.scriptInvulnerable = true;
			}
		}
		else if (!invulnerable && (existing != null)) {
			remove(simulation, existing);
		}
		this.invulnerable = this.scriptInvulnerable || this.buffInvulnerable;
	}

	public void setInvulnerableSource(final CSimulation simulation, final StateModBuff source, final boolean active) {
		if (active) {
			addStateModBuff(source);
		}
		else {
			removeStateModBuff(source);
		}
		computeUnitState(simulation, StateModBuffType.INVULNERABLE);
	}

	/**
	 * A worker leaving a gold mine or an Orc building it was inside. Saves from before the per-source state kept that
	 * in the script flag, which leaving cleared.
	 */
	public void endInsideInvulnerable(final CSimulation simulation) {
		this.scriptInvulnerable = false;
		setInvulnerableSource(simulation, INSIDE_INVULNERABLE, false);
	}

	@Override
	public boolean isInvulnerable() {
		return this.invulnerable;
	}

	public boolean isAutoTargetable() {
		return this.autoTargetable;
	}

	public void setWorker(final CUnit unit, final boolean inside) {
		this.worker = unit;
		this.workerInside = inside;
	}

	public CUnit getWorker() {
		return this.worker;
	}

	public void addPowerWorker() {
		this.powerWorkers++;
	}

	public void removePowerWorker() {
		this.powerWorkers--;
	}

	public int getPowerWorkers() {
		return this.powerWorkers;
	}

	public void nudgeAround(final CSimulation simulation, final CUnit structure) {
		setPointAndCheckUnstuck(structure.getX(), structure.getY(), simulation);
	}

	/**
	 * Measured in 1.29: a trained unit goes to the first 32 cell, along the same square spiral as
	 * {@link #findNearestFreeCell} from the trainer's centre, whose lower left corner lies outside the trainer's whole
	 * pathing texture (its walkable border too), whatever the unit's collision size. The spiral's first leg turns with
	 * the rally point: none goes down, up and to the right goes up, up and to the left goes left, and so on. The
	 * centre must also be pathable for this unit, because the engine cannot move a unit off a blocked start.
	 */
	public void placeTrainedUnit(final CSimulation simulation, final CUnit trainer, final AbilityTarget rallyPoint) {
		final ArgbImage trainerPathing = trainer.getUnitType().getBuildingPathingPixelMap();
		if (isBuilding() || !trainer.isBuilding() || (trainerPathing == null)) {
			nudgeAround(simulation, trainer);
			return;
		}
		final CWorldCollision collision = simulation.getWorldCollision();
		final PathingGrid pathingGrid = simulation.getPathingGrid();
		final float collisionSize = this.unitType.getCollisionSize();
		if (this.collisionRectangle != null) {
			tempRect.set(this.collisionRectangle);
		}
		else {
			tempRect.setSize(16, 16);
		}
		final float halfWidth = (trainerPathing.getWidth() * 32) / 2f;
		final float halfHeight = (trainerPathing.getHeight() * 32) / 2f;
		final float trainerX = trainer.getX();
		final float trainerY = trainer.getY();
		int direction = 0;
		if ((rallyPoint != null) && (rallyPoint != trainer)) {
			final double rallyDx = rallyPoint.getX() - trainerX;
			final double rallyDy = rallyPoint.getY() - trainerY;
			if ((rallyDx != 0) || (rallyDy != 0)) {
				double angle = StrictMath.toDegrees(StrictMath.atan2(rallyDy, rallyDx));
				if (angle < 0) {
					angle += 360;
				}
				final int[] directionByQuadrant = { 2, 3, 0, 1 };
				direction = directionByQuadrant[Math.min(3, (int) (angle / 90))];
			}
		}
		final float baseX = (float) Math.floor(trainerX / 32f) * 32f;
		final float baseY = (float) Math.floor(trainerY / 32f) * 32f;
		final int[] stepX = { 0, 1, 0, -1 };
		final int[] stepY = { -1, 0, 1, 0 };
		int cellX = 0;
		int cellY = 0;
		for (int legLength = 1; legLength <= 128; legLength++) {
			for (int leg = 0; leg < 2; leg++) {
				for (int step = 0; step < legLength; step++) {
					final float cx = baseX + (cellX * 32f);
					final float cy = baseY + (cellY * 32f);
					if ((Math.abs(cx - trainerX) > halfWidth) || (Math.abs(cy - trainerY) > halfHeight)) {
						tempRect.setCenter(cx + 16f, cy + 16f);
						if (!collision.intersectsAnythingOtherThan(tempRect, this, getMovementType(), false)
								&& pathingGrid.isPathable(cx, cy, getMovementType(), collisionSize)
								&& pathingGrid.isPathable(cx + 16f, cy + 16f, getMovementType(), collisionSize)) {
							setPoint(cx + 16f, cy + 16f, collision, simulation.getRegionManager());
							simulation.unitRepositioned(this);
							return;
						}
					}
					cellX += stepX[direction];
					cellY += stepY[direction];
				}
				direction = (direction + 1) % 4;
			}
		}
		nudgeAround(simulation, trainer);
	}

	@Override
	public void setLife(final CSimulation simulation, final float life) {
		applyLife(simulation, life);
		checkStateEvents(false);
	}

	private void applyLife(final CSimulation simulation, final float life) {
		final boolean wasDead = isDead();
		super.setLife(simulation, life);
		if (isDead() && !wasDead) {
			kill(simulation, null);
		}
		this.stateNotifier.lifeChanged();
	}

	private int queue(final CSimulation game, final War3ID rawcode, final QueueItemType queueItemType) {
		for (int i = 0; i < this.buildQueue.length; i++) {
			if (this.buildQueue[i] == null) {
				setBuildQueueItem(game, i, rawcode, queueItemType);
				if ((queueItemType == QueueItemType.UNIT) || (queueItemType == QueueItemType.RESEARCH)
						|| (queueItemType == QueueItemType.SACRIFICE)) {
					final CPlayer player = game.getPlayer(this.playerIndex);
					player.addTechtreeInProgress(game, rawcode);
				}
				return i;
			}
		}
		return -1;
	}

	/**
	 * Measured on 1.29: an item waiting at the front for food takes it as soon as the player's food used drops or food
	 * cap rises, in the middle of whatever changed it; raising the food cap ceiling alone does not wake it.
	 */
	private void retryQueuedFood(final CSimulation game) {
		final CPlayer player = game.getPlayer(this.playerIndex);
		final War3ID rawcode = this.buildQueue[0];
		if ((rawcode == null) || this.queuedUnitFoodPaid || isDead()) {
			player.removeFoodWaiter(this);
			return;
		}
		final int foodNeeded;
		if (this.buildQueueTypes[0] == QueueItemType.UNIT) {
			foodNeeded = game.getUnitData().getUnitType(rawcode).getFoodUsed();
		}
		else if (this.buildQueueTypes[0] == QueueItemType.HERO_REVIVE) {
			foodNeeded = game.getUnit(rawcode.getValue()).getUnitType().getFoodUsed();
		}
		else {
			foodNeeded = 0;
		}
		if ((player.getFoodUsed() + foodNeeded) <= player.getFoodCapLimit()) {
			player.removeFoodWaiter(this);
			this.queuedUnitFoodPaid = true;
			player.setFoodUsed(player.getFoodUsed() + foodNeeded);
			fireBuildQueueStartEvents(game);
		}
	}

	/**
	 * Measured on 1.29: the start events of a queue item fire when it begins progress at the front of the queue,
	 * after it was paid for, and not while it waits behind another item or for food.
	 */
	private void fireBuildQueueStartEvents(final CSimulation game) {
		final War3ID rawcode = this.buildQueue[0];
		if ((rawcode == null) || !this.queuedUnitFoodPaid) {
			return;
		}
		switch (this.buildQueueTypes[0]) {
		case UNIT:
		case SACRIFICE:
			fireTrainEvents(game, JassGameEventsWar3.EVENT_UNIT_TRAIN_START,
					JassGameEventsWar3.EVENT_PLAYER_UNIT_TRAIN_START, rawcode);
			break;
		case RESEARCH:
			fireResearchEvents(game, JassGameEventsWar3.EVENT_UNIT_RESEARCH_START,
					JassGameEventsWar3.EVENT_PLAYER_UNIT_RESEARCH_START, rawcode);
			break;
		default:
			break;
		}
	}

	public War3ID[] getBuildQueue() {
		return this.buildQueue;
	}

	public QueueItemType[] getBuildQueueTypes() {
		return this.buildQueueTypes;
	}

	public boolean isBuildQueueActive() {
		return this.buildQueueTypes[0] != null;
	}

	/**
	 * The unit type queued for training at the given queue slot, else null. Measured on 1.29: every unit in a
	 * training queue, not only the one in progress, counts as an incomplete unit of its player.
	 */
	public War3ID getQueuedTrainingUnitTypeId(final int index) {
		if ((this.buildQueueTypes[index] == QueueItemType.UNIT) && !isDead()) {
			return this.buildQueue[index];
		}
		return null;
	}

	public float getBuildQueueTimeRemaining(final CSimulation simulation) {
		if (!isBuildQueueActive()) {
			return 0;
		}
		switch (this.buildQueueTypes[0]) {
		case RESEARCH: {
			final War3ID rawcode = this.buildQueue[0];
			final CUpgradeType trainedUnitType = simulation.getUpgradeData().getType(rawcode);
			return trainedUnitType.getBuildTime(simulation.getPlayer(this.playerIndex).getTechtreeUnlocked(rawcode));
		}
		case SACRIFICE:
		case UNIT: {
			final CUnitType trainedUnitType = simulation.getUnitData().getUnitType(this.buildQueue[0]);
			return trainedUnitType.getBuildTime();
		}
		case HERO_REVIVE: {
			return heroReviveTime(simulation, simulation.getUnit(this.buildQueue[0].getValue()));
		}
		default:
			return 0;
		}
	}

	private static float heroReviveTime(final CSimulation simulation, final CUnit hero) {
		return simulation.getGameplayConstants().getHeroReviveTime(hero.getUnitType().getBuildTime(),
				hero.getHeroData().getHeroLevel())
				* simulation.getPlayer(hero.getPlayerIndex()).getHandicapReviveTime();
	}

	public void cancelBuildQueueItem(final CSimulation game, final int cancelIndex) {
		cancelBuildQueueItem(game, cancelIndex, true);
	}

	/**
	 * The cancel order takes back the last item of the queue, not the one in progress.
	 */
	public void cancelLastBuildQueueItem(final CSimulation game) {
		int index = this.buildQueueTypes.length - 1;
		while ((index > 0) && (this.buildQueueTypes[index] == null)) {
			index--;
		}
		cancelBuildQueueItem(game, index, true);
	}

	/**
	 * Measured on 1.29: a building that dies refunds its queue and gives back the food before its death events, but
	 * fires no cancel events.
	 */
	public void clearBuildQueueOnDeath(final CSimulation game) {
		for (int i = this.buildQueueTypes.length - 1; i >= 0; i--) {
			cancelBuildQueueItem(game, i, false);
		}
	}

	private void cancelBuildQueueItem(final CSimulation game, final int cancelIndex, final boolean fireEvents) {
		if ((cancelIndex >= 0) && (cancelIndex < this.buildQueueTypes.length)) {
			final QueueItemType cancelledType = this.buildQueueTypes[cancelIndex];
			if (cancelledType != null) {
				if (fireEvents) {
					// Measured on 1.29: a queued item fires its cancel events too, and they see the player's
					// resources and food from before the refund.
					switch (cancelledType) {
					case UNIT:
					case SACRIFICE:
						fireTrainEvents(game, JassGameEventsWar3.EVENT_UNIT_TRAIN_CANCEL,
								JassGameEventsWar3.EVENT_PLAYER_UNIT_TRAIN_CANCEL, this.buildQueue[cancelIndex]);
						break;
					case RESEARCH:
						fireResearchEvents(game, JassGameEventsWar3.EVENT_UNIT_RESEARCH_CANCEL,
								JassGameEventsWar3.EVENT_PLAYER_UNIT_RESEARCH_CANCEL, this.buildQueue[cancelIndex]);
						break;
					default:
						break;
					}
					if (this.buildQueueTypes[cancelIndex] != cancelledType) {
						// a cancel trigger already changed the queue
						return;
					}
				}
				if (cancelIndex == 0) {
					this.constructionProgress = 0.0f;
					switch (cancelledType) {
					case SACRIFICE:
					case RESEARCH: {
						break;
					}
					case UNIT: {
						final CPlayer player = game.getPlayer(this.playerIndex);
						final CUnitType unitType = game.getUnitData().getUnitType(this.buildQueue[cancelIndex]);
						player.setFoodUsed(player.getFoodUsed() - unitType.getFoodUsed());
						break;
					}
					case HERO_REVIVE: {
						final CPlayer player = game.getPlayer(this.playerIndex);
						final CUnitType unitType = game.getUnit(this.buildQueue[cancelIndex].getValue()).getUnitType();
						player.setFoodUsed(player.getFoodUsed() - unitType.getFoodUsed());
						break;
					}
					}
				}
				switch (cancelledType) {
				case RESEARCH: {
					final CPlayer player = game.getPlayer(this.playerIndex);
					final CUpgradeType upgradeType = game.getUpgradeData().getType(this.buildQueue[cancelIndex]);
					player.refundFor(upgradeType);
					player.removeTechtreeInProgress(game, this.buildQueue[cancelIndex]);
					break;
				}
				case UNIT: {
					final CPlayer player = game.getPlayer(this.playerIndex);
					final CUnitType unitType = game.getUnitData().getUnitType(this.buildQueue[cancelIndex]);
					refundQueuedUnit(player, unitType, cancelIndex);
					player.removeTechtreeInProgress(game, this.buildQueue[cancelIndex]);
					break;
				}
				case SACRIFICE: {
					final CPlayer player = game.getPlayer(this.playerIndex);
					final CUnitType unitType = game.getUnitData().getUnitType(this.buildQueue[cancelIndex]);
					refundQueuedUnit(player, unitType, cancelIndex);
					player.removeTechtreeInProgress(game, this.buildQueue[cancelIndex]);

					getWorker().setHidden(false);
					break;
				}
				case HERO_REVIVE: {
					final CPlayer player = game.getPlayer(this.playerIndex);
					final CUnit hero = game.getUnit(this.buildQueue[cancelIndex].getValue());
					final CUnitType unitType = hero.getUnitType();
					final CAbilityHero heroData = hero.getHeroData();
					heroData.setReviving(false);
					final CGameplayConstants gameplayConstants = game.getGameplayConstants();
					player.refund(
							gameplayConstants.getHeroReviveGoldCost(unitType.getGoldCost(), heroData.getHeroLevel()),
							gameplayConstants.getHeroReviveLumberCost(unitType.getLumberCost(),
									heroData.getHeroLevel()));
					break;
				}
				}
				shiftBuildQueue(game, cancelIndex);
				if (fireEvents && (cancelIndex == 0)) {
					fireBuildQueueStartEvents(game);
				}
				this.stateNotifier.queueChanged();
			}
		}
	}

	private void shiftBuildQueue(final CSimulation game, final int fromIndex) {
		for (int i = fromIndex; i < (this.buildQueue.length - 1); i++) {
			setBuildQueueItem(game, i, this.buildQueue[i + 1], this.buildQueueTypes[i + 1]);
			this.buildQueueHeroToken[i] = this.buildQueueHeroToken[i + 1];
		}
		setBuildQueueItem(game, this.buildQueue.length - 1, null, null);
		this.buildQueueHeroToken[this.buildQueue.length - 1] = false;
	}

	/**
	 * A cancelled hero that was trained for a free hero token gives the token back, not its price.
	 */
	private void refundQueuedUnit(final CPlayer player, final CUnitType unitType, final int index) {
		if (this.buildQueueHeroToken[index]) {
			player.setHeroTokens(player.getHeroTokens() + 1);
		}
		else {
			player.refundFor(unitType);
		}
	}

	private void payForQueuedUnit(final CPlayer player, final CUnitType unitType) {
		int index = this.buildQueue.length - 1;
		while ((index > 0) && (this.buildQueue[index] == null)) {
			index--;
		}
		final boolean heroToken = unitType.isHero() && (player.getHeroTokens() > 0);
		this.buildQueueHeroToken[index] = heroToken;
		if (heroToken) {
			player.setHeroTokens(player.getHeroTokens() - 1);
		}
		else {
			player.chargeFor(unitType);
		}
	}

	public void setBuildQueueItem(final CSimulation game, final int index, final War3ID rawcode,
			final QueueItemType queueItemType) {
		this.buildQueue[index] = rawcode;
		this.buildQueueTypes[index] = queueItemType;
		final CPlayer player = game.getPlayer(this.playerIndex);
		if (index == 0) {
			this.queuedUnitFoodPaid = true;
			player.removeFoodWaiter(this);
			if (rawcode != null) {
				if (queueItemType == QueueItemType.UNIT) {
					final CUnitType unitType = game.getUnitData().getUnitType(this.buildQueue[index]);
					if (unitType.getFoodUsed() != 0) {
						final int newFoodUsed = player.getFoodUsed() + unitType.getFoodUsed();
						if (newFoodUsed <= player.getFoodCapLimit()) {
							player.setFoodUsed(newFoodUsed);
						} else {
							this.queuedUnitFoodPaid = false;
							player.addFoodWaiter(this, () -> retryQueuedFood(game));
							game.getCommandErrorListener().showInterfaceError(this.playerIndex,
									CommandStringErrorKeys.NOT_ENOUGH_FOOD);
							player.removeTechtreeInProgress(game, rawcode);
						}
					}
				} else if (queueItemType == QueueItemType.HERO_REVIVE) {
					final CUnitType unitType = game.getUnit(this.buildQueue[index].getValue()).getUnitType();
					if (unitType.getFoodUsed() != 0) {
						final int newFoodUsed = player.getFoodUsed() + unitType.getFoodUsed();
						if (newFoodUsed <= player.getFoodCapLimit()) {
							player.setFoodUsed(newFoodUsed);
						} else {
							this.queuedUnitFoodPaid = false;
							player.addFoodWaiter(this, () -> retryQueuedFood(game));
							game.getCommandErrorListener().showInterfaceError(this.playerIndex,
									CommandStringErrorKeys.NOT_ENOUGH_FOOD);
						}
					}
				}
			}
		}
	}

	public void queueTrainingUnit(final CSimulation game, final War3ID rawcode) {
		final int index = queue(game, rawcode, QueueItemType.UNIT);
		if (index >= 0) {
			payForQueuedUnit(game.getPlayer(this.playerIndex), game.getUnitData().getUnitType(rawcode));
			if (index == 0) {
				fireBuildQueueStartEvents(game);
			}
		}
	}

	public void queueSacrificingUnit(final CSimulation game, final War3ID rawcode, final CUnit sacrifice) {
		final int index = queue(game, rawcode, QueueItemType.SACRIFICE);
		if (index >= 0) {
			sacrifice.setHidden(true);
			setWorker(sacrifice, true);

			payForQueuedUnit(game.getPlayer(this.playerIndex), game.getUnitData().getUnitType(rawcode));
			if (index == 0) {
				fireBuildQueueStartEvents(game);
			}
		}
	}

	public void queueRevivingHero(final CSimulation game, final CUnit hero) {
		final CAbilityHero heroData = hero == null ? null : hero.getHeroData();
		if ((heroData == null) || heroData.isReviving()) {
			return;
		}
		if (queue(game, new War3ID(hero.getHandleId()), QueueItemType.HERO_REVIVE) >= 0) {
			hero.getHeroData().setReviving(true);
			final CPlayer player = game.getPlayer(this.playerIndex);
			final int heroReviveGoldCost = game.getGameplayConstants()
					.getHeroReviveGoldCost(hero.getUnitType().getGoldCost(), hero.getHeroData().getHeroLevel());
			final int heroReviveLumberCost = game.getGameplayConstants()
					.getHeroReviveLumberCost(hero.getUnitType().getLumberCost(), hero.getHeroData().getHeroLevel());
			player.charge(heroReviveGoldCost, heroReviveLumberCost);
		}
	}

	/**
	 * Brings a dead hero back next to the building that revives it (altar or tavern). Food and costs are the
	 * caller's.
	 */
	public void reviveHeroAt(final CSimulation game, final CUnit reviver, final float lifeFactor,
			final float manaFactor, final float manaStart) {
		final CAbilityHero heroData = getHeroData();
		heroData.setReviving(false);
		heroData.setAwaitingRevive(false);
		unlockTechtreeOnRevive(game);
		this.corpse = false;
		this.boneCorpse = false;
		this.freshCorpse = false;
		this.deathTurnTick = 0;
		setX(reviver.getX());
		setY(reviver.getY());
		game.getWorldCollision().addUnit(this);
		setPoint(reviver.getX(), reviver.getY(), game.getWorldCollision(), game.getRegionManager());
		setHidden(false);
		setLife(game, getMaximumLife() * lifeFactor);
		game.takeShadowPoolIds(this);
		setMana((getMaximumMana() * manaFactor) + (manaStart * this.unitType.getManaInitial()));
		nudgeAround(game, reviver);
		game.unitRepositioned(this); // dont blend animation
	}

	private void unlockTechtreeOnRevive(final CSimulation game) {
		final CPlayer player = game.getPlayer(this.playerIndex);
		player.addTechtreeUnlocked(game, this.unitType.getTypeId());
		for (final War3ID upgradedFromTypeId : this.upgradedFromTypeIds) {
			player.addTechtreeUnlocked(game, upgradedFromTypeId);
		}
	}

	/**
	 * ReviveHero from a script: the hero comes back at the point with the same life and mana rules as an altar.
	 */
	public void reviveHeroAtPoint(final CSimulation game, final float x, final float y, final boolean doEyecandy) {
		final CAbilityHero heroData = getHeroData();
		heroData.setReviving(false);
		heroData.setAwaitingRevive(false);
		unlockTechtreeOnRevive(game);
		this.corpse = false;
		this.boneCorpse = false;
		this.freshCorpse = false;
		this.deathTurnTick = 0;
		setX(x);
		setY(y);
		game.getWorldCollision().addUnit(this);
		setPoint(x, y, game.getWorldCollision(), game.getRegionManager());
		setHidden(false);
		setLife(game, getMaximumLife());
		game.takeShadowPoolIds(this);
		setMana(getMaximumMana());
		// Measured on 1.29: a hero revived by script uses its food again at once.
		game.getPlayer(this.playerIndex).setUnitFoodUsed(this, this.unitType.getFoodUsed());
		game.unitRepositioned(this);
	}

	public boolean isPathingDisabled() {
		return this.pathingDisabled;
	}

	public void setPathingDisabled(final boolean pathingDisabled) {
		this.pathingDisabled = pathingDisabled;
		setNoCollisionMovementType(pathingDisabled);
	}

	public void resetCooldowns() {
		this.rawcodeToCooldownExpireTime.clear();
		this.rawcodeToCooldownStartTime.clear();
		this.cooldownEndTime = 0;
		fireCooldownsChangedEvent();
	}

	public void queueResearch(final CSimulation game, final War3ID rawcode) {
		final int index = queue(game, rawcode, QueueItemType.RESEARCH);
		if (index >= 0) {
			final CPlayer player = game.getPlayer(this.playerIndex);
			final CUpgradeType upgradeType = game.getUpgradeData().getType(rawcode);
			player.chargeFor(upgradeType);
			if (index == 0) {
				fireBuildQueueStartEvents(game);
			}
		}
	}

	public static enum QueueItemType {
		UNIT,
		RESEARCH,
		HERO_REVIVE,
		SACRIFICE;
	}

	public void setRallyPoint(final AbilityTarget target) {
		this.rallyPoint = target;
		this.stateNotifier.rallyPointChanged();
	}

	public void internalPublishHeroStatsChanged() {
		this.stateNotifier.heroStatsChanged();
	}

	public AbilityTarget getRallyPoint() {
		return this.rallyPoint;
	}

	private static interface RallyProvider {
		float getX();

		float getY();
	}

	@Override
	public <T> T visit(final AbilityTargetVisitor<T> visitor) {
		return visitor.accept(this);
	}

	@Override
	public <T> T visit(final CWidgetVisitor<T> visitor) {
		return visitor.accept(this);
	}

	private static final class UseAbilityOnTargetByIdVisitor implements AbilityTargetVisitor<Void> {
		private static final UseAbilityOnTargetByIdVisitor INSTANCE = new UseAbilityOnTargetByIdVisitor();
		private CSimulation game;
		private CUnit trainedUnit;
		private int playerIndex;
		private int rallyOrderId;

		private UseAbilityOnTargetByIdVisitor reset(final CSimulation game, final CUnit trainedUnit,
				final int playerIndex, final int rallyOrderId) {
			this.game = game;
			this.trainedUnit = trainedUnit;
			this.playerIndex = playerIndex;
			this.rallyOrderId = rallyOrderId;
			return this;
		}

		@Override
		public Void accept(final AbilityPointTarget target) {
			CAbility abilityToUse = null;
			for (final CAbility ability : this.trainedUnit.getAbilities()) {
				ability.checkCanUse(this.game, this.trainedUnit, this.playerIndex, this.rallyOrderId, false,
						BooleanAbilityActivationReceiver.INSTANCE.reset());
				if (BooleanAbilityActivationReceiver.INSTANCE.isOk()) {
					final BooleanAbilityTargetCheckReceiver<AbilityPointTarget> targetCheckReceiver = BooleanAbilityTargetCheckReceiver
							.<AbilityPointTarget>getInstance().reset();
					ability.checkCanTarget(this.game, this.trainedUnit, this.playerIndex, this.rallyOrderId, false, target,
							targetCheckReceiver);
					if (targetCheckReceiver.isTargetable()) {
						abilityToUse = ability;
					}
				}
			}
			if (abilityToUse != null) {
				this.trainedUnit.order(this.game, new COrderTargetPoint(this.playerIndex, abilityToUse.getHandleId(),
						this.rallyOrderId, target, false), false);
			}
			return null;
		}

		@Override
		public Void accept(final CUnit targetUnit) {
			return acceptWidget(this.game, this.trainedUnit, this.rallyOrderId, targetUnit);
		}

		private Void acceptWidget(final CSimulation game, final CUnit trainedUnit, final int rallyOrderId,
				final CWidget target) {
			CAbility abilityToUse = null;
			for (final CAbility ability : trainedUnit.getAbilities()) {
				ability.checkCanUse(game, trainedUnit, this.playerIndex, rallyOrderId, false, BooleanAbilityActivationReceiver.INSTANCE.reset());
				if (BooleanAbilityActivationReceiver.INSTANCE.isOk()) {
					final BooleanAbilityTargetCheckReceiver<CWidget> targetCheckReceiver = BooleanAbilityTargetCheckReceiver
							.<CWidget>getInstance().reset();
					ability.checkCanTarget(game, trainedUnit, this.playerIndex, rallyOrderId, false, target, targetCheckReceiver);
					if (targetCheckReceiver.isTargetable()) {
						abilityToUse = ability;
					}
				}
			}
			if (abilityToUse != null) {
				trainedUnit.order(game, new COrderTargetWidget(this.playerIndex, abilityToUse.getHandleId(),
						rallyOrderId, target.getHandleId(), false), false);
			}
			return null;
		}

		@Override
		public Void accept(final CDestructable target) {
			return acceptWidget(this.game, this.trainedUnit, this.rallyOrderId, target);
		}

		@Override
		public Void accept(final CItem target) {
			return acceptWidget(this.game, this.trainedUnit, this.rallyOrderId, target);
		}
	}

	public int getFoodMade() {
		return this.foodMade;
	}

	public int getFoodUsed() {
		return this.foodUsed;
	}

	public int setFoodMade(final int foodMade) {
		final int delta = foodMade - this.foodMade;
		this.foodMade = foodMade;
		return delta;
	}

	public int setFoodUsed(final int foodUsed) {
		final int delta = foodUsed - this.foodUsed;
		this.foodUsed = foodUsed;
		return delta;
	}

	public void setDefaultBehavior(final CBehavior defaultBehavior) {
		this.defaultBehavior = defaultBehavior;
	}

	public CAbilityGoldMinable getGoldMineData() {
		for (int i = 0, n = this.abilities.size(); i < n; i++) {
			final CAbility ability = this.abilities.get(i);
			if (ability instanceof CAbilityGoldMinable) {
				return (CAbilityGoldMinable) ability;
			}
		}
		return null;
	}

	public CAbilityOverlayedMine getOverlayedGoldMineData() {
		for (int i = 0, n = this.abilities.size(); i < n; i++) {
			final CAbility ability = this.abilities.get(i);
			if (ability instanceof CAbilityOverlayedMine) {
				return (CAbilityOverlayedMine) ability;
			}
		}
		return null;
	}

	public int getGold() {
		for (int i = 0, n = this.abilities.size(); i < n; i++) {
			final CAbility ability = this.abilities.get(i);
			if (ability instanceof CAbilityGoldMinable) {
				return ((CAbilityGoldMinable) ability).getGold();
			}
			if (ability instanceof CAbilityOverlayedMine) {
				return ((CAbilityOverlayedMine) ability).getGold();
			}
		}
		return 0;
	}

	public void setGold(final int goldAmount) {
		for (final CAbility ability : this.abilities) {
			if (ability instanceof CAbilityGoldMinable) {
				((CAbilityGoldMinable) ability).setGold(goldAmount);
			}
			if (ability instanceof CAbilityOverlayedMine) {
				((CAbilityOverlayedMine) ability).setGold(goldAmount);
			}
		}
	}

	public Queue<COrder> getOrderQueue() {
		return this.orderQueue;
	}

	public COrder getCurrentOrder() {
		return this.lastStartedOrder;
	}

	/**
	 * Measured on 1.29: an animation a script sets on a unit walking under an order is not
	 * replaced by the walk; it repeats while the unit keeps walking, until the unit gets another order or the order
	 * ends. An idle unit plays it once.
	 */
	public void holdScriptAnimation() {
		this.scriptAnimationOrder = this.lastStartedOrder;
	}

	public void releaseScriptAnimation() {
		this.scriptAnimationOrder = null;
	}

	public boolean isScriptAnimationHeld() {
		if ((this.scriptAnimationOrder != null) && (this.scriptAnimationOrder != this.lastStartedOrder)) {
			this.scriptAnimationOrder = null;
		}
		return this.scriptAnimationOrder != null;
	}

	public CAbilityHero getHeroData() {
		for (int i = 0, n = this.abilities.size(); i < n; i++) {
			final CAbility ability = this.abilities.get(i);
			if (ability instanceof CAbilityHero) {
				return (CAbilityHero) ability;
			}
		}
		return null;
	}

	public CAbilityRoot getRootData() {
		for (int i = 0, n = this.abilities.size(); i < n; i++) {
			final CAbility ability = this.abilities.get(i);
			if (ability instanceof CAbilityRoot) {
				return (CAbilityRoot) ability;
			}
		}
		return null;
	}

	public CAbilityInventory getInventoryData() {
		for (int i = 0, n = this.abilities.size(); i < n; i++) {
			final CAbility ability = this.abilities.get(i);
			if (ability instanceof CAbilityInventory) {
				return (CAbilityInventory) ability;
			}
		}
		return null;
	}

	public CAbilityNeutralBuilding getNeutralBuildingData() {
		for (int i = 0, n = this.abilities.size(); i < n; i++) {
			final CAbility ability = this.abilities.get(i);
			if (ability instanceof CAbilityNeutralBuilding) {
				return (CAbilityNeutralBuilding) ability;
			}
		}
		return null;
	}

	public CAbilityCargoHold getCargoData() {
		for (int i = 0, n = this.abilities.size(); i < n; i++) {
			final CAbility ability = this.abilities.get(i);
			if (ability instanceof CAbilityCargoHold) {
				return (CAbilityCargoHold) ability;
			}
		}
		return null;
	}

	public void setUnitSpecificAttacks(final List<CUnitAttack> unitSpecificAttacks) {
		this.unitSpecificAttacks = unitSpecificAttacks;
	}

	public void setUnitSpecificCurrentAttacks(final List<CUnitAttack> unitSpecificCurrentAttacks) {
		this.unitSpecificCurrentAttacks = unitSpecificCurrentAttacks;
		computeDerivedFields(NonStackingStatBuffType.ATKSPD);
	}

	public List<CUnitAttack> getUnitSpecificAttacks() {
		return this.unitSpecificAttacks;
	}

	public List<CUnitAttack> getCurrentAttacks() {
		if (this.disableAttacks) {
			return Collections.emptyList();
		}
		if (this.unitSpecificCurrentAttacks != null) {
			return this.unitSpecificCurrentAttacks;
		}
		return Collections.emptyList();
	}

	public void setDisableAttacks(final boolean disableAttacks) {
		this.disableAttacks = disableAttacks;
		this.stateNotifier.attacksChanged();
	}

	public boolean isDisableAttacks() {
		return this.disableAttacks;
	}

	public void onPickUpItem(final CSimulation game, final CItem item, final boolean playUserUISounds) {
		this.stateNotifier.inventoryChanged();
		if (playUserUISounds) {
			game.unitPickUpItemEvent(this, item);
		}
		firePickUpItemEvents(game, item);
	}

	public void onDropItem(final CSimulation game, final CItem droppedItem, final boolean playUserUISounds) {
		this.stateNotifier.inventoryChanged();
		if (playUserUISounds) {
			game.unitDropItemEvent(this, droppedItem);
		}
	}

	public boolean isInRegion(final CRegion region) {
		return this.containingRegions.contains(region);
	}

	@Override
	public float getMaxLife() {
		return this.maximumLife;
	}

	private static final class RegionCheckerImpl implements CRegionEnumFunction {
		private CUnit unit;
		private CRegionManager regionManager;

		public RegionCheckerImpl reset(final CUnit unit, final CRegionManager regionManager) {
			this.unit = unit;
			this.regionManager = regionManager;
			return this;
		}

		@Override
		public boolean call(final CRegion region) {
			if (this.unit.containingRegions.add(region)) {
				if (!this.unit.priorContainingRegions.contains(region)) {
					this.regionManager.onUnitEnterRegion(this.unit, region);
				}
			}
			return false;
		}

	}

	public boolean isBuilding() {
		return this.structure;
	}

	public void setStructure(final boolean flag) {
		this.structure = flag;
	}

	/**
	 * Measured on 1.29: an uprooted ancient is still a structure for IsUnitType and is counted by
	 * GetPlayerStructureCount with incomplete structures, though it walks and paths as a unit.
	 */
	public boolean isUprooted() {
		return this.uprooted;
	}

	public void setUprooted(final boolean uprooted) {
		this.uprooted = uprooted;
	}

	/**
	 * Measured: a Castle placed by script trains a Spell Breaker that requires a Keep, as an upgraded
	 * Castle would.
	 */
	public void unlockUpgradedFromTypes(final CSimulation simulation, final CPlayer player) {
		for (final War3ID upgradedFromTypeId : simulation.getUnitData().getUpgradedFromTypeIds(this.typeId)) {
			this.upgradedFromTypeIds.add(upgradedFromTypeId);
			player.addTechtreeUnlocked(simulation, upgradedFromTypeId);
		}
	}

	public void onRemove(final CSimulation simulation) {
		final CPlayer player = simulation.getPlayer(this.playerIndex);
		if (WarsmashConstants.FIRE_DEATH_EVENTS_ON_REMOVEUNIT) {
			// Firing userspace triggers here causes items to appear around the player bases
			// in melee games.
			// (See "Remove creeps and critters from used start locations" implementation)
			setLife(simulation, 0);
		} else {
			if (!isDead()) {
				if (this.constructing) {
					player.removeTechtreeInProgress(simulation, this.unitType.getTypeId());
				} else {
					player.removeTechtreeUnlocked(simulation, this.unitType.getTypeId());
					for (final War3ID upgradedFromTypeId : this.upgradedFromTypeIds) {
						player.removeTechtreeUnlocked(simulation, upgradedFromTypeId);
					}
				}
			} // else techtree was removed upon death
			setHidden(true);
			// setting hidden to let things that refer to this before it gets garbage
			// collected see it as basically worthless
		}
		killPathingInstance();
		simulation.getWorldCollision().removeUnit(this);
		this.removedFromGame = true;
	}

	/**
	 * After the step that removed it, a unit's handle no longer refers to anything: as measured in the original game,
	 * scripts then read type 0, position 0, no owner and not hidden.
	 */
	public boolean isRemovedFromGame() {
		return this.removedFromGame;
	}

	private static enum StateListenerUpdateType {
		ADD,
		REMOVE;
	}

	private static final class StateListenerUpdate {
		private final CUnitStateListener listener;
		private final StateListenerUpdateType updateType;

		public StateListenerUpdate(final CUnitStateListener listener, final StateListenerUpdateType updateType) {
			this.listener = listener;
			this.updateType = updateType;
		}

		public CUnitStateListener getListener() {
			return this.listener;
		}

		public StateListenerUpdateType getUpdateType() {
			return this.updateType;
		}
	}

	public void cancelConstruction(final CSimulation game) {
		final float refundRate = game.getGameplayConstants().getConstructionRefundRate();
		game.getPlayer(this.playerIndex).refund((int) (this.unitType.getGoldCost() * refundRate),
				(int) (this.unitType.getLumberCost() * refundRate));
		this.setLife(game, 0);
		final CPlayer player = game.getPlayer(this.playerIndex);
		final List<CWidgetEvent> eventList = getEventList(JassGameEventsWar3.EVENT_UNIT_CONSTRUCT_CANCEL);
		if (eventList != null) {
			for (final CWidgetEvent event : new ArrayList<>(eventList)) {
				event.fire(this, CommonTriggerExecutionScope.unitConstructCancelScope(
						JassGameEventsWar3.EVENT_UNIT_CONSTRUCT_CANCEL, event.getTrigger(), this, player));
			}
		}
		player.firePlayerUnitEvents(this,
				(eventType, trigger, unit) -> CommonTriggerExecutionScope.unitConstructCancelScope(eventType, trigger,
						unit, player),
				JassGameEventsWar3.EVENT_PLAYER_UNIT_CONSTRUCT_CANCEL);
	}

	public void cancelUpgrade(final CSimulation game) {
		if (this.upgradeIdType == null) {
			return;
		}
		revertUpgrade(game, game.getGameplayConstants().getUpgradeRefundRate());
	}

	private void revertUpgrade(final CSimulation game, final float refundRate) {
		final CPlayer player = game.getPlayer(this.playerIndex);
		player.setUnitFoodUsed(this, this.unitType.getFoodUsed());
		int goldCost, lumberCost;
		final CUnitType newUpgradeUnitType = game.getUnitData().getUnitType(this.upgradeIdType);
		if (game.getGameplayConstants().isRelativeUpgradeCosts()) {
			goldCost = newUpgradeUnitType.getGoldCost() - this.unitType.getGoldCost();
			lumberCost = newUpgradeUnitType.getLumberCost() - this.unitType.getLumberCost();
		} else {
			goldCost = newUpgradeUnitType.getGoldCost();
			lumberCost = newUpgradeUnitType.getLumberCost();
		}
		player.refund((int) (goldCost * refundRate), (int) (lumberCost * refundRate));

		final Iterator<CAbility> abilityIterator = this.abilities.iterator();
		while (abilityIterator.hasNext()) {
			final CAbility ability = abilityIterator.next();
			if (ability instanceof CAbilityBuildInProgress) {
				abilityIterator.remove();
			} else {
				ability.setDisabled(false, CAbilityDisableType.CONSTRUCTION);
				ability.setIconShowing(true);
			}
		}
		checkDisabledAbilities(game, false);

		game.unitCancelUpgradingEvent(this, this.upgradeIdType);
		this.upgradeIdType = null;
		this.constructing = false;
		this.constructionProgress = 0;
		this.unitAnimationListener.playAnimation(true, PrimaryTag.STAND, SequenceUtils.EMPTY, 0.0f, true);
		firePlayerThenUnitEvents(game, JassGameEventsWar3.EVENT_UNIT_UPGRADE_CANCEL,
				JassGameEventsWar3.EVENT_PLAYER_UNIT_UPGRADE_CANCEL);
	}

	public void beginUpgrade(final CSimulation game, final War3ID rawcode) {
		this.upgradeIdType = rawcode;
		this.constructing = true;
		this.constructionProgress = 0;

		final CPlayer player = game.getPlayer(this.playerIndex);
		final CUnitType newUpgradeUnitType = game.getUnitData().getUnitType(rawcode);
		player.setUnitFoodUsed(this, newUpgradeUnitType.getFoodUsed());
		int goldCost, lumberCost;
		if (game.getGameplayConstants().isRelativeUpgradeCosts()) {
			goldCost = newUpgradeUnitType.getGoldCost() - this.unitType.getGoldCost();
			lumberCost = newUpgradeUnitType.getLumberCost() - this.unitType.getLumberCost();
		} else {
			goldCost = newUpgradeUnitType.getGoldCost();
			lumberCost = newUpgradeUnitType.getLumberCost();
		}
		player.charge(goldCost, lumberCost);
		add(game, new CAbilityBuildInProgress(game.getHandleIdAllocator().createId()));
		for (final CAbility ability : getAbilities()) {
			ability.visit(AbilityDisableWhileUpgradingVisitor.INSTANCE);
		}
		checkDisabledAbilities(game, true);
		player.addTechtreeInProgress(game, rawcode);

		game.unitUpgradingEvent(this, rawcode);
		this.unitAnimationListener.playAnimation(true, PrimaryTag.BIRTH, SequenceUtils.EMPTY, 0.0f, true);
		firePlayerThenUnitEvents(game, JassGameEventsWar3.EVENT_UNIT_UPGRADE_START,
				JassGameEventsWar3.EVENT_PLAYER_UNIT_UPGRADE_START);
	}

	public void setUnitState(final CSimulation game, final CUnitState whichUnitState, final float value) {
		switch (whichUnitState) {
		// Measured: the original keeps a script's life and mana within the unit's maximum, and leaves the maximum
		// itself as it is.
		case LIFE:
			applyLife(game, Math.min(value, getMaximumLife()));
			break;
		case MANA:
			this.mana = Math.max(0, Math.min(value, getMaximumMana()));
			this.stateNotifier.manaChanged();
			break;
		default:
			return;
		}
		checkStateEvents(true);
	}

	public RemovableTriggerEvent addStateEvent(final GlobalScope globalScope, final Trigger whichTrigger,
			final CUnitState state, final CLimitOp limitOp, final double limitValue) {
		final CUnitStateEvent event = new CUnitStateEvent(globalScope, this, whichTrigger, state, limitOp, limitValue);
		addEvent(event);
		return event;
	}

	private void checkStateEvents(final boolean written) {
		final List<CWidgetEvent> eventList = getEventList(JassGameEventsWar3.EVENT_UNIT_STATE_LIMIT);
		if (eventList != null) {
			for (int i = eventList.size() - 1; i >= 0; i--) {
				if (i < eventList.size()) {
					((CUnitStateEvent) eventList.get(i)).check(this, written);
				}
			}
		}
	}

	public float getUnitState(final CSimulation game, final CUnitState whichUnitState) {
		switch (whichUnitState) {
		case LIFE:
			return getLife();
		case MANA:
			return getMana();
		case MAX_LIFE:
			return getMaximumLife();
		case MAX_MANA:
			return getMaximumMana();
		}
		return 0;
	}

	/** A unit state as a script reads it, at the exact time of the timer now firing. */
	public float getScriptUnitState(final CSimulation game, final CUnitState whichUnitState) {
		final float value = getUnitState(game, whichUnitState);
		switch (whichUnitState) {
		case LIFE:
			return value - regenNotYetDue(game, this.life, this.lifeAfterRegen, this.lifeRegenGiven);
		case MANA:
			return value - regenNotYetDue(game, this.mana, this.manaAfterRegen, this.manaRegenGiven);
		default:
			return value;
		}
	}

	private float regenNotYetDue(final CSimulation game, final float value, final float afterRegen,
			final float given) {
		if ((given == 0) || (value != afterRegen) || (this.regenTick != (game.getGameTurnTick() - 1))) {
			return 0;
		}
		final double exactTime = game.getTimerFiringExactTime();
		if (Double.isNaN(exactTime)) {
			return 0;
		}
		final double ahead = (game.getGameTurnTick() * (double) WarsmashConstants.SIMULATION_STEP_TIME) - exactTime;
		if (ahead <= 0) {
			return 0;
		}
		return (float) (given * Math.min(1, ahead / WarsmashConstants.SIMULATION_STEP_TIME));
	}

	public boolean isUnitType(final CUnitTypeJass whichUnitType) {
		switch (whichUnitType) {
		case HERO:
			return isHero() && !this.illusion;
		case DEAD:
			return isDead();
		case STRUCTURE:
			return isBuilding() || this.uprooted;

		case FLYING:
			return this.targetedAs.contains(CTargetType.AIR);
		case GROUND:
			return this.targetedAs.contains(CTargetType.GROUND);

		case ATTACKS_FLYING:
			for (final CUnitAttack attack : getCurrentAttacks()) {
				if (attack.getTargetsAllowed().contains(CTargetType.AIR)) {
					return true;
				}
			}
			return false;
		case ATTACKS_GROUND:
			for (final CUnitAttack attack : getCurrentAttacks()) {
				if (attack.getTargetsAllowed().contains(CTargetType.GROUND)) {
					return true;
				}
			}
			return false;

		case MELEE_ATTACKER:
			boolean hasAttacks = false;
			for (final CUnitAttack attack : getCurrentAttacks()) {
				if (attack.getWeaponType() != CWeaponType.NORMAL) {
					return false;
				}
				hasAttacks = true;
			}
			return hasAttacks;

		case RANGED_ATTACKER:
			for (final CUnitAttack attack : getCurrentAttacks()) {
				if (attack.getWeaponType() != CWeaponType.NORMAL) {
					return true;
				}
			}
			return false;

		case GIANT:
			return this.classifications.contains(CUnitClassification.GIANT);
		case SUMMONED:
			return this.classifications.contains(CUnitClassification.SUMMONED);
		case STUNNED:
			// 1.29: false also on a stunned unit, and UnitAddType cannot set it.
			return false;
		case PLAGUED:
			return getFirstAbilityOfType(CBuffDisease.class) != null;
		case SNARED:
			boolean isSnared = false;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.SNARED) {
					if (buff.getValue() != 0) {
						isSnared = true;
					}
				}
			}
			return isSnared;
		case UNDEAD:
			return this.classifications.contains(CUnitClassification.UNDEAD);
		case MECHANICAL:
			return this.classifications.contains(CUnitClassification.MECHANICAL);
		case PEON:
			return this.classifications.contains(CUnitClassification.PEON);
		case SAPPER:
			return this.classifications.contains(CUnitClassification.SAPPER);
		case TOWNHALL:
			return this.classifications.contains(CUnitClassification.TOWNHALL);
		case ANCIENT:
			return this.classifications.contains(CUnitClassification.ANCIENT);

		case TAUREN:
			return this.classifications.contains(CUnitClassification.TAUREN);
		case POISONED:
			throw new UnsupportedOperationException(
					"cannot ask engine if unit is poisoned: poison is not yet implemented");
		case POLYMORPHED:
			boolean isPolymorphed = false;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.POLYMORPHED) {
					if (buff.getValue() != 0) {
						isPolymorphed = true;
					}
				}
			}
			return isPolymorphed;
		case SLEEPING:
			boolean isSleeping = false;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.SLEEPING) {
					if (buff.getValue() != 0) {
						isSleeping = true;
					}
				}
			}
			return isSleeping;
		case RESISTANT:
			return this.resistant;
		case ETHEREAL:
			boolean isEthereal = false;
			for (final StateModBuff buff : this.stateModBuffs) {
				if (buff.getBuffType() == StateModBuffType.ETHEREAL) {
					if (buff.getValue() != 0) {
						isEthereal = true;
					}
				}
			}
			return isEthereal;
		case MAGIC_IMMUNE:
			return isMagicImmune() || isLimitedMagicImmune();
		}
		return false;
	}

	public void addCheesyStateModBuff(final CSimulation game, final StateModBuffType type) {
		// this might be less efficient than the original state mod buff system,
		// and cannot access the Detector Levels, etc, with the value
		addStateModBuff(new StateModBuff(type, CHEESY_UNIQUE_NUMBER));
		computeUnitState(game, type);
	}

	public boolean removeCheesyStateModBuff(final CSimulation game, final StateModBuffType type) {
		// this might be less efficient than the original state mod buff system,
		// and cannot access the Detector Levels, etc, with the value
		StateModBuff toRemove = null;
		for (final StateModBuff stateModBuff : this.stateModBuffs) {
			if ((stateModBuff.getBuffType() == type) && (stateModBuff.getValue() == CHEESY_UNIQUE_NUMBER)) {
				toRemove = stateModBuff;
				break;
			}
		}
		if (toRemove != null) {
			removeStateModBuff(toRemove);
			computeUnitState(game, type);
			return true;
		}
		return false;
	}

	public boolean addUnitType(final CSimulation game, final CUnitTypeJass whichUnitType) {
		switch (whichUnitType) {
		case HERO:
		case ATTACKS_FLYING:
		case ATTACKS_GROUND:
		case MELEE_ATTACKER:
		case RANGED_ATTACKER:
		case PLAGUED:
		case POISONED:
		case POLYMORPHED:
			// types which we are not allowing them to add and remove
			// I read online on a Hive thing that maybe users expect the API for these types
			// to be something that doesn't work, i.e. "IsUnitType(UNIT_TYPE_STUNNED)" would
			// return false on a stunned unit, but would return true if they use
			// "UnitAddType(UNIT_TYPE_STUNNED)"
			// then check for it. Seems extremely bogus but it's possible some custom
			// content might
			// depend on the pointless "feature" of adding and remove a setting that doesn't
			// work.
			// If you are here reading this because you're playing something that depends on
			// this
			// "feature" then this would be the place to add it
			return false;
		case SNARED:
			addCheesyStateModBuff(game, StateModBuffType.SNARED);
			return true;
		case STUNNED:
			return false;
		case SLEEPING:
			addCheesyStateModBuff(game, StateModBuffType.SLEEPING);
			return true;
		case DEAD:
			if (!isDead()) {
				kill(game);
				return true;
			}
			return false;
		case STRUCTURE:
			if (!isBuilding()) {
				// below is maybe not what wc3 actually does. the "Factory" ability
				// for pocket factory sets it to structure kind of like this,
				// but probably doesn't interact with its world collision (?)
				game.getWorldCollision().removeUnit(this);
				setStructure(true);
				game.getWorldCollision().addUnit(this);
				return true;
			}
			return false;
		case FLYING:
			return this.targetedAs.add(CTargetType.AIR);
		case GROUND:
			return this.targetedAs.add(CTargetType.GROUND);
		case GIANT:
			return this.classifications.add(CUnitClassification.GIANT);
		case SUMMONED:
			return this.classifications.add(CUnitClassification.SUMMONED);
		case UNDEAD:
			return this.classifications.add(CUnitClassification.UNDEAD);
		case MECHANICAL:
			return this.classifications.add(CUnitClassification.MECHANICAL);
		case PEON:
			return this.classifications.add(CUnitClassification.PEON);
		case SAPPER:
			return this.classifications.add(CUnitClassification.SAPPER);
		case TOWNHALL:
			return this.classifications.add(CUnitClassification.TOWNHALL);
		case ANCIENT:
			return this.classifications.add(CUnitClassification.ANCIENT);
		case TAUREN:
			return this.classifications.add(CUnitClassification.TAUREN);
		case RESISTANT:
			addCheesyStateModBuff(game, StateModBuffType.RESISTANT);
			return true;
		case ETHEREAL:
			addCheesyStateModBuff(game, StateModBuffType.ETHEREAL);
			return true;
		case MAGIC_IMMUNE:
			addCheesyStateModBuff(game, StateModBuffType.MAGIC_IMMUNE);
			return true;
		}
		return false;
	}

	public boolean removeUnitType(final CSimulation game, final CUnitTypeJass whichUnitType) {
		switch (whichUnitType) {
		case HERO:
		case ATTACKS_FLYING:
		case ATTACKS_GROUND:
		case MELEE_ATTACKER:
		case RANGED_ATTACKER:
		case PLAGUED:
		case POISONED:
		case POLYMORPHED:
			// types which we are not allowing them to add and remove
			// I read online on a Hive thing that maybe users expect the API for these types
			// to be something that doesn't work, i.e. "IsUnitType(UNIT_TYPE_STUNNED)" would
			// return false on a stunned unit, but would return true if they use
			// "UnitAddType(UNIT_TYPE_STUNNED)"
			// then check for it. Seems extremely bogus but it's possible some custom
			// content might
			// depend on the pointless "feature" of adding and remove a setting that doesn't
			// work.
			// If you are here reading this because you're playing something that depends on
			// this
			// "feature" then this would be the place to add it
			return false;
		case SNARED:
			return removeCheesyStateModBuff(game, StateModBuffType.SNARED);
		case STUNNED:
			return false;
		case SLEEPING:
			return removeCheesyStateModBuff(game, StateModBuffType.SLEEPING);
		case DEAD:
			if (isDead()) {
				resurrect(game);
				return true;
			}
			return false;
		case STRUCTURE:
			if (isBuilding()) {
				// below is maybe not what wc3 actually does. the "Factory" ability
				// for pocket factory sets it to structure kind of like this,
				// but probably doesn't interact with its world collision (?)
				game.getWorldCollision().removeUnit(this);
				setStructure(false);
				game.getWorldCollision().addUnit(this);
				return true;
			}
			return false;
		case FLYING:
			return this.targetedAs.remove(CTargetType.AIR);
		case GROUND:
			return this.targetedAs.remove(CTargetType.GROUND);
		case GIANT:
			return this.classifications.remove(CUnitClassification.GIANT);
		case SUMMONED:
			return this.classifications.remove(CUnitClassification.SUMMONED);
		case UNDEAD:
			return this.classifications.remove(CUnitClassification.UNDEAD);
		case MECHANICAL:
			return this.classifications.remove(CUnitClassification.MECHANICAL);
		case PEON:
			return this.classifications.remove(CUnitClassification.PEON);
		case SAPPER:
			return this.classifications.remove(CUnitClassification.SAPPER);
		case TOWNHALL:
			return this.classifications.remove(CUnitClassification.TOWNHALL);
		case ANCIENT:
			return this.classifications.remove(CUnitClassification.ANCIENT);
		case TAUREN:
			return this.classifications.remove(CUnitClassification.TAUREN);
		case RESISTANT:
			return removeCheesyStateModBuff(game, StateModBuffType.RESISTANT);
		case ETHEREAL:
			return removeCheesyStateModBuff(game, StateModBuffType.ETHEREAL);
		case MAGIC_IMMUNE:
			return removeCheesyStateModBuff(game, StateModBuffType.MAGIC_IMMUNE);
		}
		return false;
	}

	public Set<CUnitAppliedUpgrade> getAppliedUpgrades() {
		return this.appliedUpgrades;
	}

	public int getLevelOfAppliedUpgrade(War3ID id) {
		CUnitAppliedUpgrade upgrade = getAppliedUpgrade(id);
		if (upgrade != null) {
			return upgrade.getLevel();
		}
		return 0;
	}

	private CUnitAppliedUpgrade getAppliedUpgrade(War3ID id) {
		for (CUnitAppliedUpgrade upgrade : this.appliedUpgrades) {
			if (upgrade.getType().getTypeId().equals(id)) {
				return upgrade;
			}
		}
		return null;
	}

	public void applyUpgrade(CSimulation simulation, CUpgradeType upgradeType, int level) {
		CUnitAppliedUpgrade upgrade = getAppliedUpgrade(upgradeType.getTypeId());
		if (upgrade != null) {
			if (upgrade.getLevel() == level) {
				Log.warn("Unit already has upgrade? " + upgrade.getLevel() + " (target" + level + ")");
				return;
			}
			unapplyUpgrade(simulation, upgradeType, upgrade);
		}
		for (CUpgradeEffect upgradeEffect : upgradeType.getUpgradeEffects()) {
			upgradeEffect.apply(simulation, this, level);
		}
		this.appliedUpgrades.add(new CUnitAppliedUpgrade(upgradeType, level));
		this.notifyUpgradesChanged();
	}

	public void unapplyUpgrade(CSimulation simulation, CUpgradeType upgradeType) {
		CUnitAppliedUpgrade upgrade = getAppliedUpgrade(upgradeType.getTypeId());
		unapplyUpgrade(simulation, upgradeType, upgrade);
	}

	public void unapplyUpgrade(CSimulation simulation, CUpgradeType upgradeType, CUnitAppliedUpgrade upgrade) {
		if (upgrade != null) {
			for (CUpgradeEffect upgradeEffect : upgradeType.getUpgradeEffects()) {
				upgradeEffect.unapply(simulation, this, upgrade.getLevel());
			}
		}
		this.appliedUpgrades.remove(upgrade);
	}

	public int getTriggerEditorCustomValue() {
		return this.triggerEditorCustomValue;
	}

	public void setTriggerEditorCustomValue(final int triggerEditorCustomValue) {
		this.triggerEditorCustomValue = triggerEditorCustomValue;
	}

	/** The unit's name: its type's, unless a trigger renamed this unit (BlzSetUnitName). */
	public String getName() {
		return this.nameOverride == null ? this.unitType.getName() : this.nameOverride;
	}

	public void setName(final String name) {
		this.nameOverride = name;
	}

	public static String maybeMeaningfulName(final CUnit unit) {
		if (unit == null) {
			return "null";
		}
		return unit.getUnitType().getName();
	}

	public void fireCooldownsChangedEvent() {
		this.stateNotifier.ordersChanged();
	}

	/**
	 * The ability a script names by rawcode. Besides the leveling abilities this finds the engine's own ones
	 * (attack, move, build, train queue, upgrade, hero, shops), which the original also lets scripts read, remove and
	 * make permanent.
	 */
	public CAbility getAbilityByRawcode(final War3ID rawcode) {
		final CLevelingAbility leveling = getAbility(GetAbilityByRawcodeVisitor.getInstance().reset(rawcode));
		if (leveling != null) {
			return leveling;
		}
		for (int i = 0, n = this.abilities.size(); i < n; i++) {
			final CAbility ability = this.abilities.get(i);
			// Measured: the original has no 'Arev' on an altar (reviving is part of its train queue, 'Aque').
			if (!(ability instanceof CLevelingAbility) && !(ability instanceof CAbilityReviveHero)
					&& rawcode.equals(ability.getAlias())) {
				return ability;
			}
		}
		return null;
	}

	/**
	 * An ability that owned these behaviors left the unit: a unit busy with one of them (or walking for one) stops,
	 * and one that would return to one of them by default returns to stop instead.
	 */
	public void releaseBehaviors(final CSimulation game, final CBehavior... released) {
		boolean busy = false;
		for (final CBehavior behavior : released) {
			if (behavior == null) {
				continue;
			}
			if (this.defaultBehavior == behavior) {
				this.defaultBehavior = this.stopBehavior;
			}
			if ((this.currentBehavior == behavior) || ((this.currentBehavior == this.moveBehavior)
					&& (this.moveBehavior != null) && (this.moveBehavior.getRangedBehavior() == behavior))) {
				busy = true;
			}
		}
		if (busy) {
			performDefaultBehavior(game);
		}
	}

	public int getAbilityLevel(final War3ID abilityId) {
		final CLevelingAbility ability = getAbility(GetAbilityByRawcodeVisitor.getInstance().reset(abilityId));
		if (ability == null) {
			return 0;
		} else {
			return ability.getLevel();
		}
	}

	public boolean chargeMana(final int manaCost) {
		if (this.mana >= manaCost) {
			setMana(this.mana - manaCost);
			return true;
		}
		return false;
	}

	public void fireSpellEvents(final CSimulation game, final JassGameEventsWar3 eventId, final CAbility ability,
			AbilityTarget target) {
		// 1.29: finish, and the endcast after it, have no target unit and target
		// point 0,0; an endcast without finish (interrupted) keeps the target.
		if (eventId == JassGameEventsWar3.EVENT_UNIT_SPELL_FINISH) {
			this.finishedSpell = ability;
			target = null;
		}
		else if (eventId == JassGameEventsWar3.EVENT_UNIT_SPELL_ENDCAST) {
			if (this.finishedSpell == ability) {
				target = null;
			}
			this.finishedSpell = null;
		}
		if (eventId == JassGameEventsWar3.EVENT_UNIT_SPELL_EFFECT) {
			game.spellEffectEvent(this, ability);
			final CUnit targetUnit = (target == null) ? null : target.visit(AbilityTargetVisitor.UNIT);
			if (targetUnit != null) {
				targetUnit.onSpellEffect(game, this);
			}
		}
		if (target == null) {
			fireSpellEventsNoTarget(game, eventId, ability);
		} else {
			target.visit(new AbilityTargetVisitor<Object>() {

				@Override
				public Object accept(final AbilityPointTarget target) {
					fireSpellEventsPointTarget(game, eventId, ability, target);
					return null;
				}

				@Override
				public Object accept(final CUnit target) {
					fireSpellEventsUnitTarget(game, eventId, ability, target);
					return null;
				}

				@Override
				public Object accept(final CDestructable target) {
					fireSpellEventsDestructableTarget(game, eventId, ability, target);
					return null;
				}

				@Override
				public Object accept(final CItem target) {
					fireSpellEventsItemTarget(game, eventId, ability, target);
					return null;
				}

			});
		}
	}

	public void fireSpellEventsNoTarget(final CSimulation game, final JassGameEventsWar3 eventId,
			final CAbility ability) {
		final List<CWidgetEvent> eventList = getEventList(eventId);
		if (eventList != null) {
			for (final CWidgetEvent event : eventList) {
				event.fire(this, CommonTriggerExecutionScope.unitSpellNoTargetScope(eventId, event.getTrigger(),
						ability, this, ability.getAlias()));
			}
		}
		game.getPlayer(this.playerIndex).fireSpellEventsNoTarget(unitSpellEventToPlayerEvent(eventId), ability, this);
		game.fireSpellEventsNoTarget(eventId, ability, this);
	}

	public void fireSpellEventsPointTarget(final CSimulation game, final JassGameEventsWar3 eventId,
			final CAbility ability, final AbilityPointTarget target) {
		final List<CWidgetEvent> eventList = getEventList(eventId);
		if (eventList != null) {
			for (final CWidgetEvent event : eventList) {
				event.fire(this, CommonTriggerExecutionScope.unitSpellPointScope(eventId, event.getTrigger(), ability,
						this, target, ability.getAlias()));
			}
		}
		game.getPlayer(this.playerIndex).fireSpellEventsPointTarget(unitSpellEventToPlayerEvent(eventId), ability, this,
				target);
		game.fireSpellEventsPointTarget(eventId, ability, this, target);
	}

	public void fireSpellEventsUnitTarget(final CSimulation game, final JassGameEventsWar3 eventId,
			final CAbility ability, final CUnit target) {
		final List<CWidgetEvent> eventList = getEventList(eventId);
		if (eventList != null) {
			for (final CWidgetEvent event : eventList) {
				event.fire(this, CommonTriggerExecutionScope.unitSpellTargetUnitScope(eventId, event.getTrigger(),
						ability, this, target, ability.getAlias()));
			}
		}
		game.getPlayer(this.playerIndex).fireSpellEventsUnitTarget(unitSpellEventToPlayerEvent(eventId), ability, this,
				target);
		game.fireSpellEventsUnitTarget(eventId, ability, this, target);
	}

	public void fireSpellEventsItemTarget(final CSimulation game, final JassGameEventsWar3 eventId,
			final CAbility ability, final CItem target) {
		final List<CWidgetEvent> eventList = getEventList(eventId);
		if (eventList != null) {
			for (final CWidgetEvent event : eventList) {
				event.fire(this, CommonTriggerExecutionScope.unitSpellTargetItemScope(eventId, event.getTrigger(),
						ability, this, target, ability.getAlias()));
			}
		}
		game.getPlayer(this.playerIndex).fireSpellEventsItemTarget(unitSpellEventToPlayerEvent(eventId), ability, this,
				target);
		game.fireSpellEventsItemTarget(eventId, ability, this, target);
	}

	public void fireSpellEventsDestructableTarget(final CSimulation game, final JassGameEventsWar3 eventId,
			final CAbility ability, final CDestructable target) {
		final List<CWidgetEvent> eventList = getEventList(eventId);
		if (eventList != null) {
			for (final CWidgetEvent event : eventList) {
				event.fire(this, CommonTriggerExecutionScope.unitSpellTargetDestructableScope(eventId,
						event.getTrigger(), ability, this, target, ability.getAlias()));
			}
		}
		game.getPlayer(this.playerIndex).fireSpellEventsDestructableTarget(unitSpellEventToPlayerEvent(eventId),
				ability, this, target);
		game.fireSpellEventsDestructableTarget(eventId, ability, this, target);
	}

	private JassGameEventsWar3 unitSpellEventToPlayerEvent(final JassGameEventsWar3 eventId) {
		switch (eventId) {
		case EVENT_UNIT_SPELL_CAST:
			return JassGameEventsWar3.EVENT_PLAYER_UNIT_SPELL_CAST;
		case EVENT_UNIT_SPELL_CHANNEL:
			return JassGameEventsWar3.EVENT_PLAYER_UNIT_SPELL_CHANNEL;
		case EVENT_UNIT_SPELL_EFFECT:
			return JassGameEventsWar3.EVENT_PLAYER_UNIT_SPELL_EFFECT;
		case EVENT_UNIT_SPELL_FINISH:
			return JassGameEventsWar3.EVENT_PLAYER_UNIT_SPELL_FINISH;
		case EVENT_UNIT_SPELL_ENDCAST:
			return JassGameEventsWar3.EVENT_PLAYER_UNIT_SPELL_ENDCAST;
		default:
			return null;
		}
	}

	/**
	 * Selection events go to the unit and to the player who selected it, not to its owner.
	 */
	public void fireSelectionEvents(final CSimulation game, final CPlayer selector, final boolean selected) {
		final JassGameEventsWar3 type = selected ? JassGameEventsWar3.EVENT_UNIT_SELECTED
				: JassGameEventsWar3.EVENT_UNIT_DESELECTED;
		final List<CWidgetEvent> eventList = getEventList(type);
		if (eventList != null) {
			for (int i = eventList.size() - 1; i >= 0; i--) {
				final CWidgetEvent event = eventList.get(i);
				event.fire(this, CommonTriggerExecutionScope.simpleUnitScope(type, event.getTrigger(), this, selector));
			}
		}
		selector.fireSimpleUnitEvents(this, selected ? JassGameEventsWar3.EVENT_PLAYER_UNIT_SELECTED
				: JassGameEventsWar3.EVENT_PLAYER_UNIT_DESELECTED, game);
	}

	public void fireSimpleEvents(final CSimulation game, JassGameEventsWar3 type) {
		final List<CWidgetEvent> eventList = getEventList(type);
		if (eventList != null) {
			for (int i = eventList.size() - 1; i >= 0; i--) {
				CWidgetEvent event = eventList.get(i);
				event.fire(this, CommonTriggerExecutionScope.simpleUnitScope(type, event.getTrigger(), this,
						game.getPlayer(this.playerIndex)));
			}
		}
		game.getPlayer(this.playerIndex).fireSimpleUnitEvents(this, type, game);
	}

	public void firePickUpItemEvents(final CSimulation game, final CItem item) {
		final List<CWidgetEvent> eventList = getEventList(JassGameEventsWar3.EVENT_UNIT_PICKUP_ITEM);
		if (eventList != null) {
			for (int i = eventList.size() - 1; i >= 0; i--) {
				CWidgetEvent event = eventList.get(i);
				event.fire(this, CommonTriggerExecutionScope.unitPickupItemScope(
						JassGameEventsWar3.EVENT_UNIT_PICKUP_ITEM, event.getTrigger(), this, item));
			}
		}
		game.getPlayer(this.playerIndex).firePickUpItemEvents(this, item, game);
	}

	public void fireUseItemEvents(final CSimulation game, final CItem item) {
		fireItemEvents(game, item, JassGameEventsWar3.EVENT_UNIT_USE_ITEM, JassGameEventsWar3.EVENT_PLAYER_UNIT_USE_ITEM);
	}

	public void fireDropItemEvents(final CSimulation game, final CItem item) {
		fireItemEvents(game, item, JassGameEventsWar3.EVENT_UNIT_DROP_ITEM,
				JassGameEventsWar3.EVENT_PLAYER_UNIT_DROP_ITEM);
	}

	private void fireItemEvents(final CSimulation game, final CItem item, final JassGameEventsWar3 unitEvent,
			final JassGameEventsWar3 playerEvent) {
		final List<CWidgetEvent> eventList = getEventList(unitEvent);
		if (eventList != null) {
			for (int i = eventList.size() - 1; i >= 0; i--) {
				final CWidgetEvent event = eventList.get(i);
				event.fire(this,
						CommonTriggerExecutionScope.unitPickupItemScope(unitEvent, event.getTrigger(), this, item));
			}
		}
		game.getPlayer(this.playerIndex).fireItemEvents(this, item, playerEvent);
	}

	public void fireOrderEvents(final CSimulation game, final COrderNoTarget order) {
		final List<CWidgetEvent> eventList = getEventList(JassGameEventsWar3.EVENT_UNIT_ISSUED_ORDER);
		if (eventList != null) {
			for (int i = eventList.size() - 1; i >= 0; i--) {
				CWidgetEvent event = eventList.get(i);
				event.fire(this, CommonTriggerExecutionScope.unitOrderScope(JassGameEventsWar3.EVENT_UNIT_ISSUED_ORDER,
						event.getTrigger(), this, order.getOrderId()));
			}
		}
		game.getPlayer(this.playerIndex).fireOrderEvents(this, game, order);
	}

	public void fireOrderEvents(final CSimulation game, final COrderTargetPoint order) {
		final List<CWidgetEvent> eventList = getEventList(JassGameEventsWar3.EVENT_UNIT_ISSUED_POINT_ORDER);
		if (eventList != null) {
			final AbilityPointTarget target = order.getTarget(game);
			for (int i = eventList.size() - 1; i >= 0; i--) {
				CWidgetEvent event = eventList.get(i);
				event.fire(this,
						CommonTriggerExecutionScope.unitOrderPointScope(
								JassGameEventsWar3.EVENT_UNIT_ISSUED_POINT_ORDER, event.getTrigger(), this,
								order.getOrderId(), target.x, target.y));
			}
		}
		game.getPlayer(this.playerIndex).fireOrderEvents(this, game, order);
	}

	public void fireOrderEvents(final CSimulation game, final COrderTargetWidget order) {
		final List<CWidgetEvent> eventList = getEventList(JassGameEventsWar3.EVENT_UNIT_ISSUED_TARGET_ORDER);
		if (eventList != null) {
			final CWidget target = order.getTarget(game);
			for (int i = eventList.size() - 1; i >= 0; i--) {
				CWidgetEvent event = eventList.get(i);
				event.fire(this,
						CommonTriggerExecutionScope.unitOrderTargetScope(
								JassGameEventsWar3.EVENT_UNIT_ISSUED_TARGET_ORDER, event.getTrigger(), this,
								order.getOrderId(), target));
			}
		}
		game.getPlayer(this.playerIndex).fireOrderEvents(this, game, order);
	}

	public void fireConstructFinishEvents(final CSimulation game) {
		final CUnit constructingUnit = this.worker;
		final List<CWidgetEvent> eventList = getEventList(JassGameEventsWar3.EVENT_UNIT_CONSTRUCT_FINISH);
		if (eventList != null) {
			for (int i = eventList.size() - 1; i >= 0; i--) {
				CWidgetEvent event = eventList.get(i);
				event.fire(this, CommonTriggerExecutionScope.unitConstructFinishScope(
						JassGameEventsWar3.EVENT_UNIT_CONSTRUCT_FINISH, event.getTrigger(), this, constructingUnit));
			}
		}
		game.getPlayer(this.playerIndex).fireConstructFinishEvents(this, game, constructingUnit);
	}

	/**
	 * Creep experience is reduced by the hero level it is gained at: a kill that takes the hero past a level is
	 * counted at each level's rate in turn, as the original game does.
	 */
	private static int creepXp(final CGameplayConstants gameplayConstants, final CAbilityHero heroData,
			final float rawXp) {
		float remaining = rawXp;
		float gained = 0;
		int level = heroData.getHeroLevel();
		int xp = heroData.getXp();
		while (remaining > 0) {
			final float factor = gameplayConstants.getHeroFactorXp(level);
			if (factor <= 0) {
				break;
			}
			final float toNextLevel = level < gameplayConstants.getMaxHeroLevel()
					? gameplayConstants.getNeedHeroXPSum(level) - xp
					: Float.MAX_VALUE;
			if ((remaining * factor) <= toNextLevel) {
				gained += remaining * factor;
				break;
			}
			gained += toNextLevel;
			xp += (int) toNextLevel;
			remaining -= toNextLevel / factor;
			level++;
		}
		return Math.round(gained);
	}

	public void fireSummonEvents(final CSimulation game) {
		final List<CWidgetEvent> eventList = getEventList(JassGameEventsWar3.EVENT_UNIT_SUMMON);
		if (eventList != null) {
			for (int i = eventList.size() - 1; i >= 0; i--) {
				final CWidgetEvent event = eventList.get(i);
				event.fire(this, CommonTriggerExecutionScope.unitSummonerScope(JassGameEventsWar3.EVENT_UNIT_SUMMON,
						event.getTrigger(), this, game.getPlayer(this.playerIndex)));
			}
		}
	}

	public void fireHeroLevelEvents(final CSimulation game) {
		final List<CWidgetEvent> eventList = getEventList(JassGameEventsWar3.EVENT_UNIT_HERO_LEVEL);
		if (eventList != null) {
			for (int i = eventList.size() - 1; i >= 0; i--) {
				final CWidgetEvent event = eventList.get(i);
				event.fire(this, CommonTriggerExecutionScope.playerHeroRevivableScope(
						JassGameEventsWar3.EVENT_UNIT_HERO_LEVEL, event.getTrigger(), this));
			}
		}
		game.getPlayer(this.playerIndex).fireHeroLevelEvents(this);
	}

	public void fireHeroSkillEvents(final CSimulation game, final War3ID skill, final int skillLevel) {
		final List<CWidgetEvent> eventList = getEventList(JassGameEventsWar3.EVENT_UNIT_HERO_SKILL);
		if (eventList != null) {
			for (int i = eventList.size() - 1; i >= 0; i--) {
				final CWidgetEvent event = eventList.get(i);
				event.fire(this, CommonTriggerExecutionScope.heroSkillScope(JassGameEventsWar3.EVENT_UNIT_HERO_SKILL,
						event.getTrigger(), this, skill.getValue(), skillLevel));
			}
		}
		game.getPlayer(this.playerIndex).firePlayerUnitEvents(this,
				(eventType, trigger, unit) -> CommonTriggerExecutionScope.heroSkillScope(eventType, trigger, unit,
						skill.getValue(), skillLevel),
				JassGameEventsWar3.EVENT_PLAYER_HERO_SKILL);
	}

	public void fireLoadedEvents(final CSimulation game, final CUnit transport) {
		final List<CWidgetEvent> eventList = getEventList(JassGameEventsWar3.EVENT_UNIT_LOADED);
		if (eventList != null) {
			for (int i = eventList.size() - 1; i >= 0; i--) {
				final CWidgetEvent event = eventList.get(i);
				event.fire(this, CommonTriggerExecutionScope.unitLoadedScope(JassGameEventsWar3.EVENT_UNIT_LOADED,
						event.getTrigger(), this, transport));
			}
		}
		game.getPlayer(this.playerIndex).firePlayerUnitEvents(this,
				(eventType, trigger, unit) -> CommonTriggerExecutionScope.unitLoadedScope(eventType, trigger, unit,
						transport),
				JassGameEventsWar3.EVENT_PLAYER_UNIT_LOADED);
	}

	public void fireTrainFinishEvents(final CSimulation game, final CUnit trainedUnit) {
		final CPlayer player = game.getPlayer(this.playerIndex);
		player.fireTrainFinishEvents(this, game, trainedUnit);
		final List<CWidgetEvent> eventList = getEventList(JassGameEventsWar3.EVENT_UNIT_TRAIN_FINISH);
		if (eventList != null) {
			for (int i = eventList.size() - 1; i >= 0; i--) {
				CWidgetEvent event = eventList.get(i);
				event.fire(this, CommonTriggerExecutionScope.unitTrainFinishScope(
						JassGameEventsWar3.EVENT_UNIT_TRAIN_FINISH, event.getTrigger(), this, trainedUnit, player));
			}
		}
	}

	private void fireTrainEvents(final CSimulation game, final JassGameEventsWar3 unitEventType,
			final JassGameEventsWar3 playerEventType, final War3ID trainedUnitType) {
		final CPlayer player = game.getPlayer(this.playerIndex);
		player.firePlayerUnitEvents(this, (eventType, trigger, unit) -> CommonTriggerExecutionScope
				.unitTrainScope(eventType, trigger, unit, trainedUnitType, player), playerEventType);
		final List<CWidgetEvent> eventList = getEventList(unitEventType);
		if (eventList != null) {
			for (final CWidgetEvent event : new ArrayList<>(eventList)) {
				event.fire(this, CommonTriggerExecutionScope.unitTrainScope(unitEventType, event.getTrigger(), this,
						trainedUnitType, player));
			}
		}
	}

	private void fireResearchEvents(final CSimulation game, final JassGameEventsWar3 unitEventType,
			final JassGameEventsWar3 playerEventType, final War3ID researched) {
		final CPlayer player = game.getPlayer(this.playerIndex);
		player.firePlayerUnitEvents(this, (eventType, trigger, unit) -> CommonTriggerExecutionScope
				.unitResearchFinishScope(eventType, trigger, unit, researched, player), playerEventType);
		final List<CWidgetEvent> eventList = getEventList(unitEventType);
		if (eventList != null) {
			for (final CWidgetEvent event : new ArrayList<>(eventList)) {
				event.fire(this, CommonTriggerExecutionScope.unitResearchFinishScope(unitEventType,
						event.getTrigger(), this, researched, player));
			}
		}
	}

	private void firePlayerThenUnitEvents(final CSimulation game, final JassGameEventsWar3 unitEventType,
			final JassGameEventsWar3 playerEventType) {
		final CPlayer player = game.getPlayer(this.playerIndex);
		player.fireSimpleUnitEvents(this, playerEventType, game);
		final List<CWidgetEvent> eventList = getEventList(unitEventType);
		if (eventList != null) {
			for (final CWidgetEvent event : new ArrayList<>(eventList)) {
				event.fire(this, CommonTriggerExecutionScope.simpleUnitScope(unitEventType, event.getTrigger(), this,
						player));
			}
		}
	}

	public void fireResearchFinishEvents(final CSimulation game, final War3ID researched) {
		game.getPlayer(this.playerIndex).fireResearchFinishEvents(this, game, researched);
		final List<CWidgetEvent> eventList = getEventList(JassGameEventsWar3.EVENT_UNIT_RESEARCH_FINISH);
		if (eventList != null) {
			for (int i = eventList.size() - 1; i >= 0; i--) {
				CWidgetEvent event = eventList.get(i);
				event.fire(this, CommonTriggerExecutionScope.unitResearchFinishScope(
						JassGameEventsWar3.EVENT_UNIT_RESEARCH_FINISH, event.getTrigger(), this, researched, game.getPlayer(this.playerIndex)));
			}
		}
	}

	public boolean isHero() {
		CAbility hdata = getHeroData();
		return hdata != null && !hdata.isDisabled(); // in future maybe do this with better performance
	}

	public boolean isUnitAlly(final CPlayer whichPlayer) {
		return whichPlayer.hasAlliance(getPlayerIndex(), CAllianceType.PASSIVE);
	}

	public ResourceType backToWork(final CSimulation game, final ResourceType defaultResourceType) {
		// if possible, check if this is a worker and send it to work
		for (final CAbility ability : this.abilities) {
			if (ability instanceof CAbilityHarvest) {
				final CAbilityHarvest abilityHarvest = (CAbilityHarvest) ability;
				final int carriedResourceAmount = abilityHarvest.getCarriedResourceAmount();
				final ResourceType carriedResourceType = abilityHarvest.getCarriedResourceType();
				if (carriedResourceAmount != 0) {
					switch (carriedResourceType) {
					case GOLD:
						if (carriedResourceAmount >= abilityHarvest.getGoldCapacity()) {
							abilityHarvest.getBehaviorReturnResources().reset(game);
							this.order(game, OrderIds.returnresources,
									abilityHarvest.getBehaviorReturnResources().findNearestDropoffPoint(game));
						} else {
							this.order(game, OrderIds.harvest, CBehaviorReturnResources.findNearestMine(this, game));
						}
						return ResourceType.GOLD;
					case LUMBER:
						if (carriedResourceAmount >= abilityHarvest.getLumberCapacity()) {
							abilityHarvest.getBehaviorReturnResources().reset(game);
							this.order(game, OrderIds.returnresources,
									abilityHarvest.getBehaviorReturnResources().findNearestDropoffPoint(game));
						} else {
							this.order(game, OrderIds.harvest, CBehaviorReturnResources.findNearestTree(this,
									abilityHarvest, game, abilityHarvest.getLastHarvestTarget()));
						}
						return ResourceType.LUMBER;
					default:
						throw new IllegalStateException(
								"Worker was carrying a resource of unsupported type: " + carriedResourceType);
					}
				} else if (carriedResourceType != null) {
					if (carriedResourceType == ResourceType.GOLD) {
						this.order(game, OrderIds.harvest, CBehaviorReturnResources.findNearestMine(this, game));
						return ResourceType.GOLD;
					} else if (carriedResourceType == ResourceType.LUMBER) {
						this.order(game, OrderIds.harvest, CBehaviorReturnResources.findNearestTree(this,
								abilityHarvest, game, abilityHarvest.getLastHarvestTarget()));
						return ResourceType.LUMBER;
					}
				} else if (defaultResourceType != null) {
					if (((defaultResourceType == ResourceType.GOLD) || (abilityHarvest.getLumberCapacity() == 0))
							&& (abilityHarvest.getGoldCapacity() > 0)) {
						this.order(game, OrderIds.harvest, CBehaviorReturnResources.findNearestMine(this, game));
						return ResourceType.GOLD;
					} else if (abilityHarvest.getLumberCapacity() > 0) {
						this.order(game, OrderIds.harvest, CBehaviorReturnResources.findNearestTree(this,
								abilityHarvest, game, abilityHarvest.getLastHarvestTarget()));
						return ResourceType.LUMBER;
					}
				}
			}
		}

		return null;
	}

	public void notifyAttacksChanged() {
		this.stateNotifier.attacksChanged();
	}

	public void notifyOrdersChanged() {
		this.stateNotifier.ordersChanged();
	}

	public void notifyAbilitiesChanged() {
		this.stateNotifier.abilitiesChanged();
	}

	public void notifyUpgradesChanged() {
		this.stateNotifier.upgradesChanged();
	}

	public void setConstructionConsumesWorker(final boolean constructionConsumesWorker) {
		this.constructionConsumesWorker = constructionConsumesWorker;
	}

	public boolean isConstructionConsumesWorker() {
		return this.constructionConsumesWorker;
	}

	public CDefenseType getDefenseType() {
		return this.defenseType;
	}

	public void setDefenseType(final CDefenseType defenseType) {
		this.defenseType = defenseType;
	}

	/**
	 * Ultravision (the Night Elf ability and research effect) keeps the day sight radius at night.
	 */
	public float getCurrentSightRadius(final CSimulation game) {
		if (game.isDay() || (this.ultravisionSources > 0)) {
			return this.unitType.getSightRadiusDay();
		}
		return this.unitType.getSightRadiusNight();
	}

	public void addUltravision() {
		this.ultravisionSources++;
	}

	public void removeUltravision() {
		if (this.ultravisionSources > 0) {
			this.ultravisionSources--;
		}
	}

	public boolean isUltravision() {
		return this.ultravisionSources > 0;
	}

	public EnumSet<CTargetType> getTargetedAs() {
		return this.targetedAs;
	}

	public void updateFogOfWar(final CSimulation game) {
		updateFogOfWar(game, game.getPlayer(this.playerIndex).getFogOfWar());
	}

	public void updateFogOfWar(final CSimulation game, final CPlayerFogOfWar fogOfWar) {
		if (!isDead() && !this.hidden) {
			final float sightRadius = getCurrentSightRadius(game);
			if (sightRadius > 0) {
				final int sightCells = CPlayerFogOfWar.getSightCells(sightRadius);
				final boolean flying = getMovementType() == MovementType.FLY;
				final float myX = getX();
				final float myY = getY();
				final int myZ = flying ? Integer.MAX_VALUE : game.getTerrainHeight(myX, myY);
				final PathingGrid pathingGrid = game.getPathingGrid();
				byte detections = 0;
				for (int d = 0, dn = this.detectorLevels.size(); d < dn; d++) {
					final int detectionLevel = this.detectorLevels.get(d);
					if ((detectionLevel >> 8) > 0) {
						detections |= (byte) detectionLevel;
					}
				}
				fogOfWar.setDetecting(pathingGrid.getFogOfWarIndexX(myX), pathingGrid.getFogOfWarIndexY(myY),
						CFogState.VISIBLE, detections);

				final int myXi = pathingGrid.getFogOfWarIndexX(myX);
				final int myYi = pathingGrid.getFogOfWarIndexY(myY);
				// Terrain and sight blockers are read at the first pathing cell of each fog cell, not at the unit's offset
				// within its cell: the cell centre hides far more than 1.29 does, and the offset made it depend on the step.
				final float cellX = pathingGrid.getFogOfWarSampleX(myXi);
				final float cellY = pathingGrid.getFogOfWarSampleY(myYi);
				for (int a = 1; a <= sightCells; a++) {
					detections = 0;
					for (int d = 0, dn = this.detectorLevels.size(); d < dn; d++) {
						final int detectionLevel = this.detectorLevels.get(d);
						if ((detectionLevel >> 8) > a) {
							detections |= (byte) detectionLevel;
						}
					}

					fogOfWar.checkCardinalVision(game, pathingGrid, detections, flying, myXi, myYi - a, myXi,
							(myYi - a) + 1, cellX, cellY - (a * CPlayerFogOfWar.GRID_STEP), cellX,
							cellY - ((a - 1) * CPlayerFogOfWar.GRID_STEP), myZ);
					fogOfWar.checkCardinalVision(game, pathingGrid, detections, flying, myXi, myYi + a, myXi,
							(myYi + a) - 1, cellX, cellY + (a * CPlayerFogOfWar.GRID_STEP), cellX,
							cellY + ((a - 1) * CPlayerFogOfWar.GRID_STEP), myZ);
					fogOfWar.checkCardinalVision(game, pathingGrid, detections, flying, myXi - a, myYi,
							(myXi - a) + 1, myYi, cellX - (a * CPlayerFogOfWar.GRID_STEP), cellY,
							cellX - ((a - 1) * CPlayerFogOfWar.GRID_STEP), cellY, myZ);
					fogOfWar.checkCardinalVision(game, pathingGrid, detections, flying, myXi + a, myYi,
							(myXi + a) - 1, myYi, cellX + (a * CPlayerFogOfWar.GRID_STEP), cellY,
							cellX + ((a - 1) * CPlayerFogOfWar.GRID_STEP), cellY, myZ);
				}

				for (int y = 1; y <= sightCells; y++) {
					for (int x = 1; x <= sightCells; x++) {
						final float distance = (x * x) + (y * y);
						detections = 0;
						for (int d = 0, dn = this.detectorLevels.size(); d < dn; d++) {
							final int detectionLevel = this.detectorLevels.get(d);
							if (((detectionLevel >> 8) * (detectionLevel >> 8)) > distance) {
								detections |= (byte) detectionLevel;
							}
						}
						if (CPlayerFogOfWar.isInSight(x, y, sightCells)) {
							final int xf = x * CPlayerFogOfWar.GRID_STEP;
							final int yf = y * CPlayerFogOfWar.GRID_STEP;

							fogOfWar.checkDiagonalVision(game, pathingGrid, detections, flying, x, y, myXi - x,
									myYi - y, (myXi - x) + 1, (myYi - y) + 1, cellX - xf, cellY - yf,
									(cellX - xf) + CPlayerFogOfWar.GRID_STEP, (cellY - yf) + CPlayerFogOfWar.GRID_STEP,
									myZ);
							fogOfWar.checkDiagonalVision(game, pathingGrid, detections, flying, x, y, myXi - x,
									myYi + y, (myXi - x) + 1, (myYi + y) - 1, cellX - xf, cellY + yf,
									(cellX - xf) + CPlayerFogOfWar.GRID_STEP, (cellY + yf) - CPlayerFogOfWar.GRID_STEP,
									myZ);
							fogOfWar.checkDiagonalVision(game, pathingGrid, detections, flying, x, y, myXi + x,
									myYi - y, (myXi + x) - 1, (myYi - y) + 1, cellX + xf, cellY - yf,
									(cellX + xf) - CPlayerFogOfWar.GRID_STEP, (cellY - yf) + CPlayerFogOfWar.GRID_STEP,
									myZ);
							fogOfWar.checkDiagonalVision(game, pathingGrid, detections, flying, x, y, myXi + x,
									myYi + y, (myXi + x) - 1, (myYi + y) - 1, cellX + xf, cellY + yf,
									(cellX + xf) - CPlayerFogOfWar.GRID_STEP, (cellY + yf) - CPlayerFogOfWar.GRID_STEP,
									myZ);
						}
					}
				}
			}
		}
	}

	public void setExplodesOnDeath(final boolean explodesOnDeath) {
		this.explodesOnDeath = explodesOnDeath;
	}

	public void setExplodesOnDeathBuffId(final War3ID explodesOnDeathBuffId) {
		this.explodesOnDeathBuffId = explodesOnDeathBuffId;
	}

	public boolean isExplodesOnDeath() {
		return this.explodesOnDeath;
	}

	public List<CUnitAttackPreDamageListener> getPreDamageListenersForPriority(
			final CUnitAttackPreDamageListenerPriority priority) {
		return this.preDamageListeners.get(priority);
	}

	public void addPreDamageListener(final CUnitAttackPreDamageListenerPriority priority,
			final CUnitAttackPreDamageListener listener) {
		List<CUnitAttackPreDamageListener> list = this.preDamageListeners.get(priority);
		if (list == null) {
			list = new ArrayList<>();
		}
		list.add(0, listener);
	}

	public void removePreDamageListener(final CUnitAttackPreDamageListenerPriority priority,
			final CUnitAttackPreDamageListener listener) {
		final List<CUnitAttackPreDamageListener> list = this.preDamageListeners.get(priority);
		if (list != null) {
			list.remove(listener);
		}
	}

	public List<CUnitBehaviorChangeListener> getBehaviorChangeListeners() {
		return this.behaviorChangeListeners;
	}

	public void addBehaviorChangeListener(final CUnitBehaviorChangeListener listener) {
		this.behaviorChangeListeners.add(0, listener);
	}

	public void removeBehaviorChangeListener(final CUnitBehaviorChangeListener listener) {
		this.behaviorChangeListeners.remove(listener);
	}

	public void addDamageTakenModificationListener(final CUnitAttackDamageTakenModificationListener listener) {
		this.damageTakenModificationListeners.add(0, listener);
	}

	public void removeDamageTakenModificationListener(final CUnitAttackDamageTakenModificationListener listener) {
		this.damageTakenModificationListeners.remove(listener);
	}

	public void addFinalDamageTakenModificationListener(
			final CUnitAttackFinalDamageTakenModificationListener listener) {
		this.finalDamageTakenModificationListeners.add(0, listener);
	}

	public void removeFinalDamageTakenModificationListener(
			final CUnitAttackFinalDamageTakenModificationListener listener) {
		this.finalDamageTakenModificationListeners.remove(listener);
	}

	public void addDamageTakenListener(final CUnitAttackDamageTakenListener listener) {
		this.damageTakenListeners.add(0, listener);
	}

	public void removeDamageTakenListener(final CUnitAttackDamageTakenListener listener) {
		this.damageTakenListeners.remove(listener);
	}

	public List<CUnitDeathReplacementEffect> getDeathReplacementEffectsForPriority(
			final CUnitDeathReplacementEffectPriority priority) {
		return this.deathReplacementEffects.get(priority);
	}

	public void addDeathReplacementEffect(final CUnitDeathReplacementEffectPriority priority,
			final CUnitDeathReplacementEffect listener) {
		List<CUnitDeathReplacementEffect> list = this.deathReplacementEffects.get(priority);
		if (list == null) {
			list = new ArrayList<>();
		}
		list.add(0, listener);
	}

	public void removeDeathReplacementEffect(final CUnitDeathReplacementEffectPriority priority,
			final CUnitDeathReplacementEffect listener) {
		final List<CUnitDeathReplacementEffect> list = this.deathReplacementEffects.get(priority);
		if (list != null) {
			list.remove(listener);
		}
	}

	public void addEvasionListener(final CUnitAttackEvasionListener listener) {
		this.evasionListeners.add(0, listener);
	}

	public void removeEvasionListener(final CUnitAttackEvasionListener listener) {
		this.evasionListeners.remove(listener);
	}

	public void addAttackProjReactionListener(final CUnitAttackProjReactionListener listener) {
		this.attackProjReactionListeners.add(0, listener);
	}

	public void removeAttackProjReactionListener(final CUnitAttackProjReactionListener listener) {
		this.attackProjReactionListeners.remove(listener);
	}

	public void addAbilityProjReactionListener(final CUnitAbilityProjReactionListener listener) {
		this.abilityProjReactionListeners.add(0, listener);
	}

	public void removeAbilityProjReactionListener(final CUnitAbilityProjReactionListener listener) {
		this.abilityProjReactionListeners.remove(listener);
	}

	public void addAbilityEffectReactionListener(final CUnitAbilityEffectReactionListener listener) {
		this.abilityEffectReactionListeners.add(0, listener);
	}

	public void removeAbilityEffectReactionListener(final CUnitAbilityEffectReactionListener listener) {
		this.abilityEffectReactionListeners.remove(listener);
	}

	public List<CUnitAttackPostDamageListener> getPostDamageListeners() {
		return this.postDamageListeners;
	}

	public void addPostDamageListener(final CUnitAttackPostDamageListener listener) {
		this.postDamageListeners.add(0, listener);
	}

	public void removePostDamageListener(final CUnitAttackPostDamageListener listener) {
		this.postDamageListeners.remove(listener);
	}

	public void addAttackModifier(final CUnitAttackModifier modifier) {
		this.attackModifiers.add(0, modifier);
	}

	public void removeAttackModifier(final CUnitAttackModifier modifier) {
		this.attackModifiers.remove(modifier);
	}

	public void beginCooldown(final CSimulation game, final War3ID abilityId, final float cooldownDuration) {
		final int gameTurnTick = game.getGameTurnTick();
		this.rawcodeToCooldownExpireTime.put(abilityId.getValue(),
				gameTurnTick + CTimer.ceilSteps(cooldownDuration));
		this.rawcodeToCooldownStartTime.put(abilityId.getValue(), gameTurnTick);
		fireCooldownsChangedEvent();
	}

	public int getCooldownRemainingTicks(final CSimulation game, final War3ID abilityId) {
		final int expireTime = this.rawcodeToCooldownExpireTime.get(abilityId.getValue(), -1);
		final int gameTurnTick = game.getGameTurnTick();
		if ((expireTime == -1) || (expireTime <= gameTurnTick)) {
			return 0;
		}
		return expireTime - gameTurnTick;
	}

	public int getCooldownLengthDisplayTicks(final CSimulation game, final War3ID abilityId) {
		final int startTime = this.rawcodeToCooldownStartTime.get(abilityId.getValue(), -1);
		final int expireTime = this.rawcodeToCooldownExpireTime.get(abilityId.getValue(), -1);
		if ((startTime == -1) || (expireTime == -1)) {
			return 0;
		}
		return expireTime - startTime;
	}

	public boolean isRaisable() {
		return this.raisable;
	}

	public boolean isDecays() {
		return this.decays;
	}

	public boolean isNoUnitCollision() {
		return this.noUnitCollision;
	}

	public boolean isNoBuildingCollision() {
		return this.noBuildingCollision;
	}

	/** Whether the unit is a corpse raised by Animate Dead, which counts for neither food nor its player's units. */
	public boolean isAnimatedCorpse() {
		return this.animatedCorpse;
	}

	public void setRaisable(final boolean raisable) {
		this.raisable = raisable;
	}

	public void setDecays(final boolean decays) {
		this.decays = decays;
	}

	public void setMagicImmune(final boolean magicImmune) {
		this.magicImmune = magicImmune;
	}

	public boolean isMagicImmune() {
		return this.magicImmune;
	}

	public void setLimitedMagicImmune(final boolean limitedMagicImmune) {
		this.limitedMagicImmune = limitedMagicImmune;
	}

	public boolean isLimitedMagicImmune() {
		return this.limitedMagicImmune;
	}

	public boolean isMorphImmune() {
		return this.morphImmune;
	}

	public boolean isFalseDeath() {
		return this.falseDeath;
	}

	public void setFalseDeath(final boolean falseDeath) {
		this.falseDeath = falseDeath;
	}

	public void setAutocastAbility(final CSimulation simulation, final CAutocastAbility autocastAbility) {
		if (this.autocastAbility != null && !this.autocastAbility.equals(autocastAbility)) {
			this.autocastAbility.setAutoCastOn(simulation, this, false, false);
		}
		this.autocastAbility = autocastAbility;
	}

	public void updateSeenBy(final CSimulation simulation, final int playerCount) {
		// A dead unit counts as visible everywhere; dying unseen under the black mask does not make it seen (1.29).
		if (isDead()) {
			return;
		}
		for (int i = 0; i < playerCount; i++) {
			if (((this.seenByPlayers & (1L << i)) == 0) && isVisible(simulation, i)) {
				this.seenByPlayers |= 1L << i;
			}
		}
	}

	public boolean isSeenBy(final int playerIndex) {
		return (this.seenByPlayers & (1L << playerIndex)) != 0;
	}

	public boolean isVisible(final CSimulation simulation, final int toPlayerIndex) {
		final CPlayer toPlayer = simulation.getPlayer(toPlayerIndex);
		final CPlayer owner = simulation.getPlayer(this.playerIndex);
		if (this.isDead() || (((toPlayerIndex == this.playerIndex)
				|| owner.hasAlliance(toPlayerIndex, CAllianceType.SHARED_VISION)
				|| owner.hasAlliance(toPlayerIndex, CAllianceType.SHARED_VISION_FORCED))
				&& (getCurrentSightRadius(simulation) > 0))) {
			return true;
		}
		// Takes effect at once, not at the next fog update (measured).
		if (simulation.isUnitVisionShared(this, toPlayer)) {
			return true;
		}
		if ((this.invisLevels > 0) && ((this.detections & (1 << toPlayerIndex)) == 0)) {
			return toPlayer.getFogOfWar().isDetecting(simulation, simulation.getPathingGrid(), getX(), getY(),
					this.invisLevels);
		}
		// Measured on 1.29: the neutral players see every unit, behind trees or out of any sight range alike, so
		// creeps acquire and attack without line of sight.
		if (simulation.isNeutralPlayer(toPlayerIndex)) {
			return true;
		}
		return toPlayer.getFogOfWar().isVisible(simulation, simulation.getPathingGrid(), getX(), getY());
	}

	public boolean hasUniqueFlag(String flag) {
		if (this.uniqueFlags != null) {
			return this.uniqueFlags.contains(flag);
		}
		return false;
	}

	public void addUniqueFlag(String flag) {
		if (this.uniqueFlags == null) {
			this.uniqueFlags = new ArrayList<>();
		}
		this.uniqueFlags.add(flag);
	}

	public void removeUniqueFlag(String flag) {
		if (this.uniqueFlags != null) {
			this.uniqueFlags.remove(flag);
		}
	}

	public boolean isSelectable(final CSimulation simulation, final int toPlayerIndex) {
		return ((this.unselectablePlayers & (1 << toPlayerIndex)) == 0);
	}

	public boolean isUnenumerable() {
		return this.unenumerable;
	}

	public boolean isHideMinimapIcon() {
		return hideMinimapIcon;
	}

	/** UnitSetUsesAltIcon: the minimap shows the map's alternate icon (SetAltMinimapIcon) for this unit. */
	public boolean isUsesAltMinimapIcon() {
		return this.usesAltMinimapIcon;
	}

	public void setUsesAltMinimapIcon(final boolean usesAltMinimapIcon) {
		this.usesAltMinimapIcon = usesAltMinimapIcon;
	}

	final class CTimerUnitFade extends CTimer {
		private final StateModBuff stateModBuff;

		public CTimerUnitFade(final StateModBuff stateModBuff) {
			this.stateModBuff = stateModBuff;
		}

		@Override
		public void onFire(final CSimulation simulation) {
			addStateModBuff(this.stateModBuff);
			computeUnitState(simulation, StateModBuffType.INVISIBLE);
		}

	}
}
