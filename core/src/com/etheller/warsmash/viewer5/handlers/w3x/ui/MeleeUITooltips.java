package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.parsers.fdf.datamodel.AnchorDefinition;
import com.etheller.warsmash.parsers.fdf.datamodel.FramePoint;
import com.etheller.warsmash.parsers.fdf.frames.AbstractUIFrame;
import com.etheller.warsmash.parsers.fdf.frames.SetPoint;
import com.etheller.warsmash.parsers.fdf.frames.SimpleStatusBarFrame;
import com.etheller.warsmash.parsers.fdf.frames.StringFrame;
import com.etheller.warsmash.parsers.fdf.frames.TextureFrame;
import com.etheller.warsmash.parsers.fdf.frames.UIFrame;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.Bounds;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderItem;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.ability.ItemUI;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameplayConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.hero.CAbilityHero;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.mine.CAbilityGoldMinable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.mine.CAbilityOverlayedMine;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.ResourceType;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.command.ClickableActionFrame;

/**
 * The console's tool tips, the hover tips over the world and the health and mana bars drawn
 * over units.
 */
final class MeleeUITooltips {
	private StringFrame tooltipText;
	private StringFrame tooltipUberTipText;
	private UIFrame[] tooltipResourceFrames;
	private TextureFrame[] tooltipResourceIconFrames;
	private StringFrame[] tooltipResourceTextFrames;
	private SetPoint uberTipNoResourcesSetPoint;
	private SetPoint uberTipWithResourcesSetPoint;
	private final java.util.Map<UIFrame, String[]> frameTooltips = new java.util.HashMap<>();

	private static final int RESOURCE_GOLD = 0;
	private static final int RESOURCE_LUMBER = 1;
	private static final int RESOURCE_SUPPLY = 2;
	private static final int RESOURCE_UPKEEP = 3;
	private static final float RESOURCE_BAR_ICON_WIDTH = 0.01640625f;

	final MeleeUI ui;

	MeleeUITooltips(final MeleeUI ui) {
		this.ui = ui;
	}

	void createTooltips() {
		this.ui.tooltipFrame = this.ui.rootFrame.createFrame("SmashToolTip", this.ui.rootFrame, 0, 0);
		this.ui.tooltipFrame.addAnchor(new AnchorDefinition(FramePoint.BOTTOMRIGHT,
				GameUI.convertX(this.ui.uiViewport, 0.f), GameUI.convertY(this.ui.uiViewport, 0.176f)));
		this.ui.tooltipFrame.setWidth(GameUI.convertX(this.ui.uiViewport, 0.280f));
		this.tooltipText = (StringFrame) this.ui.rootFrame.getFrameByName("SmashToolTipText", 0);
		this.tooltipText.setWidth(GameUI.convertX(this.ui.uiViewport, 0.274f));
		this.tooltipText.addAnchor(new AnchorDefinition(FramePoint.TOPLEFT, GameUI.convertX(this.ui.uiViewport, 0.003f),
				GameUI.convertY(this.ui.uiViewport, -0.003f)));
		this.ui.tooltipFrame.setVisible(false);

		this.ui.hovertipFrame = this.ui.rootFrame.createFrame("SmashHoverTip", this.ui.rootFrame, 0, 0);
		this.ui.hovertipText = (StringFrame) this.ui.rootFrame.getFrameByName("SmashHoverTipText", 0);
		this.ui.hovertipText.setWidth(GameUI.convertX(this.ui.uiViewport, 0.274f));
		this.ui.hovertipText.addAnchor(new AnchorDefinition(FramePoint.TOPLEFT,
				GameUI.convertX(this.ui.uiViewport, 0.006f), GameUI.convertY(this.ui.uiViewport, -0.006f)));
		this.ui.hovertipFrame.setVisible(false);

		this.tooltipUberTipText = (StringFrame) this.ui.rootFrame.getFrameByName("SmashUberTipText", 0);
		this.tooltipUberTipText.setWidth(GameUI.convertX(this.ui.uiViewport, 0.274f));
		this.uberTipNoResourcesSetPoint = new SetPoint(FramePoint.TOPLEFT, this.tooltipText, FramePoint.BOTTOMLEFT, 0,
				GameUI.convertY(this.ui.uiViewport, -0.004f));
		this.uberTipWithResourcesSetPoint = new SetPoint(FramePoint.TOPLEFT, this.tooltipText, FramePoint.BOTTOMLEFT, 0,
				GameUI.convertY(this.ui.uiViewport, -0.014f));
		this.tooltipUberTipText.addSetPoint(this.uberTipNoResourcesSetPoint);
		this.tooltipResourceFrames = new UIFrame[ResourceType.VALUES.length];
		this.tooltipResourceIconFrames = new TextureFrame[ResourceType.VALUES.length];
		this.tooltipResourceTextFrames = new StringFrame[ResourceType.VALUES.length];
		for (int i = 0; i < this.tooltipResourceFrames.length; i++) {
			this.tooltipResourceFrames[i] = this.ui.rootFrame.createFrame("SmashToolTipIconResource",
					this.ui.tooltipFrame, 0, i);
			this.tooltipResourceIconFrames[i] = (TextureFrame) this.ui.rootFrame
					.getFrameByName("SmashToolTipIconResourceBackdrop", i);
			this.tooltipResourceTextFrames[i] = (StringFrame) this.ui.rootFrame
					.getFrameByName("SmashToolTipIconResourceLabel", i);
			this.tooltipResourceFrames[i].addSetPoint(new SetPoint(FramePoint.TOPLEFT, this.tooltipText,
					FramePoint.BOTTOMLEFT, GameUI.convertX(this.ui.uiViewport, 0.004f + (0.032f * i)),
					GameUI.convertY(this.ui.uiViewport, -0.001f)));
			// have we really no better API than the below???
			((AbstractUIFrame) this.ui.tooltipFrame).add(this.tooltipResourceFrames[i]);
			this.ui.rootFrame.remove(this.tooltipResourceFrames[i]);
		}
	}

	String getWorldFrameHoverTipText(final CSimulation game, final RenderWidget whichUnit) {
		if (whichUnit instanceof RenderUnit) {
			final RenderUnit renderUnit = (RenderUnit) whichUnit;
			final CUnit simulationUnit = renderUnit.getSimulationUnit();
			final CAbilityHero heroData = simulationUnit.getHeroData();
			final int simulationUnitPlayerIndex = simulationUnit.getFakePlayerIndex();
			final boolean neutralHostile = simulationUnitPlayerIndex == (WarsmashConstants.MAX_PLAYERS - 4);
			final boolean neutralPassive = simulationUnitPlayerIndex == (WarsmashConstants.MAX_PLAYERS - 1);
			String returnValue = "";
			if ((simulationUnitPlayerIndex != this.ui.localPlayer.getId())
					&& (!this.ui.localPlayer.hasAlliance(simulationUnitPlayerIndex, CAllianceType.SHARED_CONTROL))
					&& (simulationUnitPlayerIndex < (WarsmashConstants.MAX_PLAYERS - 4))) {
				final CPlayer unitPlayer = game.getPlayer(simulationUnitPlayerIndex);
				final String name = unitPlayer.getName();
				if (name != null) {
					if (unitPlayer.hasAlliance(this.ui.localPlayer.getId(), CAllianceType.PASSIVE)) {
						if (unitPlayer.hasAlliance(this.ui.localPlayer.getId(), CAllianceType.SHARED_SPELLS)) {
							returnValue = "|CFF00FF00" + name;
						}
						else {
							returnValue = "|CFFFFFF00" + name;
						}
					}
					else {
						returnValue = "|CFFFF0000" + name;
					}
				}
			}
			if (heroData != null) {
				if (!returnValue.isEmpty()) {
					returnValue += "|r|n";
				}
				final String level = this.ui.rootFrame.getTemplates().getDecoratedString("LEVEL");
				return returnValue + heroData.getProperName() + "|n" + level + " " + heroData.getHeroLevel();
			}
			final CAbilityGoldMinable goldMineData = simulationUnit.getGoldMineData();
			final CAbilityOverlayedMine blightedGoldMineData = simulationUnit.getOverlayedGoldMineData();
			final boolean neutral = (neutralPassive && simulationUnit.isBuilding()) || neutralHostile
					|| (goldMineData != null) || (blightedGoldMineData != null);
			if (neutral) {
				if (!returnValue.isEmpty()) {
					returnValue += "|n";
				}
				returnValue += simulationUnit.getName();
				if (goldMineData != null) {
					final String colonGold = this.ui.rootFrame.getTemplates().getDecoratedString("COLON_GOLD");
					returnValue += "|n" + colonGold + " " + goldMineData.getGold();
				}
				else {
					if (blightedGoldMineData != null) {
						final String colonGold = this.ui.rootFrame.getTemplates().getDecoratedString("COLON_GOLD");
						returnValue += "|n" + colonGold + " " + blightedGoldMineData.getGold();
					}
				}
				final int creepLevel = simulationUnit.getUnitType().getLevel();
				if (neutralHostile && (creepLevel > 0)) {
					final String level = this.ui.rootFrame.getTemplates().getDecoratedString("LEVEL");
					returnValue += "|n" + level + " " + creepLevel;
				}
			}
			if (!returnValue.isEmpty()) {
				return returnValue;
			}
		}
		else if (whichUnit instanceof RenderItem) {
			final RenderItem renderItem = (RenderItem) whichUnit;
			final ItemUI itemUI = this.ui.war3MapViewer.getAbilityDataUI()
					.getItemUI(renderItem.getSimulationItem().getTypeId());
			return itemUI.getName();
		}
		else if (whichUnit instanceof RenderDestructable) {
			final RenderDestructable renderDest = (RenderDestructable) whichUnit;
			final String name = renderDest.getSimulationDestructable().getDestType().getName();
			return name;
		}
		return null;
	}

	void showAllStatusBars() {
		final CSimulation simulation = this.ui.war3MapViewer.simulation;
		final int localPlayerIndex = this.ui.war3MapViewer.getLocalPlayerIndex();
		final int width = Gdx.graphics.getWidth();
		final int height = Gdx.graphics.getHeight();
		for (final RenderUnit unit : this.ui.war3MapViewer.units) {
			final CUnit simulationUnit = unit.getSimulationUnit();
			if ((unit == this.ui.mouseOverUnit) || this.ui.dragSelectPreviewUnits.contains(unit)
					|| simulationUnit.isDead() || simulationUnit.isInvulnerable()
					|| !unit.isSelectable(simulation, localPlayerIndex)) {
				continue;
			}
			MeleeUI.clickLocationTemp.set(unit.getX(), unit.getY(), unit.getZ());
			this.ui.war3MapViewer.worldScene.camera.worldToScreen(MeleeUI.screenCoordsVector,
					MeleeUI.clickLocationTemp);
			if ((MeleeUI.screenCoordsVector.x < 0) || (MeleeUI.screenCoordsVector.x > width)
					|| (MeleeUI.screenCoordsVector.y < 0) || (MeleeUI.screenCoordsVector.y > height)) {
				continue;
			}
			positionHealthBar(getHpBar(), unit, 1.0f);
			if (simulationUnit.getMaximumMana() > 0) {
				positionManaBar(getHpBar(), unit, 1.0f);
			}
		}
	}

	void positionHealthBar(final SimpleStatusBarFrame simpleStatusBarFrame, final RenderWidget unit,
			final float alpha) {
		final float lifeRatioRemaining = unit.getSimulationWidget().getLife() / unit.getSimulationWidget().getMaxLife();
		positionStatusBar(simpleStatusBarFrame, unit, alpha, lifeRatioRemaining,
				Math.min(1.0f, 2.0f - (lifeRatioRemaining * 2)), Math.min(1.0f, lifeRatioRemaining * 2), 0, 0);
	}

	void positionManaBar(final SimpleStatusBarFrame simpleStatusBarFrame, final RenderUnit unit,
			final float alpha) {
		positionStatusBar(simpleStatusBarFrame, unit, alpha,
				unit.getSimulationUnit().getMana() / unit.getSimulationUnit().getMaximumMana(), 0, 0, 1, 16f);
	}

	private void positionStatusBar(final SimpleStatusBarFrame simpleStatusBarFrame, final RenderWidget unit,
			final float alpha, final float ratio, final float red, final float green, final float blue,
			final float offsetY) {
		simpleStatusBarFrame.setVisible(true);
		MeleeUI.clickLocationTemp.x = unit.getX();
		MeleeUI.clickLocationTemp.y = unit.getY();
		MeleeUI.clickLocationTemp.z = unit.getZ();
		final Bounds unitBounds = unit.getInstance().getBounds();
		if (unitBounds != null) {
			final BoundingBox unitBoundsBox = unitBounds.getBoundingBox();
			if (unitBoundsBox != null) {
				MeleeUI.clickLocationTemp.z += unitBoundsBox.max.z;
			}
		}
		this.ui.war3MapViewer.worldScene.camera.worldToScreen(MeleeUI.screenCoordsVector, MeleeUI.clickLocationTemp);
		simpleStatusBarFrame.getBarFrame().setTexture("SimpleHpBarConsole", this.ui.rootFrame);
		simpleStatusBarFrame.getBorderFrame().setTexture("Textures\\Black32.blp", this.ui.rootFrame);
		simpleStatusBarFrame.getBorderFrame().setColor(0f, 0f, 0f, alpha);
		simpleStatusBarFrame.getBarFrame().setColor(red, green, blue, alpha);
		final Vector2 unprojected = this.ui.uiViewport.unproject(MeleeUI.screenCoordsVector);
		simpleStatusBarFrame.setWidth((unit.getSelectionScale() * 1.5f * Gdx.graphics.getWidth()) / 2560);
		simpleStatusBarFrame.setHeight(16);
		simpleStatusBarFrame.addSetPoint(new SetPoint(FramePoint.CENTER, this.ui.rootFrame, FramePoint.BOTTOMLEFT,
				unprojected.x, unprojected.y - offsetY));
		simpleStatusBarFrame.setValue(ratio);
		simpleStatusBarFrame.positionBounds(this.ui.rootFrame, this.ui.uiViewport);
	}

	SimpleStatusBarFrame getHpBar() {
		final SimpleStatusBarFrame simpleStatusBarFrame;
		if (this.ui.hpBarFrameIndex >= this.ui.hpBarFrames.size()) {
			simpleStatusBarFrame = new SimpleStatusBarFrame("SmashHpBar" + this.ui.hpBarFrameIndex, this.ui.rootFrame,
					true, true, 3.0f);
			this.ui.rootFrame.add(simpleStatusBarFrame);
			this.ui.hpBarFrames.add(simpleStatusBarFrame);
		}
		else {
			simpleStatusBarFrame = this.ui.hpBarFrames.get(this.ui.hpBarFrameIndex);
		}
		this.ui.hpBarFrameIndex++;
		return simpleStatusBarFrame;
	}

	void setFrameTooltip(final UIFrame frame, final String key) {
		if (frame != null) {
			this.frameTooltips.put(frame, new String[] { key, key + "_UBER" });
		}
	}

	void loadTooltip(final ClickableActionFrame mousedUIFrame) {
		showTooltip(mousedUIFrame.getToolTip(), mousedUIFrame.getUberTip(), mousedUIFrame.getToolTipGoldCost(),
				mousedUIFrame.getToolTipLumberCost(), mousedUIFrame.getToolTipFoodCost(),
				mousedUIFrame.getToolTipManaCost());
	}

	void loadFrameTooltip(final UIFrame frame) {
		final String[] keys = this.frameTooltips.get(frame);
		if (keys == null) {
			this.ui.tooltipFrame.setVisible(false);
			return;
		}
		showTooltip(this.ui.rootFrame.getTemplates().getDecoratedString(keys[0]),
				this.ui.rootFrame.getTemplates().getDecoratedString(keys[1]), 0, 0, 0, 0);
	}

	/** The resource bar part under the point: its text and, left of gold, lumber and food, the icon. */
	int resourceAt(final float x, final float y) {
		if ((this.ui.resourceBar == null) || !this.ui.resourceBar.isVisible() || !this.ui.consoleUI.isVisible()) {
			return -1;
		}
		final StringFrame[] texts = { this.ui.resourceBarGoldText, this.ui.resourceBarLumberText,
				this.ui.resourceBarSupplyText, this.ui.resourceBarUpkeepText };
		for (int i = 0; i < texts.length; i++) {
			final com.badlogic.gdx.math.Rectangle bounds = texts[i].getRenderBounds();
			final float left = bounds.x - (i == RESOURCE_UPKEEP ? 0 : GameUI.convertX(this.ui.uiViewport,
					RESOURCE_BAR_ICON_WIDTH));
			if ((x >= left) && (x <= (bounds.x + bounds.width)) && (y >= bounds.y)
					&& (y <= (bounds.y + bounds.height))) {
				return i;
			}
		}
		return -1;
	}

	void loadResourceTooltip(final int resource) {
		final String title;
		final String uberTip;
		switch (resource) {
		case RESOURCE_GOLD:
			title = decorated("GOLD");
			uberTip = decorated("RESOURCE_UBERTIP_GOLD");
			break;
		case RESOURCE_LUMBER:
			title = decorated("LUMBER");
			uberTip = decorated("RESOURCE_UBERTIP_LUMBER");
			break;
		case RESOURCE_SUPPLY:
			title = decorated("COLON_FOOD").replaceAll(":\\s*$", "");
			uberTip = decorated("RESOURCE_UBERTIP_SUPPLY");
			break;
		default:
			title = decorated("UPKEEP");
			uberTip = decorated("RESOURCE_UBERTIP_UPKEEP") + upkeepLevelsText();
			break;
		}
		showTooltip(title, uberTip, 0, 0, 0, 0);
	}

	private String decorated(final String key) {
		return this.ui.rootFrame.getTemplates().getDecoratedString(key);
	}

	/** One line per upkeep level up to the food ceiling: its food range, name and the share of income kept. */
	private String upkeepLevelsText() {
		final CGameplayConstants constants = this.ui.war3MapViewer.simulation.getGameplayConstants();
		final int[] usage = constants.getUpkeepUsage();
		final float[] goldTax = constants.getUpkeepGoldTax();
		final float[] lumberTax = constants.getUpkeepLumberTax();
		final int ceiling = this.ui.localPlayer.getFoodCapCeiling();
		final String[] names = { "UPKEEP_NONE", "UPKEEP_LOW", "UPKEEP_HIGH" };
		boolean taxesLumber = false;
		for (final float tax : lumberTax) {
			taxesLumber |= tax != 0;
		}
		final StringBuilder text = new StringBuilder();
		int low = 0;
		for (int level = 0; low <= ceiling; level++) {
			final int high = (level < usage.length) ? Math.min(usage[level], ceiling) : ceiling;
			final String name = decorated(names[Math.min(level, names.length - 1)]);
			final int goldKept = Math.round(100 * (1 - tax(goldTax, level)));
			if (taxesLumber) {
				text.append(String.format(decorated("RESOURCE_UBERTIP_UPKEEP_INFO_WOOD"), low, high, name, goldKept,
						Math.round(100 * (1 - tax(lumberTax, level)))));
			}
			else {
				text.append(String.format(decorated("RESOURCE_UBERTIP_UPKEEP_INFO"), low, high, name, goldKept));
			}
			low = high + 1;
		}
		return text.toString();
	}

	private static float tax(final float[] taxes, final int level) {
		return taxes.length == 0 ? 0 : taxes[Math.min(level, taxes.length - 1)];
	}

	private void showTooltip(final String toolTip, final String uberTip, final int goldCost, final int lumberCost,
			final int foodCost, final int manaCost) {
		if ((toolTip == null) || (uberTip == null)) {
			this.ui.tooltipFrame.setVisible(false);
		}
		else {
			this.ui.rootFrame.setText(this.tooltipUberTipText, GameOptions.get().isEnhancedTooltips() ? uberTip : "");
			int resourceIndex = 0;
			if (goldCost != 0) {
				this.tooltipResourceFrames[resourceIndex].setVisible(true);
				this.tooltipResourceIconFrames[resourceIndex].setTexture("ToolTipGoldIcon", this.ui.rootFrame);
				this.ui.rootFrame.setText(this.tooltipResourceTextFrames[resourceIndex], Integer.toString(goldCost));
				resourceIndex++;
			}
			if (lumberCost != 0) {
				this.tooltipResourceFrames[resourceIndex].setVisible(true);
				this.tooltipResourceIconFrames[resourceIndex].setTexture("ToolTipLumberIcon", this.ui.rootFrame);
				this.ui.rootFrame.setText(this.tooltipResourceTextFrames[resourceIndex], Integer.toString(lumberCost));
				resourceIndex++;
			}
			if (foodCost != 0) {
				this.tooltipResourceFrames[resourceIndex].setVisible(true);
				this.tooltipResourceIconFrames[resourceIndex].setTexture("ToolTipSupplyIcon", this.ui.rootFrame);
				this.ui.rootFrame.setText(this.tooltipResourceTextFrames[resourceIndex], Integer.toString(foodCost));
				resourceIndex++;
			}
			if (manaCost != 0) {
				this.tooltipResourceFrames[resourceIndex].setVisible(true);
				this.tooltipResourceIconFrames[resourceIndex].setTexture("ToolTipManaIcon", this.ui.rootFrame);
				this.ui.rootFrame.setText(this.tooltipResourceTextFrames[resourceIndex], Integer.toString(manaCost));
				resourceIndex++;
			}
			for (int i = resourceIndex; i < this.tooltipResourceFrames.length; i++) {
				this.tooltipResourceFrames[i].setVisible(false);
			}
			float resourcesHeight;
			if (resourceIndex != 0) {
				this.tooltipUberTipText.addSetPoint(this.uberTipWithResourcesSetPoint);
				resourcesHeight = 0.014f;
			}
			else {
				this.tooltipUberTipText.addSetPoint(this.uberTipNoResourcesSetPoint);
				resourcesHeight = 0.004f;
			}
			this.ui.rootFrame.setText(this.tooltipText, toolTip);
			final float predictedViewportHeight = this.tooltipText.getPredictedViewportHeight() + GameUI
					.convertY(this.ui.uiViewport,
					resourcesHeight) + this.tooltipUberTipText.getPredictedViewportHeight() + GameUI
							.convertY(this.ui.uiViewport, 0.003f);
			this.ui.tooltipFrame.setHeight(predictedViewportHeight);
			this.ui.tooltipFrame.positionBounds(this.ui.rootFrame, this.ui.uiViewport);
			this.ui.tooltipFrame.setVisible(true);
		}
	}
}
