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
import java.io.File;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.core.domain.model.tool.Location;
import org.setms.sew.core.domain.model.tool.Suggestion;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.outbound.tool.file.FileOutputSink;
import org.setms.sew.intellij.editor.VirtualFileInputSource;
import org.setms.sew.intellij.tool.ToolRunner;

public class ApplySuggestion implements IntentionAction {

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
    ToolRunner.applySuggestion(
        tool,
        suggestion.code(),
        location,
        project,
        new VirtualFileInputSource(psiFile, tool),
        new FileOutputSink(new File(psiFile.getVirtualFile().getPath())));
    WriteCommandAction.runWriteCommandAction(
        project,
        () -> {
          psiFile.getVirtualFile().refresh(false, false); // reload VFS
          PsiManager.getInstance(project).reloadFromDisk(psiFile); // reload PSI
        });
  }
}
