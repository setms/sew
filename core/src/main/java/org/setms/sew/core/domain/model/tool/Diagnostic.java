package org.setms.sew.core.domain.model.tool;


import java.util.Arrays;
import java.util.SequencedCollection;

public record Diagnostic(
    Level level, String message, Location location, SequencedCollection<Suggestion> suggestions) {

  public Diagnostic(Level level, String message) {
    this(level, message, null);
  }

  public Diagnostic(Level level, String message, Location location, Suggestion... suggestions) {
    this(level, message, location, Arrays.asList(suggestions));
  }
}
