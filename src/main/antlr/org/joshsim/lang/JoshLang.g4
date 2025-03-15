grammar JoshLang;

@header {
  package org.joshsim.lang.antlr;
}

// Base values
STR_: '"' ~[",]* '"';

COMMENT: '#' ~[\r\n]* -> channel(HIDDEN);

FLOAT_: [0-9]* '.' [0-9]+;

INTEGER_: [0-9]+;

// Syntax
ADD_: '+';
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
SUB_: '-';
MULT_: '*';
NEQ_: '!=';
PERCENT_: '%';
POW_: '^';
RBRAC_: ']';
RCURLY_: '}';
RPAREN_: ')';

// Keywords
ALIAS_: 'alias';
ALL_: 'all';
AND_: 'and';
AS_: 'as';
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
FALSE_: 'false';
FORCE_: 'force';
FROM_: 'from';
HERE_: 'here';
IF_: 'if';
IMPORT_: 'import';
INIT_: 'init';
LATITUDE_: 'latitude';
LIMIT_: 'limit';
LONGITUDE_: 'longitude';
MANAGEMENT_: 'management';
MAP_: 'map';
MEAN_: 'mean';
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
STD_: 'std';
STEP_: 'step';
TO_: 'to';
TRUE_: 'true';
UNIFORM_: 'uniform';
UNIT_: 'unit';
WITH_: 'with';
WITHIN_: 'within';
WITHOUT_: 'without';

// Dynamic
IDENTIFIER_: [A-Za-z][A-Za-z0-9]*;

// Whitespace
WHITE_SPACE: [ \u000B\t\r\n] -> channel(HIDDEN);

// Identifiers
nakedIdentifier: (IDENTIFIER_|INIT_|START_|STEP_|END_|HERE_|CURRENT_|PRIOR_|STATE_);
identifier: nakedIdentifier (DOT_ (nakedIdentifier))*;

// Values
number: (SUB_|ADD_)? (FLOAT_ | INTEGER_);

unitsValue: number (identifier|PERCENT_);

string: STR_;

// Sample targets
sampleTarget: UNIFORM_ FROM_ low=expression TO_ high=expression # uniformSample
  | NORMAL_ WITH_ MEAN_ OF_ mean=expression STD_ OF_ std=expression # normalSample
  | expression # expressionSample
  ;

// Statement
expression: unitsValue # simpleExpression
  | number # simpleNumber
  | string # simpleString
  | bool # simpleBoolExpression
  | ALL_ # allExpression
  | identifier # identifierExpression
  | unitsValue (LATITUDE_ | LONGITUDE_) COMMA_ unitsValue (LATITUDE_ | LONGITUDE_) # position
  | operand=expression AS_ target=identifier # cast
  | FORCE_ operand=expression AS_ target=identifier # castForce
  | name=funcName LPAREN_ expression (COMMA_ expression)* RPAREN_ # functionCall
  | left=expression POW_ right=expression # powExpression
  | left=expression op=(MULT_ | DIV_) right=expression # multiplyExpression
  | left=expression op=(ADD_ | SUB_) right=expression # additionExpression
  | LPAREN_ expression RPAREN_ # parenExpression
  | identifier LBRAC_ expression RBRAC_ # slice
  | SAMPLE_ target=sampleTarget # sampleSimple
  | SAMPLE_ count=expression FROM_ target=sampleTarget # sampleParam
  | SAMPLE_ count=expression FROM_ target=sampleTarget replace=(WITH_ | WITHOUT_) REPLACEMENT_ # sampleParamReplacement
  | LIMIT_ operand=expression TO_ LBRAC_ limit=expression COMMA_ RBRAC_ # limitMaxExpression
  | LIMIT_ operand=expression TO_ LBRAC_ COMMA_ limit=expression RBRAC_ # limitMinExpression
  | LIMIT_ operand=expression TO_ LBRAC_ lower=expression COMMA_ upper=expression RBRAC_ # limitBoundExpression
  | MAP_ operand=expression FROM_ LBRAC_ fromlow=expression COMMA_ fromhigh=expression RBRAC_ TO_ LBRAC_ tolow=expression COMMA_ tohigh=expression RBRAC_ # mapLinear
  | MAP_ operand=expression FROM_ LBRAC_ fromlow=expression COMMA_ fromhigh=expression RBRAC_ TO_ LBRAC_ tolow=expression COMMA_ tohigh=expression RBRAC_ method=identifier # mapParam
  | CREATE_ target=identifier # createSingleExpression
  | CREATE_ count=expression OF_ target=identifier # createVariableExpression
  | left=expression op=(NEQ_ | GT_ | LT_ | EQEQ_ | LTEQ_ | GTEQ_) right=expression # condition
  | pos=expression IF_ cond=expression ELSE_ neg=expression # conditional
  ;

funcName: (MEAN_ | STD_) # reservedFuncName
  | identifier # identifierFuncName
  ;

assignment: CONST_ identifier EQ_ expression;

return: RETURN_ expression;

statement: (assignment | return);

bool: (TRUE_ | FALSE_);

// Callables
lambda: expression;

fullBody: LCURLY_ statement* RCURLY_;

callable: (fullBody | lambda);

// Event handlers
eventHandler: identifier EQ_ callable;

conditionalEventSelector: COLON_ (IF_|ELIF_) LPAREN_ expression RPAREN_;

elseEventSelector: COLON_ ELSE_;

eventHandlerGroupMember: (conditionalEventSelector|elseEventSelector)? EQ_ callable;

eventHandlerGroup: identifier eventHandlerGroupMember*;

eventHandlerGeneral: (eventHandler | eventHandlerGroup);

// Regular stanzas
stateStanza: START_ STATE_ STR_ eventHandlerGeneral* END_ STATE_;

agentStanzaType: (DISTURBANCE_ | EXTERNAL_ | ORGANISM_ | MANAGEMENT_ | PATCH_ | SIMULATION_);

agentStanza: START_ agentStanzaType identifier (eventHandlerGeneral | stateStanza)* END_ agentStanzaType;

// Unit definitions
unitConversion: ALIAS_ identifier # noopConversion
  | identifier EQ_ expression # activeConversion
  ;

unitStanza: START_ UNIT_ name=identifier unitConversion* END_ UNIT_;

// Imports and config
configStatement: CONFIG_ expression;

importStatement: IMPORT_ expression;

// Program
program: (configStatement | importStatement | unitStanza | agentStanza)*;
