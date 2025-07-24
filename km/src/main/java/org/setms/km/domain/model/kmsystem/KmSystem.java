package org.setms.km.domain.model.kmsystem;

import java.util.Collection;
import java.util.Optional;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.format.Format;
import org.setms.km.domain.model.tool.BaseTool;
import org.setms.km.domain.model.tool.ToolRegistry;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Level;
import org.setms.km.domain.model.workspace.ArtifactDefinition;
import org.setms.km.domain.model.workspace.Workspace;

public class KmSystem {

  private final Workspace workspace;

  public KmSystem(Workspace workspace) {
    this.workspace = workspace;
    this.workspace.registerArtifactChangedHandler(this::artifactChanged);
    ToolRegistry.allTools()
        .map(BaseTool::getInputs)
        .flatMap(Collection::stream)
        .map(
            input ->
                new ArtifactDefinition(
                    input.type(),
                    input.glob(),
                    Optional.ofNullable(input.format()).map(Format::newParser).orElse(null)))
        .distinct()
        .forEach(workspace::registerArtifactType);
  }

  private void artifactChanged(Artifact artifact) {
    var maybeTool = ToolRegistry.handling(artifact.getClass());
    var valid = true;
    if (maybeTool.isPresent()) {
      var tool = maybeTool.get();
      valid =
          tool.validate(workspace).stream().map(Diagnostic::level).noneMatch(Level.ERROR::equals);
      if (valid) {
        tool.build(workspace);
      }
    }
    if (valid) {
      ToolRegistry.dependingOn(artifact.getClass()).forEach(tool -> tool.build(workspace));
    }
  }
}
