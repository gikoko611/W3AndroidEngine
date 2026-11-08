package com.etheller.warsmash.desktop.launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.etheller.warsmash.launcher.DataFolderCheck;

/** Where the game data may be on a computer, and the folder chooser for the rest. */
final class GameDataFolders {
	private static final String[] INSTALL_FOLDERS = { "Program Files (x86)\\Warcraft III",
		"Program Files\\Warcraft III", "Games\\Warcraft III", "Warcraft III" };

	private GameDataFolders() {
	}

	/** The installed game in its usual folders, else null. */
	static File findInstalled() {
		final List<File> candidates = new ArrayList<>();
		final String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		if (os.contains("win")) {
			final File[] roots = File.listRoots();
			if (roots != null) {
				for (final File root : roots) {
					for (final String folder : INSTALL_FOLDERS) {
						candidates.add(new File(root, folder));
					}
				}
			}
		}
		else if (os.contains("mac")) {
			candidates.add(new File("/Applications/Warcraft III"));
		}
		else {
			final String home = System.getProperty("user.home", "");
			candidates.add(new File(home,
					".wine/drive_c/Program Files (x86)/Warcraft III"));
			candidates.add(new File(home, "Games/Warcraft III"));
		}
		for (final File candidate : candidates) {
			if (candidate.isDirectory()) {
				final File located = DataFolderCheck.locate(candidate);
				final File folder = located != null ? located : candidate;
				if (DataFolderCheck.detect(folder) != DataFolderCheck.Kind.NONE) {
					return folder;
				}
			}
		}
		return null;
	}

	/**
	 * Asks for a folder, or a game data package zip, away from the window's thread; null when the player cancels.
	 */
	static void choose(final File current, final Consumer<File> chosen) {
		SwingUtilities.invokeLater(() -> {
			final JFileChooser chooser = new JFileChooser(current);
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.setAcceptAllFileFilterUsed(false);
			chooser.setFileFilter(new FileNameExtensionFilter("Game data folder or package (.zip)", "zip"));
			final int answer = chooser.showOpenDialog(null);
			chosen.accept(answer == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null);
		});
	}
}
