package org.setms.km.domain.model.tool;

import static org.setms.km.domain.model.validation.Level.WARN;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;

class FooTool extends BaseTool {

  static final String MESSAGE = "Some message";
  static final String CODE = "Some code";
  static final String SUGGESTION = "Some suggestion";

  @Override
  public List<Input<?>> getInputs() {
    return List.of(new Input<>("foo", null, Foo.class));
  }

  @Override
  public Optional<Output> getOutputs() {
    return Optional.empty();
  }

  @Override
  protected void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    diagnostics.add(
        new Diagnostic(
            WARN, MESSAGE, new Location("foo", "Baz"), List.of(new Suggestion(CODE, SUGGESTION))));
  }
}
