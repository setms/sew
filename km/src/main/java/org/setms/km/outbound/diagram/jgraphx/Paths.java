package org.setms.km.outbound.diagram.jgraphx;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@EqualsAndHashCode
public class Paths {

  private final List<Path> items = new ArrayList<>();

  public Paths(Path path) {
    items.add(path);
  }

  public Paths(Set<Path> paths) {
    items.addAll(paths);
  }

  public Paths(Paths paths) {
    items.addAll(paths.items);
  }

  public Path getFirst() {
    return items.getFirst();
  }

  public Stream<Path> stream() {
    return items.stream();
  }

  public void add(Path path) {
    if (!items.contains(path)) {
      items.add(path);
    }
  }

  public void remove(Path path) {
    items.remove(path);
  }

  public void removeAll(Paths paths) {
    items.removeAll(paths.items);
  }

  public void replace(Path existing, Path replacement) {
    var index = items.indexOf(existing);
    if (index < 0) {
      throw new IllegalArgumentException("Unknown path: " + existing);
    }
    items.set(index, replacement);
  }

  public int size() {
    return items.size();
  }

  public boolean isEmpty() {
    return items.isEmpty();
  }

  public boolean hasItems() {
    return !isEmpty();
  }

  public boolean contains(Path path) {
    return items.contains(path);
  }

  Paths with(Path path) {
    var result = new LinkedHashSet<>(items);
    result.add(path);
    return new Paths(result);
  }

  @Override
  public String toString() {
    return "[%s]".formatted(items.stream().map(Path::toString).collect(joining(", ")));
  }
}
