package org.setms.sew.format;

import lombok.Value;

@Value
public class DataString implements DataItem {

  String value;

  @Override
  public String toString() {
    return "'%s'".formatted(value);
  }
}
