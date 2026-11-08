package com.etheller.warsmash.viewer5.handlers.w3x.save;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SavedObjectGraphTest {

	enum Kind {
		FOOTMAN, KNIGHT, GRYPHON
	}

	static final class External {
		final String name;

		External(final String name) {
			this.name = name;
		}
	}

	static final class Dropped {
	}

	static final class OldBuild {
		int orderId;
	}

	static class MovedBase {
		int orderId;
	}

	static final class NewBuild extends MovedBase {
	}

	static final class OldHolder {
		String name;
		Map<String, Integer> counts;
		int orderId;
		Map<String, Integer> shared;
	}

	static final class Part {
		final Map<String, Integer> counts = new HashMap<>();
		int orderId;
		final List<String> added = new ArrayList<>();
	}

	static final class NewHolder {
		String name;
		@SavedComponent
		final Part part = new Part();
		Map<String, Integer> shared;
	}

	static final class Mover {
		int id;
	}

	static final class Turns {
		final Map<Mover, int[]> withdrawn = new IdentityHashMap<>();
		Mover kept;
	}

	static final class Key {
		int id;

		Key(final int id) {
			this.id = id;
		}

		@Override
		public int hashCode() {
			return this.id;
		}

		@Override
		public boolean equals(final Object obj) {
			return (obj instanceof Key) && (((Key) obj).id == this.id);
		}
	}

	static class Base {
		int baseValue;
	}

	static final class Thing extends Base {
		int intValue;
		long longValue;
		float floatValue;
		double doubleValue;
		boolean booleanValue;
		char charValue;
		byte byteValue;
		short shortValue;
		Integer boxed;
		String name;
		Kind kind;
		Class<?> type;
		int[] ints;
		float[][] grid;
		Object[] mixed;
		Thing self;
		Thing other;
		List<Thing> list;
		LinkedList<String> linked;
		Map<String, Integer> hashMap;
		LinkedHashMap<String, Thing> linkedMap;
		Set<Key> keys;
		Map<Key, String> keyed;
		EnumMap<Kind, Integer> enumMap;
		EnumSet<Kind> enumSet;
		External external;
		Dropped dropped;
		Runnable lambda;
	}

	private static final class Rules implements SavedObjectGraph.Rules {
		private final Map<String, Object> externals = new HashMap<>();
		private final List<String> droppedFields = new ArrayList<>();

		@Override
		public String externalName(final Object object) {
			return object instanceof External ? ((External) object).name : null;
		}

		@Override
		public boolean isInPlace(final Object object) {
			return false;
		}

		@Override
		public boolean isDropped(final Object object) {
			return object instanceof Dropped;
		}

		@Override
		public Object replace(final Object object) {
			return object;
		}

		@Override
		public Object resolveExternal(final String name) {
			return this.externals.get(name);
		}

		@Override
		public void reportDropped(final Class<?> holder, final String field, final Object object) {
			this.droppedFields.add(field);
		}
	}

	private static Object roundTrip(final Object root, final Rules writeRules, final Rules readRules)
			throws IOException {
		final ByteArrayOutputStream bodyBytes = new ByteArrayOutputStream();
		final SavedObjectGraph.Writer writer;
		try (DataOutputStream bodies = new DataOutputStream(bodyBytes)) {
			writer = new SavedObjectGraph.Writer(writeRules, bodies);
			writer.writeRoot(root);
		}
		final byte[] table = writer.finishTable();
		SavedObjectGraph.checkClassesMatch(table);
		try (DataInputStream bodies = new DataInputStream(new ByteArrayInputStream(bodyBytes.toByteArray()))) {
			return new SavedObjectGraph.Reader(readRules, table, bodies).readRoot();
		}
	}

	private static byte[] bytes(final Object root, final Rules rules) throws IOException {
		final ByteArrayOutputStream bodyBytes = new ByteArrayOutputStream();
		final SavedObjectGraph.Writer writer;
		try (DataOutputStream bodies = new DataOutputStream(bodyBytes)) {
			writer = new SavedObjectGraph.Writer(rules, bodies);
			writer.writeRoot(root);
		}
		final ByteArrayOutputStream all = new ByteArrayOutputStream();
		all.write(writer.finishTable());
		all.write(bodyBytes.toByteArray());
		return all.toByteArray();
	}

	private static Thing sample(final External external) {
		final Thing thing = new Thing();
		thing.baseValue = -7;
		thing.intValue = 42;
		thing.longValue = Long.MIN_VALUE + 3;
		thing.floatValue = 0.1f;
		thing.doubleValue = Math.PI;
		thing.booleanValue = true;
		thing.charValue = 'Ž';
		thing.byteValue = -128;
		thing.shortValue = 31000;
		thing.boxed = 1234567;
		thing.name = "Arthas ąčęėįšųūž";
		thing.kind = Kind.KNIGHT;
		thing.type = Thing.class;
		thing.ints = new int[] { 3, 1, 4, 1, 5 };
		thing.grid = new float[][] { { 1.5f, -2f }, { Float.NaN } };
		final Thing other = new Thing();
		other.name = "other";
		other.other = thing;
		thing.self = thing;
		thing.other = other;
		thing.mixed = new Object[] { "text", 7, other, null, Kind.GRYPHON };
		thing.list = new ArrayList<>(Arrays.asList(other, thing, other));
		thing.linked = new LinkedList<>(Arrays.asList("c", "a", "b"));
		thing.hashMap = new HashMap<>();
		for (int i = 0; i < 50; i++) {
			thing.hashMap.put("key" + i, i * i);
		}
		thing.linkedMap = new LinkedHashMap<>();
		thing.linkedMap.put("z", other);
		thing.linkedMap.put("a", thing);
		thing.linkedMap.put("m", null);
		thing.keys = new HashSet<>(Arrays.asList(new Key(5), new Key(17), new Key(2)));
		thing.keyed = new HashMap<>();
		thing.keyed.put(new Key(9), "nine");
		thing.enumMap = new EnumMap<>(Kind.class);
		thing.enumMap.put(Kind.GRYPHON, 3);
		thing.enumMap.put(Kind.FOOTMAN, 1);
		thing.enumSet = EnumSet.of(Kind.FOOTMAN, Kind.GRYPHON);
		thing.external = external;
		thing.dropped = new Dropped();
		thing.lambda = () -> {
		};
		return thing;
	}

	@Test
	void roundTripKeepsFieldsAndSharedReferences() throws IOException {
		final External savedExternal = new External("player0");
		final External loadedExternal = new External("player0");
		final Thing thing = sample(savedExternal);
		final Rules writeRules = new Rules();
		final Rules readRules = new Rules();
		readRules.externals.put("player0", loadedExternal);

		final Thing loaded = (Thing) roundTrip(thing, writeRules, readRules);

		assertNotSame(thing, loaded);
		assertEquals(-7, loaded.baseValue);
		assertEquals(42, loaded.intValue);
		assertEquals(Long.MIN_VALUE + 3, loaded.longValue);
		assertEquals(Float.floatToIntBits(0.1f), Float.floatToIntBits(loaded.floatValue));
		assertEquals(Double.doubleToLongBits(Math.PI), Double.doubleToLongBits(loaded.doubleValue));
		assertTrue(loaded.booleanValue);
		assertEquals('Ž', loaded.charValue);
		assertEquals(-128, loaded.byteValue);
		assertEquals(31000, loaded.shortValue);
		assertEquals(Integer.valueOf(1234567), loaded.boxed);
		assertEquals(thing.name, loaded.name);
		assertSame(Kind.KNIGHT, loaded.kind);
		assertSame(Thing.class, loaded.type);
		assertArrayEquals(thing.ints, loaded.ints);
		assertEquals(2, loaded.grid.length);
		assertArrayEquals(thing.grid[0], loaded.grid[0]);
		assertTrue(Float.isNaN(loaded.grid[1][0]));

		// an object referenced from several places comes back as one object
		assertSame(loaded, loaded.self);
		assertSame(loaded, loaded.other.other);
		assertEquals("other", loaded.other.name);
		assertSame(loaded.other, loaded.mixed[2]);
		assertEquals("text", loaded.mixed[0]);
		assertEquals(7, loaded.mixed[1]);
		assertNull(loaded.mixed[3]);
		assertSame(Kind.GRYPHON, loaded.mixed[4]);
		assertEquals(3, loaded.list.size());
		assertSame(loaded.other, loaded.list.get(0));
		assertSame(loaded, loaded.list.get(1));
		assertSame(loaded.other, loaded.list.get(2));

		assertEquals(Arrays.asList("c", "a", "b"), loaded.linked);
		assertEquals(thing.hashMap, loaded.hashMap);
		assertEquals(Arrays.asList("z", "a", "m"), new ArrayList<>(loaded.linkedMap.keySet()));
		assertSame(loaded.other, loaded.linkedMap.get("z"));
		assertSame(loaded, loaded.linkedMap.get("a"));
		assertTrue(loaded.linkedMap.containsKey("m"));
		assertEquals(thing.keys, loaded.keys);
		assertTrue(loaded.keys.contains(new Key(17)));
		assertEquals("nine", loaded.keyed.get(new Key(9)));
		assertEquals(thing.enumMap, loaded.enumMap);
		assertEquals(Arrays.asList(Kind.FOOTMAN, Kind.GRYPHON), new ArrayList<>(loaded.enumMap.keySet()));
		assertEquals(thing.enumSet, loaded.enumSet);

		// objects the rules name are the loading game's own
		assertSame(loadedExternal, loaded.external);
		// dropped and unstorable references are left out and reported
		assertNull(loaded.dropped);
		assertNull(loaded.lambda);
		assertTrue(writeRules.droppedFields.contains("dropped"));
		assertTrue(writeRules.droppedFields.contains("lambda"));
	}

	@Test
	void aFieldMovedUpIntoASuperclassKeepsItsValue() throws IOException {
		final OldBuild old = new OldBuild();
		old.orderId = 7;
		final ByteArrayOutputStream bodyBytes = new ByteArrayOutputStream();
		final SavedObjectGraph.Writer writer;
		try (DataOutputStream bodies = new DataOutputStream(bodyBytes)) {
			writer = new SavedObjectGraph.Writer(new Rules(), bodies);
			writer.writeRoot(old);
		}
		// The save of an engine where the field was still declared by the class itself.
		final String table = new String(writer.finishTable(), StandardCharsets.ISO_8859_1).replace("$OldBuild",
				"$NewBuild");
		try (DataInputStream bodies = new DataInputStream(new ByteArrayInputStream(bodyBytes.toByteArray()))) {
			final Object loaded = new SavedObjectGraph.Reader(new Rules(), table.getBytes(StandardCharsets.ISO_8859_1),
					bodies).readRoot();
			assertEquals(7, ((NewBuild) loaded).orderId);
		}
	}

	@Test
	void fieldsSplitOffIntoAComponentKeepTheirValues() throws IOException {
		final OldHolder old = new OldHolder();
		old.name = "Thrall";
		old.counts = new HashMap<>();
		old.counts.put("STACK", 3);
		old.orderId = 7;
		old.shared = old.counts;
		final ByteArrayOutputStream bodyBytes = new ByteArrayOutputStream();
		final SavedObjectGraph.Writer writer;
		try (DataOutputStream bodies = new DataOutputStream(bodyBytes)) {
			writer = new SavedObjectGraph.Writer(new Rules(), bodies);
			writer.writeRoot(old);
		}
		// The save of an engine where the part's fields were still the holder's own.
		final byte[] table = new String(writer.finishTable(), StandardCharsets.ISO_8859_1)
				.replace("$OldHolder", "$NewHolder").getBytes(StandardCharsets.ISO_8859_1);
		SavedObjectGraph.checkClassesMatch(table);
		try (DataInputStream bodies = new DataInputStream(new ByteArrayInputStream(bodyBytes.toByteArray()))) {
			final NewHolder loaded = (NewHolder) new SavedObjectGraph.Reader(new Rules(), table, bodies).readRoot();
			assertEquals("Thrall", loaded.name);
			assertEquals(7, loaded.part.orderId);
			assertEquals(Integer.valueOf(3), loaded.part.counts.get("STACK"));
			assertSame(loaded.shared, loaded.part.counts);
			assertTrue(loaded.part.added.isEmpty());
		}

		final NewHolder current = new NewHolder();
		current.part.orderId = 9;
		current.part.counts.put("aura", 2);
		final NewHolder reloaded = (NewHolder) roundTrip(current, new Rules(), new Rules());
		assertNotSame(current.part, reloaded.part);
		assertEquals(9, reloaded.part.orderId);
		assertEquals(Integer.valueOf(2), reloaded.part.counts.get("aura"));
	}

	@Test
	void identityMapsWithPlainKeysAreStored() throws IOException {
		final Turns turns = new Turns();
		turns.kept = new Mover();
		turns.kept.id = 4;
		turns.withdrawn.put(turns.kept, new int[] { 7, 2 });
		final Turns loaded = (Turns) roundTrip(turns, new Rules(), new Rules());
		assertTrue(loaded.withdrawn instanceof IdentityHashMap);
		assertEquals(4, loaded.kept.id);
		assertArrayEquals(new int[] { 7, 2 }, loaded.withdrawn.get(loaded.kept));
	}

	@Test
	void numberedJassClassesFollowTheFormatOfTheSave() {
		final String environment = "com.etheller.warsmash.parsers.jass.Jass2$CommonEnvironment$";
		final String observer = "com.etheller.warsmash.parsers.jass.PlayerNatives$ObserverTimer";
		assertEquals("com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimerExpireTriggerEvent",
				SavedObjectGraph.classNameInFormat(environment + "8", 2));
		assertEquals(observer, SavedObjectGraph.classNameInFormat(environment + "9", 2));
		assertEquals(observer, SavedObjectGraph.classNameInFormat(environment + "8", 4));
		assertEquals(observer, SavedObjectGraph.classNameInFormat(environment + "8", 6));
		assertEquals("com.etheller.warsmash.parsers.jass.TriggerNatives$TickEventRegistration",
				SavedObjectGraph.classNameInFormat(environment + "14", 2));
		assertEquals("com.etheller.warsmash.parsers.jass.TriggerNatives$UnitTickEventRegistration",
				SavedObjectGraph.classNameInFormat(environment + "14", 6));
		assertEquals(environment + "9", SavedObjectGraph.classNameInFormat(environment + "9", 6));
	}

	@Test
	void savingTheLoadedGraphGivesTheSameBytes() throws IOException {
		final Rules rules = new Rules();
		final External external = new External("player0");
		rules.externals.put("player0", external);
		final Thing thing = sample(external);
		thing.lambda = null;
		thing.dropped = null;
		final Thing loaded = (Thing) roundTrip(thing, rules, rules);
		assertArrayEquals(bytes(thing, rules), bytes(loaded, rules));
	}
}
