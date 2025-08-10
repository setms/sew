package org.setms.sew.intellij.plugin.editor;

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
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public abstract class EditorWithPreviewProvider implements FileEditorProvider, DumbAware {

  private final Class<? extends FileType> fileTypeClass;

  @Override
  public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
    return fileTypeClass.isInstance(file.getFileType());
  }

  @Override
  public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
    return new TextEditorWithPreview(editorFor(project, file), new HtmlPreview(project, file));
  }

  protected TextEditor editorFor(Project project, VirtualFile file) {
    return (TextEditor) TextEditorProvider.getInstance().createEditor(project, file);
  }

  @Override
  public @NotNull FileEditorPolicy getPolicy() {
    return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
  }
}
