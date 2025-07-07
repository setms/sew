package org.setms.sew.intellij.structure;

import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.setms.sew.intellij.lang.sal.SalLanguage;

public class SalStructureViewFactory implements PsiStructureViewFactory {

  @Override
  public @Nullable StructureViewBuilder getStructureViewBuilder(@NotNull PsiFile psiFile) {
    if (psiFile.getLanguage().getID().equals(SalLanguage.ID)) {
      return new SalStructureViewBuilder(psiFile);
    }
    return null;
  }
}
