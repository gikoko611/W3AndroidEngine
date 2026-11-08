package com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign;

import java.util.ArrayList;
import java.util.List;

import com.etheller.interpreter.ast.util.AllocatedHandle;

/**
 * Multiboard contents: a title over a grid of items, each with a value text, an icon and a width. The row and column
 * counts, their limits and what a new board and a new item hold are measured against 1.29.
 */
public final class CMultiboard extends AllocatedHandle {
	public static final float DEFAULT_ITEM_WIDTH = 0.03f;

	private final List<List<Item>> rows = new ArrayList<>();
	private int columnCount;
	private String title = "";
	private int titleColor = -1;
	private boolean displayed;
	private boolean minimized;
	private int version;

	/** Goes up with every change, so that a drawn board knows when to lay itself out again. */
	public int getVersion() {
		return this.version;
	}

	private void changed() {
		this.version++;
	}

	public int getRowCount() {
		return this.rows.size();
	}

	public int getColumnCount() {
		return this.columnCount;
	}

	public void setRowCount(final int count) {
		// 1.29 takes 300 rows and columns as they are; a negative count stops it, here it empties the board.
		final int rowCount = Math.max(0, count);
		while (this.rows.size() > rowCount) {
			this.rows.remove(this.rows.size() - 1);
		}
		while (this.rows.size() < rowCount) {
			final List<Item> row = new ArrayList<>();
			for (int i = 0; i < this.columnCount; i++) {
				row.add(new Item());
			}
			this.rows.add(row);
		}
		changed();
	}

	public void setColumnCount(final int count) {
		this.columnCount = Math.max(0, count);
		for (final List<Item> row : this.rows) {
			while (row.size() > this.columnCount) {
				row.remove(row.size() - 1);
			}
			while (row.size() < this.columnCount) {
				row.add(new Item());
			}
		}
		changed();
	}

	/** The item at the cell, or null outside the grid. */
	public Item getItem(final int row, final int column) {
		if ((row < 0) || (row >= this.rows.size()) || (column < 0) || (column >= this.columnCount)) {
			return null;
		}
		return this.rows.get(row).get(column);
	}

	public List<List<Item>> getRows() {
		return this.rows;
	}

	/** MultiboardClear: the grid is emptied, the title stays. */
	public void clear() {
		this.rows.clear();
		this.columnCount = 0;
		changed();
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(final String title) {
		this.title = title == null ? "" : title;
		changed();
	}

	public int getTitleColor() {
		return this.titleColor;
	}

	public void setTitleColor(final int titleColor) {
		this.titleColor = titleColor;
		changed();
	}

	public boolean isDisplayed() {
		return this.displayed;
	}

	public void setDisplayed(final boolean displayed) {
		this.displayed = displayed;
		changed();
	}

	public boolean isMinimized() {
		return this.minimized;
	}

	public void setMinimized(final boolean minimized) {
		this.minimized = minimized;
		changed();
	}

	public void setItemsStyle(final boolean showValue, final boolean showIcon) {
		for (final List<Item> row : this.rows) {
			for (final Item item : row) {
				item.showValue = showValue;
				item.showIcon = showIcon;
			}
		}
		changed();
	}

	public void setItemsValue(final String value) {
		for (final List<Item> row : this.rows) {
			for (final Item item : row) {
				item.value = value;
			}
		}
		changed();
	}

	public void setItemsValueColor(final int color) {
		for (final List<Item> row : this.rows) {
			for (final Item item : row) {
				item.valueColor = color;
			}
		}
		changed();
	}

	public void setItemsWidth(final float width) {
		for (final List<Item> row : this.rows) {
			for (final Item item : row) {
				item.width = width;
			}
		}
		changed();
	}

	public void setItemsIcon(final String icon) {
		for (final List<Item> row : this.rows) {
			for (final Item item : row) {
				item.icon = icon;
			}
		}
		changed();
	}

	/** Lets the board know one of its items changed through a multiboarditem handle. */
	public void itemChanged() {
		changed();
	}

	public static final class Item {
		public String value = "";
		public int valueColor = -1;
		public String icon = "";
		public boolean showValue = true;
		public boolean showIcon = true;
		public float width = DEFAULT_ITEM_WIDTH;
	}

	/**
	 * What MultiboardGetItem returns: a handle of its own on one cell. Each call makes a new one, which the script
	 * gives back with MultiboardReleaseItem.
	 */
	public static final class ItemHandle extends AllocatedHandle {
		private final CMultiboard board;
		private final int row;
		private final int column;

		public ItemHandle(final CMultiboard board, final int row, final int column) {
			this.board = board;
			this.row = row;
			this.column = column;
		}

		/** The cell it points at, or null once the grid no longer has it. */
		public Item getItem() {
			return this.board.getItem(this.row, this.column);
		}

		public CMultiboard getBoard() {
			return this.board;
		}
	}
}
