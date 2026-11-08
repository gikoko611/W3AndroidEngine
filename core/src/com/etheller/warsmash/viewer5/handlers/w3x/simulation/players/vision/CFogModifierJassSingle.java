package com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision;

import com.etheller.interpreter.ast.util.AllocatedHandle;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;

public class CFogModifierJassSingle extends AllocatedHandle implements CFogModifierJass {
	private final int playerIndex;
	private final CFogModifier modifier;

	public CFogModifierJassSingle(int playerIndex, CFogModifier modifier) {
		this.playerIndex = playerIndex;
		this.modifier = modifier;
	}

	@Override
	public void setEnabled(boolean flag) {
		this.modifier.setEnabled(flag);
	}

	@Override
	public void applyNow(final CSimulation game) {
		this.modifier.applyNow(game, game.getPlayer(this.playerIndex));
	}

	@Override
	public void destroy(CSimulation game) {
		game.getPlayer(this.playerIndex).removeFogModifer(game, this.modifier);
	}

}
