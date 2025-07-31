package org.setms.sew.intellij.modules;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.intellij.editor.EditorWithPreviewProvider;

public class ModulesEditorProvider extends EditorWithPreviewProvider {

  public ModulesEditorProvider() {
    super(ModulesFileType.class);
  }

  @Override
  public @NotNull @NonNls String getEditorTypeId() {
    return "sew-modules-editor";
  }
}
