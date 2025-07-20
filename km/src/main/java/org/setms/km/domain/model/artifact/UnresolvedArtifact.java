package org.setms.km.domain.model.artifact;

import lombok.Getter;

public class UnresolvedArtifact extends Artifact {

  @Getter private final String type;

  public UnresolvedArtifact(FullyQualifiedName fullyQualifiedName, String type) {
    super(fullyQualifiedName);
    this.type = type;
  }

  @Override
  public String toString() {
    return "%s(%s)".formatted(type, super.toString());
  }
}
