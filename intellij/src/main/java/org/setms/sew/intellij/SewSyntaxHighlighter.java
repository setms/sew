package org.setms.sew.intellij;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class SewSyntaxHighlighter extends SyntaxHighlighterBase {

  @Override
  public @NotNull Lexer getHighlightingLexer() {
    return new SewLexerAdapter();
  }

  @Override
  public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
    if (tokenType == SewTokenTypes.STRING) {
      return pack(DefaultLanguageHighlighterColors.STRING);
    } else if (tokenType == SewTokenTypes.IDENTIFIER) {
      return pack(DefaultLanguageHighlighterColors.IDENTIFIER);
    } else if (tokenType == SewTokenTypes.COMMENT) {
      return pack(DefaultLanguageHighlighterColors.LINE_COMMENT);
    } else if (tokenType == SewTokenTypes.EQ
        || tokenType == SewTokenTypes.COMMA
        || tokenType == SewTokenTypes.DOT
        || tokenType == SewTokenTypes.LBRACE
        || tokenType == SewTokenTypes.RBRACE
        || tokenType == SewTokenTypes.LBRACK
        || tokenType == SewTokenTypes.RBRACK) {
      return pack(DefaultLanguageHighlighterColors.OPERATION_SIGN);
    } else if (tokenType == SewTokenTypes.PACKAGE) {
      return pack(DefaultLanguageHighlighterColors.KEYWORD);
    }
    return TextAttributesKey.EMPTY_ARRAY;
  }
}
