package org.setms.km.domain.model.kmsystem;

import java.util.*;
import org.setms.km.domain.model.tool.BaseTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.Output;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;

class MainTool extends BaseTool {

  static final SequencedSet<Diagnostic> validations = new LinkedHashSet<>();
  static boolean validated;
  static boolean built;

  public static void init(Diagnostic diagnostic) {
    init();
    validations.add(diagnostic);
  }

  public static void init() {
    validated = false;
    built = false;
    validations.clear();
  }

  @Override
  public List<Input<?>> getInputs() {
    return List.of(new Input<>("main", new TestFormat(), MainArtifact.class));
  }

  @Override
  public Optional<Output> getOutputs() {
    return Optional.empty();
  }

  @Override
  public void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    diagnostics.addAll(validations);
    validated = true;
  }

  @Override
  public void build(
      ResolvedInputs inputs, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    built = true;
  }
}
