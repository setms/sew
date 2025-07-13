package org.setms.sew.core.domain.model.sdlc;

import jakarta.validation.constraints.NotEmpty;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode
public class FullyQualifiedName implements Comparable<FullyQualifiedName> {

  @NotEmpty private final String value;

  public FullyQualifiedName(String packageName, String name) {
    this("%s.%s".formatted(packageName, name));
  }

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

  @Override
  public int compareTo(FullyQualifiedName that) {
    return this.value.compareTo(that.value);
  }
}
