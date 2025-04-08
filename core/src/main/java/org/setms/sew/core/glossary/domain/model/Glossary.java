package org.setms.sew.core.glossary.domain.model;

import static lombok.AccessLevel.PRIVATE;
import static org.setms.sew.core.util.Validation.validate;

import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

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
