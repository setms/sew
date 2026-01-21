package org.setms.swe.domain.model.sdlc.acceptancetest;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class ElementVariable extends Variable<Link, FieldAssignment> {

  public ElementVariable(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
