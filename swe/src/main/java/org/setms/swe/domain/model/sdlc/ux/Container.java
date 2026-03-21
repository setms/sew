package org.setms.swe.domain.model.sdlc.ux;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;

/** A layout element that arranges its children in a given direction. */
@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Container extends Artifact {

  private Direction direction;

  public Container(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
