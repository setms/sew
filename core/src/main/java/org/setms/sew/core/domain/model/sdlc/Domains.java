package org.setms.sew.core.domain.model.sdlc;

import jakarta.validation.constraints.NotEmpty;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Domains extends NamedObject {

  @NotEmpty private List<Domain> domains;

  public Domains(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  @Getter
  @Setter
  @Accessors(chain = true)
  @ToString(callSuper = true)
  @EqualsAndHashCode(callSuper = true)
  public static class Domain extends NamedObject {

    @NotEmpty private Set<Pointer> content;
    private Set<Pointer> dependsOn;

    public Domain(FullyQualifiedName fullyQualifiedName) {
      super(fullyQualifiedName);
    }

    public Set<Pointer> dependsOn() {
      return Optional.ofNullable(dependsOn).orElseGet(Collections::emptySet);
    }
  }
}
