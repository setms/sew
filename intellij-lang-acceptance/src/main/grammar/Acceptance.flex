package org.setms.sew.intellij.lang.acceptance;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;
import static org.setms.sew.intellij.lang.acceptance.AcceptanceElementTypes.*;
%%

%class AcceptanceLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType

%{
%}

IDENTIFIER=[a-z][a-zA-Z_]*
STRING=\"([^\"\r\n]*)\"
TYPE=(aggregate|alternative|businessRequirement|calendarEvent|clockEvent|command|decision|domain|entity|event|externalSystem|field|hotspot|module|modules|owner|policy|readModel|scenario|screen|scope|subdomain|term|useCase|user|userRequirement|valueObject)
NAME=[A-Z][a-zA-Z0-9]*
WHITE_SPACE=[ \t]+
NEWLINE=[\r\n]

%%

{WHITE_SPACE}                { return TokenType.WHITE_SPACE; }
{NEWLINE}                   { return NEWLINE; }
{TYPE}                      { return TYPE; }
{NAME}                      { return NAME; }

"|"                          { return PIPE; }
"-"                          { return DASH; }
","                          { return COMMA; }
"."                          { return DOT; }
"="                          { return EQ; }
"("                          { return LPAREN; }
")"                          { return RPAREN; }

{STRING}                    { return STRING; }
{IDENTIFIER}                { return IDENTIFIER; }

.                           { return TokenType.BAD_CHARACTER; }
