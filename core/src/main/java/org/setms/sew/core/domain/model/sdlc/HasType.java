package org.setms.sew.core.domain.model.sdlc;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = HasTypeValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface HasType {
  String message() default "is missing or has wrong type";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  String type();
}
