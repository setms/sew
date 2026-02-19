package org.setms.km.test;

import static org.setms.km.domain.model.tool.AppliedSuggestion.created;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Set;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.tool.StandaloneTool;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.workspace.Resource;

public class StandaloneTestTool extends StandaloneTool {

  public static final String SUGGESTION_CODE = "standalone.create.file";

  public boolean validated;
  private Diagnostic diagnostic;

  public void init(Diagnostic diagnostic) {
    init();
    this.diagnostic = diagnostic;
  }

  public void init() {
    validated = false;
    diagnostic = null;
  }

  @Override
  public Set<Input<? extends Artifact>> validationContext() {
    return Set.of(MainTool.INPUT);
  }

  @Override
  public void validate(
      ResolvedInputs inputs, Resource<?> root, Collection<Diagnostic> diagnostics) {
    validated = true;
    if (diagnostic != null) {
      diagnostics.add(diagnostic);
    }
  }

  @Override
  protected AppliedSuggestion doApply(
      String suggestionCode, Location location, ResolvedInputs inputs, Resource<?> output)
      throws IOException {
    if (!SUGGESTION_CODE.equals(suggestionCode)) {
      return AppliedSuggestion.none();
    }
    var file = output.select("/standalone.txt");
    try (var writer = new PrintWriter(file.writeTo())) {
      writer.println("standalone");
    }
    return created(file);
  }
}
