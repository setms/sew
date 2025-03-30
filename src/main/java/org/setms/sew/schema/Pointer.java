package org.setms.sew.schema;

import java.util.Collection;
import lombok.Value;

@Value
@SuppressWarnings("ClassCanBeRecord")
public class Pointer {

  String id;

  @Override
  public String toString() {
    return "-> %s".formatted(id);
  }

  public <T extends NamedObject> T resolveFrom(Collection<T> candidates) {
    return candidates.stream()
        .filter(t -> id.equals(t.getName()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Dangling pointer " + id));
  }
}
