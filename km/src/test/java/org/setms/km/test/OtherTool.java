package org.setms.km.test;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.setms.km.domain.model.tool.BaseTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.Output;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;

public class OtherTool extends BaseTool {

  public static boolean validated;
  public static boolean built;

  public static void init() {
    validated = false;
    built = false;
  }

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
  public void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    validated = true;
  }

  @Override
  public void build(
      ResolvedInputs inputs, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    built = true;
  }
}
