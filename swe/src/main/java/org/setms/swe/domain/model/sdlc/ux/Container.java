package org.setms.swe.domain.model.sdlc.ux;

import java.util.List;
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
public non-sealed class Container extends Artifact implements WireframeElement {

  private Direction direction;
  private List<WireframeElement> children;

  public Container(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
