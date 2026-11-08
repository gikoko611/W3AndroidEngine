package com.etheller.warsmash.viewer5.handlers.w3x;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.units.Element;
import com.etheller.warsmash.util.StringBundle;

/**
 * Since 1.32 the acknowledgement tables name dialogue labels (e.g. FootmanWhat1) instead of files; the
 * Dialogue*Base tables map each label to its file.
 */
final class DialogueLabels {
	private static final String[] GROUPS = { "Creeps", "Demon", "Human", "Naga", "NightElf", "Orc", "Undead" };

	private DialogueLabels() {
	}

	/** Puts the files of the dialogue labels in place of the labels of a sound table. */
	static void resolve(final DataSource dataSource, final DataTable soundTable, final StringBundle strings)
			throws IOException {
		final DataTable dialogue = new DataTable(strings);
		for (final String group : GROUPS) {
			try (InputStream stream = dataSource.getResourceAsStream("UI\\SoundInfo\\Dialogue" + group + "Base.slk")) {
				if (stream != null) {
					dialogue.readSLK(stream);
				}
			}
		}
		if (dialogue.keySet().isEmpty()) {
			return;
		}
		// A few rows have a misspelt label (HPitLordPitLordYesYes1); the acknowledgement table names them by their
		// facial animation label, which is the label as it should be.
		final Map<String, Element> byFacialLabel = new HashMap<>();
		for (final String key : dialogue.keySet()) {
			final Element dialogueRow = dialogue.get(key);
			final String facialLabel = dialogueRow.getField("FacialAnimationLabel");
			if (!facialLabel.isEmpty() && (dialogue.get(facialLabel) == null)) {
				byFacialLabel.put(facialLabel.toLowerCase(Locale.US), dialogueRow);
			}
		}
		for (final String key : soundTable.keySet()) {
			final Element row = soundTable.get(key);
			final String directoryBase = row.getField("DirectoryBase");
			if (!directoryBase.isEmpty() && !"_".equals(directoryBase)) {
				continue;
			}
			final StringBuilder fileNames = new StringBuilder();
			boolean resolved = false;
			for (final String name : row.getField("FileNames").split(",")) {
				final String label = name.trim();
				Element dialogueRow = null;
				if (label.indexOf('.') == -1) {
					dialogueRow = dialogue.get(label);
					if (dialogueRow == null) {
						dialogueRow = byFacialLabel.get(label.toLowerCase(Locale.US));
					}
				}
				String fileName = label;
				if (dialogueRow != null) {
					final String filePath = dialogueRow.getField("Filepath");
					final int extension = filePath.lastIndexOf('.');
					fileName = (extension == -1 ? filePath : filePath.substring(0, extension)).replace('/', '\\');
					resolved = true;
				}
				if (fileNames.length() > 0) {
					fileNames.append(',');
				}
				fileNames.append(fileName);
			}
			if (resolved) {
				row.setField("DirectoryBase", "");
				row.setField("FileNames", Arrays.asList(fileNames.toString().split(",")));
			}
		}
	}
}
