package org.setms.sew.intellij.usecase;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.intellij.editor.EditorWithPreviewProvider;

public class UseCaseEditorProvider extends EditorWithPreviewProvider {

  public UseCaseEditorProvider() {
    super(UseCaseFileType.class);
  }

  @Override
  public @NotNull @NonNls String getEditorTypeId() {
    return "sew-use-case-editor";
  }
}
