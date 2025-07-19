package org.setms.sew.core.domain.model.sdlc.acceptance;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.design.FieldType;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class FieldVariable extends Variable<FieldType, String> {

  public FieldVariable(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
