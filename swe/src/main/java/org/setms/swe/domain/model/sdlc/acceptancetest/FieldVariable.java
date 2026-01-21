package org.setms.swe.domain.model.sdlc.acceptancetest;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.swe.domain.model.sdlc.design.FieldType;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class FieldVariable extends Variable<FieldType, String> {

  public FieldVariable(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
