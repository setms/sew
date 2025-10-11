package org.setms.km.outbound.diagram.jgraphx;

import static java.util.stream.Collectors.toSet;

public record PartialLane(Path combinedPath, Paths sources, boolean containsReversedPath) {

  public PartialLane {
    if (!sources.stream()
        .flatMap(Path::stream)
        .collect(toSet())
        .containsAll(combinedPath.toList())) {
      throw new IllegalArgumentException();
    }
  }

  public int size() {
    return combinedPath().size();
  }

  @Override
  public String toString() {
    return "%s from %s".formatted(combinedPath, sources);
  }

  Lane toLane() {
    return new Lane(combinedPath());
  }
}
