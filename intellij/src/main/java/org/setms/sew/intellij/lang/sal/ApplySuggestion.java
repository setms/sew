package org.setms.sew.intellij.lang.sal;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.IncorrectOperationException;
import java.io.File;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.sew.intellij.plugin.km.ProcessOrchestratorService;

public class ApplySuggestion implements IntentionAction {

  @SafeFieldForPreview private final Suggestion suggestion;
  @SafeFieldForPreview private final Location location;
  @SafeFieldForPreview private final PsiElement psiElement;

  public ApplySuggestion(Suggestion suggestion, Location location, PsiElement psiElement) {
    this.suggestion = suggestion;
    this.location = location;
    this.psiElement = psiElement;
  }

  @Override
  public @NotNull IntentionPreviewInfo generatePreview(
      @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    return IntentionPreviewInfo.EMPTY;
  }

  @Override
  public @NotNull @IntentionFamilyName String getFamilyName() {
    return "Software engineering workbench";
  }

  @Override
  public @IntentionName @NotNull String getText() {
    return suggestion.message();
  }

  @Override
  public boolean startInWriteAction() {
    return true;
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
    var file =
        Optional.ofNullable(psiFile.getVirtualFile())
            .orElseGet(() -> psiElement.getContainingFile().getVirtualFile());
    if (file == null) {
      return;
    }
    var service = project.getService(ProcessOrchestratorService.class);
    if (service.isNotReady()) {
      service.whenReady().thenRunAsync(() -> invoke(project, editor, psiFile));
      return;
    }
    applySuggestion(project, psiFile, service, file);
  }

  public void applySuggestion(
      Project project, PsiFile psiFile, ProcessOrchestratorService service, VirtualFile file) {
    var appliedSuggestion =
        service
            .getKmSystem()
            .applySuggestion(service.getWorkspace().find(file), suggestion.code(), location);
    var created =
        appliedSuggestion.createdOrChanged().stream()
            .map(service.getWorkspace()::toFile)
            .filter(File::isFile)
            .toList();
    reloadVirtualFileAndPsi(project, psiFile, file, created);
  }

  public void reloadVirtualFileAndPsi(
      Project project, PsiFile psiFile, VirtualFile file, Collection<File> toOpen) {
    file.refresh(true, false);
    ApplicationManager.getApplication()
        .invokeLater(
            () -> {
              ApplicationManager.getApplication()
                  .runWriteAction(() -> PsiManager.getInstance(project).reloadFromDisk(psiFile));
              if (!toOpen.isEmpty()) {
                var fileEditorManager = FileEditorManager.getInstance(project);
                toOpen.stream()
                    .map(LocalFileSystem.getInstance()::refreshAndFindFileByIoFile)
                    .filter(Objects::nonNull)
                    .forEach(
                        virtualFile -> {
                          WriteCommandAction.runWriteCommandAction(
                              project,
                              () ->
                                  project
                                      .getService(ProcessOrchestratorService.class)
                                      .getWorkspace()
                                      .changed(virtualFile));
                          fileEditorManager.openFile(virtualFile, true);
                        });
              }
            });
  }
}
