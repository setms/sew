package org.setms.sew.intellij.acceptancetest;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import java.awt.BorderLayout;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.intellij.plugins.markdown.lang.MarkdownFileType;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class TablesEditor implements TextEditor {

  private final Editor editor;
  private final JPanel component;
  private final VirtualFile file;

  public TablesEditor(Project project, VirtualFile file) {
    this.file = file;
    var document = FileDocumentManager.getInstance().getDocument(file);
    if (document == null) {
      throw new IllegalStateException("Document not found");
    }
    this.editor =
        EditorFactory.getInstance()
            .createEditor(
                document,
                project,
                MarkdownFileType.INSTANCE, // force Markdown-like styling
                false);

    this.component = new JPanel(new BorderLayout());
    this.component.add(editor.getComponent(), BorderLayout.CENTER);
    document.addDocumentListener(new TablesDocumentListener(editor));
  }

  @Override
  public @NotNull Editor getEditor() {
    return editor;
  }

  @Override
  public boolean canNavigateTo(@NotNull Navigatable navigatable) {
    return navigatable.canNavigate();
  }

  @Override
  public void navigateTo(@NotNull Navigatable navigatable) {
    if (canNavigateTo(navigatable)) {
      navigatable.navigate(true);
    }
  }

  @Override
  public @NotNull JComponent getComponent() {
    return component;
  }

  @Override
  public @Nullable JComponent getPreferredFocusedComponent() {
    return editor.getContentComponent();
  }

  @Override
  public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
    return "Markdown Tables Editor";
  }

  @Override
  public void setState(@NotNull FileEditorState fileEditorState) {}

  @Override
  public boolean isModified() {
    return FileDocumentManager.getInstance().isFileModified(file);
  }

  @Override
  public boolean isValid() {
    return file.isValid();
  }

  @Override
  public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {}

  @Override
  public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {}

  @Override
  public void dispose() {
    EditorFactory.getInstance().releaseEditor(editor);
  }

  @Override
  public <T> @Nullable T getUserData(@NotNull Key<T> key) {
    return editor.getUserData(key);
  }

  @Override
  public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
    editor.putUserData(key, value);
  }

  @Override
  public @NotNull VirtualFile getFile() {
    return file;
  }
}
