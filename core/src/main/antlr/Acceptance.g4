grammar Acceptance;

test: sut variables scenarios EOF;

sut: table;

variables: table;

scenarios: table;

table
    : header row+ NEWLINE*
    ;

header
    : PIPE WHITE_SPACE? heading (PIPE heading)* PIPE NEWLINE separator
    ;

row
    : PIPE WHITE_SPACE? cell (PIPE cell)* PIPE NEWLINE
    ;

heading
    : IDENTIFIER
    ;

cell
    : expressionList
    | STRING
    | IDENTIFIER
    ;

expressionList
    : expression (COMMA expression)*
    ;

expression
    : IDENTIFIER EQ value
    ;

value
    : STRING
    | IDENTIFIER
    ;

separator
    : PIPE WHITE_SPACE? DASH+ (PIPE DASH+)* PIPE NEWLINE
    ;


PIPE        : '|';
DASH        : '-';
COMMA       : ',';
EQ          : '=';
LPAREN      : '(';
RPAREN      : ')';

STRING      : '"' (~["\r\n])* '"';
IDENTIFIER  : [a-zA-Z_][a-zA-Z_0-9.]*;
WHITE_SPACE : [ \t]+ -> skip;
NEWLINE     : '\r'? '\n';
