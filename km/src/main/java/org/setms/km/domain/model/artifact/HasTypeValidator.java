package org.setms.km.domain.model.artifact;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;

public class HasTypeValidator implements ConstraintValidator<HasType, Object> {

  private String type;

  @Override
  public void initialize(HasType constraintAnnotation) {
    type = constraintAnnotation.value();
  }

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    return switch (value) {
      case null -> true;
      case Link link -> link.hasType(type);
      case List<?> links ->
          links.stream().allMatch(obj -> obj instanceof Link link && link.hasType(type));
      default -> false;
    };
  }
}
