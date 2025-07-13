package org.setms.sew.core.domain.model.validation;

import java.util.Arrays;
import java.util.SequencedCollection;
import org.setms.sew.core.domain.model.tool.Suggestion;

public record Diagnostic(
    Level level, String message, Location location, SequencedCollection<Suggestion> suggestions) {

  public Diagnostic(Level level, String message) {
    this(level, message, null);
  }

  public Diagnostic(Level level, String message, Location location, Suggestion... suggestions) {
    this(level, message, location, Arrays.asList(suggestions));
  }

  @Override
  public String toString() {
    return "%s%s: %s"
        .formatted(level, location == null ? "" : " at %s".formatted(location), message);
  }
}
