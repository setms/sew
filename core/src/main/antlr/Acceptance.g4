grammar Acceptance;

test: sut NEWLINE+ variables NEWLINE+ scenarios NEWLINE* EOF;

sut: table;

variables: table;

scenarios: table;

table
    : header row+
    ;

header
    : PIPE heading (PIPE heading)* PIPE NEWLINE separator
    ;

heading
    : IDENTIFIER
    | TYPE
    ;

separator
    : PIPE DASH+ (PIPE DASH+)* PIPE NEWLINE
    ;

row
    : PIPE cell? (PIPE cell?)* PIPE NEWLINE
    ;

cell
    : typedReference
    | qualifiedName
    | fields
    | OBJECT_NAME
    | IDENTIFIER
    | STRING
    | TYPE
    ;

typedReference : TYPE LPAREN OBJECT_NAME RPAREN ;

qualifiedName : (IDENTIFIER DOT)+ OBJECT_NAME;

fields : (field COMMA)* field;

field: OBJECT_NAME EQ (IDENTIFIER | STRING);

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
IDENTIFIER   : [a-z] [a-zA-Z_]*;
STRING       : '"' (~["\r\n])* '"';

COMMA        : ',';
DOT          : '.';
EQ           : '=';
LPAREN       : '(';
RPAREN       : ')';

WHITE_SPACE  : [ \t]+ -> skip;
NEWLINE      : [\r\n];
