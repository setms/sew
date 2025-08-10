package org.setms.km.domain.model.tool;

import static java.util.Collections.emptySet;
import static org.setms.km.domain.model.validation.Level.ERROR;

import java.util.LinkedHashSet;
import java.util.Set;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;

public record AppliedSuggestion(Set<Resource<?>> createdOrChanged, Set<Diagnostic> diagnostics) {

  public AppliedSuggestion() {
    this(emptySet(), emptySet());
  }

  public AppliedSuggestion with(Resource<?> resource) {
    var allCreatedOrChanged = new LinkedHashSet<>(createdOrChanged);
    allCreatedOrChanged.add(resource);
    return new AppliedSuggestion(allCreatedOrChanged, diagnostics);
  }

  public AppliedSuggestion with(Exception e) {
    return with(new Diagnostic(ERROR, e.getMessage()));
  }

  public AppliedSuggestion with(Diagnostic diagnostic) {
    var allDiagnostics = new LinkedHashSet<>(diagnostics);
    allDiagnostics.add(diagnostic);
    return new AppliedSuggestion(createdOrChanged, allDiagnostics);
  }
}
