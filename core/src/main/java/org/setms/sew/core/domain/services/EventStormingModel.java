package org.setms.sew.core.domain.services;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.setms.sew.core.domain.model.sdlc.Pointer;

class EventStormingModel {

  private final Collection<Follow> follows = new HashSet<>();

  public void add(Pointer element, Pointer followedBy) {
    follows.add(new Follow(element.withoutAttributes(), followedBy.withoutAttributes()));
  }

  public Set<Pointer> elements() {
    return follows.stream().flatMap(f -> Stream.of(f.element(), f.followedBy())).collect(toSet());
  }

  public Stream<Sequence> findSequences(String... types) {
    var result = new HashSet<Sequence>();
    addSequences(asList(types), result);
    return result.stream();
  }

  private void addSequences(List<String> types, Collection<Sequence> sequences) {
    if (types.size() < 2) {
      throw new IllegalArgumentException("Need at least 2 types to make a sequence");
    }
    if (types.size() > 3) {
      throw new UnsupportedOperationException("Can only find sequences up to 2 segments long");
    }
    var found =
        follows.stream()
            .filter(f -> f.element().isType(types.getFirst()) && f.followedBy.isType(types.get(1)))
            .toList();
    if (types.size() == 2) {
      found.stream().map(f -> new Sequence(f.element(), f.followedBy())).forEach(sequences::add);
      return;
    }
    found.stream()
        .flatMap(
            segment1 ->
                follows.stream()
                    .filter(
                        segment2 ->
                            segment2.element.equals(segment1.followedBy)
                                && segment2.followedBy.isType(types.get(2)))
                    .map(segment2 -> new Sequence(segment1.element, segment2.followedBy)))
        .forEach(sequences::add);
  }

  @Override
  public String toString() {
    return follows.stream()
        .map(f -> "%s -> %s".formatted(f.element, f.followedBy))
        .sorted()
        .collect(joining(System.lineSeparator()));
  }

  private record Follow(Pointer element, Pointer followedBy) {

    @Override
    public String toString() {
      return "%s -> %s".formatted(element, followedBy);
    }
  }
}
