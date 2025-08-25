/**
 * ANTLR grammar for Josh job variation specifications.
 *
 * <div>
 * ANTLR grammar for parsing job file variation specifications used in
 * grid search functionality. Supports:
 *
 * <ul>
 *   <li>Semicolon (;) separated file specifications: name1=path1;name2=path2</li>
 *   <li>Exactly one equals sign per specification</li>
 *   <li>Flexible whitespace handling</li>
 *   <li>Support for URI schemes and Windows file paths</li>
 *   <li>Future extension: comma-separated file lists within specifications</li>
 * </ul>
 *
 * Example: example.jshc=test_data/example_1.jshc;other.jshd=test_data/other_1.jshd
 * </div>
 *
 * @license BSD-3-Clause
 */

grammar JoshJobVariation;

@header {
package org.joshsim.lang.antlr;
}

// Lexer rules
SEMICOLON_ : ';' ;
EQUALS_ : '=' ;

// Match any characters except semicolon, equals, and whitespace for identifiers  
TEXT_ : ~[ \t\r\n;=]+ ;

// Whitespace handling (skip)
WS_ : [ \t\r\n]+ -> skip ;

// Parser rules  
jobVariation : fileSpec (SEMICOLON_ fileSpec)* EOF ;

fileSpec : filename EQUALS_ filepath ;

filename : TEXT_ ;

filepath : TEXT_ (EQUALS_ TEXT_)* ;