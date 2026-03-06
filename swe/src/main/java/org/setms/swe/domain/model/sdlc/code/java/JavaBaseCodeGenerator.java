package org.setms.swe.domain.model.sdlc.code.java;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;

@Getter(AccessLevel.PROTECTED)
@RequiredArgsConstructor
abstract class JavaBaseCodeGenerator {

  private final String topLevelPackage;

  protected String packageFor(Artifact artifact, String additionalPackage) {
    var lastSegment = topLevelPackage.substring(topLevelPackage.lastIndexOf('.') + 1);
    return lastSegment.equals(artifact.getPackage())
        ? "%s.%s".formatted(topLevelPackage, additionalPackage)
        : "%s.%s.%s".formatted(topLevelPackage, artifact.getPackage(), additionalPackage);
  }

  protected static CodeArtifact codeArtifact(String packageName, String name, String code) {
    return new CodeArtifact(new FullyQualifiedName(packageName, name)).setCode(code);
  }
}
