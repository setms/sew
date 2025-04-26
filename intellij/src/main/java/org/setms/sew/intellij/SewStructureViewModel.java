package org.setms.sew.intellij;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.TextEditorBasedStructureViewModel;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class SewStructureViewModel extends TextEditorBasedStructureViewModel {

  public SewStructureViewModel(PsiFile file) {
    super(file);
  }

  @Override
  public @NotNull StructureViewTreeElement getRoot() {
    return new SewStructureViewElement(getPsiFile());
  }

  @Override
  protected Class<?> @NotNull [] getSuitableClasses() {
    return new Class[] { SewObject.class, SewProperty.class };
  }
}
