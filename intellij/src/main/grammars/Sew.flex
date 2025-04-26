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

WHITE_SPACE=[ \t\r\n]+
IDENTIFIER=[a-zA-Z0-9_()]+
STRING=\"([^\"\r\n]*)\"
COMMENT="#"[^\r\n]*

%%

{WHITE_SPACE}               { return TokenType.WHITE_SPACE; }
{COMMENT}                   { return COMMENT; }

"package"                   { return PACKAGE; }

"="                         { return EQ; }
"."                         { return DOT; }
","                         { return COMMA; }
"{"                         { return LBRACE; }
"}"                         { return RBRACE; }
"["                         { return LBRACK; }
"]"                         { return RBRACK; }

{STRING}                    { return STRING; }
{IDENTIFIER}                { return IDENTIFIER; }

.                           { return TokenType.BAD_CHARACTER; }
