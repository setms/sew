package org.setms.sew.schema;

import lombok.Value;

@Value
@SuppressWarnings("ClassCanBeRecord")
public class Pointer {

  String id;

  @Override
  public String toString() {
    return "-> %s".formatted(id);
  }
}
