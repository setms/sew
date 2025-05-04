package org.setms.sew.core.domain.model.sdlc;

import jakarta.validation.constraints.NotEmpty;
import java.util.Collection;
import java.util.Optional;
import lombok.Value;

@Value
@SuppressWarnings("ClassCanBeRecord")
public class Pointer {

  String type;
  @NotEmpty String id;

  public <T extends NamedObject> Optional<T> resolveFrom(Collection<T> candidates) {
    return candidates.stream()
        .filter(t -> id.equals(t.getName()))
        .findFirst();
  }

  @Override
  public String toString() {
    return type == null ? "-> %s".formatted(id) : "-> %s(%s)".formatted(type, id);
  }
}
