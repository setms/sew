package org.setms.km.outbound.diagram.jgraphx;

import static java.util.stream.Collectors.toSet;

import org.jetbrains.annotations.NotNull;

public record PartialLane(Path combinedPath, Paths sources, boolean containsReversedPath) {

  public PartialLane {
    if (!sources.stream()
        .flatMap(Path::stream)
        .collect(toSet())
        .containsAll(combinedPath.toList())) {
      throw new IllegalArgumentException();
    }
  }

  public PartialLane withPrefix(Path prefix, boolean reversed) {
    return new PartialLane(
        (reversed ? prefix.reverse() : prefix).join(combinedPath),
        sources.with(prefix),
        reversed || containsReversedPath);
  }

  public PartialLane withSuffix(Path suffix, boolean reversed) {
    return new PartialLane(
        combinedPath.join(reversed ? suffix.reverse() : suffix),
        sources.with(suffix),
        reversed || containsReversedPath);
  }

  public int size() {
    return combinedPath().size();
  }

  public boolean containsSource(Path path) {
    return sources.contains(path);
  }

  @Override
  public @NotNull String toString() {
    return "%s from %s".formatted(combinedPath, sources);
  }

  Lane toLane() {
    return new Lane(combinedPath());
  }
}
