package org.setms.sew.core.domain.model.sdlc;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.setms.sew.core.domain.model.format.Strings.initLower;

import jakarta.validation.constraints.NotEmpty;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
public class Pointer implements Comparable<Pointer> {

  String type;
  @NotEmpty String id;
  @EqualsAndHashCode.Exclude Map<String, List<Pointer>> attributes;

  public Pointer(String type, String id, Map<String, List<Pointer>> attributes) {
    this.type = type;
    this.id = id;
    this.attributes = attributes;
  }

  public Pointer(String type, String id) {
    this(type, id, emptyMap());
  }

  public Pointer(NamedObject object) {
    this(typeOf(object), object.getName());
  }

  public static String typeOf(NamedObject object) {
    return initLower(object.getClass().getSimpleName());
  }

  public boolean pointsTo(NamedObject object) {
    return getType().equals(typeOf(object)) && getId().equals(object.getName());
  }

  public List<Pointer> optAttribute(String name) {
    return attributes.getOrDefault(name, emptyList());
  }

  public <T extends NamedObject> Optional<T> resolveFrom(Collection<T> candidates) {
    return Optional.ofNullable(candidates).stream()
        .flatMap(Collection::stream)
        .filter(t -> id.equals(t.getName()))
        .findFirst();
  }

  public Pointer withoutAttributes() {
    return attributes.isEmpty() ? this : new Pointer(type, id);
  }

  public boolean isType(String type) {
    return this.type.equals(type);
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
