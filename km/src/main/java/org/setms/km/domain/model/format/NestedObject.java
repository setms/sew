package org.setms.km.domain.model.format;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor
public class NestedObject extends DataObject<NestedObject> {

  private final String name;
  private String type;

  public NestedObject setType(String type) {
    this.type = type;
    return this;
  }

  @Override
  public String toString() {
    return "%s%s".formatted(name, super.toString());
  }
}
