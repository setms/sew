package org.setms.sew.intellij.lang;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.setms.km.domain.model.tool.BaseTool;
import org.setms.sew.intellij.plugin.filetype.BaseLanguageFileType;

public abstract class BaseParserDefinition implements ParserDefinition {

  private static final List<BaseLanguageFileType> fileTypes = new ArrayList<>();

  public static void addFileType(BaseLanguageFileType fileType) {
    fileTypes.add(fileType);
  }

  public static Optional<BaseTool> toolFor(VirtualFile file) {
    return Optional.ofNullable(file)
        .map(VirtualFile::getFileType)
        .filter(BaseLanguageFileType.class::isInstance)
        .map(BaseLanguageFileType.class::cast)
        .map(BaseLanguageFileType::getTool);
  }

  @Override
  public @NotNull PsiElement createElement(ASTNode node) {
    return new ASTWrapperPsiElement(node);
  }

  @Override
  public @NotNull SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
    return SpaceRequirements.MAY;
  }

  public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
    String extension = viewProvider.getFileType().getDefaultExtension();
    return fileTypes.stream()
        .filter(type -> type.getDefaultExtension().equals(extension))
        .findFirst()
        .map(fileType -> fileType.createFile(viewProvider))
        .orElseThrow(
            () -> new UnsupportedOperationException("Unknown file extension: " + extension));
  }
}
