package org.setms.sew.intellij.plugin.acceptancetest;

import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.intellij.plugin.editor.EditorWithPreviewProvider;

public class AcceptanceTestEditorProvider extends EditorWithPreviewProvider {

  public AcceptanceTestEditorProvider() {
    super(AcceptanceTestFileType.class);
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
