package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import java.util.ArrayDeque;
import java.util.Deque;

import com.badlogic.gdx.audio.Sound;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CSoundVolumeGroup;

/**
 * VolumeGroupSetVolume scales: every sound channel of the game belongs to one volume group.
 */
public final class CSoundMixer {
	private static final float[] GROUP_SCALES = new float[CSoundVolumeGroup.VALUES.length];
	private static final int MAX_TRACKED_LOOPS = 128;
	private static final Deque<PlayingLoop> PLAYING_LOOPS = new ArrayDeque<>();
	private static Runnable musicScaleListener;
	private static float soundOption = 1f;
	private static float musicOption = 1f;
	private static float ambientOption = 1f;
	private static float movementOption = 1f;
	private static float unitOption = 1f;
	static {
		reset();
	}

	private CSoundMixer() {
	}

	public static void reset() {
		for (int i = 0; i < GROUP_SCALES.length; i++) {
			GROUP_SCALES[i] = 1f;
		}
		notifyMusic();
	}

	public static void setGroupScale(final CSoundVolumeGroup group, final float scale) {
		GROUP_SCALES[group.ordinal()] = Math.max(0f, Math.min(1f, scale));
		if (group == CSoundVolumeGroup.MUSIC) {
			notifyMusic();
		}
	}

	public static float getGroupScale(final CSoundVolumeGroup group) {
		return GROUP_SCALES[group.ordinal()];
	}

	public static void forgetPlayingLoops() {
		PLAYING_LOOPS.clear();
	}

	public static void setMusicScaleListener(final Runnable listener) {
		musicScaleListener = listener;
	}

	private static void notifyMusic() {
		if (musicScaleListener != null) {
			musicScaleListener.run();
		}
	}

	public static float channelScale(final int channel) {
		final CSoundVolumeGroup group = groupOfChannel(channel);
		if (group == null) {
			return soundOption;
		}
		return GROUP_SCALES[group.ordinal()] * optionScale(group);
	}

	public static float musicScale() {
		return GROUP_SCALES[CSoundVolumeGroup.MUSIC.ordinal()] * musicOption;
	}

	/** The player's own settings; scripts change only the group scales, so VolumeGroupReset leaves these alone. */
	public static void setOptionScales(final float sound, final float music, final float ambient, final float movement,
			final float unit) {
		soundOption = sound;
		musicOption = music;
		ambientOption = ambient;
		movementOption = movement;
		unitOption = unit;
		for (final PlayingLoop loop : PLAYING_LOOPS) {
			loop.sound.setVolume(loop.id, loop.volume * channelScale(loop.channel));
		}
	}

	public static float getMusicOptionScale() {
		return musicOption;
	}

	/**
	 * A one shot sound keeps the volume it started with, but a loop has to follow a change of the settings. Stopped
	 * loops are not reported here; setting the volume of one does nothing, so they simply age out of the list.
	 */
	public static void loopStarted(final Sound sound, final long id, final float volume, final int channel) {
		if (PLAYING_LOOPS.size() >= MAX_TRACKED_LOOPS) {
			PLAYING_LOOPS.removeFirst();
		}
		PLAYING_LOOPS.addLast(new PlayingLoop(sound, id, volume, channel));
	}

	private static float optionScale(final CSoundVolumeGroup group) {
		switch (group) {
		case MUSIC:
			return musicOption;
		case AMBIENTSOUNDS:
			return soundOption * ambientOption;
		case UNITMOVEMENT:
			return soundOption * movementOption;
		case UNITSOUNDS:
			return soundOption * unitOption;
		default:
			return soundOption;
		}
	}

	private static final class PlayingLoop {
		private final Sound sound;
		private final long id;
		private final float volume;
		private final int channel;

		private PlayingLoop(final Sound sound, final long id, final float volume, final int channel) {
			this.sound = sound;
			this.id = id;
			this.volume = volume;
			this.channel = channel;
		}
	}

	// Channels of the sound data: 0 general, 1 unit selection, 2 unit acknowledgement, 3 unit movement, 4 unit ready,
	// 5 combat, 6 error, 7 music, 8 interface, 9 looping movement, 10 looping ambient, 11 animations, 12 constructions,
	// 13 birth, 14 fire.
	// Channel 0 carries the dialogue (DialogSounds has no channel) and belongs to no group: the speech and cinematic
	// presets lower every other group so that it stays audible. Channel 3 holds the units' "Yes" responses
	// (UnitAckSounds), not footsteps; the editor names the SPELLS group "Animation and Spell Sounds".
	private static CSoundVolumeGroup groupOfChannel(final int channel) {
		switch (channel) {
		case 0:
			return null;
		case 1:
		case 2:
		case 3:
		case 4:
			return CSoundVolumeGroup.UNITSOUNDS;
		case 9:
			return CSoundVolumeGroup.UNITMOVEMENT;
		case 5:
			return CSoundVolumeGroup.COMBAT;
		case 6:
		case 8:
			return CSoundVolumeGroup.UI;
		case 7:
			return CSoundVolumeGroup.MUSIC;
		case 14:
			return CSoundVolumeGroup.FIRE;
		case 10:
			return CSoundVolumeGroup.AMBIENTSOUNDS;
		default:
			return CSoundVolumeGroup.SPELLS;
		}
	}
}
