package org.setms.swe.domain.model.sdlc.technology;

import java.util.Collection;
import java.util.Optional;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;

/** Resolves technology decisions into implementations that use the decided-upon technology. */
public interface TechnologyResolver {

  /**
   * @param inputs Resolved inputs, including decisions and other artifacts
   * @param diagnostics where to store any validation issues
   * @return something that can generate unit tests, or empty if there are issues
   */
  Optional<UnitTestGenerator> unitTestGenerator(
      ResolvedInputs inputs, Collection<Diagnostic> diagnostics);

  /**
   * @param resource Workspace root resource
   * @param inputs Resolved inputs
   * @param diagnostics where to store any validation issues
   * @return something that can build code, or empty if there are issues
   */
  Optional<CodeBuilder> codeBuilder(
      Resource<?> resource, ResolvedInputs inputs, Collection<Diagnostic> diagnostics);

  /**
   * @param inputs Resolved inputs, including decisions and other artifacts
   * @param diagnostics where to store any validation issues
   * @return something that can generate production code, or empty if there are issues
   */
  Optional<CodeGenerator> codeGenerator(ResolvedInputs inputs, Collection<Diagnostic> diagnostics);

  /**
   * @param inputs Resolved inputs, including decisions and other artifacts
   * @param diagnostics where to store any validation issues
   * @return something that can test code, or empty if there are issues
   */
  Optional<CodeTester> codeTester(ResolvedInputs inputs, Collection<Diagnostic> diagnostics);

  AppliedSuggestion applySuggestion(
      String suggestionCode, Resource<?> resource, ResolvedInputs inputs);
}
