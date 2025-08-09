package org.setms.sew.intellij.km;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.kmsystem.KmSystem;
import org.setms.sew.intellij.workspace.IntellijWorkspace;

@Service(Service.Level.PROJECT)
@RequiredArgsConstructor
public final class KmSystemService {

  private final Project project;
  private KmSystem kmSystem;
  private IntellijWorkspace workspace;

  public void start() {
    workspace = new IntellijWorkspace(ProjectUtil.guessProjectDir(project));
    ApplicationManager.getApplication()
        .runWriteAction(
            () -> {
              kmSystem = new KmSystem(workspace);
            });
  }

  public KmSystem getKmSystem() {
    if (kmSystem == null) {
      start();
    }
    return kmSystem;
  }

  public IntellijWorkspace getWorkspace() {
    if (workspace == null) {
      start();
    }
    return workspace;
  }
}
