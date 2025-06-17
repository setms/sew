package org.setms.sew.intellij.filetype;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.intellij.lang.acceptance.AcceptanceLanguage;

public class AcceptanceTestFile extends PsiFileBase {

  public AcceptanceTestFile(@NotNull FileViewProvider viewProvider) {
    super(viewProvider, AcceptanceLanguage.INSTANCE);
  }

  @Override
  public @NotNull FileType getFileType() {
    return AcceptanceTestFileType.INSTANCE;
  }
}
