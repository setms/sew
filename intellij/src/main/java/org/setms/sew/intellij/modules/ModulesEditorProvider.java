package org.setms.sew.intellij.modules;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.core.inbound.tool.ModulesTool;
import org.setms.sew.intellij.editor.EditorWithPreviewProvider;

public class ModulesEditorProvider extends EditorWithPreviewProvider {

  public ModulesEditorProvider() {
    super(new ModulesTool(), ModulesFileType.class);
  }

  @Override
  public @NotNull @NonNls String getEditorTypeId() {
    return "sew-modules-editor";
  }
}
