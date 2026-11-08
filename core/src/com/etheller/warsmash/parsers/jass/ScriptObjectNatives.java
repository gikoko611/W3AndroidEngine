package com.etheller.warsmash.parsers.jass;

import java.util.List;
import java.util.function.BiConsumer;

import com.etheller.interpreter.ast.function.JassFunction;
import com.etheller.interpreter.ast.util.JassProgram;
import com.etheller.interpreter.ast.value.BooleanJassValue;
import com.etheller.interpreter.ast.value.HandleJassType;
import com.etheller.interpreter.ast.value.HandleJassValue;
import com.etheller.interpreter.ast.value.IntegerJassValue;
import com.etheller.interpreter.ast.value.JassValue;
import com.etheller.interpreter.ast.value.StringJassValue;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.RealJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.StringJassValueVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.HandleIdAllocator;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CImage;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CMultiboard;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CTrackable;

/**
 * Natives of script objects that take handle ids of their own: multiboards and their items, trackables and images.
 * Return values, limits and ids are measured against 1.29.
 */
final class ScriptObjectNatives {
	private final JassProgram program;
	private final War3MapViewer viewer;
	private final CSimulation simulation;

	private ScriptObjectNatives(final JassProgram program, final War3MapViewer viewer) {
		this.program = program;
		this.viewer = viewer;
		this.simulation = viewer.simulation;
	}

	static void register(final JassProgram program, final War3MapViewer viewer) {
		final ScriptObjectNatives natives = new ScriptObjectNatives(program, viewer);
		natives.registerMultiboards();
		natives.registerTrackables();
		natives.registerImages();
	}

	private void add(final String name, final JassFunction function) {
		this.program.getJassNativeManager().createNative(name, function);
	}

	private HandleJassType type(final String name) {
		return this.program.getGlobalScope().getHandleType(name);
	}

	private HandleIdAllocator ids() {
		return this.simulation.getHandleIdAllocator();
	}

	private static <T> T object(final List<JassValue> arguments, final int index) {
		final JassValue value = arguments.get(index);
		return value == null ? null : value.visit(ObjectJassValueVisitor.<T>getInstance());
	}

	private static String string(final List<JassValue> arguments, final int index) {
		final JassValue value = arguments.get(index);
		final String text = value == null ? null : value.visit(StringJassValueVisitor.getInstance());
		return text == null ? "" : text;
	}

	private String mapString(final List<JassValue> arguments, final int index) {
		return this.viewer.getGameUI().getTrigStr(string(arguments, index));
	}

	private static int integer(final List<JassValue> arguments, final int index) {
		return arguments.get(index).visit(IntegerJassValueVisitor.getInstance());
	}

	private static float real(final List<JassValue> arguments, final int index) {
		return arguments.get(index).visit(RealJassValueVisitor.getInstance()).floatValue();
	}

	private static boolean bool(final List<JassValue> arguments, final int index) {
		return arguments.get(index).visit(BooleanJassValueVisitor.getInstance());
	}

	private static int color(final List<JassValue> arguments, final int first) {
		return ((integer(arguments, first) & 0xFF) << 24) | ((integer(arguments, first + 1) & 0xFF) << 16)
				| ((integer(arguments, first + 2) & 0xFF) << 8) | (integer(arguments, first + 3) & 0xFF);
	}

	private void registerMultiboards() {
		final HandleJassType multiboardType = type("multiboard");
		final HandleJassType itemType = type("multiboarditem");
		add("CreateMultiboard", (arguments, globalScope, triggerScope) -> new HandleJassValue(multiboardType,
				ids().assign(new CMultiboard())));
		add("DestroyMultiboard", (arguments, globalScope, triggerScope) -> {
			final CMultiboard board = object(arguments, 0);
			if (board != null) {
				board.setDisplayed(false);
				if (this.simulation.isShownMultiboard(board)) {
					this.simulation.setShownMultiboard(null);
				}
				ids().release(board);
			}
			return null;
		});
		add("MultiboardDisplay", boardSetter((board, arguments) -> {
			final boolean show = bool(arguments, 1);
			// One board is drawn at a time, the one shown last; in 1.29 the one shown before still says it is shown.
			if (show) {
				this.simulation.setShownMultiboard(board);
			}
			else if (this.simulation.isShownMultiboard(board)) {
				this.simulation.setShownMultiboard(null);
			}
			board.setDisplayed(show);
		}));
		add("IsMultiboardDisplayed", boardGetter(board -> BooleanJassValue.of(board.isDisplayed()),
				BooleanJassValue.FALSE));
		add("MultiboardMinimize", boardSetter((board, arguments) -> board.setMinimized(bool(arguments, 1))));
		add("IsMultiboardMinimized", boardGetter(board -> BooleanJassValue.of(board.isMinimized()),
				BooleanJassValue.FALSE));
		add("MultiboardClear", boardSetter((board, arguments) -> board.clear()));
		add("MultiboardSetTitleText", boardSetter((board, arguments) -> board.setTitle(mapString(arguments, 1))));
		add("MultiboardGetTitleText", boardGetter(board -> new StringJassValue(board.getTitle()),
				new StringJassValue("")));
		add("MultiboardSetTitleTextColor",
				boardSetter((board, arguments) -> board.setTitleColor(color(arguments, 1))));
		add("MultiboardGetRowCount", boardGetter(board -> IntegerJassValue.of(board.getRowCount()),
				IntegerJassValue.of(0)));
		add("MultiboardGetColumnCount", boardGetter(board -> IntegerJassValue.of(board.getColumnCount()),
				IntegerJassValue.of(0)));
		add("MultiboardSetColumnCount", boardSetter((board, arguments) -> board.setColumnCount(integer(arguments, 1))));
		add("MultiboardSetRowCount", boardSetter((board, arguments) -> board.setRowCount(integer(arguments, 1))));
		add("MultiboardSetItemsStyle",
				boardSetter((board, arguments) -> board.setItemsStyle(bool(arguments, 1), bool(arguments, 2))));
		add("MultiboardSetItemsValue", boardSetter((board, arguments) -> board.setItemsValue(mapString(arguments, 1))));
		add("MultiboardSetItemsValueColor",
				boardSetter((board, arguments) -> board.setItemsValueColor(color(arguments, 1))));
		add("MultiboardSetItemsWidth", boardSetter((board, arguments) -> board.setItemsWidth(real(arguments, 1))));
		add("MultiboardSetItemsIcon", boardSetter((board, arguments) -> board.setItemsIcon(string(arguments, 1))));
		add("MultiboardGetItem", (arguments, globalScope, triggerScope) -> {
			final CMultiboard board = object(arguments, 0);
			final int row = integer(arguments, 1);
			final int column = integer(arguments, 2);
			if (board == null) {
				return new HandleJassValue(itemType, null);
			}
			// A new handle on every call, also for a cell outside the grid (measured).
			return new HandleJassValue(itemType, ids().assign(new CMultiboard.ItemHandle(board, row, column)));
		});
		add("MultiboardReleaseItem", (arguments, globalScope, triggerScope) -> {
			final CMultiboard.ItemHandle item = object(arguments, 0);
			if (item != null) {
				ids().release(item);
			}
			return null;
		});
		add("MultiboardSetItemStyle", itemSetter((item, arguments) -> {
			item.showValue = bool(arguments, 1);
			item.showIcon = bool(arguments, 2);
		}));
		add("MultiboardSetItemValue", itemSetter((item, arguments) -> item.value = mapString(arguments, 1)));
		add("MultiboardSetItemValueColor", itemSetter((item, arguments) -> item.valueColor = color(arguments, 1)));
		add("MultiboardSetItemWidth", itemSetter((item, arguments) -> item.width = real(arguments, 1)));
		add("MultiboardSetItemIcon", itemSetter((item, arguments) -> item.icon = string(arguments, 1)));
		add("MultiboardSuppressDisplay", (arguments, globalScope, triggerScope) -> {
			this.simulation.setMultiboardsSuppressed(bool(arguments, 0));
			return null;
		});
	}

	private interface BoardGetter {
		JassValue get(CMultiboard board);
	}

	private static JassFunction boardGetter(final BoardGetter getter, final JassValue ifNull) {
		return (arguments, globalScope, triggerScope) -> {
			final CMultiboard board = object(arguments, 0);
			return board == null ? ifNull : getter.get(board);
		};
	}

	private static JassFunction boardSetter(final BiConsumer<CMultiboard, List<JassValue>> setter) {
		return (arguments, globalScope, triggerScope) -> {
			final CMultiboard board = object(arguments, 0);
			if (board != null) {
				setter.accept(board, arguments);
			}
			return null;
		};
	}

	private static JassFunction itemSetter(final BiConsumer<CMultiboard.Item, List<JassValue>> setter) {
		return (arguments, globalScope, triggerScope) -> {
			final CMultiboard.ItemHandle handle = object(arguments, 0);
			final CMultiboard.Item item = handle == null ? null : handle.getItem();
			if (item != null) {
				setter.accept(item, arguments);
				handle.getBoard().itemChanged();
			}
			return null;
		};
	}

	private void registerTrackables() {
		final HandleJassType trackableType = type("trackable");
		add("CreateTrackable", (arguments, globalScope, triggerScope) -> {
			final CTrackable trackable = ids().assign(
					new CTrackable(string(arguments, 0), real(arguments, 1), real(arguments, 2), real(arguments, 3)));
			this.simulation.addTrackable(trackable);
			this.viewer.showTrackable(trackable);
			return new HandleJassValue(trackableType, trackable);
		});
	}

	private void registerImages() {
		final HandleJassType imageType = type("image");
		add("CreateImage", (arguments, globalScope, triggerScope) -> {
			// The size, position and origin each have a height, which a flat image does not use.
			final String file = string(arguments, 0);
			final int kind = integer(arguments, 10);
			final CImage image = new CImage(file, real(arguments, 1), real(arguments, 2), real(arguments, 4),
					real(arguments, 5), real(arguments, 6), real(arguments, 7), real(arguments, 8), kind);
			if (CImage.isKnownType(kind) && this.viewer.dataSource.has(file)) {
				this.simulation.addImage(image);
				this.viewer.updateImage(image);
			}
			return new HandleJassValue(imageType, image);
		});
		add("DestroyImage", imageSetter((image, arguments) -> this.simulation.destroyImage(image)));
		add("ShowImage", imageSetter((image, arguments) -> image.setShown(bool(arguments, 1))));
		add("SetImageConstantHeight", imageSetter(
				(image, arguments) -> image.setConstantHeight(bool(arguments, 1), real(arguments, 2))));
		add("SetImagePosition", imageSetter(
				(image, arguments) -> image.setPosition(real(arguments, 1), real(arguments, 2), real(arguments, 3))));
		add("SetImageColor", imageSetter((image, arguments) -> image.setColor(color(arguments, 1))));
		add("SetImageRender", imageSetter((image, arguments) -> image.setRender(bool(arguments, 1))));
		add("SetImageRenderAlways", imageSetter((image, arguments) -> image.setRenderAlways(bool(arguments, 1))));
		add("SetImageAboveWater", imageSetter(
				(image, arguments) -> image.setAboveWater(bool(arguments, 1), bool(arguments, 2))));
		add("SetImageType", imageSetter((image, arguments) -> image.setImageType(integer(arguments, 1))));
	}

	private JassFunction imageSetter(final BiConsumer<CImage, List<JassValue>> setter) {
		return (arguments, globalScope, triggerScope) -> {
			final CImage image = object(arguments, 0);
			if ((image != null) && image.isValid() && !image.isDestroyed()) {
				setter.accept(image, arguments);
				this.viewer.updateImage(image);
			}
			return null;
		};
	}
}
