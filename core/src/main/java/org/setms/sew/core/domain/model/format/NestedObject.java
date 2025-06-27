package org.setms.sew.core.domain.model.format;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor
public class NestedObject extends DataObject<NestedObject> {

  private final String name;

  @Override
  public String toString() {
    return "%s%s".formatted(name, super.toString());
  }
}
