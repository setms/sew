grammar Sew;

sew: scope? object+ EOF;

scope: 'package' qualifiedName;

qualifiedName: name ('.' name)*;

name: IDENTIFIER;

object: type name '{' property* '}';

type: IDENTIFIER;

property: name '=' (item | list);

list: '[' (item (',' item)*)? ']';

item: reference | string;

reference: IDENTIFIER;

string: STRING;

IDENTIFIER: [a-zA-Z0-9_()]+;

STRING: '"' (~["\r\n])* '"';

WS: [ \t\r\n]+ -> skip;
COMMENT: '#' ~[\r\n]* -> skip;
