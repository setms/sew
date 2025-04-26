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
    return new FlexAdapter(new SewLexer((java.io.Reader) null));
  }

  @Override
  public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
    if (tokenType == SewElementTypes.STRING) {
      return pack(DefaultLanguageHighlighterColors.STRING);
    }
    if (tokenType == SewElementTypes.COMMENT) {
      return pack(DefaultLanguageHighlighterColors.LINE_COMMENT);
    }
    if (tokenType == SewElementTypes.IDENTIFIER) {
      return pack(DefaultLanguageHighlighterColors.IDENTIFIER);
    }
    if (tokenType == SewElementTypes.EQ
        || tokenType == SewElementTypes.COMMA
        || tokenType == SewElementTypes.DOT
        || tokenType == SewElementTypes.LBRACE
        || tokenType == SewElementTypes.RBRACE
        || tokenType == SewElementTypes.LBRACK
        || tokenType == SewElementTypes.RBRACK) {
      return pack(DefaultLanguageHighlighterColors.OPERATION_SIGN);
    }
    if (tokenType == SewElementTypes.PACKAGE) {
      return pack(DefaultLanguageHighlighterColors.KEYWORD);
    }
    return TextAttributesKey.EMPTY_ARRAY;
  }
}
