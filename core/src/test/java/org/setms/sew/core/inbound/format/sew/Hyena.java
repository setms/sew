package org.setms.sew.core.inbound.format.sew;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;

@Getter
@Setter
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Hyena extends NamedObject {

  private String jaguar;

  public Hyena(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
