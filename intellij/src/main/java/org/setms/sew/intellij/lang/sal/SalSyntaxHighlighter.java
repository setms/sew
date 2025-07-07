package org.setms.sew.intellij.lang.sal;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class SalSyntaxHighlighter extends SyntaxHighlighterBase {

  @Override
  public @NotNull Lexer getHighlightingLexer() {
    return new FlexAdapter(new SalLexer(null));
  }

  @Override
  public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
    return pack(toTextAttributesKey(tokenType));
  }

  private TextAttributesKey toTextAttributesKey(IElementType tokenType) {
    if (tokenType == TokenType.WHITE_SPACE || tokenType == SalElementTypes.NEWLINE) {
      return null;
    }
    if (tokenType == TokenType.BAD_CHARACTER) {
      return DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE;
    }
    if (tokenType == SalElementTypes.DOT) {
      return DefaultLanguageHighlighterColors.DOT;
    }
    if (tokenType == SalElementTypes.LBRACK || tokenType == SalElementTypes.RBRACK) {
      return DefaultLanguageHighlighterColors.BRACKETS;
    }
    if (tokenType == SalElementTypes.LPAREN || tokenType == SalElementTypes.RPAREN) {
      return DefaultLanguageHighlighterColors.PARENTHESES;
    }
    if (tokenType == SalElementTypes.COMMENT) {
      return DefaultLanguageHighlighterColors.LINE_COMMENT;
    }
    if (tokenType == SalElementTypes.STRING) {
      return DefaultLanguageHighlighterColors.STRING;
    }
    if (tokenType == SalElementTypes.LBRACE || tokenType == SalElementTypes.RBRACE) {
      return DefaultLanguageHighlighterColors.BRACES;
    }
    if (tokenType == SalElementTypes.EQ || tokenType == SalElementTypes.COMMA) {
      return DefaultLanguageHighlighterColors.OPERATION_SIGN;
    }
    if (tokenType == SalElementTypes.PACKAGE || tokenType == SalElementTypes.TYPE) {
      return DefaultLanguageHighlighterColors.KEYWORD;
    }
    if (tokenType == SalElementTypes.NAME) {
      return DefaultLanguageHighlighterColors.CLASS_NAME;
    }
    if (tokenType == SalElementTypes.IDENTIFIER) {
      return DefaultLanguageHighlighterColors.IDENTIFIER;
    }
    return null;
  }
}
