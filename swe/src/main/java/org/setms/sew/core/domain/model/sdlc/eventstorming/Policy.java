package org.setms.sew.core.domain.model.sdlc.eventstorming;

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
public class Policy extends Artifact {

  @NotEmpty private String title;
  private String description;

  public Policy(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
