package org.setms.sew.core.domain.model.tool;

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
    var result = new ArrayList<>(this.segments);
    result.addAll(collect(segments));
    return new Location(result);
  }

  @Override
  public String toString() {
    return String.join("/", segments);
  }
}
