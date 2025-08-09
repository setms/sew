package org.setms.km.domain.model.tool;

import static org.setms.km.domain.model.validation.Level.WARN;

import java.util.Collection;
import java.util.List;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;

class FooTool extends BaseTool<Foo> {

  static final String MESSAGE = "Some message";
  static final String CODE = "Some code";
  static final String SUGGESTION = "Some suggestion";

  @Override
  public Input<Foo> getMainInput() {
    return new Input<>("foo", null, Foo.class);
  }

  @Override
  public void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    diagnostics.add(
        new Diagnostic(
            WARN, MESSAGE, new Location("foo", "Baz"), List.of(new Suggestion(CODE, SUGGESTION))));
  }
}
