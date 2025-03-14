grammar JoshLang;

@header {
  package org.joshsim.lang;
}

// Base values
STR_: '"' ~[",]* '"';

WHITE_SPACE: [ \u000B\t\r\n] -> channel(HIDDEN);

COMMENT: '#' ~[\r\n]* -> channel(HIDDEN);

FLOAT_: [0-9]* '.' [0-9]+;

INTEGER_: [0-9]+;

// Syntax
COLON_: ':';
COMMA_: ',';
CONCAT_: '|';
DIV_: '/';
DOT_: '.';
EQ_: '=';
EQEQ_: '==';
GT_: '>';
GTEQ_: '>=';
HASH_: '#';
LBRAC_: '[';
LCURLY_: '{';
LPAREN_: '(';
LT_: '<';
LTEQ_: '<=';
MINUS_: '-';
MULT_: '*';
NEQ_: '!=';
PERCENT_: '%';
PLUS_: '+';
POW_: '^';
RBRAC_: ']';
RCURLY_: '}';
RPAREN_: ')';

// Keywords
ALIAS_: 'alias';
ALL_: 'all';
AND_: 'and';
AT_: 'at';
CONFIG_: 'config';
CONST_: 'const';
CREATE_: 'create';
CURRENT_: 'current';
DISTURBANCE_: 'disturbance';
ELIF_: 'elif';
ELSE_: 'else';
END_: 'end';
EXTERNAL_: 'external';
FROM_: 'from';
HERE_: 'here';
IF_: 'if';
INIT_: 'init';
LATITUDE_: 'latitude';
LIMIT_: 'limit';
LONGITUDE_: 'longitude';
MANAGEMENT_: 'management';
MAP_: 'map';
NORMAL_: 'normal';
OF_: 'of';
OR_: 'or';
ORGANISM_: 'organism';
PATCH_: 'patch';
PRIOR_: 'prior';
RADIAL_: 'radial';
REPLACEMENT_: 'replacement';
RETURN_: 'return';
SAMPLE_: 'sample';
SIMULATION_: 'simulation';
START_: 'start';
STATE_: 'state';
STEP_: 'step';
TO_: 'to';
UNIFORM_: 'uniform';
WITH_: 'with';
WITHIN_: 'within';
WITHOUT_: 'without';

// Dynamic
IDENTIFIER_: [A-Za-z][A-Za-z0-9]*;

// Identifiers
identifier: IDENTIFIER_;
nestedIdentifier: identifier (DOT_ identifier)*;

// Values
number: (MINUS_|PLUS_)? (FLOAT_ | INTEGER_);

unitsValue: number identifier;

string: STR_;

// Statement
expression: number # simpleNumber
  | string # simpleString
  | identifier # simpleIdentifier
  | unitsValue # simpleExpression
  | unitsValue (LATITUDE_ | LONGITUDE_) COMMA_ unitsValue (LATITUDE_ | LONGITUDE_) # position
  | (FORCE_)? operand=expression AS_ target=identifier # cast
  | name=identifier LPAREN_ expression (COMMA_ expression)* RPAREN_ # functionCall
  | left=expression POW_ right=expression # powExpression
  | left=expression op=(MULT_ | DIV_) right=expression # multiplyExpression
  | left=expression op=(ADD_ | SUB_) right=expression # additionExpression
  | LPAREN_ expression RPAREN_ # parenExpression
  | identifier LBRAC_ expression RBRAC_ # slice
  | SAMPLE_ target=expression # sampleSimple
  | SAMPLE_ count=expression FROM_ target=expression # sampleParam
  | SAMPLE_ count=expression FROM_ target=expression replace=(WITH_ | WITHOUT_) REPLACEMENT_ # sampleParamReplacement
  | LIMIT_ operand=expression TO_ LBRAC_ limit=expression COMMA_ RBRAC_ # limitMaxExpression
  | LIMIT_ operand=expression TO_ LBRAC_ COMMA_ limit=expression RBRAC_ # limitMinExpression
  | LIMIT_ operand=expression TO_ LBRAC_ lower=expression COMMA_ upper=expression RBRAC_ # limitBoundExpression
  | MAP_ operand=expression FROM_ LBRAC_ fromlow=expression COMMA_ fromhigh=expression RBRAC_ TO_ LBRAC_ tolow=expression COMMA_ tohigh=expression RBRAC_ # mapLinear
  | MAP_ operand=expression FROM_ LBRAC_ fromlow=expression COMMA_ fromhigh=expression RBRAC_ TO_ LBRAC_ tolow=expression COMMA_ tohigh=expression RBRAC_ method=identifier # mapParam
  | left=expression op=(NEQ_ | GT_ | LT_ | EQEQ_ | LTEQ_ | GTEQ_) right=expression # condition
  | pos=expression IF_ cond=expression ELSE_ neg=expression # conditional
  ;

assignment: CONST_ identifier EQ_ expression;

return: RETURN_ expression;

statement: (assignment | return);

// Callables
lambda: expression;

fullBody: LCURLY_ statement* RCURLY_;

callable: (lambda | fullBody);

// Event handlers
eventHandler: nestedIdentifier EQ_ callable;

eventSelector: COLON_ LAREN_ expression RPAREN_;

eventHandlerGroupMember: eventSelector EQ_ callable;

eventHandlerGroup: nestedIdentifier eventHandlerGroupMember*;

eventHandlerGeneral: (eventHandler | eventHandlerGroup);

// Regular stanzas
innerStanzaType: STATE_;

innerStanza: START_ innerStanzaType eventHandlerGeneral* END_ innerStanzaType;

agentStanzaType: (DISTURBANCE_ EXTERNAL_ | ORGANISM_ | MANAGEMENT_ | PATCH_ | SIMULATION_);

agentStanza: START_ agentStanzaType (eventHandlerGeneral | innerStanza)* END_ agentStanzaType;

// Unit definitions
unitConversion: ALIAS_ identifier # noopConversion
  | identifier = statement # activeConversion
  ;

unitStanza: START_ UNIT_ name=identifier unitConversion* END_ UNIT_;

// Imports and config
configStatment: CONFIG_ expression;

importStatment: IMPORT_ expression;

// Program
program: (configStatement | importStatement | unitStanza | agentStanza)*;
