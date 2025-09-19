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
OBJECT_NAME=[A-Z][a-zA-Z0-9]*
STRING=\"([^\"\r\n]*)\"
TYPE=(aggregate|alternative|businessRequirement|calendarEvent|clockEvent|command|component|components|decision|domain|entity|event|externalSystem|field|hotspot|module|modules|owner|policy|readModel|scenario|screen|scope|subdomain|term|useCase|user|userRequirement|valueObject)

WHITE_SPACE=[ \t]+
NEWLINE=[\r\n]

%%

{WHITE_SPACE}     { return TokenType.WHITE_SPACE; }
{NEWLINE}         { return NEWLINE; }

{TYPE}            { return TYPE; }
{OBJECT_NAME}     { return OBJECT_NAME; }
{STRING}          { return STRING; }
{IDENTIFIER}      { return IDENTIFIER; }

"|"               { return PIPE; }
"-"               { return DASH; }
","               { return COMMA; }
"."               { return DOT; }
"="               { return EQ; }
"("               { return LPAREN; }
")"               { return RPAREN; }

.                 { return TokenType.BAD_CHARACTER; }
