package com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision;

import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid.MovementType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CFogState;

public class CUnitVisionFogModifier extends CFogModifier {
	private boolean enabled = true;
	private CUnit unit;
	private boolean alwaysNightVision;

	public CUnitVisionFogModifier(final CUnit unit, final boolean alwaysNightVision) {
		this.unit = unit;
		this.alwaysNightVision = alwaysNightVision;
	}

	@Override
	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public void update(final CSimulation game, final CPlayer player, final PathingGrid pathingGrid,
			final CPlayerFogOfWar fogOfWar) {
		if (enabled && !this.unit.isDead() && !this.unit.isHidden()) {
			final float sightRadius = alwaysNightVision ? this.unit.getUnitType().getSightRadiusNight()
					: this.unit.getCurrentSightRadius(game);
			if (sightRadius > 0) {
				final boolean flying = this.unit.getMovementType() == MovementType.FLY;
				final int sightCells = CPlayerFogOfWar.getSightCells(sightRadius);
				final float myX = this.unit.getX();
				final float myY = this.unit.getY();
				final int myZ = flying ? Integer.MAX_VALUE : game.getTerrainHeight(myX, myY);
				fogOfWar.setVisible(game.getPathingGrid().getFogOfWarIndexX(myX),
						game.getPathingGrid().getFogOfWarIndexY(myY), CFogState.VISIBLE);

				int myXi = game.getPathingGrid().getFogOfWarIndexX(myX);
				int myYi = game.getPathingGrid().getFogOfWarIndexY(myY);
				final float cellX = game.getPathingGrid().getFogOfWarSampleX(myXi);
				final float cellY = game.getPathingGrid().getFogOfWarSampleY(myYi);
				for (int a = 1; a <= sightCells; a++) {
					fogOfWar.checkCardinalVision(game, pathingGrid, flying, myXi, myYi - a, myXi, myYi - a + 1, cellX,
							cellY - a * CPlayerFogOfWar.GRID_STEP, cellX, cellY - (a - 1) * CPlayerFogOfWar.GRID_STEP,
							myZ);
					fogOfWar.checkCardinalVision(game, pathingGrid, flying, myXi, myYi + a, myXi, myYi + a - 1, cellX,
							cellY + a * CPlayerFogOfWar.GRID_STEP, cellX, cellY + (a - 1) * CPlayerFogOfWar.GRID_STEP,
							myZ);
					fogOfWar.checkCardinalVision(game, pathingGrid, flying, myXi - a, myYi, myXi - a + 1, myYi,
							cellX - a * CPlayerFogOfWar.GRID_STEP, cellY, cellX - (a - 1) * CPlayerFogOfWar.GRID_STEP,
							cellY, myZ);
					fogOfWar.checkCardinalVision(game, pathingGrid, flying, myXi + a, myYi, myXi + a - 1, myYi,
							cellX + a * CPlayerFogOfWar.GRID_STEP, cellY, cellX + (a - 1) * CPlayerFogOfWar.GRID_STEP,
							cellY, myZ);
				}

				for (int y = 1; y <= sightCells; y++) {
					for (int x = 1; x <= sightCells; x++) {
						if (CPlayerFogOfWar.isInSight(x, y, sightCells)) {
							int xf = x * CPlayerFogOfWar.GRID_STEP;
							int yf = y * CPlayerFogOfWar.GRID_STEP;

							fogOfWar.checkDiagonalVision(game, pathingGrid, flying, x, y, myXi - x, myYi - y,
									myXi - x + 1, myYi - y + 1, cellX - xf, cellY - yf,
									cellX - xf + CPlayerFogOfWar.GRID_STEP, cellY - yf + CPlayerFogOfWar.GRID_STEP, myZ);
							fogOfWar.checkDiagonalVision(game, pathingGrid, flying, x, y, myXi - x, myYi + y,
									myXi - x + 1, myYi + y - 1, cellX - xf, cellY + yf,
									cellX - xf + CPlayerFogOfWar.GRID_STEP, cellY + yf - CPlayerFogOfWar.GRID_STEP, myZ);
							fogOfWar.checkDiagonalVision(game, pathingGrid, flying, x, y, myXi + x, myYi - y,
									myXi + x - 1, myYi - y + 1, cellX + xf, cellY - yf,
									cellX + xf - CPlayerFogOfWar.GRID_STEP, cellY - yf + CPlayerFogOfWar.GRID_STEP, myZ);
							fogOfWar.checkDiagonalVision(game, pathingGrid, flying, x, y, myXi + x, myYi + y,
									myXi + x - 1, myYi + y - 1, cellX + xf, cellY + yf,
									cellX + xf - CPlayerFogOfWar.GRID_STEP, cellY + yf - CPlayerFogOfWar.GRID_STEP, myZ);
						}
					}
				}
			}
		}
	}
}
