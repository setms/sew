package org.setms.sew.intellij.lang.sal;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.application.ApplicationManager;
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
import org.setms.km.domain.model.file.Files;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Level;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.sew.intellij.plugin.km.KmSystemService;

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
    return "Software Engineering Workbench";
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
    var service = project.getService(KmSystemService.class);
    var created =
        service
            .getKmSystem()
            .applySuggestion(service.getWorkspace().find(file), suggestion.code(), location)
            .stream()
            .filter(
                diagnostic ->
                    diagnostic.level() == Level.INFO && diagnostic.message().startsWith("Created"))
            .map(Diagnostic::message)
            .map(message -> message.substring(7).trim())
            .map(Files::get)
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
                    .map(LocalFileSystem.getInstance()::findFileByIoFile)
                    .filter(Objects::nonNull)
                    .forEach(virtualFile -> fileEditorManager.openFile(virtualFile, true));
              }
            });
  }
}
