package org.setms.sew.intellij;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

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
    if (tokenType == SewElementTypes.IDENTIFIER) {
      return DefaultLanguageHighlighterColors.IDENTIFIER;
    }
    if (tokenType == SewElementTypes.STRING) {
      return DefaultLanguageHighlighterColors.STRING;
    }
    if (tokenType == SewElementTypes.LBRACE || tokenType == SewElementTypes.RBRACE) {
      return DefaultLanguageHighlighterColors.BRACES;
    }
    if (tokenType == SewElementTypes.EQ) {
      return DefaultLanguageHighlighterColors.OPERATION_SIGN;
    }
    if (tokenType == SewElementTypes.PACKAGE) {
      return DefaultLanguageHighlighterColors.KEYWORD;
    }
    if (tokenType == SewElementTypes.DOT) {
      return DefaultLanguageHighlighterColors.DOT;
    }
    if (tokenType == SewElementTypes.LBRACK || tokenType == SewElementTypes.RBRACK) {
      return DefaultLanguageHighlighterColors.BRACKETS;
    }
    if (tokenType == SewElementTypes.COMMENT) {
      return DefaultLanguageHighlighterColors.LINE_COMMENT;
    }
    return null;
  }
}
