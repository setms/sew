package org.setms.sew.intellij.km;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.kmsystem.KmSystem;
import org.setms.sew.intellij.workspace.IntellijWorkspace;

@Service(Service.Level.PROJECT)
@RequiredArgsConstructor
public final class KmSystemService {

  private final Project project;
  @Getter private KmSystem kmSystem;
  @Getter private IntellijWorkspace workspace;

  public void start() {
    workspace = new IntellijWorkspace(project.getWorkspaceFile());
    kmSystem = new KmSystem(workspace);
  }
}
