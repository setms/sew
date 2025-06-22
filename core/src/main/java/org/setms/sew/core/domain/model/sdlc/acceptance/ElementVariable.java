package org.setms.sew.core.domain.model.sdlc.acceptance;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.Pointer;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class ElementVariable extends Variable<Pointer, FieldAssignment> {

  public ElementVariable(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

}
