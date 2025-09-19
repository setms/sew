package org.setms.swe.domain.model.sdlc.architecture;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.HasType;
import org.setms.km.domain.model.artifact.Link;

/** Collection of {@linkplain Component}s that deploy {@linkplain Module}s. */
@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Components extends Artifact {

  @NotNull
  @HasType("modules")
  private Link deploys;

  @NotEmpty @Valid private Collection<Component> components;

  public Components(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
