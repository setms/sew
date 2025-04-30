package org.setms.sew.core.domain.model.schema;

import java.util.Collection;
import lombok.Value;

@Value
@SuppressWarnings("ClassCanBeRecord")
public class Pointer {

  String type;
  String id;

  public <T extends NamedObject> T resolveFrom(Collection<T> candidates) {
    return candidates.stream()
        .filter(t -> id.equals(t.getName()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Dangling pointer " + id));
  }

  @Override
  public String toString() {
    return type == null ? "-> %s".formatted(id) : "-> %s(%s)".formatted(type, id);
  }
}
