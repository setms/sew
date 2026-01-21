package org.setms.swe.domain.model.sdlc.acceptancetest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class AcceptanceTest extends Artifact {

  @NotNull private Link sut;
  @NotEmpty private List<@Valid Variable<?, ?>> variables;
  @NotEmpty private List<@Valid Scenario> scenarios;

  public AcceptanceTest(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  public Optional<Variable<?, ?>> findVariable(Link variable) {
    return Optional.ofNullable(variable)
        .filter(p -> p.hasType("variable"))
        .map(Link::getId)
        .flatMap(this::findVariable);
  }

  public Optional<Variable<?, ?>> findVariable(String name) {
    return variables.stream().filter(v -> name.equals(v.getName())).findFirst();
  }
}
