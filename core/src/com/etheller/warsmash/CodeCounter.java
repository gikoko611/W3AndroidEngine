package com.etheller.warsmash;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.etheller.warsmash.util.Log;

public class CodeCounter {

	public static void main(final String[] args) {
		final int sourceLines = countFile(new File("src/com/etheller"));
		Log.info(sourceLines);
	}

	public static int countFile(final File file) {
		if (file.isDirectory()) {
			int sum = 0;
			for (final File subFile : file.listFiles()) {
				sum += countFile(subFile);
			}
			return sum;
		}
		else {
			try {
				if (file.getName().toLowerCase().endsWith(".java")) {
					return Files.readAllLines(file.toPath()).size();
				}
			}
			catch (final IOException e) {
				Log.error(e);
			}
			return 0;
		}
	}

}
