package org.setms.sew.core.domain.model.tool;

import java.util.SequencedCollection;

import static java.util.Collections.emptyList;

public record Diagnostic(Level level, String message, SequencedCollection<Suggestion> suggestions) {

  public Diagnostic(Level level, String message) {
    this(level, message, emptyList());
  }
}
