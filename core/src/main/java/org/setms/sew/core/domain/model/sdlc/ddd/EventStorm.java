package org.setms.sew.core.domain.model.sdlc.ddd;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static lombok.AccessLevel.PACKAGE;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import org.setms.sew.core.domain.model.sdlc.Pointer;
import org.setms.sew.core.domain.model.sdlc.usecase.Scenario;
import org.setms.sew.core.domain.model.sdlc.usecase.UseCase;

@NoArgsConstructor(access = PACKAGE)
public class EventStorm {

  private static final String ATTR_READS = "reads";
  private static final String ATTR_UPDATES = "updates";

  private final Collection<Follow> follows = new HashSet<>();

  public EventStorm(Collection<UseCase> useCases) {
    useCases.stream()
        .flatMap(UseCase::scenarios)
        .map(Scenario::getSteps)
        .forEach(
            steps -> {
              for (var i = 0; i < steps.size() - 1; i++) {
                addStep(steps.get(i));
                add(steps.get(i), steps.get(i + 1));
              }
              addStep(steps.getLast());
            });
  }

  private void addStep(Pointer step) {
    step.optAttribute(ATTR_READS).forEach(readModel -> add(readModel, step));
    step.optAttribute(ATTR_UPDATES).forEach(readModel -> add(step, readModel));
  }

  void add(Pointer element, Pointer followedBy) {
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
    var found =
        follows.stream()
            .filter(f -> f.element().isType(types.getFirst()) && f.followedBy.isType(types.get(1)))
            .map(f -> new Sequence(f.element(), f.followedBy()))
            .toList();
    if (types.size() == 2) {
      sequences.addAll(found);
      return;
    }
    found.stream().flatMap(sequence -> findSequences(sequence, types, 2)).forEach(sequences::add);
  }

  private Stream<Sequence> findSequences(Sequence current, List<String> types, int typeIndex) {
    var found =
        follows.stream()
            .filter(
                f ->
                    f.element().equals(current.last())
                        && f.followedBy().isType(types.get(typeIndex)))
            .map(Follow::followedBy)
            .map(current::append);
    if (typeIndex == types.size() - 1) {
      return found;
    }
    return found.flatMap(sequence -> findSequences(sequence, types, typeIndex + 1));
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
