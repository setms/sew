package org.setms.sew.core.domain.model.sdlc;

import static java.util.Collections.emptyMap;

import jakarta.validation.constraints.NotEmpty;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import lombok.Value;

@Value
public class Pointer {

  String type;
  @NotEmpty String id;
  Map<String, Pointer> attributes;

  public Pointer(String type, String id, Map<String, Pointer> attributes) {
    this.type = type;
    this.id = id;
    this.attributes = attributes;
  }

  public Pointer(String type, String id) {
    this(type, id, emptyMap());
  }

  public <T extends NamedObject> Optional<T> resolveFrom(Collection<T> candidates) {
    return Optional.ofNullable(candidates).stream()
        .flatMap(Collection::stream)
        .filter(t -> id.equals(t.getName()))
        .findFirst();
  }

  @Override
  public String toString() {
    return type == null ? "-> %s".formatted(id) : "-> %s(%s)".formatted(type, id);
  }
}
