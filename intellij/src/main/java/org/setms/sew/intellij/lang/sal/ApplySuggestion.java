package org.setms.sew.intellij.lang.sal;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.setms.km.domain.model.tool.BaseTool;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.sew.intellij.tool.ToolRunner;
import org.setms.sew.intellij.workspace.IntellijWorkspace;

public class ApplySuggestion implements IntentionAction {

  @SafeFieldForPreview private final BaseTool tool;
  @SafeFieldForPreview private final Suggestion suggestion;
  @SafeFieldForPreview private final Location location;
  @SafeFieldForPreview private final PsiElement psiElement;

  public ApplySuggestion(
      BaseTool tool, Suggestion suggestion, Location location, PsiElement psiElement) {
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
    ToolRunner.applySuggestion(
        tool, suggestion.code(), location, project, new IntellijWorkspace(psiFile, tool));
    WriteCommandAction.runWriteCommandAction(
        project,
        () -> {
          psiFile.getVirtualFile().refresh(false, false); // reload VFS
          PsiManager.getInstance(project).reloadFromDisk(psiFile); // reload PSI
        });
  }
}
