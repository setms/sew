package org.setms.sew.core.domain.model.sdlc;

import static java.util.Collections.emptyMap;

import jakarta.validation.constraints.NotEmpty;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import lombok.Value;

@Value
public class Pointer implements Comparable<Pointer> {

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

  public Optional<Pointer> optAttribute(String name) {
    return Optional.ofNullable(attributes.get(name));
  }

  public <T extends NamedObject> Optional<T> resolveFrom(Collection<T> candidates) {
    return Optional.ofNullable(candidates).stream()
        .flatMap(Collection::stream)
        .filter(t -> id.equals(t.getName()))
        .findFirst();
  }

  @Override
  public int compareTo(Pointer that) {
    var result = this.type.compareTo(that.type);
    if (result == 0) {
      result = this.id.compareTo(that.id);
    }
    return result;
  }

  @Override
  public String toString() {
    return type == null ? id : "%s(%s)".formatted(type, id);
  }
}
