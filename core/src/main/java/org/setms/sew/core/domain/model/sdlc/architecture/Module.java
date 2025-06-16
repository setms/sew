package org.setms.sew.core.domain.model.sdlc.architecture;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.HasType;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.domain.model.sdlc.Pointer;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Module extends NamedObject {

  @HasType("subdomain")
  private Pointer mappedTo;

  public Module(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
