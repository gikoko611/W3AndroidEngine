package com.etheller.interpreter.ast.util;

public class JassSettings {
	public static final boolean LOG_FUNCTION_DEFINITIONS = false;
	public static final int MAX_ARRAY_SIZE = 32768; // JASS_MAX_ARRAY_SIZE of the original game
	public static final boolean CHECK_TYPES = true;
	public static final boolean DEBUG = true;
	public static final boolean CONTINUE_EXECUTING_ON_ERROR = true;
	/**
	 * The original game stops a script thread when it reaches this many operations of its script engine since it
	 * started or last waited (TriggerSleepAction, also of 0 s). Threads started by ExecuteFunc, TriggerEvaluate,
	 * TriggerExecute, ForGroup and enum filters have their own count.
	 */
	public static final int OP_LIMIT = 3000000;
}
