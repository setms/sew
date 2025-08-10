package org.setms.swe.domain.model.sdlc.eventstorming;

import jakarta.validation.constraints.NotEmpty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ExternalSystem extends Artifact {

  @NotEmpty private String display;

  public ExternalSystem(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
