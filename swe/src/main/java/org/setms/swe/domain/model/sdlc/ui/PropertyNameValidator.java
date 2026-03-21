package org.setms.swe.domain.model.sdlc.ui;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PropertyNameValidator implements ConstraintValidator<PropertyName, Property> {

  @Override
  public boolean isValid(Property property, ConstraintValidatorContext context) {
    return Properties.names().contains(property.getName());
  }
}
