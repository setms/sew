package org.setms.sew.core.domain.model.tool;

import lombok.Getter;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;

public class UnresolvedObject extends NamedObject {

  @Getter private final String type;

  public UnresolvedObject(FullyQualifiedName fullyQualifiedName, String type) {
    super(fullyQualifiedName);
    this.type = type;
  }

  @Override
  public String toString() {
    return "%s(%s)".formatted(type, super.toString());
  }
}
