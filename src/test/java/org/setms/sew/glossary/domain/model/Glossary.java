package org.setms.sew.glossary.domain.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Collection;

import static lombok.AccessLevel.PRIVATE;
import static org.setms.sew.glossary.domain.model.ValidatorFactory.validate;

@Value
@AllArgsConstructor(access = PRIVATE)
@EqualsAndHashCode(of = "id")
public class Glossary {

  @NotNull GlossaryId id;
  @NotNull Collection<Term> terms;

  @Builder
  public static Glossary of(GlossaryId id, Collection<Term> terms) {
    return validate(new Glossary(id, terms));
  }
  @Override
  public String toString() {
    return "Glossary[%s]: %d terms".formatted(id, terms.size());
  }
}
