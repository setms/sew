package org.setms.sew.core.domain.model.sdlc;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode
public abstract class NamedObject implements Comparable<NamedObject> {

  private final FullyQualifiedName fullyQualifiedName;

  public String getName() {
    return fullyQualifiedName.getName();
  }

  public String getPackage() {
    return fullyQualifiedName.getPackage();
  }

  @Override
  public String toString() {
    return fullyQualifiedName.toString();
  }

  @Override
  public int compareTo(NamedObject that) {
    return this.fullyQualifiedName.compareTo(that.fullyQualifiedName);
  }
}
