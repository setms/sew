package org.setms.swe.domain.model.sdlc.technology;

import java.util.Collection;
import java.util.Optional;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.architecture.Decision;
import org.setms.swe.domain.model.sdlc.project.Project;

/** Resolves technology decisions into implementations that use the decided-upon technology. */
public interface TechnologyResolver {

  /**
   * @param decisions Decisions made
   * @param projects Projects defined
   * @param location From where the unit test is created
   * @param diagnostics where to store any validation issues
   * @return something that can generates unit tests, or <code>Optional.empty()</code> if there are
   *     issues
   */
  Optional<UnitTestGenerator> unitTestGenerator(
      Collection<Decision> decisions,
      Collection<Project> projects,
      Location location,
      Collection<Diagnostic> diagnostics);

  /**
   * @param resource Project root resource (passed to BuildTool.validate())
   * @param inputs Resolved inputs (to access Project artifact)
   * @param diagnostics where to store any validation issues
   * @return something that can validate and generate build configuration, or empty if there are
   *     issues
   */
  Optional<BuildTool> buildTool(
      Resource<?> resource, ResolvedInputs inputs, Collection<Diagnostic> diagnostics);

  AppliedSuggestion applySuggestion(
      String suggestionCode, Resource<?> resource, ResolvedInputs inputs);
}
