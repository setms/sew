package org.setms.sew.core.glossary.domain.model;

import static lombok.AccessLevel.PRIVATE;
import static org.setms.sew.core.util.Validation.validate;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@AllArgsConstructor(access = PRIVATE)
@EqualsAndHashCode(of = "id")
public class Term {

  @NotNull TermId id;
  @NotEmpty Phrase phrase;
  @NotEmpty Definition definition;

  @Builder
  public static Term of(TermId id, Phrase phrase, Definition definition) {
    return validate(new Term(id, phrase, definition));
  }

  @Override
  public String toString() {
    return "Term[%s]: %s=%s".formatted(id, phrase, definition);
  }
}
