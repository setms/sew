package org.setms.swe.domain.model.sdlc.architecture;

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

/** Deployable container of one or more {@linkplain Module}s. */
@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Component extends Artifact {

  @HasType("module")
  private Collection<Link> deploys;

  public Component(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
