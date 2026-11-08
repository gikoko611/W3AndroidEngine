package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import com.etheller.warsmash.parsers.fdf.frames.EditBoxFrame;
import com.etheller.warsmash.parsers.fdf.frames.GlueTextButtonFrame;
import com.etheller.warsmash.parsers.fdf.frames.ListBoxFrame;
import com.etheller.warsmash.parsers.fdf.frames.SimpleFrame;
import com.etheller.warsmash.parsers.fdf.frames.StringFrame;
import com.etheller.warsmash.parsers.fdf.frames.UIFrame;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.MenuUI.MenuState;

/**
 * The profile panel of the single player menu: the player profiles, which one plays, and the name the menu shows.
 */
final class MenuProfiles {
	private final MenuUI menu;
	private final PlayerProfileManager profileManager;
	private UIFrame profilePanel;
	private EditBoxFrame newProfileEditBox;
	private GlueTextButtonFrame addProfileButton;
	private GlueTextButtonFrame deleteProfileButton;
	private GlueTextButtonFrame selectProfileButton;
	private StringFrame profileNameText;

	MenuProfiles(final MenuUI menu, final PlayerProfileManager profileManager) {
		this.menu = menu;
		this.profileManager = profileManager;
	}

	void create() {
		this.profilePanel = this.menu.rootFrame.getFrameByName("ProfilePanel", 0);
		this.profilePanel.setVisible(false);

		this.newProfileEditBox = (EditBoxFrame) this.menu.rootFrame.getFrameByName("NewProfileEditBox", 0);
		this.newProfileEditBox.setOnChange(() -> this.addProfileButton
				.setEnabled(!this.profileManager.hasProfile(this.newProfileEditBox.getText())));
		final StringFrame profileListText = (StringFrame) this.menu.rootFrame.getFrameByName("ProfileListText", 0);
		final SimpleFrame profileListContainer = (SimpleFrame) this.menu.rootFrame
				.getFrameByName("ProfileListContainer", 0);
		final ListBoxFrame profileListBox = (ListBoxFrame) this.menu.rootFrame.createFrameByType("LISTBOX",
				"ListBoxWar3", profileListContainer, "WITHCHILDREN", 0);
		profileListBox.setSetAllPoints(true);
		profileListBox.setFrameFont(profileListText.getFrameFont());
		for (final PlayerProfile profile : this.profileManager.getProfiles()) {
			profileListBox.addItem(profile.getName(), this.menu.rootFrame, this.menu.uiViewport);
		}
		profileListContainer.add(profileListBox);

		this.addProfileButton = (GlueTextButtonFrame) this.menu.rootFrame.getFrameByName("AddProfileButton", 0);
		this.deleteProfileButton = (GlueTextButtonFrame) this.menu.rootFrame.getFrameByName("DeleteProfileButton", 0);
		this.selectProfileButton = (GlueTextButtonFrame) this.menu.rootFrame.getFrameByName("SelectProfileButton", 0);
		this.selectProfileButton.setEnabled(false);
		this.deleteProfileButton.setEnabled(false);
		this.addProfileButton.setOnClick(() -> {
			final String newProfileName = this.newProfileEditBox.getText();
			if (!newProfileName.isEmpty() && !this.profileManager.hasProfile(newProfileName)) {
				this.profileManager.addProfile(newProfileName);
				profileListBox.addItem(newProfileName, this.menu.rootFrame, this.menu.uiViewport);
				this.addProfileButton.setEnabled(false);
			}
		});
		this.deleteProfileButton.setOnClick(() -> {
			final int selectedIndex = profileListBox.getSelectedIndex();
			final boolean validSelect = (selectedIndex >= 0)
					&& (selectedIndex < this.profileManager.getProfiles().size());
			if (validSelect) {
				if (this.profileManager.getProfiles().size() > 1) {
					final PlayerProfile profileToRemove = this.profileManager.getProfiles().get(selectedIndex);
					final String removeProfileName = profileToRemove.getName();
					final boolean deletingCurrentProfile = removeProfileName
							.equals(this.profileManager.getCurrentProfile());
					this.profileManager.removeProfile(profileToRemove);
					profileListBox.removeItem(selectedIndex, this.menu.rootFrame, this.menu.uiViewport);
					if (deletingCurrentProfile) {
						setCurrentProfile(this.profileManager.getProfiles().get(0).getName());
					}
				}
			}
		});
		this.selectProfileButton.setOnClick(() -> {
			final int selectedIndex = profileListBox.getSelectedIndex();
			final boolean validSelect = (selectedIndex >= 0)
					&& (selectedIndex < this.profileManager.getProfiles().size());
			if (validSelect) {
				final PlayerProfile profileToSelect = this.profileManager.getProfiles().get(selectedIndex);
				final String selectedProfileName = profileToSelect.getName();
				setCurrentProfile(selectedProfileName);

				this.menu.glueSpriteLayerTopLeft.setSequence("RealmSelection Death");
				this.profilePanel.setVisible(false);
				this.menu.menuState = MenuState.SINGLE_PLAYER;
				this.menu.setSinglePlayerButtonsEnabled(false);
			}
		});
		profileListBox.setSelectionListener((newSelectedIndex, newSelectedItem) -> {
			final boolean validSelect = newSelectedItem != null;
			this.selectProfileButton.setEnabled(validSelect);
			this.deleteProfileButton.setEnabled(validSelect);
		});
	}

	/** The single player menu's line with the name of the profile that plays. */
	void createNameText() {
		this.profileNameText = (StringFrame) this.menu.rootFrame.getFrameByName("ProfileNameText", 0);
		this.menu.rootFrame.setText(this.profileNameText, this.profileManager.getCurrentProfile());
	}

	/** The single player menu's Profile button. */
	void open() {
		this.menu.glueSpriteLayerTopLeft.setSequence("RealmSelection Birth");
		this.menu.setSinglePlayerButtonsEnabled(false);
		this.menu.menuState = MenuState.SINGLE_PLAYER_PROFILE;
	}

	void hidePanel() {
		this.profilePanel.setVisible(false);
	}

	void useProfile(final String profileName) {
		if ((profileName == null) || profileName.isEmpty()) {
			return;
		}
		if (!this.profileManager.hasProfile(profileName)) {
			this.profileManager.addProfile(profileName);
		}
		setCurrentProfile(profileName);
	}

	private void setCurrentProfile(final String selectedProfileName) {
		this.profileManager.setCurrentProfile(selectedProfileName);
		this.menu.rootFrame.setText(this.profileNameText, selectedProfileName);
	}

	/** @return whether the state is the profile panel's, which it then shows */
	boolean updateMenuState(final MenuState menuState) {
		if (menuState != MenuState.SINGLE_PLAYER_PROFILE) {
			return false;
		}
		this.profilePanel.setVisible(true);
		this.menu.setSinglePlayerButtonsEnabled(true);
		this.menu.glueSpriteLayerTopLeft.setSequence("RealmSelection Stand");
		// TODO the below should probably be some generic focusing thing when we enter a
		// new view?
		if ((this.newProfileEditBox != null) && this.newProfileEditBox.isFocusable()) {
			this.menu.setFocusFrame(this.newProfileEditBox);
		}
		return true;
	}
}
