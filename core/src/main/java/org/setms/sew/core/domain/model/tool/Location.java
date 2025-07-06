package org.setms.sew.core.domain.model.tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.setms.sew.core.domain.model.sdlc.NamedObject;

public record Location(List<String> segments) {

  public Location(String... segments) {
    this(collect(segments));
  }

  public Location(NamedObject object) {
    this(List.of(object.getPackage(), object.type(), object.getName()));
  }

  public boolean isInside(NamedObject object) {
    return segments.get(0).equals(object.getPackage())
        && segments.get(1).equals(object.type())
        && segments.get(2).equals(object.getName());
  }

  private static List<String> collect(String[] segments) {
    return Arrays.stream(segments).filter(Objects::nonNull).toList();
  }

  public Location plus(NamedObject object) {
    return plus(List.of(object.type(), object.getName()));
  }

  public Location plus(String... segments) {
    return plus(collect(segments));
  }

  public Location plus(List<String> segments) {
    var result = new ArrayList<>(this.segments);
    result.addAll(segments);
    return new Location(result);
  }

  public <T> Location plus(String collectionName, List<T> collection, T item) {
    return plus(List.of("%s[%d]".formatted(collectionName, collection.indexOf(item))));
  }

  @Override
  public String toString() {
    return String.join("/", segments);
  }
}
