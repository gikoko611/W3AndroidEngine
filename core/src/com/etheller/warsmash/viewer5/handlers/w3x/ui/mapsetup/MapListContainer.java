package com.etheller.warsmash.viewer5.handlers.w3x.ui.mapsetup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.parsers.fdf.frames.ListBoxFrame;
import com.etheller.warsmash.parsers.fdf.frames.SimpleFrame;
import com.etheller.warsmash.parsers.fdf.frames.ListBoxFrame.ListBoxSelelectionListener;
import com.etheller.warsmash.util.ListItemEnum;

public class MapListContainer {
	private final SimpleFrame mapListContainer;
	private final ListBoxFrame mapListBox;

	public MapListContainer(final GameUI rootFrame, final Viewport uiViewport, final String containerKey,
			final DataSource dataSource, final BitmapFont font) {
		this.mapListContainer = (SimpleFrame) rootFrame.getFrameByName(containerKey, 0);
		this.mapListBox = (ListBoxFrame) rootFrame.createFrameByType("LISTBOX", "MapListBox", this.mapListContainer,
				"WITHCHILDREN", 0);
		this.mapListBox.setSetAllPoints(true);
		this.mapListBox.setFrameFont(font);
		final Collection<String> listfile = dataSource.getListfile();
		final List<String> displayItemPaths = new ArrayList<>();
		for (final String file : listfile) {
			if (isListedMap(file)) {
				displayItemPaths.add(file);
			}
		}
		for (final String displayItemPath : displayItemPaths) {
			try {
				this.mapListBox.addItem(displayItemPath, ListItemEnum.ITEM_MAP, rootFrame, uiViewport);
			}
			catch (final RuntimeException e) {
				System.err.println("Skipping map with unreadable info: " + displayItemPath + " (" + e + ")");
			}
		}
		this.mapListBox.sortItems();
		this.mapListContainer.add(this.mapListBox);
	}

	/** The maps the custom game list offers: those under Maps and those at the top of a data source. */
	public static boolean isListedMap(final String file) {
		final String lower = file.toLowerCase().replace('/', '\\');
		return (lower.endsWith(".w3x") || lower.endsWith(".w3m"))
				&& ((!lower.contains("\\")) || lower.startsWith("maps\\"));
	}

	/**
	 * Selects a map as a click on it would.
	 *
	 * @return false when the list does not have it
	 */
	public boolean select(final String path) {
		final int index = this.mapListBox.indexOf(path);
		if (index < 0) {
			return false;
		}
		this.mapListBox.selectIndex(index);
		return true;
	}

	public void addSelectionListener(final ListBoxSelelectionListener listener) {
		this.mapListBox.setSelectionListener(listener);
	}

	public String getSelectedItem() {
		return this.mapListBox.getSelectedItem();
	}

	/**
	 * The original opens the list on the map last played from it (registry Map\skirmish_V0) and otherwise on the first
	 * entry.
	 */
	public void selectInitial(final String lastPlayedMap) {
		if (this.mapListBox.getSelectedItem() != null) {
			return;
		}
		final int lastPlayedIndex = lastPlayedMap == null ? -1 : this.mapListBox.indexOf(lastPlayedMap);
		this.mapListBox.selectIndex(Math.max(0, lastPlayedIndex));
	}
}
