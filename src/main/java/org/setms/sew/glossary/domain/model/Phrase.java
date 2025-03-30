package org.setms.sew.glossary.domain.model;

import static lombok.AccessLevel.PRIVATE;
import static org.setms.sew.util.Validation.validate;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

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
