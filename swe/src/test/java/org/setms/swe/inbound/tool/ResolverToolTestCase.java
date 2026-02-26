package org.setms.swe.inbound.tool;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.util.Collection;
import java.util.Optional;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.Tool;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

abstract class ResolverToolTestCase<A extends Artifact> extends ToolTestCase<A> {

  protected ResolverToolTestCase(Tool tool, Class<A> type, String sourceLocation) {
    super(tool, type, sourceLocation);
  }

  protected Diagnostic givenResolverAddingDiagnostic(TechnologyResolver resolver) {
    var diagnostic = new Diagnostic(WARN, "Something's not right");
    when(resolver.codeGenerator(any(), anyCollection()))
        .thenAnswer(
            invocation -> {
              Collection<Diagnostic> diagnostics = invocation.getArgument(1);
              diagnostics.add(diagnostic);
              return Optional.empty();
            });
    return diagnostic;
  }
}
