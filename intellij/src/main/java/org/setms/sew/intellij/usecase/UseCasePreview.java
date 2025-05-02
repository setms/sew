package org.setms.sew.intellij.usecase;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.setms.sew.core.inbound.tool.UseCaseTool;
import org.setms.sew.intellij.editor.HtmlPreview;

public class UseCasePreview extends HtmlPreview {

  public UseCasePreview(Project project, VirtualFile file) {
    super(project, file, new UseCaseTool());
  }
}
