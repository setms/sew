package org.setms.sew.core.domain.model.sdlc;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import jakarta.validation.constraints.NotEmpty;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
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
    this(object.type(), object.getName());
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

  public boolean isType(String type) {
    return this.type != null && this.type.equals(type);
  }

  public static Predicate<Pointer> testType(String type) {
    return pointer -> pointer.isType(type);
  }

  public Predicate<Pointer> testEqual() {
    return this::equals;
  }

  public boolean pointsTo(NamedObject object) {
    return isType(object.type()) && id.equals(object.getName());
  }

  @Override
  public int compareTo(Pointer that) {
    if (this.type == null && that.type != null) {
      return -1;
    }
    if (this.type != null && that.type == null) {
      return 1;
    }
    if (this.type == null) {
      return this.id.compareTo(that.id);
    }
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
