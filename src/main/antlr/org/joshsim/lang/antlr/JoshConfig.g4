/**
 * ANTLR grammar for Josh configuration files (.jshc format).
 *
 * <div>
 * ANTLR grammar for a small configuration language. Supports:
 *
 * <ul>
 *   <li>One variable per line: variableName = value units</li>
 *   <li>Comments using # character (all text after # ignored until newline)</li>
 *   <li>Empty lines allowed and ignored</li>
 *   <li>Whitespace flexible around equals sign</li>
 *   <li>Variable names follow Josh language patterns</li>
 *   <li>Values must be valid EngineValue format (number + optional units)</li>
 * </ul>
 *
 * These configuration strings may be provided in jshc files.
 * </div>
 *
 * @license BSD-3-Clause
 */

grammar JoshConfig;

@header {
package org.joshsim.lang.antlr;
}

// Lexer rules (sorted alphabetically)
COMMENT : '#' ~[\r\n]* -> channel(HIDDEN) ;

EQUALS : '=' ;

ID : [A-Za-z/][A-Za-z0-9%/]* ;

NEWLINE : [\r\n]+ -> skip ;

NUMBER : [+-]? ([0-9]+ ('.' [0-9]+)? | '.' [0-9]+) ;

WS : [ \t]+ -> skip ;

// Parser rules (sorted alphabetically)
assignment : ID EQUALS value ;

comment : COMMENT ;

config : configLine* EOF ;

configLine 
    : assignment
    | comment
    | emptyLine
    ;

emptyLine : NEWLINE ;

value : NUMBER ID? ;
