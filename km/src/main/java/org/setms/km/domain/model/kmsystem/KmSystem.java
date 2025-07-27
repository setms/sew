package org.setms.km.domain.model.kmsystem;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.format.Format;
import org.setms.km.domain.model.tool.BaseTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.tool.Tools;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Level;
import org.setms.km.domain.model.workspace.ArtifactDefinition;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.domain.model.workspace.Workspace;

public class KmSystem {

  private final Workspace workspace;

  public KmSystem(Workspace workspace) {
    this.workspace = workspace;
    this.workspace.registerArtifactChangedHandler(this::artifactChanged);
    Tools.all()
        .map(BaseTool::getInputs)
        .flatMap(Collection::stream)
        .map(
            input ->
                new ArtifactDefinition(
                    input.type(),
                    input.glob(),
                    Optional.ofNullable(input.format()).map(Format::newParser).orElse(null)))
        .distinct()
        .forEach(workspace::registerArtifactDefinition);
  }

  private void artifactChanged(Artifact artifact) {
    var maybeTool = Tools.targeting(artifact.getClass());
    var valid = true;
    if (maybeTool.isPresent()) {
      var tool = maybeTool.get();
      var diagnostics = new LinkedHashSet<Diagnostic>();
      var inputs = resolveInputs(tool, diagnostics);
      tool.validate(inputs, diagnostics);
      valid = diagnostics.stream().map(Diagnostic::level).noneMatch(Level.ERROR::equals);
      if (valid) {
        tool.build(inputs, workspace.root().select("build"), diagnostics);
      }
    }
    if (valid) {
      Tools.dependingOn(artifact.getClass())
          .forEach(
              tool -> {
                var diagnostics = new LinkedHashSet<Diagnostic>();
                var inputs = resolveInputs(tool, diagnostics);
                tool.build(inputs, workspace.root().select("build"), diagnostics);
              });
    }
  }

  private ResolvedInputs resolveInputs(BaseTool tool, Collection<Diagnostic> diagnostics) {
    var result = new ResolvedInputs();
    var validate = new AtomicBoolean(true);
    tool.getInputs()
        .forEach(
            input ->
                result.put(
                    input.name(),
                    parse(workspace.root(), input, validate.getAndSet(false), diagnostics)));
    return result;
  }

  private <T extends Artifact> List<T> parse(
      Resource<?> resource, Input<T> input, boolean validate, Collection<Diagnostic> diagnostics) {
    return input
        .format()
        .newParser()
        .parseMatching(resource, input.glob(), input.type(), validate, diagnostics)
        .toList();
  }
}
