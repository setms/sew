package org.setms.sew.core.domain.model.sdlc;

import static org.setms.sew.core.domain.model.format.Strings.initLower;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode
public abstract class NamedObject implements Comparable<NamedObject> {

  private final FullyQualifiedName fullyQualifiedName;

  public String type() {
    return initLower(getClass().getSimpleName());
  }

  public String getName() {
    return fullyQualifiedName.getName();
  }

  public String getPackage() {
    return fullyQualifiedName.getPackage();
  }

  public Pointer pointerTo() {
    return new Pointer(initLower(getClass().getSimpleName()), getName());
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
