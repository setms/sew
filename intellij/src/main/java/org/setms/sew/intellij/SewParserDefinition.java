package org.setms.sew.intellij;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

public class SewParserDefinition implements ParserDefinition {

  public static final IFileElementType FILE = new IFileElementType(SewLanguage.INSTANCE);

  @Override
  public @NotNull Lexer createLexer(Project project) {
    return new SewLexerAdapter();
  }

  @Override
  public @NotNull PsiParser createParser(Project project) {
    return (root, builder) -> {
      var marker = builder.mark();
      marker.done(root);
      return builder.getTreeBuilt(); // Return the ASTNode
    };
  }

  @Override
  public @NotNull IFileElementType getFileNodeType() {
    return FILE;
  }

  @Override
  public @NotNull TokenSet getWhitespaceTokens() {
    return TokenSet.create(SewTokenTypes.WS);
  }

  @Override
  public @NotNull TokenSet getCommentTokens() {
    return TokenSet.create(SewTokenTypes.COMMENT);
  }

  @Override
  public @NotNull TokenSet getStringLiteralElements() {
    return TokenSet.create(SewTokenTypes.STRING);
  }

  @Override
  public @NotNull PsiElement createElement(ASTNode node) {
    return new ASTWrapperPsiElement(node);
  }

  @Override
  public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
    return new SewFile(viewProvider);
  }

  @Override
  public @NotNull SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
    return SpaceRequirements.MAY;
  }
}
