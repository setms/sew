package org.setms.swe.domain.model.sdlc.code.java;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.technology.CodeGenerator;

@RequiredArgsConstructor
public class JavaCodeGenerator extends JavaArtifactGenerator implements CodeGenerator {

  private final String topLevelPackage;

  public static Optional<CodeGenerator> from(
      ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    return topLevelPackage(inputs, diagnostics).map(JavaCodeGenerator::new);
  }

  @Override
  public List<CodeArtifact> generate(Command command) {
    var packageName = packageNameFor(command);
    var className = command.getName();
    var code = "package %s;\n\nclass %s {\n}\n".formatted(packageName, className);
    return List.of(new CodeArtifact(new FullyQualifiedName(packageName, className)).setCode(code));
  }

  private String packageNameFor(Command command) {
    var commandPackage = command.getPackage();
    var lastSegment = topLevelPackage.substring(topLevelPackage.lastIndexOf('.') + 1);
    return lastSegment.equals(commandPackage)
        ? "%s.domain.model".formatted(topLevelPackage)
        : "%s.%s.domain.model".formatted(topLevelPackage, commandPackage);
  }
}
