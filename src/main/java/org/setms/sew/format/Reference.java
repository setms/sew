package org.setms.sew.format;

import lombok.Value;

@Value
@SuppressWarnings("ClassCanBeRecord")
public class Reference implements DataItem {

  String id;

  @Override
  public String toString() {
    return id;
  }
}
