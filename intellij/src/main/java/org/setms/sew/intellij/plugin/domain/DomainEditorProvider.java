package org.setms.sew.intellij.plugin.domain;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.intellij.plugin.editor.EditorWithPreviewProvider;

public class DomainEditorProvider extends EditorWithPreviewProvider {

  public DomainEditorProvider() {
    super(DomainFileType.class);
  }

  @Override
  public @NotNull @NonNls String getEditorTypeId() {
    return "sew-domain-editor";
  }
}
