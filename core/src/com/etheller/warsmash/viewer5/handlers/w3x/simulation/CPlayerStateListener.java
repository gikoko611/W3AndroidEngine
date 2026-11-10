package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import com.etheller.warsmash.util.SubscriberSetNotifier;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;

public interface CPlayerStateListener {
	void goldChanged();

	void lumberChanged();

	void foodChanged();

	void upkeepChanged();

	void heroDeath();

	void heroTokensChanged();

	/** The player changed one of its alliance settings toward another player. */
	default void allianceChanged(final int otherPlayerIndex, final CAllianceType allianceType, final boolean value) {
	}

	public static final class CPlayerStateNotifier extends SubscriberSetNotifier<CPlayerStateListener>
			implements CPlayerStateListener {
		@Override
		public void goldChanged() {
			for (final CPlayerStateListener listener : set) {
				listener.goldChanged();
			}
		}

		@Override
		public void lumberChanged() {
			for (final CPlayerStateListener listener : set) {
				listener.lumberChanged();
			}
		}

		@Override
		public void foodChanged() {
			for (final CPlayerStateListener listener : set) {
				listener.foodChanged();
			}
		}

		@Override
		public void upkeepChanged() {
			for (final CPlayerStateListener listener : set) {
				listener.upkeepChanged();
			}
		}

		@Override
		public void heroDeath() {
			for (final CPlayerStateListener listener : set) {
				listener.heroDeath();
			}
		}

		@Override
		public void heroTokensChanged() {
			for (final CPlayerStateListener listener : set) {
				listener.heroTokensChanged();
			}
		}

		@Override
		public void allianceChanged(final int otherPlayerIndex, final CAllianceType allianceType,
				final boolean value) {
			for (final CPlayerStateListener listener : set) {
				listener.allianceChanged(otherPlayerIndex, allianceType, value);
			}
		}
	}
}
