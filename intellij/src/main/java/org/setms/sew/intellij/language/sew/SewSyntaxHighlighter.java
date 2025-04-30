package org.setms.sew.intellij.language.sew;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.intellij.SewElementTypes;

public class SewSyntaxHighlighter extends SyntaxHighlighterBase {

  @Override
  public @NotNull Lexer getHighlightingLexer() {
    return new FlexAdapter(new SewLexer(null));
  }

  @Override
  public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
    return pack(toTextAttributesKey(tokenType));
  }

  private TextAttributesKey toTextAttributesKey(IElementType tokenType) {
    if (tokenType == TokenType.WHITE_SPACE || tokenType == SewElementTypes.NEWLINE) {
      return null;
    }
    if (tokenType == TokenType.BAD_CHARACTER) {
      return DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE;
    }
    if (tokenType == SewElementTypes.DOT) {
      return DefaultLanguageHighlighterColors.DOT;
    }
    if (tokenType == SewElementTypes.LBRACK || tokenType == SewElementTypes.RBRACK) {
      return DefaultLanguageHighlighterColors.BRACKETS;
    }
    if (tokenType == SewElementTypes.LPAREN || tokenType == SewElementTypes.RPAREN) {
      return DefaultLanguageHighlighterColors.PARENTHESES;
    }
    if (tokenType == SewElementTypes.COMMENT) {
      return DefaultLanguageHighlighterColors.LINE_COMMENT;
    }
    if (tokenType == SewElementTypes.STRING) {
      return DefaultLanguageHighlighterColors.STRING;
    }
    if (tokenType == SewElementTypes.LBRACE || tokenType == SewElementTypes.RBRACE) {
      return DefaultLanguageHighlighterColors.BRACES;
    }
    if (tokenType == SewElementTypes.EQ || tokenType == SewElementTypes.COMMA) {
      return DefaultLanguageHighlighterColors.OPERATION_SIGN;
    }
    if (tokenType == SewElementTypes.PACKAGE || tokenType == SewElementTypes.TYPE) {
      return DefaultLanguageHighlighterColors.KEYWORD;
    }
    if (tokenType == SewElementTypes.NAME) {
      return DefaultLanguageHighlighterColors.CLASS_NAME;
    }
    if (tokenType == SewElementTypes.IDENTIFIER) {
      return DefaultLanguageHighlighterColors.IDENTIFIER;
    }
    return null;
  }
}
