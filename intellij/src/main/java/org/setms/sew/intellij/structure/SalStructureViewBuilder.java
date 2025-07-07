package org.setms.sew.intellij.structure;

import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SalStructureViewBuilder extends TreeBasedStructureViewBuilder {

  private final PsiFile file;

  public SalStructureViewBuilder(PsiFile file) {
    this.file = file;
  }

  @Override
  public @NotNull StructureViewModel createStructureViewModel(@Nullable Editor editor) {
    return new SalStructureViewModel(file);
  }
}
