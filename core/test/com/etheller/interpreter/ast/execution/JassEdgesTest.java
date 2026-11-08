package com.etheller.interpreter.ast.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.util.JassProgram;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;

import net.warsmash.parsers.jass.SmashJassParser;

/**
 * Array index limits, integer overflow edges and rawcode literals as the original game (1.29) has them.
 */
class JassEdgesTest {
	private static final String SCRIPT = String.join("\n", //
			"globals", //
			"    integer array ia", //
			"    string array sa", //
			"    integer minInt = 0x80000000", //
			"    integer minusOne = -1", //
			"    integer stage = 0", //
			"    integer r1 = -7", //
			"    integer r2 = -7", //
			"    integer r3 = -7", //
			"    integer r4 = -7", //
			"    integer r5 = -7", //
			"    boolean b1 = false", //
			"endglobals", //
			"function Arrays takes nothing returns nothing", //
			"    local integer array la", //
			"    local integer i = 32768", //
			"    set ia[32767] = 13", //
			"    set ia[i] = 14", //
			"    set ia[-1] = 15", //
			"    set ia[2147483647] = 16", //
			"    set sa[40000] = \"x\"", //
			"    set la[32767] = 18", //
			"    set r1 = ia[32767]", //
			"    set r2 = ia[i]", //
			"    set r3 = ia[-1]", //
			"    set r4 = ia[100]", //
			"    set r5 = la[32767]", //
			"    set b1 = sa[40000] == null and sa[5] == null", //
			"    set stage = 1", //
			"endfunction", //
			"function Overflow takes nothing returns nothing", //
			"    set r1 = minInt / minusOne", //
			"    set r2 = minInt * minusOne", //
			"    set r3 = -minInt", //
			"    set r4 = 2147483647 / minusOne", //
			"    set r5 = -7 / 2", //
			"    set stage = 1", //
			"endfunction", //
			"");

	private static JassProgram program(final String source) throws Exception {
		final JassProgram program = new JassProgram();
		new SmashJassParser(new StringReader(source)).scanAndParse("test.j", program);
		program.initialize();
		return program;
	}

	private static JassProgram run(final String function) throws Exception {
		final JassProgram program = program(SCRIPT);
		final JassThread thread = program.getGlobalScope().createThread(function, Collections.emptyList(),
				TriggerExecutionScope.EMPTY);
		program.getGlobalScope().runThreadUntilCompletion(thread);
		return program;
	}

	private static int global(final JassProgram program, final String name) {
		return program.getGlobalScope().getGlobal(name).visit(IntegerJassValueVisitor.getInstance());
	}

	@Test
	void indexesOutsideTheArrayAreIgnored() throws Exception {
		final JassProgram program = run("Arrays");
		assertEquals(1, global(program, "stage"));
		assertEquals(13, global(program, "r1"));
		assertEquals(0, global(program, "r2"));
		assertEquals(0, global(program, "r3"));
		assertEquals(0, global(program, "r4"));
		assertEquals(18, global(program, "r5"));
		assertTrue(program.getGlobalScope().getGlobal("b1").visit(BooleanJassValueVisitor.getInstance()));
	}

	@Test
	void integerOverflowWrapsWithoutStoppingTheThread() throws Exception {
		final JassProgram program = run("Overflow");
		assertEquals(1, global(program, "stage"));
		// The original game crashes on MinInt / -1; the engine keeps the two's complement result.
		assertEquals(Integer.MIN_VALUE, global(program, "r1"));
		assertEquals(Integer.MIN_VALUE, global(program, "r2"));
		assertEquals(Integer.MIN_VALUE, global(program, "r3"));
		assertEquals(-2147483647, global(program, "r4"));
		assertEquals(-3, global(program, "r5"));
	}

	private static int rawcode(final String literal) throws Exception {
		final JassProgram program = program("globals\n    integer x = " + literal + "\nendglobals\n");
		return global(program, "x");
	}

	@Test
	void rawcodesHaveOneOrFourCharacters() throws Exception {
		assertEquals(97, rawcode("'a'"));
		assertEquals(0x68666F6F, rawcode("'hfoo'"));
		for (final String literal : new String[] { "''", "'ab'", "'abc'", "'abcde'" }) {
			assertThrows(Exception.class, () -> rawcode(literal), literal);
		}
	}
}
