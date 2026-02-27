package org.setms.swe.domain.model.sdlc.code.java;

import static java.util.stream.Collectors.joining;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.format.Strings;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.design.Field;
import org.setms.swe.domain.model.sdlc.design.FieldType;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;
import org.setms.swe.domain.model.sdlc.technology.CodeGenerator;

@RequiredArgsConstructor
public class JavaCodeGenerator extends JavaArtifactGenerator implements CodeGenerator {

  private final String topLevelPackage;

  public static Optional<CodeGenerator> from(
      ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    return topLevelPackage(inputs, diagnostics).map(JavaCodeGenerator::new);
  }

  @Override
  public List<CodeArtifact> generate(Command command, Entity payload) {
    return generateDomainObjectFor(command, payload);
  }

  private List<CodeArtifact> generateDomainObjectFor(Artifact artifact, Entity payload) {
    var packageName = packageNameFor(artifact);
    var className = artifact.getName();
    var components = componentsFor(payload);
    var imports = importsFor(payload);
    var importSection = imports.isEmpty() ? "" : imports + "\n\n";
    var code =
        "package %s;\n\n%spublic record %s(%s) {}\n"
            .formatted(packageName, importSection, className, components);
    return List.of(new CodeArtifact(new FullyQualifiedName(packageName, className)).setCode(code));
  }

  private String packageNameFor(Artifact artifact) {
    var commandPackage = artifact.getPackage();
    var lastSegment = topLevelPackage.substring(topLevelPackage.lastIndexOf('.') + 1);
    return lastSegment.equals(commandPackage)
        ? "%s.domain.model".formatted(topLevelPackage)
        : "%s.%s.domain.model".formatted(topLevelPackage, commandPackage);
  }

  private String componentsFor(Entity payload) {
    return fieldsOf(payload)
        .map(f -> "%s %s".formatted(toJavaType(f.getType()), Strings.initLower(f.getName())))
        .collect(joining(", "));
  }

  private static Stream<Field> fieldsOf(Entity payload) {
    return Optional.ofNullable(payload.getFields()).stream().flatMap(Collection::stream);
  }

  private static String toJavaType(FieldType type) {
    return switch (type) {
      case TEXT, SELECTION -> "String";
      case NUMBER -> "int";
      case BOOLEAN -> "boolean";
      case DATE -> "LocalDate";
      case TIME -> "LocalTime";
      case DATETIME -> "LocalDateTime";
      case ID -> "UUID";
    };
  }

  private String importsFor(Entity payload) {
    return fieldsOf(payload)
        .map(f -> toImport(f.getType()))
        .flatMap(Optional::stream)
        .distinct()
        .sorted()
        .map("import %s;"::formatted)
        .collect(joining("\n"));
  }

  private static Optional<String> toImport(FieldType type) {
    return switch (type) {
      case DATETIME -> Optional.of("java.time.LocalDateTime");
      case DATE -> Optional.of("java.time.LocalDate");
      case TIME -> Optional.of("java.time.LocalTime");
      default -> Optional.empty();
    };
  }

  @Override
  public List<CodeArtifact> generate(Event event, Entity payload) {
    return generateDomainObjectFor(event, payload);
  }
}
