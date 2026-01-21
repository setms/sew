package org.setms.swe.domain.model.sdlc.unittest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class UnitTest extends Artifact {

  @NotEmpty private List<@Valid Behavior> behaviors;
  private List<@Valid VariableAssignment> sharedVariables;

  public UnitTest(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
