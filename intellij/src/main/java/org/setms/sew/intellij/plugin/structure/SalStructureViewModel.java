package org.setms.sew.intellij.plugin.structure;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.TextEditorBasedStructureViewModel;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.intellij.lang.sal.SalObject;
import org.setms.sew.intellij.lang.sal.SalProperty;

public class SalStructureViewModel extends TextEditorBasedStructureViewModel {

  public SalStructureViewModel(PsiFile file) {
    super(file);
  }

  @Override
  public @NotNull StructureViewTreeElement getRoot() {
    return new SalStructureViewElement(getPsiFile());
  }

  @Override
  protected Class<?> @NotNull [] getSuitableClasses() {
    return new Class[] {SalObject.class, SalProperty.class};
  }
}
