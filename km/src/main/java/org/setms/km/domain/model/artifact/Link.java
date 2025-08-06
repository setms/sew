package org.setms.km.domain.model.artifact;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import jakarta.validation.constraints.NotEmpty;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
public class Link implements Comparable<Link> {

  String type;
  @NotEmpty String id;
  @EqualsAndHashCode.Exclude Map<String, List<Link>> attributes;

  public Link(String type, String id) {
    this(type, id, emptyMap());
  }

  public Link(Artifact object) {
    this(object.type(), object.getName());
  }

  public List<Link> optAttribute(String name) {
    return attributes.getOrDefault(name, emptyList());
  }

  public <T extends Artifact> Optional<T> resolveFrom(Collection<T> candidates) {
    return Optional.ofNullable(candidates).stream()
        .flatMap(Collection::stream)
        .filter(t -> id.equals(t.getName()))
        .findFirst();
  }

  public boolean hasType(String type) {
    return this.type != null && this.type.equals(type);
  }

  public static Predicate<Link> testType(String type) {
    return link -> link.hasType(type);
  }

  public Predicate<Link> testEqual() {
    return this::equals;
  }

  public boolean pointsTo(Artifact object) {
    return hasType(object.type()) && id.equals(object.getName());
  }

  @Override
  public int compareTo(Link that) {
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
