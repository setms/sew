package org.setms.swe.domain.model.sdlc.technology;

import java.util.Collection;
import java.util.Map;
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
   * Compile and assemble the project into a distributable package.
   *
   * @param resource the workspace root resource
   * @param diagnostics where to add diagnostics for any errors found
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
   * Enable a plugin in the build.
   *
   * @param plugin the plugin to enable
   * @param resource the project root resource
   */
  void enableBuildPlugin(String plugin, Resource<?> resource);

  /**
   * Add a dependency to the project.
   *
   * @param dependency the dependency ID (e.g. "org.springframework.boot:spring-boot-starter-web")
   * @param resource the project root resource
   */
  void addDependency(String dependency, Resource<?> resource);

  /**
   * Add a runtime-only dependency to the project.
   *
   * @param dependency the dependency ID (e.g. "org.postgresql:postgresql")
   * @param resource the project root resource
   */
  void addRuntimeDependency(String dependency, Resource<?> resource);

  /**
   * Configure a build task with the given properties.
   *
   * @param task the task name (e.g. "bootRun")
   * @param configuration the properties to set on the task
   * @param resource the project root resource
   */
  void configureTask(String task, Map<String, String> configuration, Resource<?> resource);

  /**
   * Apply a suggestion to fix an issue reported earlier.
   *
   * @param suggestionCode the suggestion code from the diagnostic
   * @param resource the project root resource
   * @return applied suggestion result with created resources
   */
  AppliedSuggestion applySuggestion(String suggestionCode, Resource<?> resource);
}
