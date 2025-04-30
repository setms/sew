package org.setms.sew.intellij.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.TextEditorWithPreview;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.intellij.filetype.UseCaseFileType;

public class UseCaseEditorProvider implements FileEditorProvider, DumbAware {

  @Override
  public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
    return file.getFileType() instanceof UseCaseFileType;
  }

  @Override
  public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
    var textEditor = (TextEditor) TextEditorProvider.getInstance().createEditor(project, file);
    var htmlEditor = new UseCasePreview(project, file);
    return new TextEditorWithPreview(textEditor, htmlEditor);
  }

  @Override
  public @NotNull @NonNls String getEditorTypeId() {
    return "sew-use-case-editor";
  }

  @Override
  public @NotNull FileEditorPolicy getPolicy() {
    return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
  }
}
