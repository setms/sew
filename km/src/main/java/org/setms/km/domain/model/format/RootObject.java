package org.setms.km.domain.model.format;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RootObject extends DataObject<RootObject> {

  private final String scope;
  private final String type;
  private final String name;

  @Override
  public String toString() {
    return "%s(%s.%s) %s".formatted(type, scope, name, super.toString());
  }
}
