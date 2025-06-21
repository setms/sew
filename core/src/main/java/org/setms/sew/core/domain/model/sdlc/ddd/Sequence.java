package org.setms.sew.core.domain.model.sdlc.ddd;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;
import org.setms.sew.core.domain.model.sdlc.Pointer;
import org.setms.sew.core.domain.model.sdlc.PointerResolver;

public record Sequence(List<Pointer> items) {

  public Sequence(Pointer... items) {
    this(asList(items));
  }

  public Pointer first() {
    return items.getFirst();
  }

  public Pointer last() {
    return items.getLast();
  }

  public Sequence append(Pointer item) {
    var newItems = new ArrayList<>(items);
    newItems.add(item);
    return new Sequence(newItems);
  }

  public ResolvedSequence resolve(PointerResolver resolver) {
    return new ResolvedSequence(items.stream().map(resolver::resolve).toList());
  }
}
