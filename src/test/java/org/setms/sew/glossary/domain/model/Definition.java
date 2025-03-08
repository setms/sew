package org.setms.sew.glossary.domain.model;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;
import static org.setms.sew.glossary.domain.model.ValidatorFactory.validate;

@Value
@AllArgsConstructor(access = PRIVATE)
public class Definition {

  @NotEmpty String value;

  @Builder
  public static Definition of(String value) {
    return validate(new Definition(value));
  }

  @Override
  public String toString() {
    return value;
  }
}
