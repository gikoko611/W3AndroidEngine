/*
 * Lex file for SmashJass
 */
package net.warsmash.parsers.jass;

%%
 
%byaccj /* using byaccj mode even though bison will be used to compile */

%line
%column

%class SmashJassLexer

%{

	private Object yylval;
	public Object getLVal() {
		return yylval;
	}
	
	public int getLine() {
		return yyline;
	}
	
	public int getColumn() {
		return yycolumn;
	}

	/* The next input character after the current match, or -1 at the end of the input. */
	private int peekNext() throws java.io.IOException {
		if ((zzMarkedPos >= zzEndRead) && (zzAtEOF || zzRefill())) {
			return -1;
		}
		return zzBuffer[zzMarkedPos];
	}

	/* Adds the following characters that are digits of the given radix to the current match. */
	private void extendDigits(final int radix) throws java.io.IOException {
		int c;
		while (((c = peekNext()) != -1) && (Character.digit((char) c, radix) != -1)) {
			zzMarkedPos++;
		}
	}

	/* Literals keep their low 32 bits, as in the original game, which also refuses a prefix without digits ("0x"). */
	private int parseLiteral(final int prefixLength, final int radix) {
		final String text = yytext();
		if (text.length() == prefixLength) {
			throw new IllegalStateException((getLine() + 1) + ":" + getColumn() + ": Missing digits in '" + text + "'");
		}
		int value = 0;
		for (int i = prefixLength; i < text.length(); i++) {
			final int digit = Character.digit(text.charAt(i), radix);
			if (digit == -1) {
				throw new IllegalStateException((getLine() + 1) + ":" + getColumn() + ": Invalid digit in '" + text + "'");
			}
			value = (value * radix) + digit;
		}
		return value;
	}

	/*
	 * A rawcode is its bytes as base 256 digits, so 'C' is 67, not 'C\0\0\0'. The original game takes one byte, four
	 * bytes without a backslash, or one escape alone ('\n' is 10); 'ab', 'a\\bc', '\n\n\n\n' and '\q' are syntax
	 * errors there.
	 */
	private int rawcodeValue() throws java.io.IOException {
		String text = yytext();
		if (text.equals("'\\'") && (peekNext() == '\'')) {
			// The expression stops at the quote of '\''.
			zzMarkedPos++;
			text = yytext();
		}
		final String content = text.substring(1, text.length() - 1);
		if (content.indexOf('\\') != -1) {
			final int escaped = content.length() == 2 ? rawcodeEscape(content.charAt(1)) : -1;
			if ((content.charAt(0) != '\\') || (escaped == -1)) {
				throw new IllegalStateException((getLine() + 1) + ":" + getColumn() + ": Invalid rawcode " + text);
			}
			return escaped;
		}
		final byte[] rawcodeBytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		if ((rawcodeBytes.length != 1) && (rawcodeBytes.length != 4)) {
			throw new IllegalStateException((getLine() + 1) + ":" + getColumn() + ": Invalid rawcode " + text);
		}
		int rawcode = 0;
		for (int i = 0; i < rawcodeBytes.length; i++) {
			rawcode = (rawcode << 8) | (rawcodeBytes[i] & 0xFF);
		}
		return rawcode;
	}

	private static int rawcodeEscape(final char c) {
		switch (c) {
		case 'n':
			return '\n';
		case 't':
			return '\t';
		case 'r':
			return '\r';
		case 'b':
			return '\b';
		case 'f':
			return '\f';
		case '"':
		case '\'':
		case '\\':
			return c;
		default:
			return -1;
		}
	}

	/*
	 * The regular expressions only match a lone "0" before other digits, so the original game's octal ("010" is 8),
	 * upper case hex ("0X1F") and leading zero real ("01.5") forms are read here. "08" is a syntax error there.
	 */
	private int zeroPrefixedLiteral() throws java.io.IOException {
		if (peekNext() == 'X') {
			zzMarkedPos++;
			extendDigits(16);
			yylval = parseLiteral(2, 16);
			return SmashJassParser.Lexer.HEX_CONSTANT;
		}
		extendDigits(10);
		if (peekNext() == '.') {
			zzMarkedPos++;
			extendDigits(10);
			yylval = Double.parseDouble(yytext());
			return SmashJassParser.Lexer.REAL;
		}
		yylval = parseLiteral(0, 8);
		return SmashJassParser.Lexer.INTEGER;
	}

%}


EQUALS = "="

PLUSEQUALS = "+="

MINUSEQUALS = "-="

PLUSPLUS = "++"

MINUSMINUS = "--"


GLOBALS = "globals"
ENDGLOBALS = "endglobals"

NATIVE = "native" 

FUNCTION = "function"
TAKES = "takes"
RETURNS = "returns"
ENDFUNCTION = "endfunction"
NOTHING = "nothing"

STRUCT = "struct"
ENDSTRUCT = "endstruct"

LIBRARY = "library"
LIBRARY_ONCE = "library_once"
ENDLIBRARY = "endlibrary"

SCOPE = "scope"
ENDSCOPE = "endscope"

INTERFACE = "interface"
ENDINTERFACE = "endinterface"
REQUIRES = "requires"
USES = "uses"
NEEDS = "needs"
OPTIONAL = "optional"
INITIALIZER = "initializer"
DEFAULTS = "defaults"

PRIVATE = "private"
PUBLIC = "public"
READONLY = "readonly"

OPERATOR = "operator"

IMPLEMENT = "implement"

MODULE = "module"
ENDMODULE = "endmodule"

METHOD = "method"
ENDMETHOD = "endmethod"
STATIC = "static"

CALL = "call"
SET = "set"
RETURN = "return"

ARRAY = "array"

TYPE = "type"

EXTENDS = "extends"

IF = "if"
THEN = "then"
ELSE = "else"
ENDIF = "endif"
ELSEIF = "elseif"
CONSTANT = "constant"
LOCAL = "local"
LOOP = "loop"
ENDLOOP = "endloop"
EXITWHEN = "exitwhen"
DEBUG = "debug"

STRING_LITERAL = "\""(("\\"[^])|[^"\"""\\"])*?"\""

 
INTEGER = [0]|([1-9][0-9]*)
 
HEX_CONSTANT = "0x"(([0-9]|[a-f]|[A-F])*)
DOLLAR_HEX_CONSTANT = "$"(([0-9]|[A-F])*)

RAWCODE = ("\'"[^"\'"]*?"\'")

REAL = (([0]|([1-9][0-9]*))"."[0-9]*)|("."([0-9]+))

NULL = "null"
TRUE = "true"
FALSE = "false"

NOT = "not"
OR = "or"
AND = "and"

ID = ([a-zA-Z_][a-zA-Z_0-9]*)

WS = [ \t]+

NEWLINE = "//".*?"\r\n" | "//".*?"\n" | "//".*?"\r" | "\r" "\n" | "\n" | "\r" | "//".*?

TIMES="*"
DIVIDE="/"
PLUS="+"
MINUS="-"
LESS="<"
GREATER=">"
LESS_EQUALS="<="
GREATER_EQUALS=">="
DOUBLE_EQUALS="=="
NOT_EQUALS="!="
OPEN_BRACKET="["
CLOSE_BRACKET="]"
OPEN_PARENS="("
CLOSE_PARENS=")"

COMMA=","

DOT="."

MULTILINE_COMMENT = ("/*"(("*"[^"/"])|[^"*"])*?"*/")

%%

{EQUALS} { return SmashJassParser.Lexer.EQUALS; }
{PLUSEQUALS} { return SmashJassParser.Lexer.PLUSEQUALS; }
{MINUSEQUALS} { return SmashJassParser.Lexer.MINUSEQUALS; }
{PLUSPLUS} { return SmashJassParser.Lexer.PLUSPLUS; }
{MINUSMINUS} { return SmashJassParser.Lexer.MINUSMINUS; }
{GLOBALS} { return SmashJassParser.Lexer.GLOBALS; }
{ENDGLOBALS} { return SmashJassParser.Lexer.ENDGLOBALS; }
{NATIVE} { return SmashJassParser.Lexer.NATIVE; }
{FUNCTION} { return SmashJassParser.Lexer.FUNCTION; }
{TAKES} { return SmashJassParser.Lexer.TAKES; }
{RETURNS} { return SmashJassParser.Lexer.RETURNS; }
{ENDFUNCTION} { return SmashJassParser.Lexer.ENDFUNCTION; }
{NOTHING} { return SmashJassParser.Lexer.NOTHING; }
{STRUCT} { return SmashJassParser.Lexer.STRUCT; }
{ENDSTRUCT} { return SmashJassParser.Lexer.ENDSTRUCT; }
{LIBRARY} { return SmashJassParser.Lexer.LIBRARY; }
{LIBRARY_ONCE} { return SmashJassParser.Lexer.LIBRARY_ONCE; }
{ENDLIBRARY} { return SmashJassParser.Lexer.ENDLIBRARY; }
{SCOPE} { return SmashJassParser.Lexer.SCOPE; }
{ENDSCOPE} { return SmashJassParser.Lexer.ENDSCOPE; }
{INTERFACE} { return SmashJassParser.Lexer.INTERFACE; }
{ENDINTERFACE} { return SmashJassParser.Lexer.ENDINTERFACE; }
{REQUIRES} { return SmashJassParser.Lexer.REQUIRES; }
{USES} { return SmashJassParser.Lexer.REQUIRES; }
{NEEDS} { return SmashJassParser.Lexer.REQUIRES; }
{OPTIONAL} { return SmashJassParser.Lexer.OPTIONAL; }
{INITIALIZER} { return SmashJassParser.Lexer.INITIALIZER; }
{DEFAULTS} { return SmashJassParser.Lexer.DEFAULTS; }
{PRIVATE} { return SmashJassParser.Lexer.PRIVATE; }
{PUBLIC} { return SmashJassParser.Lexer.PUBLIC; }
{READONLY} { return SmashJassParser.Lexer.READONLY; }
{OPERATOR} { return SmashJassParser.Lexer.OPERATOR; }
{IMPLEMENT} { return SmashJassParser.Lexer.IMPLEMENT; }
{MODULE} { return SmashJassParser.Lexer.MODULE; }
{ENDMODULE} { return SmashJassParser.Lexer.ENDMODULE; }
{METHOD} { return SmashJassParser.Lexer.METHOD; }
{ENDMETHOD} { return SmashJassParser.Lexer.ENDMETHOD; }
{STATIC} { return SmashJassParser.Lexer.STATIC; }
{CALL} { return SmashJassParser.Lexer.CALL; }
{SET} { return SmashJassParser.Lexer.SET; }
{RETURN} { return SmashJassParser.Lexer.RETURN; }
{ARRAY} { return SmashJassParser.Lexer.ARRAY; }
{TYPE} { return SmashJassParser.Lexer.TYPE; }
{EXTENDS} { return SmashJassParser.Lexer.EXTENDS; }
{IF} { return SmashJassParser.Lexer.IF; }
{THEN} { return SmashJassParser.Lexer.THEN; }
{ELSE} { return SmashJassParser.Lexer.ELSE; }
{ENDIF} { return SmashJassParser.Lexer.ENDIF; }
{ELSEIF} { return SmashJassParser.Lexer.ELSEIF; }
{CONSTANT} { return SmashJassParser.Lexer.CONSTANT; }
{LOCAL} { return SmashJassParser.Lexer.LOCAL; }
{LOOP} { return SmashJassParser.Lexer.LOOP; }
{ENDLOOP} { return SmashJassParser.Lexer.ENDLOOP; }
{EXITWHEN} { return SmashJassParser.Lexer.EXITWHEN; }
{DEBUG} { return SmashJassParser.Lexer.DEBUG; }
{STRING_LITERAL} {
	final String stringLiteralText = yytext();
	final String parsedString = stringLiteralText.substring(1, stringLiteralText.length() - 1).replace("\\\\", "\\");
	yylval = parsedString;
	return SmashJassParser.Lexer.STRING_LITERAL; }
{INTEGER} {
	if (yytext().equals("0")) {
		return zeroPrefixedLiteral();
	}
	yylval = parseLiteral(0, 10);
	return SmashJassParser.Lexer.INTEGER; }
{HEX_CONSTANT} {
	yylval = parseLiteral(2, 16);
	return SmashJassParser.Lexer.HEX_CONSTANT; }
{DOLLAR_HEX_CONSTANT} {
	/* lower case digits are valid too ("$1f") */
	extendDigits(16);
	yylval = parseLiteral(1, 16);
	return SmashJassParser.Lexer.DOLLAR_HEX_CONSTANT; }
{RAWCODE} {
	yylval = rawcodeValue();
	return SmashJassParser.Lexer.RAWCODE; }
{REAL} {
	yylval = com.etheller.interpreter.ast.util.JassNumbers.parseLiteral(yytext());
	return SmashJassParser.Lexer.REAL; }
{NULL} { return SmashJassParser.Lexer.NULL; }
{TRUE} { return SmashJassParser.Lexer.TRUE; }
{FALSE} { return SmashJassParser.Lexer.FALSE; }
{NOT} { return SmashJassParser.Lexer.NOT; }
{OR} { return SmashJassParser.Lexer.OR; }
{AND} { return SmashJassParser.Lexer.AND; }
{ID} {
	yylval = yytext();
	return SmashJassParser.Lexer.ID; }
{WS} { /* skip this */ }
{NEWLINE} { return SmashJassParser.Lexer.NEWLINE; }
{MULTILINE_COMMENT} { /* skip this */ }
{TIMES} { return SmashJassParser.Lexer.TIMES; }
{DIVIDE} { return SmashJassParser.Lexer.DIVIDE; }
{PLUS} { return SmashJassParser.Lexer.PLUS; }
{MINUS} { return SmashJassParser.Lexer.MINUS; }
{LESS} { return SmashJassParser.Lexer.LESS; }
{GREATER} { return SmashJassParser.Lexer.GREATER; }
{LESS_EQUALS} { return SmashJassParser.Lexer.LESS_EQUALS; }
{GREATER_EQUALS} { return SmashJassParser.Lexer.GREATER_EQUALS; }
{DOUBLE_EQUALS} { return SmashJassParser.Lexer.DOUBLE_EQUALS; }
{NOT_EQUALS} { return SmashJassParser.Lexer.NOT_EQUALS; }
{OPEN_BRACKET} { return SmashJassParser.Lexer.OPEN_BRACKET; }
{CLOSE_BRACKET} { return SmashJassParser.Lexer.CLOSE_BRACKET; }
{OPEN_PARENS} { return SmashJassParser.Lexer.OPEN_PAREN; }
{CLOSE_PARENS} { return SmashJassParser.Lexer.CLOSE_PAREN; }

{COMMA} { return SmashJassParser.Lexer.COMMA; }
{DOT} { return SmashJassParser.Lexer.DOT; }

/* detect errors, print unknown chars */
[^] { throw new IllegalStateException((getLine()+1) + ":" + getColumn() +": Unexpected character '" + yytext() + "'"); }
