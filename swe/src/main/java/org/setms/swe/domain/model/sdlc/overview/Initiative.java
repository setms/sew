package org.setms.swe.domain.model.sdlc.overview;

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
public class Initiative extends Artifact {

  @NotEmpty private String organization;
  @NotEmpty private String title;

  public Initiative(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
