package org.setms.km.domain.model.format;

import lombok.Value;

@Value
public class DataEnum implements DataItem {

  String name;

  @Override
  public String toString() {
    return "<%s>".formatted(name);
  }
}
