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
import org.setms.swe.domain.model.sdlc.ddd.Domain;

/** Collection of {@linkplain Module}s that together implement a {@linkplain Domain}. */
@Getter
@Setter
@Accessors(chain = true)
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
