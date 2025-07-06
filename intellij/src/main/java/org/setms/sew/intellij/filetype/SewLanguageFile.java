package org.setms.sew.intellij.filetype;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.intellij.lang.sew.SewLanguage;

public class SewLanguageFile extends PsiFileBase {

  private final SewLanguageFileType fileType;

  protected SewLanguageFile(@NotNull FileViewProvider viewProvider, SewLanguageFileType fileType) {
    super(viewProvider, SewLanguage.INSTANCE);
    this.fileType = fileType;
  }

  @Override
  public @NotNull FileType getFileType() {
    return fileType;
  }
}
