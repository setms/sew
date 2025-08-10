package org.setms.swe.domain.model.sdlc.ddd;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static lombok.AccessLevel.PACKAGE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.setms.km.domain.model.artifact.Link;
import org.setms.swe.domain.model.sdlc.usecase.Scenario;
import org.setms.swe.domain.model.sdlc.usecase.UseCase;

@NoArgsConstructor(access = PACKAGE)
public class EventStorm {

  private static final String ATTR_READS = "reads";
  private static final String ATTR_UPDATES = "updates";

  @Getter(PACKAGE)
  private final Collection<Sequence> sequences = new LinkedHashSet<>();

  public EventStorm(Collection<UseCase> useCases) {
    useCases.stream()
        .flatMap(UseCase::scenarios)
        .map(Scenario::getSteps)
        .filter(not(Collection::isEmpty))
        .forEach(
            steps ->
                precursorsOf(steps.getFirst())
                    .forEach(
                        precursor -> {
                          sequences.add(toSequence(precursor.items(), steps));
                          splitFromReads(precursor, steps);
                          splitFromUpdates(precursor, steps);
                          sequences.remove(precursor);
                        }));
  }

  private List<Sequence> precursorsOf(Link start) {
    if (start.hasType("event")) {
      var result =
          sequences.stream()
              .map(sequence -> sequence.until(start))
              .filter(not(Sequence::isEmpty))
              .toList();
      if (!result.isEmpty()) {
        return result;
      }
    }
    return List.of(new Sequence());
  }

  public void splitFromReads(Sequence precursor, List<Link> steps) {
    steps.stream()
        .filter(step -> !step.optAttribute(ATTR_READS).isEmpty())
        .forEach(
            step -> {
              var index = steps.indexOf(step);
              step.optAttribute(ATTR_READS).stream()
                  .map(
                      readModel ->
                          toSequence(
                              precursor.items(),
                              List.of(readModel),
                              steps.subList(index, steps.size())))
                  .forEach(sequences::add);
            });
  }

  @SafeVarargs
  private Sequence toSequence(List<Link>... sequences) {
    var items = new ArrayList<Link>();
    for (var sequence : sequences) {
      if (!items.isEmpty() && items.getLast().equals(sequence.getFirst())) {
        items.addAll(sequence.subList(1, sequence.size()));
      } else {
        items.addAll(sequence);
      }
    }
    return new Sequence(items);
  }

  private void splitFromUpdates(Sequence precursor, List<Link> steps) {
    steps.stream()
        .filter(step -> !step.optAttribute(ATTR_UPDATES).isEmpty())
        .forEach(
            step -> {
              var index = steps.indexOf(step);
              step.optAttribute(ATTR_UPDATES).stream()
                  .map(
                      readModel ->
                          toSequence(
                              precursor.items(), steps.subList(0, index + 1), List.of(readModel)))
                  .forEach(sequences::add);
            });
  }

  public Set<Link> elements() {
    return sequences.stream().map(Sequence::items).flatMap(Collection::stream).collect(toSet());
  }

  public Stream<Sequence> findSequences(String... types) {
    return findSequences(Arrays.stream(types).map(Link::testType).toList());
  }

  public Stream<Sequence> findSequences(List<Predicate<Link>> tests) {
    return sequences.stream()
        .map(s -> s.subSequencesMatching(tests))
        .flatMap(Collection::stream)
        .distinct();
  }

  @Override
  public String toString() {
    return sequences.stream()
        .map(Sequence::toString)
        .sorted()
        .collect(joining(System.lineSeparator()));
  }
}
