grammar Acceptance;

test:
    sut NEWLINE+ variables NEWLINE+ scenarios NEWLINE* EOF;


sut:
    sut_header separator sut_row;

sut_header:
    PIPE 'type' PIPE 'name' PIPE NEWLINE;

separator:
    PIPE DASH+ (PIPE DASH+)* PIPE NEWLINE;

sut_row:
    PIPE TYPE PIPE qualifiedName PIPE NEWLINE;

qualifiedName:
    (IDENTIFIER DOT)+ OBJECT_NAME;


variables:
    variables_header separator variables_row+;

variables_header:
    PIPE 'variable' PIPE 'type' PIPE 'definition' PIPE NEWLINE;

variables_row:
    PIPE item PIPE type PIPE definition? PIPE NEWLINE;

item:
    IDENTIFIER | TYPE;

type:
    IDENTIFIER | typedReference;

typedReference:
    TYPE LPAREN OBJECT_NAME RPAREN;

definition:
    constraints | fields;

constraints:
    (constraint COMMA)* constraint;

constraint:
    IDENTIFIER;

fields:
    (field COMMA)* field;

field:
    OBJECT_NAME EQ (IDENTIFIER | STRING);


scenarios:
    scenario_header separator scenario_row+;

scenario_header:
    (PIPE item)+ PIPE NEWLINE;

scenario_row:
    PIPE STRING PIPE (list? PIPE)* NEWLINE;

list: item (COMMA item)*;


PIPE        : '|';
DASH        : '-';

TYPE         : 'aggregate'
             | 'alternative'
             | 'businessRequirement'
             | 'calendarEvent'
             | 'clockEvent'
             | 'command'
             | 'decision'
             | 'domain'
             | 'entity'
             | 'event'
             | 'externalSystem'
             | 'field'
             | 'hotspot'
             | 'module'
             | 'modules'
             | 'owner'
             | 'policy'
             | 'readModel'
             | 'scenario'
             | 'screen'
             | 'scope'
             | 'subdomain'
             | 'term'
             | 'useCase'
             | 'user'
             | 'userRequirement'
             | 'valueObject';

OBJECT_NAME  : [A-Z] [a-zA-Z0-9]*;
IDENTIFIER   : [a-z] [a-zA-Z0-9]*;
STRING       : '"' (~["\r\n])* '"';

COMMA        : ',';
DOT          : '.';
EQ           : '=';
LPAREN       : '(';
RPAREN       : ')';

WHITE_SPACE  : [ \t]+ -> skip;
NEWLINE      : [\r\n];
