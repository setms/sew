package org.setms.sew.core.domain.model.format;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NestedObject extends DataObject<NestedObject> {

  private final String name;

  @Override
  public String toString() {
    return "%s%s".formatted(name, super.toString());
  }
}
