package org.setms.km.test;

import static org.setms.km.domain.model.validation.Level.ERROR;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.Output;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;

public class OtherTool extends TestTool {

  @Override
  public List<Input<?>> getInputs() {
    return List.of(
        new Input<>("other", new TestFormat(), OtherArtifact.class),
        new Input<>("main", new TestFormat(), MainArtifact.class));
  }

  @Override
  public Optional<Output> getOutputs() {
    return Optional.empty();
  }

  @Override
  public void build(ResolvedInputs inputs, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    super.build(inputs, resource, diagnostics);
    try (var writer = new PrintWriter(resource.select("other-report.yaml").writeTo())) {
      writer.println("report:");
      writer.println("  text: Some fine report this is");
    } catch (IOException e) {
      diagnostics.add(new Diagnostic(ERROR, e.getMessage()));
    }
  }
}
