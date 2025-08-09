package org.setms.km.test;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.SequencedSet;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.BaseTool;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;

public abstract class TestTool<A extends Artifact> extends BaseTool<A> {

  private final SequencedSet<Diagnostic> validationDiagnostics = new LinkedHashSet<>();
  private final SequencedSet<Diagnostic> buildDiagnostics = new LinkedHashSet<>();
  public boolean validated;
  public boolean built;

  public void init(Diagnostic validationDiagnostic, Diagnostic buildDiagnostic) {
    init();
    if (validationDiagnostic != null) {
      validationDiagnostics.add(validationDiagnostic);
    }
    if (buildDiagnostic != null) {
      buildDiagnostics.add(buildDiagnostic);
    }
  }

  public void init() {
    validated = false;
    built = false;
    validationDiagnostics.clear();
    buildDiagnostics.clear();
  }

  @Override
  public void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    diagnostics.addAll(validationDiagnostics);
    validated = true;
  }

  @Override
  public void build(
      ResolvedInputs inputs, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    diagnostics.addAll(buildDiagnostics);
    built = true;
  }
}
