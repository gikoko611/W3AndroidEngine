package com.etheller.warsmash.viewer5.handlers.w3x.simulation.players;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class CPlayerUnitOrderListenerDelayingTest {
	@Test
	void inputsGivenWhilePublishingWaitForTheNextStep() {
		final List<String> calls = new ArrayList<>();
		final CPlayerUnitOrderListenerDelaying[] delaying = new CPlayerUnitOrderListenerDelaying[1];
		final CPlayerUnitOrderListener delegate = (CPlayerUnitOrderListener) Proxy.newProxyInstance(
				getClass().getClassLoader(), new Class<?>[] { CPlayerUnitOrderListener.class }, (proxy, method, args) -> {
					calls.add(method.getName());
					// A skipped cinematic makes the script select units for the local player.
					if ("issueGuiPlayerEvent".equals(method.getName())) {
						delaying[0].selectUnits(new int[0], new int[] { 1 });
					}
					return null;
				});
		delaying[0] = new CPlayerUnitOrderListenerDelaying(delegate);

		delaying[0].issueImmediateOrder(1, 2, 3, false);
		delaying[0].issueGuiPlayerEvent(4);
		delaying[0].publishDelayedActions();
		assertEquals(List.of("issueImmediateOrder", "issueGuiPlayerEvent"), calls);

		delaying[0].publishDelayedActions();
		assertEquals(List.of("issueImmediateOrder", "issueGuiPlayerEvent", "selectUnits"), calls);

		delaying[0].publishDelayedActions();
		assertEquals(3, calls.size());
	}
}
