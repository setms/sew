package org.setms.swe.domain.model.sdlc.technology;

import java.util.Collection;
import java.util.Optional;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.architecture.Decisions;
import org.setms.swe.domain.model.sdlc.project.Project;

/** Resolves technology decisions into implementations that use the decided-upon technology. */
public interface TechnologyResolver {

  /**
   * @param decisions Decisions made
   * @param projects Projects defined
   * @param location From where the unit test is created
   * @param diagnostics where to store any validation issues
   * @return something that can generate unit tests, or empty if there are issues
   */
  Optional<UnitTestGenerator> unitTestGenerator(
      Decisions decisions,
      Collection<Project> projects,
      Location location,
      Collection<Diagnostic> diagnostics);

  /**
   * @param resource Workspace root resource
   * @param inputs Resolved inputs
   * @param diagnostics where to store any validation issues
   * @return something that can build code, or empty if there are issues
   */
  Optional<CodeBuilder> codeBuilder(
      Resource<?> resource, ResolvedInputs inputs, Collection<Diagnostic> diagnostics);

  AppliedSuggestion applySuggestion(
      String suggestionCode, Resource<?> resource, ResolvedInputs inputs);
}
