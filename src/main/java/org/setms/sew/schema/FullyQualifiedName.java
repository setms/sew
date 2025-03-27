package org.setms.sew.schema;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode
public class FullyQualifiedName {

  private final String value;

  public String getName() {
    var index = value.lastIndexOf('.');
    return value.substring(index + 1);
  }

  public String getPackage() {
    var index = value.lastIndexOf('.');
    return index < 0 ? null : value.substring(0, index);
  }

  @Override
  public String toString() {
    return value;
  }
}
