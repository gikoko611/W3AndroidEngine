package com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision;

import com.etheller.interpreter.ast.util.CAllocatedHandle;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;

public interface CFogModifierJass extends CAllocatedHandle {
	void setEnabled(boolean flag);

	void destroy(CSimulation game);

	void applyNow(CSimulation game);
}
