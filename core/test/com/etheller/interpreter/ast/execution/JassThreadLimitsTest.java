package com.etheller.interpreter.ast.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.util.JassProgram;
import com.etheller.interpreter.ast.value.IntegerJassValue;
import com.etheller.interpreter.ast.value.RealJassValue;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.RealJassValueVisitor;

import net.warsmash.parsers.jass.SmashJassParser;

/**
 * How many loop turns fit into one thread's operation budget, and what stops a thread, as measured in the original
 * game (1.29).
 */
class JassThreadLimitsTest {
	private static final String SCRIPT = String.join("\n", //
			"globals", //
			"    integer count = 0", //
			"    integer junk = 0", //
			"    integer zero = 0", //
			"    boolean no = false", //
			"endglobals", //
			"function Two takes integer a, integer b returns nothing", //
			"endfunction", //
			"function Locals takes nothing returns nothing", //
			"    local integer a = 1", //
			"    local integer b", //
			"endfunction", //
			"function Base takes nothing returns nothing", //
			"    loop", //
			"        exitwhen count >= 2000000", //
			"        set count = count + 1", //
			"    endloop", //
			"endfunction", //
			"function CallArgs takes nothing returns nothing", //
			"    loop", //
			"        exitwhen count >= 2000000", //
			"        set count = count + 1", //
			"        call Two(1, 2)", //
			"    endloop", //
			"endfunction", //
			"function IfElse takes nothing returns nothing", //
			"    loop", //
			"        exitwhen count >= 2000000", //
			"        set count = count + 1", //
			"        if no then", //
			"            set junk = 1", //
			"        else", //
			"            set junk = 2", //
			"        endif", //
			"    endloop", //
			"endfunction", //
			"function CallLocals takes nothing returns nothing", //
			"    loop", //
			"        exitwhen count >= 2000000", //
			"        set count = count + 1", //
			"        call Locals()", //
			"    endloop", //
			"endfunction", //
			"function Divide takes nothing returns nothing", //
			"    set count = 1", //
			"    set junk = 7 / zero", //
			"    set count = 2", //
			"endfunction", //
			"function DivideReal takes nothing returns nothing", //
			"    set count = 1", //
			"    set junk = R2I(7.0 / I2R(zero))", //
			"    set count = 2", //
			"endfunction", //
			"");

	private static JassProgram program() throws Exception {
		final JassProgram program = new JassProgram();
		program.getJassNativeManager().createNative("I2R", (arguments, globalScope,
				triggerScope) -> RealJassValue.of(arguments.get(0).visit(IntegerJassValueVisitor.getInstance())));
		program.getJassNativeManager().createNative("R2I", (arguments, globalScope, triggerScope) -> IntegerJassValue
				.of(arguments.get(0).visit(RealJassValueVisitor.getInstance()).intValue()));
		final String natives = "native I2R takes integer i returns real\nnative R2I takes real r returns integer\n";
		new SmashJassParser(new StringReader(natives + SCRIPT)).scanAndParse("test.j", program);
		program.initialize();
		return program;
	}

	private static int run(final String function) throws Exception {
		final JassProgram program = program();
		final JassThread thread = program.getGlobalScope().createThread(function, Collections.emptyList(),
				TriggerExecutionScope.EMPTY);
		program.getGlobalScope().runThreadUntilCompletion(thread);
		assertTrue(thread.isHalted());
		return program.getGlobalScope().getGlobal("count").visit(IntegerJassValueVisitor.getInstance());
	}

	@Test
	void loopsStopWhereTheOriginalStops() throws Exception {
		assertEquals(230769, run("Base"));
		assertEquals(130435, run("CallArgs"));
		assertEquals(166666, run("IfElse"));
		assertEquals(150000, run("CallLocals"));
	}

	@Test
	void divisionByZeroStopsTheThread() throws Exception {
		assertEquals(1, run("Divide"));
		assertEquals(1, run("DivideReal"));
	}
}
