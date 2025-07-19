package org.setms.km.domain.model.validation;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

import java.util.Collection;
import lombok.Getter;

@Getter
public class ValidationException extends IllegalArgumentException {

  private final Collection<Diagnostic> diagnostics;

  public ValidationException(Collection<Diagnostic> diagnostics) {
      super(diagnostics.stream().map(Diagnostic::toString).collect(joining(lineSeparator())));
    this.diagnostics = diagnostics;
  }
}
