package org.setms.sew.intellij.language.sew;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.inbound.tool.UseCaseTool;
import org.setms.sew.intellij.editor.VirtualFileInputSource;
import org.setms.sew.intellij.language.LevelSeverity;
import org.setms.sew.intellij.usecase.UseCaseFileType;

public class SewAnnotator implements Annotator {

  private final Map<String, Tool> toolsByExtension =
      Map.of(UseCaseFileType.INSTANCE.getDefaultExtension(), new UseCaseTool());

  @Override
  public void annotate(@NotNull PsiElement psiElement, @NotNull AnnotationHolder holder) {
    if (!"package".equals(psiElement.getText())) {
      return;
    }
    var file = psiElement.getContainingFile();
    var tool = toolsByExtension.get(file.getVirtualFile().getExtension());
    if (tool == null) {
      return;
    }
    tool.validate(new VirtualFileInputSource(file.getVirtualFile(), tool))
        .forEach(
            diagnostic -> {
              holder
                  .newAnnotation(LevelSeverity.of(diagnostic.level()), diagnostic.message())
                  .create();
            });
  }
}
