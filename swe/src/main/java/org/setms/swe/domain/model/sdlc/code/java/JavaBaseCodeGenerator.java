package org.setms.swe.domain.model.sdlc.code.java;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.design.FieldType;
import org.setms.swe.domain.model.sdlc.eventstorming.Aggregate;

@Getter(AccessLevel.PROTECTED)
@RequiredArgsConstructor
abstract class JavaBaseCodeGenerator {

  private final String topLevelPackage;

  protected String packageFor(Artifact artifact, String additionalPackage) {
    return applicationName().equals(artifact.getPackage())
        ? "%s.%s".formatted(topLevelPackage, additionalPackage)
        : "%s.%s.%s".formatted(topLevelPackage, artifact.getPackage(), additionalPackage);
  }

  protected String applicationName() {
    return topLevelPackage.substring(topLevelPackage.lastIndexOf('.') + 1);
  }

  protected static CodeArtifact codeArtifact(String packageName, String name, String code) {
    return new CodeArtifact(new FullyQualifiedName(packageName, name)).setCode(code);
  }

  protected String rootEntityNameOf(Aggregate aggregate) {
    return aggregate.getRoot() != null ? aggregate.getRoot().getId() : aggregate.getName();
  }

  protected String toJavaType(FieldType type) {
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
}
