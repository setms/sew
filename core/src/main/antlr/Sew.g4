grammar Sew;

sew : scope? object+ ;

scope : PACKAGE qualifiedName NEWLINE ;

qualifiedName : IDENTIFIER (DOT IDENTIFIER)* ;

object : TYPE OBJECT_NAME LBRACE NEWLINE property* RBRACE NEWLINE ;

property : IDENTIFIER EQ (item | list)  NEWLINE ;

list : LBRACK NEWLINE? (item (COMMA NEWLINE? item)*)? NEWLINE? RBRACK ;

item : OBJECT_NAME
     | STRING
     | typedReference
     ;

typedReference : TYPE LPAREN OBJECT_NAME (COMMA attribute)* RPAREN ;

attribute : IDENTIFIER EQ attributeValue ;

attributeValue : TYPE LPAREN OBJECT_NAME RPAREN ;


PACKAGE      : 'package';
TYPE         : 'aggregate'
             | 'alternative'
             | 'boundedContext'
             | 'businessRequirement'
             | 'calendarEvent'
             | 'clockEvent'
             | 'command'
             | 'contextMap'
             | 'decision'
             | 'entity'
             | 'event'
             | 'externalSystem'
             | 'field'
             | 'hotspot'
             | 'owner'
             | 'policy'
             | 'readModel'
             | 'scenario'
             | 'screen'
             | 'scope'
             | 'term'
             | 'useCase'
             | 'user'
             | 'userRequirement'
             | 'valueObject';

OBJECT_NAME  : [A-Z] [a-zA-Z0-9]*;
IDENTIFIER   : [a-z] [a-zA-Z]*;
STRING       : '"' (~["\r\n])* '"';

COMMENT      : '#' ~[\r\n]*[\r\n]+ -> skip;

COMMA        : ',';
DOT          : '.';
EQ           : '=';
LBRACE       : '{';
RBRACE       : '}';
LBRACK       : '[';
RBRACK       : ']';
LPAREN       : '(';
RPAREN       : ')';

WHITE_SPACE  : [ \t]+ -> skip;
NEWLINE      : [\r\n]+;
