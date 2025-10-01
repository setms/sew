package org.setms.km.domain.model.tool;

import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;

import java.util.Collection;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.workspace.Resource;

/**
 * A {@linkplain Tool tool} that stands on its own, in the sense that it doesn't need a particular
 * artifact to operate on.
 */
public abstract non-sealed class StandaloneTool extends Tool {

  /**
   * Validate the {@linkplain #validationContext() context}.
   *
   * @param inputs the context to validate
   * @param diagnostics where to store any validation issues
   */
  public void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    // For descendants to override
  }

  /**
   * Apply a suggestion.
   *
   * @param suggestionCode the suggestion to apply
   * @param location where in the input to apply the suggestion
   * @param inputs inputs to use
   * @param output where to store outputs
   * @return artifacts created/changed and diagnostics
   */
  public AppliedSuggestion applySuggestion(
      String suggestionCode, Location location, ResolvedInputs inputs, Resource<?> output) {
    try {
      return doApply(suggestionCode, location, inputs, output);
    } catch (Exception e) {
      return failedWith(e);
    }
  }

  protected AppliedSuggestion doApply(
      String suggestionCode, Location location, ResolvedInputs inputs, Resource<?> output)
      throws Exception {
    // For descendants to override
    return AppliedSuggestion.unknown(suggestionCode);
  }

  /**
   * Build reports.
   *
   * @param inputs the context for building the reports
   * @param output where to store output
   * @param diagnostics where to store any issues
   */
  public void buildReports(
      ResolvedInputs inputs, Resource<?> output, Collection<Diagnostic> diagnostics) {
    // For descendants to override
  }
}
