package org.setms.sew.core.domain.model.format;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Value;

@Value
public class Reference implements DataItem {

  String type;
  String id;
  Map<String, List<Reference>> attributes;

  public Reference(String type, String id, Map<String, List<Reference>> attributes) {
    this.type = type;
    this.id = id;
    this.attributes = attributes;
  }

  public Reference(String type, String id) {
    this(type, id, Collections.emptyMap());
  }

  public Reference(String id) {
    this(null, id);
  }

  @Override
  public String toString() {
    return id;
  }
}
