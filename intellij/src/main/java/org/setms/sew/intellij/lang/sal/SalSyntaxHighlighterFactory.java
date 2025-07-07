package org.setms.sew.intellij.lang.sal;

import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class SalSyntaxHighlighterFactory extends SyntaxHighlighterFactory {

  @Override
  public @NotNull SyntaxHighlighter getSyntaxHighlighter(Project project, VirtualFile file) {
    return new SalSyntaxHighlighter();
  }
}
