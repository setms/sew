package org.setms.km.test;

import static org.setms.km.domain.model.validation.Level.ERROR;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.GlobInput;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;

public class OtherTool extends TestTool<OtherArtifact> {

  @Override
  public Input<OtherArtifact> getMainInput() {
    return new GlobInput<>("other", new TestFormat(), OtherArtifact.class);
  }

  @Override
  public Set<Input<? extends Artifact>> additionalInputs() {
    return Set.of(new GlobInput<>("main", new TestFormat(), MainArtifact.class));
  }

  @Override
  public void build(
      ResolvedInputs inputs, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    super.build(inputs, resource, diagnostics);
    try (var writer = new PrintWriter(resource.select("other-report.yaml").writeTo())) {
      writer.println("report:");
      writer.println("  text: Some fine report this is");
    } catch (IOException e) {
      diagnostics.add(new Diagnostic(ERROR, e.getMessage()));
    }
  }
}
