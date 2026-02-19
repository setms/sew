package org.setms.swe.domain.model.sdlc.project;

import jakarta.validation.constraints.NotEmpty;
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
public class Project extends Artifact {

  @NotEmpty private String title;

  public Project(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
