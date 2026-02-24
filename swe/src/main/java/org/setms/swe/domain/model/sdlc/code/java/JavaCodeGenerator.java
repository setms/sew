package org.setms.swe.domain.model.sdlc.code.java;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.technology.CodeGenerator;

public class JavaCodeGenerator extends JavaArtifactGenerator implements CodeGenerator {

  public static Optional<CodeGenerator> from(
      ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    return topLevelPackage(inputs, diagnostics).map(_ -> new JavaCodeGenerator());
  }

  @Override
  public List<CodeArtifact> generate(Command command) {
    throw new UnsupportedOperationException("Not yet implemented");
  }
}
