package org.setms.sew.core.domain.model.sdlc;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Module extends NamedObject {

  @HasType(type = "subdomain")
  private Pointer mappedTo;

  public Module(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
