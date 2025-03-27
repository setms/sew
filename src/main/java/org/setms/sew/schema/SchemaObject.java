package org.setms.sew.schema;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode
public abstract class SchemaObject {

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
}
