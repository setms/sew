package org.setms.sew.intellij.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.TextEditorWithPreview;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.core.domain.model.tool.Tool;

public abstract class EditorWithPreviewProvider implements FileEditorProvider, DumbAware {

  private final Tool tool;
  private final Class<? extends FileType> fileTypeClass;

  protected EditorWithPreviewProvider(Tool tool, Class<? extends FileType> fileTypeClass) {
    this.tool = tool;
    this.fileTypeClass = fileTypeClass;
  }

  @Override
  public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
    return fileTypeClass.isInstance(file.getFileType());
  }

  @Override
  public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
    var textEditor = (TextEditor) TextEditorProvider.getInstance().createEditor(project, file);
    var htmlEditor = new HtmlPreview(project, file, tool);
    return new TextEditorWithPreview(textEditor, htmlEditor);
  }

  @Override
  public @NotNull FileEditorPolicy getPolicy() {
    return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
  }
}
