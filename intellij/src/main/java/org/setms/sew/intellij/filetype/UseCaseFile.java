package org.setms.sew.intellij.filetype;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.intellij.language.sew.SewLanguage;

public class UseCaseFile extends PsiFileBase {

  public UseCaseFile(@NotNull FileViewProvider viewProvider) {
    super(viewProvider, SewLanguage.INSTANCE);
  }

  @Override
  public @NotNull FileType getFileType() {
    return UseCaseFileType.INSTANCE;
  }
}
