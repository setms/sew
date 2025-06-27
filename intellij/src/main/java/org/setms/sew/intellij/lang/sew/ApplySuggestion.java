package org.setms.sew.intellij.lang.sew;

import static org.setms.sew.core.domain.model.tool.Level.ERROR;
import static org.setms.sew.core.domain.model.tool.Level.INFO;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.IncorrectOperationException;
import java.io.File;
import java.net.URI;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.core.domain.model.tool.Diagnostic;
import org.setms.sew.core.domain.model.tool.Location;
import org.setms.sew.core.domain.model.tool.Suggestion;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.outbound.tool.file.FileOutputSink;
import org.setms.sew.intellij.editor.VirtualFileInputSource;

public class ApplySuggestion implements IntentionAction {

  private static final String CREATED_PREFIX = "Created ";

  private final Tool tool;
  private final Suggestion suggestion;
  private final Location location;
  private final PsiElement psiElement;

  public ApplySuggestion(
      Tool tool, Suggestion suggestion, Location location, PsiElement psiElement) {
    this.tool = tool;
    this.suggestion = suggestion;
    this.location = location;
    this.psiElement = psiElement;
  }

  @Override
  public @NotNull @IntentionFamilyName String getFamilyName() {
    var result = tool.getClass().getSimpleName();
    if (result.endsWith("Tool")) {
      result = result.substring(0, result.length() - 4);
    }
    return result;
  }

  @Override
  public @IntentionName @NotNull String getText() {
    return suggestion.message();
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile psiFile) {
    if (editor == null || psiFile == null || !psiElement.isValid()) {
      return false;
    }
    var offset = editor.getCaretModel().getOffset();
    var range = psiElement.getTextRange();
    return range != null && range.containsOffset(offset);
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile)
      throws IncorrectOperationException {
    PsiDocumentManager.getInstance(project).commitAllDocuments();
    var diagnostics =
        tool.apply(
            suggestion.code(),
            new VirtualFileInputSource(psiFile, tool),
            location,
            new FileOutputSink(new File(psiFile.getVirtualFile().getPath())));
    diagnostics.stream()
        .filter(d -> d.level() == ERROR)
        .forEach(
            diagnostic ->
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("Sew")
                    .createNotification(diagnostic.message(), NotificationType.ERROR)
                    .notify(project));
    diagnostics.stream()
        .filter(d -> d.level() == INFO)
        .map(Diagnostic::message)
        .filter(m -> m.startsWith(CREATED_PREFIX))
        .map(m -> m.substring(CREATED_PREFIX.length()).trim())
        .map(URI::create)
        .map(File::new)
        .filter(File::isFile)
        .map(LocalFileSystem.getInstance()::refreshAndFindFileByIoFile)
        .filter(Objects::nonNull)
        .forEach(file -> FileEditorManager.getInstance(project).openFile(file, true));
    WriteCommandAction.runWriteCommandAction(
        project,
        () -> {
          psiFile.getVirtualFile().refresh(false, false); // reload VFS
          PsiManager.getInstance(project).reloadFromDisk(psiFile); // reload PSI
        });
  }
}
