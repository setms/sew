package org.setms.sew.glossary.domain.model;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;
import static org.setms.sew.glossary.domain.model.ValidatorFactory.validate;

@Value
@AllArgsConstructor(access = PRIVATE)
public class Phrase {

  @NotEmpty String value;

  @Builder
  public static Phrase of(String value) {
    return validate(new Phrase(value));
  }

  @Override
  public String toString() {
    return value;
  }
}
