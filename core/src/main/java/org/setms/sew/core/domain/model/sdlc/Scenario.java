package org.setms.sew.core.domain.model.sdlc;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Scenario extends NamedObject {

  @NotEmpty private String title;
  private String description;
  @NotEmpty private List<Pointer> steps;

  public Scenario(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  public Stream<Pointer> steps() {
    return steps.stream();
  }
}
