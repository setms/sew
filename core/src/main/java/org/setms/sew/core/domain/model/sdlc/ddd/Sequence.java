package org.setms.sew.core.domain.model.sdlc.ddd;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import org.setms.sew.core.domain.model.sdlc.Pointer;
import org.setms.sew.core.domain.model.sdlc.PointerResolver;

public record Sequence(List<Pointer> items) {

  public Sequence(Pointer... items) {
    this(asList(items));
  }

  public boolean isEmpty() {
    return items.isEmpty();
  }

  public Pointer first() {
    return items.getFirst();
  }

  public Pointer last() {
    return items.getLast();
  }

  public Sequence until(Pointer item) {
    return new Sequence(items.subList(0, 1 + items.indexOf(item)));
  }

  public Collection<Sequence> subSequencesMatching(List<Predicate<Pointer>> predicates) {
    var result = new ArrayList<Sequence>();
    var predicateIndex = 0;
    for (var i = 0; i < items.size(); i++) {
      if (predicates.get(predicateIndex).test(items.get(i))) {
        predicateIndex++;
        if (predicateIndex == predicates.size()) {
          result.add(new Sequence(items.subList(i - predicates.size() + 1, i + 1)));
          predicateIndex = 0;
        }
      } else {
        predicateIndex = 0;
      }
    }
    return result;
  }

  public ResolvedSequence resolve(PointerResolver resolver) {
    return new ResolvedSequence(items.stream().map(resolver::resolve).toList());
  }

  @Override
  public String toString() {
    return items.stream().map(Pointer::toString).collect(joining(" -> "));
  }
}
