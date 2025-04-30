package org.setms.sew.core.domain.model.format;

import lombok.Value;

@Value
public class Reference implements DataItem {

  String type;
  String id;

  public Reference(String type, String id) {
    this.type = type;
    this.id = id;
  }

  public Reference(String id) {
    this(null, id);
  }

  @Override
  public String toString() {
    return id;
  }
}
