package org.setms.swe.domain.model.sdlc.technology;

import java.util.Collection;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;

public interface CodeBuilder {

  /**
   * Validate everything's in place for the builder to work properly.
   *
   * @param resource the workspace root resource
   * @param diagnostics where to add diagnostics for issues
   */
  void validate(Resource<?> resource, Collection<Diagnostic> diagnostics);

  /**
   * Compile the project and collect diagnostics for any errors found.
   *
   * @param resource the workspace root resource
   * @param diagnostics where to add diagnostics for compilation errors
   */
  void build(Resource<?> resource, Collection<Diagnostic> diagnostics);

  /**
   * Add a build plugin to the project, fetching its latest version from the plugin portal.
   *
   * @param pluginId the plugin ID (e.g. "org.springframework.boot")
   * @param resource the project root resource
   */
  void addBuildPlugin(String pluginId, Resource<?> resource);

  /**
   * Add a dependency to the project.
   *
   * @param dependency the dependency ID (e.g. "org.springframework.boot:spring-boot-starter-web")
   * @param resource the project root resource
   */
  void addDependency(String dependency, Resource<?> resource);

  /**
   * Apply a suggestion to fix an issue reported earlier.
   *
   * @param suggestionCode the suggestion code from the diagnostic
   * @param resource the project root resource
   * @return applied suggestion result with created resources
   */
  AppliedSuggestion applySuggestion(String suggestionCode, Resource<?> resource);
}
