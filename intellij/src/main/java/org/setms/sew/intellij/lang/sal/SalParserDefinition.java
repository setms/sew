package org.setms.sew.intellij.lang.sal;

import com.intellij.lang.PsiParser;
import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.intellij.lang.BaseParserDefinition;

public class SalParserDefinition extends BaseParserDefinition {

  private static final IFileElementType FILE = new IFileElementType(SalLanguage.INSTANCE);

  @Override
  public @NotNull Lexer createLexer(Project project) {
    return new FlexAdapter(new SalLexer(null));
  }

  @Override
  public @NotNull PsiParser createParser(Project project) {
    return new SalParser();
  }

  @Override
  public @NotNull IFileElementType getFileNodeType() {
    return FILE;
  }

  @Override
  public @NotNull TokenSet getWhitespaceTokens() {
    return TokenSet.create(TokenType.WHITE_SPACE);
  }

  @Override
  public @NotNull TokenSet getCommentTokens() {
    return TokenSet.create(SalElementTypes.COMMENT);
  }

  @Override
  public @NotNull TokenSet getStringLiteralElements() {
    return TokenSet.create(SalElementTypes.STRING);
  }
}
