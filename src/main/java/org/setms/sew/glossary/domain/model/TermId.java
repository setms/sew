package org.setms.sew.glossary.domain.model;

import static java.util.UUID.randomUUID;
import static lombok.AccessLevel.PRIVATE;
import static org.setms.sew.util.Validation.validate;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor(access = PRIVATE)
public class TermId {

  @NotNull UUID value;

  @Builder
  public static TermId of(UUID value) {
    return validate(new TermId(value));
  }

  public static TermId random() {
    return of(randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
