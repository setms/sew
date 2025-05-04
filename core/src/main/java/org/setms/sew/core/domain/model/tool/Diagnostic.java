package org.setms.sew.core.domain.model.tool;

import static java.util.Collections.emptyList;

import java.util.SequencedCollection;

public record Diagnostic(
    Level level, String message, Location location, SequencedCollection<Suggestion> suggestions) {

  public Diagnostic(Level level, String message) {
    this(level, message, null);
  }

  public Diagnostic(Level level, String message, Location location) {
    this(level, message, location, emptyList());
  }
}
