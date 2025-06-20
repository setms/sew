package org.setms.sew.intellij.usecase;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.intellij.lang.sew.SewLanguage;

public class UseCaseFile extends PsiFileBase {

  public UseCaseFile(@NotNull FileViewProvider viewProvider) {
    super(viewProvider, SewLanguage.INSTANCE);
  }

  @Override
  public @NotNull FileType getFileType() {
    return UseCaseFileType.INSTANCE;
  }
}
