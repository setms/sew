package org.setms.sew.core.domain.model.sdlc.usecase;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.domain.model.sdlc.Pointer;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class UseCase extends NamedObject {

  @NotEmpty private String title;
  private String description;
  private List<Pointer> terms;
  private List<Pointer> captures;
  @NotEmpty private List<Scenario> scenarios;

  public UseCase(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  public Stream<Scenario> scenarios() {
    return scenarios.stream();
  }
}
