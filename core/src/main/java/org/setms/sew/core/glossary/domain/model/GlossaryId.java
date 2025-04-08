package org.setms.sew.core.glossary.domain.model;

import static java.util.UUID.randomUUID;
import static lombok.AccessLevel.PRIVATE;
import static org.setms.sew.core.util.Validation.validate;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

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
