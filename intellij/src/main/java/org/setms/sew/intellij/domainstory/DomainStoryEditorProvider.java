package org.setms.sew.intellij.domainstory;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.intellij.editor.EditorWithPreviewProvider;

public class DomainStoryEditorProvider extends EditorWithPreviewProvider {

  public DomainStoryEditorProvider() {
    super(DomainStoryFileType.class);
  }

  @Override
  public @NotNull @NonNls String getEditorTypeId() {
    return "sew-domainstory-editor";
  }
}
