package org.setms.sew.intellij.plugin.acceptancetest;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import org.jetbrains.annotations.NotNull;

class TablesDocumentListener implements DocumentListener {

  private final Editor editor;

  public TablesDocumentListener(Editor editor) {
    this.editor = editor;
  }

  @Override
  public void documentChanged(@NotNull DocumentEvent event) {
    var document = event.getDocument();
    if (isInvalidFile(document)) {
      return;
    }
    var table = Table.from(document, event.getOffset());
    if (table == null) {
      return;
    }
    var delta = table.addText(event.getNewFragment().toString());
    if (delta != null) {
      ApplicationManager.getApplication()
          .invokeLater(
              () -> {
                if (isInvalidFile(document)) {
                  return;
                }
                WriteCommandAction.runWriteCommandAction(
                    null, () -> document.replaceString(delta.start(), delta.end(), delta.text()));
                var caretModel = editor.getCaretModel();
                caretModel.moveToOffset(delta.newCaret());
                editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
              });
    }
  }

  private boolean isInvalidFile(Document document) {
    var file = FileDocumentManager.getInstance().getFile(document);
    return file == null
        || !file.isValid()
        || !(file.getFileType() instanceof AcceptanceTestFileType);
  }
}
