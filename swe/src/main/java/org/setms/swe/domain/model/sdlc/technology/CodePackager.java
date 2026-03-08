package org.setms.swe.domain.model.sdlc.technology;

import java.util.Collection;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;

/** Packages built code into a distributable artifact. */
public interface CodePackager {

  /**
   * Package the built code into a distributable artifact.
   *
   * @param resource the workspace root resource
   * @param diagnostics where to add diagnostics for any errors found
   */
  void packageCode(Resource<?> resource, Collection<Diagnostic> diagnostics);

  /**
   * Apply a suggestion to fix an issue reported earlier.
   *
   * @param suggestionCode the suggestion code from the diagnostic
   * @param resource the project root resource
   * @param inputs Resolved inputs, including decisions and other artifacts
   * @return applied suggestion result with created resources
   */
  default AppliedSuggestion applySuggestion(
      String suggestionCode, Resource<?> resource, ResolvedInputs inputs) {
    return AppliedSuggestion.none();
  }
}
