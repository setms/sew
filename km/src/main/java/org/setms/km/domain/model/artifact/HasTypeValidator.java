package org.setms.km.domain.model.artifact;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class HasTypeValidator implements ConstraintValidator<HasType, Link> {

  private String type;

  @Override
  public void initialize(HasType constraintAnnotation) {
    type = constraintAnnotation.value();
  }

  @Override
  public boolean isValid(Link value, ConstraintValidatorContext context) {
    return value == null || value.hasType(type);
  }
}
