package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.parsers.fdf.datamodel.FramePoint;
import com.etheller.warsmash.parsers.fdf.frames.EditBoxFrame;
import com.etheller.warsmash.parsers.fdf.frames.GlueTextButtonFrame;
import com.etheller.warsmash.parsers.fdf.frames.ListBoxFrame;
import com.etheller.warsmash.parsers.fdf.frames.SetPoint;
import com.etheller.warsmash.parsers.fdf.frames.SimpleFrame;
import com.etheller.warsmash.parsers.fdf.frames.StringFrame;
import com.etheller.warsmash.parsers.fdf.frames.UIFrame;
import com.etheller.warsmash.util.AbstractListItemProperty;
import com.etheller.warsmash.util.ListItemStringProperty;
import com.etheller.warsmash.viewer5.handlers.w3x.save.SavedGame;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CSaveStorage;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.command.FocusableFrame;

/**
 * The Save Game and Load Game dialogs: a list of the profile's saved games, a name to save under, and the overwrite
 * and delete questions. The game menu and the single player menu both open it.
 */
public final class SavedGameDialog {
	public interface Host {
		void focusOn(FocusableFrame frame);

		void saveGame(String name);

		void loadGame(String name);

		void closed();
	}

	private final GameUI rootFrame;
	private final Viewport uiViewport;
	private final SimpleFrame holder;
	private final UIFrame backdrop;
	private final Host host;
	private final UIFrame panel;
	private final UIFrame saveLoadContainer;
	private final UIFrame saveAndLoad;
	private final UIFrame loadOnly;
	private final UIFrame saveOnly;
	private final UIFrame overwriteContainer;
	private final UIFrame overwriteOnly;
	private final UIFrame deleteContainer;
	private final UIFrame deleteOnly;
	private final ListBoxFrame fileList;
	private final EditBoxFrame nameBox;
	private final GlueTextButtonFrame loadButton;
	private final GlueTextButtonFrame saveButton;
	private final GlueTextButtonFrame deleteButton;
	private final StringFrame overwriteMessage;
	private final StringFrame deleteMessage;
	private final List<ShortcutButton> loadButtons = new ArrayList<>();
	private final List<ShortcutButton> saveButtons = new ArrayList<>();
	private final List<ShortcutButton> overwriteButtons = new ArrayList<>();
	private final List<ShortcutButton> deleteButtons = new ArrayList<>();
	private List<ShortcutButton> activeButtons;
	private Runnable cancelAction;
	private Runnable enterAction;
	private boolean showing;
	private boolean selectionLoadable;
	private StringFrame reasonText;
	private final Map<String, SavedGame.Loadability> loadability = new HashMap<>();

	public SavedGameDialog(final GameUI rootFrame, final Viewport uiViewport, final SimpleFrame holder,
			final UIFrame backdrop, final Host host) {
		this.rootFrame = rootFrame;
		this.uiViewport = uiViewport;
		this.holder = holder;
		this.backdrop = backdrop;
		this.host = host;
		this.panel = rootFrame.createFrame("EscMenuSaveGamePanel", holder, 0, 0);
		holder.add(this.panel);
		this.saveLoadContainer = rootFrame.getFrameByName("EscMenuSaveLoadContainer", 0);
		this.saveAndLoad = rootFrame.getFrameByName("SaveAndLoad", 0);
		this.loadOnly = rootFrame.getFrameByName("LoadOnly", 0);
		this.saveOnly = rootFrame.getFrameByName("SaveOnly", 0);
		this.overwriteContainer = rootFrame.getFrameByName("EscMenuOverwriteContainer", 0);
		this.overwriteOnly = rootFrame.getFrameByName("OverwriteOnly", 0);
		this.deleteContainer = rootFrame.getFrameByName("EscMenuDeleteContainer", 0);
		this.deleteOnly = rootFrame.getFrameByName("DeleteOnly", 0);
		this.overwriteMessage = (StringFrame) rootFrame.getFrameByName("OverwriteMessageText", 0);
		this.deleteMessage = (StringFrame) rootFrame.getFrameByName("DeleteMessageText", 0);
		this.nameBox = (EditBoxFrame) rootFrame.getFrameByName("SaveGameFileEditBox", 0);
		this.nameBox.setFilterAllowAny();
		this.nameBox.setOnChange(this::updateButtons);
		this.nameBox.setOnEnter(this::saveClicked);
		final StringFrame nameText = (StringFrame) rootFrame.getFrameByName("SaveGameFileEditBoxText", 0);
		final SimpleFrame fileListFrame = (SimpleFrame) rootFrame.getFrameByName("FileListFrame", 0);
		this.fileList = (ListBoxFrame) rootFrame.createFrameByType("LISTBOX", "ListBoxWar3", fileListFrame,
				"WITHCHILDREN", 0);
		this.fileList.setSetAllPoints(true);
		this.fileList.setFrameFont(nameText.getFrameFont());
		this.fileList.setSelectionListener((index, item) -> {
			if ((item != null) && this.saveOnly.isVisible()) {
				this.nameBox.setText(item, rootFrame, uiViewport);
			}
			final SavedGame.Loadability state = item == null ? null : this.loadability.get(item);
			this.selectionLoadable = this.loadOnly.isVisible() && (state == SavedGame.Loadability.LOADABLE);
			rootFrame.setText(this.reasonText,
					this.loadOnly.isVisible() && (state != null) ? reasonOf(state) : "");
			updateButtons();
		});
		fileListFrame.add(this.fileList);
		// The load panel has the save panel's name box place free under the list; why a save cannot be loaded goes
		// there.
		this.reasonText = (StringFrame) rootFrame.createFrame("EscMenuSaveDialogTextTemplate", this.loadOnly, 0, 0);
		this.reasonText.addSetPoint(new SetPoint(FramePoint.TOPLEFT, fileListFrame, FramePoint.BOTTOMLEFT, 0,
				GameUI.convertY(uiViewport, -0.006f)));
		this.reasonText.setWidth(fileListFrame.getAssignedWidth());
		this.reasonText.setHeight(GameUI.convertY(uiViewport, 0.03f));
		if (this.loadOnly instanceof SimpleFrame) {
			((SimpleFrame) this.loadOnly).add(this.reasonText);
		}
		rootFrame.setText(this.reasonText, "");

		this.loadButton = button("LoadGameLoadButton", "KEY_LOAD_SHORTCUT", this.loadButtons, this::loadClicked);
		button("LoadGameCancelButton", "KEY_CANCEL_SHORTCUT", this.loadButtons, this::close);
		this.saveButton = button("SaveGameSaveButton", "KEY_SAVE_SHORTCUT", this.saveButtons, this::saveClicked);
		this.deleteButton = button("SaveGameDeleteButton", "KEY_DELETE_SHORTCUT", this.saveButtons,
				this::deleteClicked);
		button("SaveGameCancelButton", "KEY_CANCEL_SHORTCUT", this.saveButtons, this::close);
		button("OverwriteOverwriteButton", "KEY_OVERWRITE_SHORTCUT", this.overwriteButtons, () -> {
			final String name = enteredName();
			hide();
			host.saveGame(name);
		});
		button("OverwriteCancelButton", "KEY_CANCEL_SHORTCUT", this.overwriteButtons, this::showSave);
		button("DeleteDeleteButton", "KEY_DELETE_SHORTCUT", this.deleteButtons, () -> {
			CSaveStorage.savedGame(enteredName()).delete();
			showSave();
		});
		button("DeleteCancelButton", "KEY_CANCEL_SHORTCUT", this.deleteButtons, this::showSave);
		this.panel.setVisible(false);
	}

	private GlueTextButtonFrame button(final String frameName, final String shortcutKey,
			final List<ShortcutButton> group, final Runnable action) {
		final GlueTextButtonFrame button = (GlueTextButtonFrame) this.rootFrame.getFrameByName(frameName, 0);
		button.setOnClick(action);
		final int labelKey = MenuHotkey.of(button);
		group.add(new ShortcutButton(button, labelKey != Input.Keys.UNKNOWN ? labelKey : shortcutOf(shortcutKey),
				action));
		return button;
	}

	private int shortcutOf(final String key) {
		final String letter = this.rootFrame.getTemplates().getDecoratedString(key);
		// A missing string comes back as its own key, which is no shortcut.
		if ((letter == null) || letter.isEmpty() || letter.equals(key)) {
			return Input.Keys.UNKNOWN;
		}
		final int keycode = Input.Keys.valueOf(letter.substring(0, 1).toUpperCase());
		return keycode < 0 ? Input.Keys.UNKNOWN : keycode;
	}

	public void showSave() {
		showFiles(false);
		this.saveOnly.setVisible(true);
		this.loadOnly.setVisible(false);
		this.activeButtons = this.saveButtons;
		this.cancelAction = this::close;
		this.enterAction = this::saveClicked;
		final String current = this.nameBox.getText();
		if ((current == null) || current.isEmpty()) {
			this.nameBox.setText(defaultName(), this.rootFrame, this.uiViewport);
		}
		updateButtons();
		this.host.focusOn(this.nameBox);
	}

	public void showLoad() {
		showFiles(true);
		this.saveOnly.setVisible(false);
		this.loadOnly.setVisible(true);
		this.activeButtons = this.loadButtons;
		this.cancelAction = this::close;
		this.enterAction = this::loadClicked;
		updateButtons();
		this.host.focusOn(null);
	}

	private void showFiles(final boolean forLoading) {
		this.loadability.clear();
		final List<AbstractListItemProperty> items = new ArrayList<>();
		for (final String name : CSaveStorage.savedGameNames()) {
			Color color = null;
			if (forLoading) {
				final SavedGame.Loadability state = SavedGame.loadability(CSaveStorage.savedGame(name));
				this.loadability.put(name, state);
				if (state != SavedGame.Loadability.LOADABLE) {
					color = Color.GRAY;
				}
			}
			items.add(new ListItemStringProperty(name, color));
		}
		this.fileList.setSelectedIndex(-1);
		this.fileList.setItems(items, this.rootFrame, this.uiViewport);
		this.rootFrame.setText(this.reasonText, "");
		showContainer(this.saveLoadContainer);
		this.saveAndLoad.setVisible(true);
	}

	private void showContainer(final UIFrame container) {
		this.showing = true;
		this.panel.setVisible(true);
		this.saveLoadContainer.setVisible(container == this.saveLoadContainer);
		this.overwriteContainer.setVisible(container == this.overwriteContainer);
		this.deleteContainer.setVisible(container == this.deleteContainer);
		this.saveAndLoad.setVisible(container == this.saveLoadContainer);
		this.overwriteOnly.setVisible(container == this.overwriteContainer);
		this.deleteOnly.setVisible(container == this.deleteContainer);
		fit(this.holder, this.backdrop, container, this.rootFrame, this.uiViewport);
	}

	static void fit(final SimpleFrame holder, final UIFrame backdrop, final UIFrame sizedBy, final GameUI rootFrame,
			final Viewport uiViewport) {
		holder.setWidth(sizedBy.getAssignedWidth());
		holder.setHeight(sizedBy.getAssignedHeight());
		backdrop.setWidth(sizedBy.getAssignedWidth());
		backdrop.setHeight(sizedBy.getAssignedHeight());
		holder.positionBounds(rootFrame, uiViewport);
	}

	private String reasonOf(final SavedGame.Loadability state) {
		switch (state) {
		case DAMAGED:
			return this.rootFrame.getTemplates().getDecoratedString("SAVE_ERROR_BAD_SAVE");
		case OTHER_VERSION:
			return "|CFFFF0000This game was saved by another version of the game and cannot be loaded.";
		default:
			return "";
		}
	}

	private String defaultName() {
		final List<String> names = CSaveStorage.savedGameNames();
		final String base = this.rootFrame.getTemplates().getDecoratedString("SAVE_GAME");
		for (int i = 1;; i++) {
			final String name = base + " " + i;
			if (!names.contains(name)) {
				return name;
			}
		}
	}

	private String enteredName() {
		return CSaveStorage.safeName(this.nameBox.getText().trim());
	}

	private void updateButtons() {
		final boolean hasSelection = this.fileList.getSelectedItem() != null;
		this.loadButton.setEnabled(hasSelection && this.selectionLoadable);
		final boolean hasName = (this.nameBox.getText() != null) && !this.nameBox.getText().trim().isEmpty();
		this.saveButton.setEnabled(hasName);
		this.deleteButton.setEnabled(hasName && CSaveStorage.savedGame(enteredName()).exists());
	}

	private void loadClicked() {
		final String name = this.fileList.getSelectedItem();
		if ((name != null) && this.selectionLoadable) {
			hide();
			this.host.loadGame(name);
		}
	}

	private void saveClicked() {
		if ((this.nameBox.getText() == null) || this.nameBox.getText().trim().isEmpty()) {
			return;
		}
		final String name = enteredName();
		if (CSaveStorage.savedGame(name).exists()) {
			this.rootFrame.setText(this.overwriteMessage,
					this.rootFrame.getTemplates().getDecoratedString("OVERWRITE_MESSAGE").replace("%s", name));
			askQuestion(this.overwriteContainer, this.overwriteButtons);
		}
		else {
			hide();
			this.host.saveGame(name);
		}
	}

	private void deleteClicked() {
		final String name = enteredName();
		if (CSaveStorage.savedGame(name).exists()) {
			this.rootFrame.setText(this.deleteMessage,
					this.rootFrame.getTemplates().getDecoratedString("DELETE_MESSAGE").replace("%s", name));
			askQuestion(this.deleteContainer, this.deleteButtons);
		}
	}

	private void askQuestion(final UIFrame container, final List<ShortcutButton> buttons) {
		this.host.focusOn(null);
		showContainer(container);
		this.saveOnly.setVisible(false);
		this.loadOnly.setVisible(false);
		this.activeButtons = buttons;
		this.cancelAction = this::showSave;
		this.enterAction = buttons.get(0).action;
	}

	private void hide() {
		this.showing = false;
		this.host.focusOn(null);
		this.panel.setVisible(false);
	}

	private void close() {
		hide();
		this.host.closed();
	}

	public boolean isShowing() {
		return this.showing;
	}

	/** Keys for the open dialog; the name box has first go at them through the host's focus. */
	public boolean keyDown(final int keycode) {
		if (keycode == Input.Keys.ESCAPE) {
			this.cancelAction.run();
			return true;
		}
		if (keycode == Input.Keys.ENTER) {
			this.enterAction.run();
			return true;
		}
		for (final ShortcutButton button : this.activeButtons) {
			if ((button.keycode == keycode) && button.frame.isEnabled()) {
				button.action.run();
				return true;
			}
		}
		return true;
	}

	private static final class ShortcutButton {
		private final GlueTextButtonFrame frame;
		private final int keycode;
		private final Runnable action;

		private ShortcutButton(final GlueTextButtonFrame frame, final int keycode, final Runnable action) {
			this.frame = frame;
			this.keycode = keycode;
			this.action = action;
		}
	}
}
