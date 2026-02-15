package org.setms.km.domain.model.tool;

import static org.setms.km.domain.model.validation.Level.WARN;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;

class FooTool extends ArtifactTool<Foo> {

  static final String MESSAGE = "Some message";
  static final String CODE = "Some code";
  static final String SUGGESTION = "Some suggestion";

  @Override
  public Set<Input<? extends Foo>> validationTargets() {
    return Set.of(new GlobInput<>("foo", null, Foo.class));
  }

  @Override
  public void validate(Foo foo, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    diagnostics.add(
        new Diagnostic(
            WARN, MESSAGE, new Location("foo", "Baz"), List.of(new Suggestion(CODE, SUGGESTION))));
  }
}
