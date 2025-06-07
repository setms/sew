package org.setms.sew.core.domain.model.sdlc;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class HasTypeValidator implements ConstraintValidator<HasType, Pointer> {

  private String type;

  @Override
  public void initialize(HasType constraintAnnotation) {
    type = constraintAnnotation.type();
  }

  @Override
  public boolean isValid(Pointer value, ConstraintValidatorContext context) {
    return value.isType(type);
  }
}
