package org.setms.swe.domain.model.sdlc.architecture;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = RegisteredTopicAndChoiceValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisteredTopicAndChoice {

  String message() default "unknown topic or invalid choice for topic";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
