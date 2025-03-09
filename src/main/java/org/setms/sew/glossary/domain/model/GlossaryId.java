package org.setms.sew.glossary.domain.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

import static java.util.UUID.randomUUID;
import static lombok.AccessLevel.PRIVATE;
import static org.setms.sew.glossary.domain.model.ValidatorFactory.validate;

@Value
@AllArgsConstructor(access = PRIVATE)
public class GlossaryId {

  @NotNull UUID value;

  @Builder
  public static GlossaryId of(UUID value) {
    return validate(new GlossaryId(value));
  }

  public static GlossaryId random() {
    return of(randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
