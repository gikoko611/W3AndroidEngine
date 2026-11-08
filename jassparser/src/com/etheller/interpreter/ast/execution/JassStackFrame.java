package com.etheller.interpreter.ast.execution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.etheller.interpreter.ast.execution.instruction.BeginFunctionInstruction;
import com.etheller.interpreter.ast.util.CAllocatedHandle;
import com.etheller.interpreter.ast.util.HandleReferences;
import com.etheller.interpreter.ast.value.HandleJassValue;
import com.etheller.interpreter.ast.value.JassValue;

public class JassStackFrame {
	public BeginFunctionInstruction functionNameMetaData;
	public int returnAddressInstructionPtr;
	public JassStackFrame stackBase;
	public List<JassValue> contents;
	public int debugLineNo;
	private int parameterCount;
	/**
	 * Slots whose handle is counted as referenced: those set by an assignment. A local's starting value and a
	 * parameter are not counted, so a later assignment must not take a reference off what they held.
	 */
	private boolean[] countedSlots;

	public JassStackFrame(final int argumentCount) {
		this.contents = new ArrayList<>(argumentCount);
		this.parameterCount = argumentCount;
	}

	public JassStackFrame() {
		this.contents = new ArrayList<>();
	}

	public JassValue getLast(final int offset) {
		return this.contents.get(this.contents.size() - 1 - offset);
	}

	public void push(final JassValue value) {
		this.contents.add(value);
	}

	public JassValue pop() {
		final int slot = this.contents.size() - 1;
		final JassValue jassValue = this.contents.remove(slot);
		if ((this.countedSlots != null) && (slot < this.countedSlots.length) && this.countedSlots[slot]) {
			this.countedSlots[slot] = false;
			HandleReferences.change(jassValue, -1);
		}
		return jassValue;
	}

	public JassValue peek() {
		return getLast(0);
	}

	/** Stores value in a local or parameter slot and moves the slot's reference to it. */
	public void assign(final int slot, final JassValue value) {
		final JassValue old = this.contents.set(slot, value);
		if ((this.countedSlots == null) || (this.countedSlots.length <= slot)) {
			this.countedSlots = Arrays.copyOf(this.countedSlots == null ? new boolean[0] : this.countedSlots,
					Math.max(slot + 1, this.contents.size()));
		}
		HandleReferences.change(value, 1);
		if (this.countedSlots[slot]) {
			HandleReferences.change(old, -1);
		}
		this.countedSlots[slot] = true;
	}

	/**
	 * The thread waits: in 1.29 a local's starting value points at its handle like an assigned value, so the id of a
	 * destroyed handle waits until the woken thread sets the local to something else (measured). Counted only now,
	 * because before a wait the function cannot outlive the step that frees the id.
	 */
	public void countLocalStartingValues() {
		final int size = this.contents.size();
		if ((this.countedSlots == null) || (this.countedSlots.length < size)) {
			this.countedSlots = Arrays.copyOf(this.countedSlots == null ? new boolean[0] : this.countedSlots, size);
		}
		for (int i = this.parameterCount; i < size; i++) {
			if (!this.countedSlots[i]) {
				this.countedSlots[i] = true;
				HandleReferences.change(this.contents.get(i), 1);
			}
		}
	}

	/**
	 * At the return: handles the function's locals still point at keep their ids for good, as in 1.29. Parameters let
	 * go of theirs.
	 */
	public void keepLocalHandleIds() {
		if (this.countedSlots != null) {
			for (int i = 0; (i < this.parameterCount) && (i < this.countedSlots.length); i++) {
				if (this.countedSlots[i]) {
					HandleReferences.change(this.contents.get(i), -1);
				}
			}
		}
		for (int i = this.parameterCount; i < this.contents.size(); i++) {
			final JassValue value = this.contents.get(i);
			if (value instanceof HandleJassValue) {
				final Object handle = ((HandleJassValue) value).getJavaValue();
				if (handle instanceof CAllocatedHandle) {
					((CAllocatedHandle) handle).keepHandleId();
				}
			}
		}
	}
}
