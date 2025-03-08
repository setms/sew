package org.setms.sew.glossary.domain.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.NoArgsConstructor;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
class ValidatorFactory {

  private static final Validator validator = initValidator();

  private static Validator initValidator() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      return factory.getValidator();
    }
  }

  public static <T> T validate(T instance) {
    var violations =
        validator.validate(instance).stream()
            .map(ConstraintViolation::getMessage)
            .collect(joining(lineSeparator()));
    if (!violations.isEmpty()) {
      throw new IllegalArgumentException(violations);
    }
    return instance;
  }
}
