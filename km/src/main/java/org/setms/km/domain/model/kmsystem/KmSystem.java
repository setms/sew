package org.setms.km.domain.model.kmsystem;

import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.ToolRegistry;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Level;
import org.setms.km.domain.model.workspace.Workspace;

public class KmSystem {

  private final Workspace workspace;

  public KmSystem(Workspace workspace) {
    this.workspace = workspace;
    this.workspace.registerChangeHandler(this::artifactChanged);
  }

  private void artifactChanged(Artifact artifact) {
    ToolRegistry.handling(artifact.getClass())
        .ifPresent(
            tool -> {
              if (tool.validate(workspace).stream()
                  .map(Diagnostic::level)
                  .noneMatch(Level.ERROR::equals)) {
                tool.build(workspace);
              }
            });
  }
}
