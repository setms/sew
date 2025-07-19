package org.setms.km.domain.model.validation;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;
import static lombok.AccessLevel.PRIVATE;

import jakarta.validation.Validator;
import java.util.ArrayList;
import lombok.NoArgsConstructor;
import org.setms.km.domain.model.artifact.Artifact;

@NoArgsConstructor(access = PRIVATE)
public class Validation {

  private static final Validator validator = initValidator();

  private static Validator initValidator() {
    try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
      return factory.getValidator();
    }
  }

  public static <T> T validate(T instance) {
    var violations =
        validator.validate(instance).stream()
            .map(
                violation -> "%s %s".formatted(violation.getPropertyPath(), violation.getMessage()))
            .collect(joining(lineSeparator()));
    if (!violations.isEmpty()) {
      throw new IllegalArgumentException(violations);
    }
    if (instance instanceof Artifact namedObject) {
      var diagnostics = new ArrayList<Diagnostic>();
      namedObject.validate(new Location(namedObject), diagnostics);
      if (!diagnostics.isEmpty()) {
        throw new ValidationException(diagnostics);
      }
    }
    return instance;
  }
}
