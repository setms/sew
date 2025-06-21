package org.setms.sew.intellij.acceptancetest;

import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.core.inbound.tool.AcceptanceTestTool;
import org.setms.sew.intellij.editor.EditorWithPreviewProvider;

public class AcceptanceTestEditorProvider extends EditorWithPreviewProvider {

  public AcceptanceTestEditorProvider() {
    super(new AcceptanceTestTool(), AcceptanceTestFileType.class);
  }

  @Override
  public @NotNull @NonNls String getEditorTypeId() {
    return "sew-acceptance-test-editor";
  }

  @Override
  protected TextEditor editorFor(Project project, VirtualFile file) {
    return new TablesEditor(project, file);
  }
}
