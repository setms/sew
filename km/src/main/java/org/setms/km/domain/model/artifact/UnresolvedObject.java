package org.setms.km.domain.model.artifact;

import lombok.Getter;

public class UnresolvedObject extends Artifact {

  @Getter private final String type;

  public UnresolvedObject(FullyQualifiedName fullyQualifiedName, String type) {
    super(fullyQualifiedName);
    this.type = type;
  }

  @Override
  public String toString() {
    return "%s(%s)".formatted(type, super.toString());
  }
}
