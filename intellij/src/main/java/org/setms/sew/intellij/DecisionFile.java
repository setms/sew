package org.setms.sew.intellij;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

public class DecisionFile extends PsiFileBase {

  public DecisionFile(@NotNull FileViewProvider viewProvider) {
    super(viewProvider, SewLanguage.INSTANCE);
  }

  @Override
  public @NotNull FileType getFileType() {
    return DecisionFileType.INSTANCE;
  }
}
