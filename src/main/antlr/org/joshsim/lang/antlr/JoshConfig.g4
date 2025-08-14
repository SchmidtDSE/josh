/**
 * ANTLR grammar for Josh configuration files (.jshc format).
 *
 * Supports:
 * - One variable per line: variableName = value units
 * - Comments using # character (all text after # ignored until newline)
 * - Empty lines allowed and ignored
 * - Whitespace flexible around equals sign
 * - Variable names follow pattern: [A-Za-z][A-Za-z0-9]*
 * - Values must be valid EngineValue format (number + optional units)
 *
 * @license BSD-3-Clause
 */

grammar JoshConfig;

@header {
package org.joshsim.lang.antlr;
}

// Parser rules
config : configLine* EOF ;

configLine 
    : assignment
    | comment
    | emptyLine
    ;

assignment : ID EQUALS value ;

value : NUMBER ID? ;

comment : COMMENT ;

emptyLine : NEWLINE ;

// Lexer rules
ID : [A-Za-z/][A-Za-z0-9%/]* ;

NUMBER : [+-]? ([0-9]+ ('.' [0-9]+)? | '.' [0-9]+) ;

EQUALS : '=' ;

COMMENT : '#' ~[\r\n]* -> channel(HIDDEN) ;

NEWLINE : [\r\n]+ -> skip ;

WS : [ \t]+ -> skip ;