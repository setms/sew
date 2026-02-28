package org.setms.swe.domain.model.sdlc.technology;

import java.util.Collection;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;

public interface CodeTester {

  /**
   * Run the project's tests and collect diagnostics for any failures found.
   *
   * @param resource the workspace root resource
   * @param diagnostics where to add diagnostics for test failures
   */
  void test(Resource<?> resource, Collection<Diagnostic> diagnostics);
}
