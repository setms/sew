package org.setms.km.domain.model.validation;

import java.util.Arrays;
import java.util.Optional;
import java.util.SequencedCollection;

public record Diagnostic(
    Level level, String message, Location location, SequencedCollection<Suggestion> suggestions)
    implements Comparable<Diagnostic> {

  public Diagnostic(Level level, String message) {
    this(level, message, null);
  }

  public Diagnostic(Level level, String message, Location location, Suggestion... suggestions) {
    this(level, message, location, Arrays.asList(suggestions));
  }

  public boolean hasSuggestion() {
    return !suggestions.isEmpty();
  }

  public boolean hasSingleSuggestion() {
    return suggestions.size() == 1;
  }

  @Override
  public int compareTo(Diagnostic that) {
    var result = this.message.compareTo(that.message);
    if (result == 0) {
      result = this.locationString().compareTo(that.locationString());
    }
    return result;
  }

  private String locationString() {
    return Optional.ofNullable(location).map(Location::toString).orElse("");
  }

  @Override
  public String toString() {
    return "%s%s: %s"
        .formatted(level, location == null ? "" : " at %s".formatted(location), message);
  }
}
