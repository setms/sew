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

public class MainTool extends TestTool {

  @Override
  public Input<MainArtifact> validationTarget() {
    return new GlobInput<>("main", TestFormat.INSTANCE, MainArtifact.class);
  }

  @Override
  public Set<Input<? extends Artifact>> reportingContext() {
    return Set.of(new GlobInput<>("main", TestFormat.INSTANCE, MainArtifact.class));
  }

  @Override
  public void buildReportsFor(
      Artifact artifact,
      ResolvedInputs inputs,
      Resource<?> resource,
      Collection<Diagnostic> diagnostics) {
    super.buildReportsFor(artifact, inputs, resource, diagnostics);
    try (var writer = new PrintWriter(resource.select("report1/output.txt").writeTo())) {
      writer.println("report1");
    } catch (IOException e) {
      diagnostics.add(new Diagnostic(ERROR, e.getMessage()));
    }
    try (var writer = new PrintWriter(resource.select("report2/index.html").writeTo())) {
      writer.println("<html><body><p>Some fine report this is</p></body></html>");
    } catch (IOException e) {
      diagnostics.add(new Diagnostic(ERROR, e.getMessage()));
    }
  }
}
