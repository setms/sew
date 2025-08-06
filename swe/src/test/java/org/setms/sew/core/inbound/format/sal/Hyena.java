package org.setms.sew.core.inbound.format.sal;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;

@Getter
@Setter
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Hyena extends Artifact {

  private String jaguar;

  public Hyena(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
