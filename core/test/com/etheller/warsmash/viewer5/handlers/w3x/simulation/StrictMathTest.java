package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * The simulation, the script runtime and what scripts read of the camera take their sines, roots of sums and the like
 * from StrictMath: Math may use the processor's own instructions, which can differ in the last bit between a desktop
 * and an Android device, and a game played on both must stay the same step for step. Math.sqrt, toRadians and
 * toDegrees give the same bits everywhere.
 */
class StrictMathTest {
	private static final Pattern PLATFORM_MATH = Pattern.compile("(?<![\\w.])Math\\.(sin|cos|tan|asin|acos|atan|atan2"
			+ "|hypot|pow|exp|expm1|log|log10|log1p|cbrt|sinh|cosh|tanh)\\s*\\(");
	private static final Pattern LIBGDX_MATH = Pattern.compile("\\bMathUtils\\.(sin|cos|atan2|sinDeg|cosDeg)"
			+ "|\\bQuaternion\\b");

	private static final String[] SOURCES = { "src/com/etheller/warsmash/viewer5/handlers/w3x/simulation",
			"src/com/etheller/warsmash/parsers/jass", "../jassparser/src/com/etheller/interpreter",
			"src/com/etheller/warsmash/viewer5/handlers/w3x/camera/GameCameraManager.java" };

	@Test
	void simulationAndScriptsUseStrictMath() throws IOException {
		final List<String> found = new ArrayList<>();
		int files = 0;
		for (final String source : SOURCES) {
			final Path root = Paths.get(source);
			assertTrue(Files.exists(root), "missing " + root.toAbsolutePath());
			final List<Path> sourceFiles;
			try (Stream<Path> paths = Files.walk(root)) {
				sourceFiles = paths.filter(path -> path.toString().endsWith(".java")).collect(Collectors.toList());
			}
			for (final Path file : sourceFiles) {
				files++;
				final List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
				for (int i = 0; i < lines.size(); i++) {
					final String line = lines.get(i);
					final String code = line.contains("//") ? line.substring(0, line.indexOf("//")) : line;
					if (PLATFORM_MATH.matcher(code).find() || LIBGDX_MATH.matcher(code).find()) {
						found.add(file.getFileName() + ":" + (i + 1) + ": " + line.trim());
					}
				}
			}
		}
		assertEquals(Collections.emptyList(), found);
		assertTrue(files > 100, "the sources were not found: " + files);
	}
}
