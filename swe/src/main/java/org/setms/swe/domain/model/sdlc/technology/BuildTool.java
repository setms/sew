package org.setms.swe.domain.model.sdlc.technology;

import java.util.Collection;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.inbound.tool.TechnologyResolverImpl;

/**
 * Validates and generates build configuration files.
 *
 * <p>Implementations are build-tool specific (e.g., Gradle, Maven). They know what configuration
 * files they need, validate whether they exist, and generate them when requested. The selection of
 * which implementation to use is handled by {@link TechnologyResolverImpl}.
 */
public interface BuildTool {

  /**
   * Validate everything's in place for the build tool to work properly.
   *
   * @param resource the project root resource
   * @param diagnostics where to add diagnostics for issues
   */
  void validate(Resource<?> resource, Collection<Diagnostic> diagnostics);

  /**
   * Apply a suggestion to fix an issue reported earlier.
   *
   * @param suggestionCode the suggestion code from the diagnostic
   * @param resource the project root resource
   * @return applied suggestion result with created resources
   */
  AppliedSuggestion applySuggestion(String suggestionCode, Resource<?> resource);
}
