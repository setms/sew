package org.setms.sew.core.domain.model.sdlc.acceptance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
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
public class AcceptanceTest extends NamedObject {

  @NotNull private Pointer sut;
  @NotEmpty @Valid private List<Variable<?, ?>> variables;
  @NotEmpty @Valid private List<Scenario> scenarios;

  public AcceptanceTest(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
