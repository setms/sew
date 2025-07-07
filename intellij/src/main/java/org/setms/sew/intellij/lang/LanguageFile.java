package org.setms.sew.intellij.lang;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

public class LanguageFile extends PsiFileBase {

  private final LanguageFileType fileType;

  public LanguageFile(@NotNull FileViewProvider viewProvider, LanguageFileType fileType) {
    super(viewProvider, fileType.getLanguage());
    this.fileType = fileType;
  }

  @Override
  public @NotNull FileType getFileType() {
    return fileType;
  }
}
