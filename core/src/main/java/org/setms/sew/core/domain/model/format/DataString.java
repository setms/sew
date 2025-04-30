package org.setms.sew.core.domain.model.format;

import lombok.Value;

@Value
public class DataString implements DataItem {

  String value;

  @Override
  public String toString() {
    return "'%s'".formatted(value);
  }
}
