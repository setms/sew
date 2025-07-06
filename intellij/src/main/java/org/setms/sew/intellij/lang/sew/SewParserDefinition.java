package org.setms.sew.intellij.lang.sew;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.intellij.filetype.SewLanguageFileType;

public class SewParserDefinition implements ParserDefinition {

  private static final IFileElementType FILE = new IFileElementType(SewLanguage.INSTANCE);

  private static final List<SewLanguageFileType> fileTypes = new ArrayList<>();

  public static void addFileType(SewLanguageFileType fileType) {
    fileTypes.add(fileType);
  }

  @Override
  public @NotNull Lexer createLexer(Project project) {
    return new FlexAdapter(new SewLexer(null));
  }

  @Override
  public @NotNull PsiParser createParser(Project project) {
    return new SewParser();
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
    return TokenSet.create(SewElementTypes.COMMENT);
  }

  @Override
  public @NotNull TokenSet getStringLiteralElements() {
    return TokenSet.create(SewElementTypes.STRING);
  }

  @Override
  public @NotNull PsiElement createElement(ASTNode node) {
    return new ASTWrapperPsiElement(node);
  }

  @Override
  public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
    String extension = viewProvider.getFileType().getDefaultExtension();
    return fileTypes.stream()
        .filter(type -> type.getDefaultExtension().equals(extension))
        .findFirst()
        .map(fileType -> fileType.createFile(viewProvider))
        .orElseThrow(
            () -> new UnsupportedOperationException("Unknown file extension: " + extension));
  }

  @Override
  public @NotNull SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
    return SpaceRequirements.MAY;
  }
}
