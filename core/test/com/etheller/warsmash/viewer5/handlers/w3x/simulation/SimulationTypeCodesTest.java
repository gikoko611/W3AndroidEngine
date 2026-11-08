package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CEffect;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;

/**
 * Every behavior, effect and timer class the checksum can name has a code; only anonymous and private classes go by
 * their superclass's.
 */
class SimulationTypeCodesTest {
	private static final Class<?>[] FAMILIES = { CBehavior.class, CEffect.class, CTimer.class };

	@Test
	void everyNamedSimulationTypeHasACode() throws Exception {
		final List<String> missing = new ArrayList<>();
		final int[] checked = new int[1];
		final Path location = Paths.get(CSimulation.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		if (Files.isDirectory(location)) {
			scan(location, missing, checked);
		}
		else {
			try (FileSystem jar = FileSystems.newFileSystem(location, (ClassLoader) null)) {
				scan(jar.getPath("/"), missing, checked);
			}
		}
		assertEquals(Collections.emptyList(), missing);
		assertTrue(checked[0] > 50, "the class folder was not found: " + checked[0]);
	}

	private static void scan(final Path root, final List<String> missing, final int[] checked) throws IOException {
		try (Stream<Path> files = Files.walk(root.resolve("com/etheller/warsmash"))) {
			files.filter(file -> file.toString().endsWith(".class")).forEach(file -> {
				final String relative = root.relativize(file).toString().replace('\\', '/');
				final String className = relative.substring(0, relative.length() - ".class".length()).replace('/',
						'.');
				final Class<?> type;
				try {
					type = Class.forName(className, false, CSimulation.class.getClassLoader());
					if (type.isAnonymousClass() || type.isLocalClass() || type.isSynthetic()
							|| Modifier.isPrivate(type.getModifiers()) || !inFamily(type)) {
						return;
					}
				}
				catch (final ClassNotFoundException | LinkageError e) {
					return;
				}
				checked[0]++;
				if (!SimulationTypeCodes.has(type)) {
					missing.add(className);
				}
			});
		}
	}

	private static boolean inFamily(final Class<?> type) {
		for (final Class<?> family : FAMILIES) {
			if (family.isAssignableFrom(type)) {
				return true;
			}
		}
		return false;
	}

	@Test
	void anonymousAndUnknownTypesFallBack() {
		final CTimer anonymous = new CTimer() {
			@Override
			public void onFire(final CSimulation simulation) {
			}
		};
		assertEquals("Timer", SimulationTypeCodes.of(anonymous, "?"));
		assertEquals("Effect", SimulationTypeCodes.of((CEffect) game -> false, "?"));
		assertEquals("?", SimulationTypeCodes.of(new Object(), "?"));
		assertEquals("-", SimulationTypeCodes.of(null, "?"));
	}
}
