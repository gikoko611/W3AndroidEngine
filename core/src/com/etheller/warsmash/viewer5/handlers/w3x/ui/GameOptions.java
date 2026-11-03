package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CSaveStorage;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound.CSoundMixer;

/**
 * The player's Options menu settings, kept per profile. Nothing here may reach the simulation: players of one game can
 * differ in all of it. A missing setting keeps what the engine does without a settings file.
 */
public final class GameOptions {
	public static final int NEUTRAL_GAMMA = 50;
	public static final int NEUTRAL_SCROLL = 50;
	public static final int PARTICLES_LOW = 0;
	public static final int PARTICLES_MEDIUM = 1;
	public static final int PARTICLES_HIGH = 2;
	private static final String FILE_NAME = "Options.dat";
	private static final float[] PARTICLE_FACTORS = { 0.5f, 1.0f, 1.5f };
	private static final float SLOWEST_SCROLL_FACTOR = 0.25f;
	private static final float FASTEST_SCROLL_FACTOR = 2f;
	private static GameOptions current;

	private final Map<String, Integer> values = new HashMap<>();

	private GameOptions() {
	}

	public static GameOptions get() {
		if (current == null) {
			current = new GameOptions();
			current.read();
		}
		return current;
	}

	/** The profile changed. */
	public static void reload() {
		current = null;
		get().apply();
	}

	private void read() {
		CSaveStorage.read(file(), in -> {
			final int count = in.readInt();
			for (int i = 0; i < count; i++) {
				final String key = in.readUTF();
				this.values.put(key, in.readInt());
			}
		});
	}

	public boolean save() {
		return CSaveStorage.writeAtomically(file(), out -> {
			out.writeInt(this.values.size());
			for (final Map.Entry<String, Integer> entry : this.values.entrySet()) {
				out.writeUTF(entry.getKey());
				out.writeInt(entry.getValue());
			}
		});
	}

	private static FileHandle file() {
		return CSaveStorage.profileFile(FILE_NAME);
	}

	/** Makes the settings that live outside this class take effect; the rest are read where they are used. */
	public void apply() {
		final float sound = isSound() ? getSoundVolume() / 100f : 0;
		final float music = isMusic() ? getMusicVolume() / 100f : 0;
		CSoundMixer.setOptionScales(sound, music, isAmbientSounds() ? 1 : 0, isMovementSounds() ? 1 : 0,
				isUnitSounds() ? 1 : 0);
		WarsmashConstants.MODEL_DETAIL_PARTICLE_FACTOR = PARTICLE_FACTORS[getParticles()];
		if (isVSyncAdjustable()) {
			try {
				Gdx.graphics.setVSync(isVSync());
			}
			catch (final RuntimeException e) {
				// Some desktop drivers refuse the swap interval while another game holds the GPU; the map still runs.
				e.printStackTrace();
			}
		}
	}

	public static boolean isVSyncAdjustable() {
		return Gdx.app.getType() == Application.ApplicationType.Desktop;
	}

	private int number(final String key, final int fallback, final int max) {
		final Integer value = this.values.get(key);
		return value == null ? fallback : Math.max(0, Math.min(max, value));
	}

	private boolean flag(final String key, final boolean fallback) {
		final Integer value = this.values.get(key);
		return value == null ? fallback : value != 0;
	}

	private void setFlag(final String key, final boolean value) {
		this.values.put(key, value ? 1 : 0);
	}

	public boolean isSound() {
		return flag("sound", true);
	}

	public void setSound(final boolean value) {
		setFlag("sound", value);
	}

	public int getSoundVolume() {
		return number("soundVolume", 100, 100);
	}

	public void setSoundVolume(final int value) {
		this.values.put("soundVolume", value);
	}

	public boolean isMusic() {
		return flag("music", true);
	}

	public void setMusic(final boolean value) {
		setFlag("music", value);
	}

	public int getMusicVolume() {
		// Warcraft III's own default: music at 40, sound effects at 100.
		return number("musicVolume", 40, 100);
	}

	public void setMusicVolume(final int value) {
		this.values.put("musicVolume", value);
	}

	public boolean isAmbientSounds() {
		return flag("ambientSounds", true);
	}

	public void setAmbientSounds(final boolean value) {
		setFlag("ambientSounds", value);
	}

	public boolean isMovementSounds() {
		return flag("movementSounds", true);
	}

	public void setMovementSounds(final boolean value) {
		setFlag("movementSounds", value);
	}

	public boolean isUnitSounds() {
		return flag("unitSounds", true);
	}

	public void setUnitSounds(final boolean value) {
		setFlag("unitSounds", value);
	}

	public boolean isSubtitles() {
		return flag("subtitles", true);
	}

	public void setSubtitles(final boolean value) {
		setFlag("subtitles", value);
	}

	public int getMouseScroll() {
		return number("mouseScroll", NEUTRAL_SCROLL, 100);
	}

	public void setMouseScroll(final int value) {
		this.values.put("mouseScroll", value);
	}

	public boolean isMouseScrollDisabled() {
		return flag("mouseScrollDisabled", false);
	}

	public void setMouseScrollDisabled(final boolean value) {
		setFlag("mouseScrollDisabled", value);
	}

	public int getKeyboardScroll() {
		return number("keyboardScroll", NEUTRAL_SCROLL, 100);
	}

	public void setKeyboardScroll(final int value) {
		this.values.put("keyboardScroll", value);
	}

	public float getMouseScrollFactor() {
		return scrollFactor(getMouseScroll());
	}

	public float getKeyboardScrollFactor() {
		return scrollFactor(getKeyboardScroll());
	}

	private static float scrollFactor(final int value) {
		if (value <= NEUTRAL_SCROLL) {
			return SLOWEST_SCROLL_FACTOR + (((1 - SLOWEST_SCROLL_FACTOR) * value) / NEUTRAL_SCROLL);
		}
		return 1 + (((FASTEST_SCROLL_FACTOR - 1) * (value - NEUTRAL_SCROLL)) / (100 - NEUTRAL_SCROLL));
	}

	public boolean isEnhancedTooltips() {
		return flag("enhancedTooltips", true);
	}

	public void setEnhancedTooltips(final boolean value) {
		setFlag("enhancedTooltips", value);
	}

	public boolean isAlwaysShowStatusBars() {
		return flag("statusBars", false);
	}

	public void setAlwaysShowStatusBars(final boolean value) {
		setFlag("statusBars", value);
	}

	public boolean isFormationMovement() {
		return flag("formationMovement", true);
	}

	public void setFormationMovement(final boolean value) {
		setFlag("formationMovement", value);
	}

	/** Whether Alt held while ordering inverts the formation movement preference. */
	public boolean isFormationToggle() {
		return flag("formationToggle", true);
	}

	public void setFormationToggle(final boolean value) {
		setFlag("formationToggle", value);
	}

	public boolean isMinimapTerrain() {
		return flag("minimapTerrain", true);
	}

	public void setMinimapTerrain(final boolean value) {
		setFlag("minimapTerrain", value);
	}

	public boolean isMinimapCreeps() {
		return flag("minimapCreeps", true);
	}

	public void setMinimapCreeps(final boolean value) {
		setFlag("minimapCreeps", value);
	}

	public int getGamma() {
		return number("gamma", NEUTRAL_GAMMA, 100);
	}

	public void setGamma(final int value) {
		this.values.put("gamma", value);
	}

	public int getParticles() {
		return number("particles", PARTICLES_HIGH, PARTICLES_HIGH);
	}

	public void setParticles(final int value) {
		this.values.put("particles", value);
	}

	public boolean isUnitShadows() {
		return flag("unitShadows", true);
	}

	public void setUnitShadows(final boolean value) {
		setFlag("unitShadows", value);
	}

	public boolean isVSync() {
		return flag("vsync", true);
	}

	public void setVSync(final boolean value) {
		setFlag("vsync", value);
	}
}
