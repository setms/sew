package org.setms.sew.intellij.domain;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.core.inbound.tool.DomainTool;
import org.setms.sew.intellij.editor.EditorWithPreviewProvider;
import org.setms.sew.intellij.filetype.DomainFileType;

public class DomainEditorProvider extends EditorWithPreviewProvider {

  public DomainEditorProvider() {
    super(new DomainTool(), DomainFileType.class);
  }

  @Override
  public @NotNull @NonNls String getEditorTypeId() {
    return "sew-domain-editor";
  }
}
