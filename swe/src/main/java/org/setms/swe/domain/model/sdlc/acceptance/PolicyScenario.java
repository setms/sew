package org.setms.swe.domain.model.sdlc.acceptance;

import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.HasType;
import org.setms.km.domain.model.artifact.Link;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PolicyScenario extends Scenario {

  @HasType("variable")
  private Link init;

  @NotNull
  @HasType("variable")
  private Link handles;

  @NotNull
  @HasType("variable")
  private Link issued;

  public PolicyScenario(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
