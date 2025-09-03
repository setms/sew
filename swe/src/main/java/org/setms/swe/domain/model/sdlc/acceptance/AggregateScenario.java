package org.setms.swe.domain.model.sdlc.acceptance;

import jakarta.validation.constraints.NotNull;
import java.util.List;
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
public class AggregateScenario extends Scenario {

  @HasType("variable")
  private List<Link> init;

  @NotNull
  @HasType("variable")
  private Link accepts;

  @HasType("variable")
  private List<Link> state;

  @HasType("variable")
  private Link emitted;

  public AggregateScenario(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
