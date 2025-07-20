package org.setms.km.domain.model.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public record Location(List<String> segments) {

  public Location(String... segments) {
    this(collect(segments));
  }

  private static List<String> collect(String[] segments) {
    return Arrays.stream(segments).filter(Objects::nonNull).toList();
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
