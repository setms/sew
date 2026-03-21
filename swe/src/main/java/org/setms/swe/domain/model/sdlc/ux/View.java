package org.setms.swe.domain.model.sdlc.ux;

import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.HasType;
import org.setms.km.domain.model.artifact.Link;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public non-sealed class View extends Artifact implements WireframeElement {

  @NotNull
  @HasType("entity")
  private Link content;

  private boolean multiple;

  public View(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
