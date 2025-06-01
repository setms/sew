package org.setms.sew.intellij.contextmap;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.core.inbound.tool.ContextMapTool;
import org.setms.sew.intellij.editor.EditorWithPreviewProvider;
import org.setms.sew.intellij.filetype.ContextMapFileType;

public class ContextMapEditorProvider extends EditorWithPreviewProvider {

  public ContextMapEditorProvider() {
    super(new ContextMapTool(), ContextMapFileType.class);
  }

  @Override
  public @NotNull @NonNls String getEditorTypeId() {
    return "sew-context-map-editor";
  }
}
