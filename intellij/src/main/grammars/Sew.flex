package org.setms.sew.intellij;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;
import static org.setms.sew.intellij.SewElementTypes.*;
%%

%class SewLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType

%{
%}

COMMENT="#"[^\r\n]*[\r\n]+
IDENTIFIER=[a-z][a-zA-Z]*
STRING=\"([^\"\r\n]*)\"
TYPE=(aggregate|alternative|businessRequirement|command|decision|entity|event|field|owner|readModel|scenario|screen|scope|term|useCase|user|userRequirement|valueObject)
NAME=[A-Z][a-zA-Z0-9]*
WHITE_SPACE=[ \t]+
NEWLINE=[\r\n]+

%%

{WHITE_SPACE}               { return TokenType.WHITE_SPACE; }
{COMMENT}                   { return COMMENT; }
{NEWLINE}                   { return NEWLINE; }

"package"                   { return PACKAGE; }

{TYPE}                      { return TYPE; }
{NAME}                      { return NAME; }

"="                         { return EQ; }
"."                         { return DOT; }
","                         { return COMMA; }
"{"                         { return LBRACE; }
"}"                         { return RBRACE; }
"["                         { return LBRACK; }
"]"                         { return RBRACK; }
"("                         { return LPAREN; }
")"                         { return RPAREN; }

{STRING}                    { return STRING; }
{IDENTIFIER}                { return IDENTIFIER; }

.                           { return TokenType.BAD_CHARACTER; }
