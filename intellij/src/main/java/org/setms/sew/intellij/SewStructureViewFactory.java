package org.setms.sew.intellij;

import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SewStructureViewFactory implements PsiStructureViewFactory {

  @Override
  public @Nullable StructureViewBuilder getStructureViewBuilder(@NotNull PsiFile psiFile) {
    if (psiFile.getLanguage().getID().equals(SewLanguage.ID)) {
      return new SewStructureViewBuilder(psiFile);
    }
    return null;
  }
}
