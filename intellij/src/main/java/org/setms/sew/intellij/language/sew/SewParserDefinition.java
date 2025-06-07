package org.setms.sew.intellij.language.sew;

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
import org.jetbrains.annotations.NotNull;
import org.setms.sew.intellij.SewElementTypes;
import org.setms.sew.intellij.domain.DomainFile;
import org.setms.sew.intellij.filetype.AggregateFile;
import org.setms.sew.intellij.filetype.ClockEventFile;
import org.setms.sew.intellij.filetype.CommandFile;
import org.setms.sew.intellij.filetype.DecisionFile;
import org.setms.sew.intellij.filetype.EventFile;
import org.setms.sew.intellij.filetype.OwnerFile;
import org.setms.sew.intellij.filetype.PolicyFile;
import org.setms.sew.intellij.filetype.ReadModelFile;
import org.setms.sew.intellij.filetype.UserFile;
import org.setms.sew.intellij.modules.ModulesFile;
import org.setms.sew.intellij.usecase.UseCaseFile;

public class SewParserDefinition implements ParserDefinition {

  private static final IFileElementType FILE = new IFileElementType(SewLanguage.INSTANCE);

  @Override
  public @NotNull Lexer createLexer(Project project) {
    return new FlexAdapter(new SewLexer(null));
  }

  @Override
  public @NotNull PsiParser createParser(Project project) {
    return new org.setms.sew.intellij.SewParser();
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
    return switch (extension) {
      case "aggregate" -> new AggregateFile(viewProvider);
      case "clockEvent" -> new ClockEventFile(viewProvider);
      case "command" -> new CommandFile(viewProvider);
      case "decision" -> new DecisionFile(viewProvider);
      case "domain" -> new DomainFile(viewProvider);
      case "event" -> new EventFile(viewProvider);
      case "owner" -> new OwnerFile(viewProvider);
      case "modules" -> new ModulesFile(viewProvider);
      case "policy" -> new PolicyFile(viewProvider);
      case "readModel" -> new ReadModelFile(viewProvider);
      case "useCase" -> new UseCaseFile(viewProvider);
      case "user" -> new UserFile(viewProvider);
      default -> throw new UnsupportedOperationException("Unknown file extension: " + extension);
    };
  }

  @Override
  public @NotNull SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
    return SpaceRequirements.MAY;
  }
}
