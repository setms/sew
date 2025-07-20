package org.setms.sew.core.domain.model.sdlc.architecture;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.HasType;
import org.setms.km.domain.model.artifact.Link;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Modules extends Artifact {

  @NotNull
  @HasType("domain")
  private Link mappedTo;

  @NotEmpty @Valid private Collection<Module> modules;

  public Modules(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
