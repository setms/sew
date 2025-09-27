package org.setms.sew.intellij.plugin.km;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import org.setms.km.domain.model.kmsystem.KmSystem;
import org.setms.km.domain.model.kmsystem.OutOfDateArtifact;
import org.setms.sew.intellij.plugin.workspace.IntellijWorkspace;

public class IntellijKmSystem extends KmSystem {

  private final Project project;

  public IntellijKmSystem(Project project) {
    super(new IntellijWorkspace(ProjectUtil.guessProjectDir(project)));
    this.project = project;
  }

  protected void validateArtifactsInBackground() {
    ApplicationManager.getApplication()
        .executeOnPooledThread(
            () ->
                outOfDateArtifacts().stream()
                    .filter(artifact -> artifact.path().startsWith("/"))
                    .forEach(this::update));
  }

  private void update(OutOfDateArtifact artifact) {
    ApplicationManager.getApplication()
        .invokeLater(
            () ->
                WriteCommandAction.runWriteCommandAction(
                    project, () -> updateOutOfDateArtifact(artifact)));
  }
}
