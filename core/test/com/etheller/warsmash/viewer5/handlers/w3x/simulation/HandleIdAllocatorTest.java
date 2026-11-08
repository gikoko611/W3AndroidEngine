package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import com.etheller.interpreter.ast.execution.JassStackFrame;
import com.etheller.interpreter.ast.util.AllocatedHandle;
import com.etheller.interpreter.ast.value.HandleJassValue;

class HandleIdAllocatorTest {
	private static final class Handle extends AllocatedHandle {
	}

	private static Handle handle(final HandleIdAllocator allocator) {
		return allocator.assign(new Handle());
	}

	@Test
	void handlesAreNumberedFromTheFirstHandleIdOneAfterAnother() {
		final HandleIdAllocator allocator = new HandleIdAllocator();
		assertEquals(0x100000, handle(allocator).getHandleId());
		assertEquals(0x100001, allocator.createHandleId());
		assertEquals(0x100002, handle(allocator).getHandleId());
	}

	@Test
	void abilityIdsDoNotUseTheHandleTable() {
		final HandleIdAllocator allocator = new HandleIdAllocator();
		allocator.createId();
		allocator.createId();
		assertEquals(0x100000, allocator.createHandleId());
	}

	@Test
	void assignKeepsAnIdAlreadyGiven() {
		final HandleIdAllocator allocator = new HandleIdAllocator();
		final Handle handle = handle(allocator);
		assertSame(handle, allocator.assign(handle));
		assertEquals(0x100000, handle.getHandleId());
		assertEquals(0x100001, allocator.createHandleId());
	}

	@Test
	void freedIdsComeBackOnlyAfterTheStepLastOneFirst() {
		final HandleIdAllocator allocator = new HandleIdAllocator();
		final Handle a = handle(allocator);
		final Handle b = handle(allocator);
		final Handle c = handle(allocator);
		allocator.release(a);
		allocator.release(c);
		assertEquals(0x100003, allocator.createHandleId());
		allocator.endStep();
		assertEquals(c.getHandleId(), allocator.createHandleId());
		assertEquals(a.getHandleId(), allocator.createHandleId());
		assertEquals(0x100004, allocator.createHandleId());
		assertEquals(0x100001, b.getHandleId());
	}

	@Test
	void freedIdsAndReleasedHandlesKeepTheirOrder() {
		final HandleIdAllocator allocator = new HandleIdAllocator();
		final Handle a = handle(allocator);
		final int unitId = allocator.createHandleId();
		final Handle b = handle(allocator);
		allocator.release(a);
		allocator.freeHandleId(unitId);
		allocator.release(b);
		allocator.endStep();
		assertEquals(b.getHandleId(), allocator.createHandleId());
		assertEquals(unitId, allocator.createHandleId());
		assertEquals(a.getHandleId(), allocator.createHandleId());
	}

	@Test
	void aHandleDestroyedTwiceGivesItsIdBackOnce() {
		final HandleIdAllocator allocator = new HandleIdAllocator();
		final Handle a = handle(allocator);
		allocator.release(a);
		allocator.endStep();
		final Handle reused = handle(allocator);
		assertEquals(a.getHandleId(), reused.getHandleId());
		allocator.release(a);
		allocator.endStep();
		assertEquals(0x100001, allocator.createHandleId());
	}

	@Test
	void anIdKeptByALocalVariableIsNeverGivenAgain() {
		final HandleIdAllocator allocator = new HandleIdAllocator();
		final Handle keptBefore = handle(allocator);
		final Handle keptAfter = handle(allocator);
		keptBefore.keepHandleId();
		allocator.release(keptBefore);
		allocator.release(keptAfter);
		keptAfter.keepHandleId();
		allocator.endStep();
		assertEquals(0x100002, allocator.createHandleId());
		assertEquals(0, allocator.getFreeHandleIdCount());
	}

	@Test
	void localsKeepTheirHandlesIdsAtTheReturnButParametersDoNot() {
		final HandleIdAllocator allocator = new HandleIdAllocator();
		final Handle parameter = handle(allocator);
		final Handle local = handle(allocator);
		final JassStackFrame frame = new JassStackFrame(1);
		frame.push(new HandleJassValue(null, parameter));
		frame.push(new HandleJassValue(null, local));
		frame.keepLocalHandleIds();
		assertFalse(parameter.isHandleIdKept());
		assertTrue(local.isHandleIdKept());
	}

	@Test
	void textTagsHaveAHundredIdsFromNinetyNineDown() {
		final HandleIdAllocator allocator = new HandleIdAllocator();
		assertEquals(99, allocator.createTextTagId());
		assertEquals(98, allocator.createTextTagId());
		allocator.freeTextTagId(99);
		allocator.freeTextTagId(99);
		assertEquals(99, allocator.createTextTagId());
		final HashSet<Integer> ids = new HashSet<>(Arrays.asList(99, 98));
		for (int i = 0; i < 98; i++) {
			ids.add(allocator.createTextTagId());
		}
		assertEquals(100, ids.size());
		assertEquals(0x100000, allocator.createTextTagId());
	}

	@Test
	void lightningsAreCountedApartFromOne() {
		final HandleIdAllocator allocator = new HandleIdAllocator();
		assertEquals(1, allocator.createLightningId());
		assertEquals(2, allocator.createLightningId());
		assertEquals(0x100000, allocator.createHandleId());
	}

	@Test
	void objectsWithoutAnIdFieldAreNumberedOnceInTheOrderAsked() {
		final HandleIdAllocator allocator = new HandleIdAllocator();
		final Object first = new Object();
		final Object second = new Object();
		assertEquals(0x100000, allocator.handleIdOf(second));
		assertEquals(0x100001, allocator.handleIdOf(first));
		assertEquals(0x100000, allocator.handleIdOf(second));
		final Handle unnumbered = new Handle();
		assertEquals(0x100002, allocator.handleIdOf(unnumbered));
		assertEquals(0x100002, unnumbered.getHandleId());
	}

	@Test
	void anIdWaitsWhileAScriptReferenceToItsHandleRemains() {
		final HandleIdAllocator allocator = new HandleIdAllocator();
		final Handle referenced = handle(allocator);
		referenced.changeReferenceCount(1);
		allocator.release(referenced);
		allocator.endStep();
		assertEquals(0x100001, allocator.createHandleId());
		referenced.changeReferenceCount(-1);
		allocator.endStep();
		assertEquals(0x100000, allocator.createHandleId());
		referenced.changeReferenceCount(1);
		referenced.changeReferenceCount(-1);
		allocator.endStep();
		assertEquals(0x100002, allocator.createHandleId());
	}

	@Test
	void aSaveFromBeforeTheHandleTableGoesOnAfterItsIds() {
		final HandleIdAllocator allocator = new HandleIdAllocator();
		for (int i = 0; i < 10; i++) {
			allocator.createId();
		}
		allocator.initAddedFields(new HashSet<>(Arrays.asList("nextHandleId", "freeHandleIds", "freeHandleIdCount",
				"nextLightningId", "freeTextTagIds", "freeTextTagIdCount", "releasedHandles", "untrackedHandles")));
		assertEquals(HandleIdAllocator.LEGACY_FIRST_ID + 10, allocator.createHandleId());
		assertNotEquals(HandleIdAllocator.LEGACY_FIRST_ID + 11, allocator.createId());
		assertEquals(99, allocator.createTextTagId());
		assertEquals(1, allocator.createLightningId());
	}
}
