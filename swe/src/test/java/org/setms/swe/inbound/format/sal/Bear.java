package org.setms.swe.inbound.format.sal;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;

@Getter
@Setter
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Bear extends Artifact {

  private String dingo;
  private List<String> fox;
  private List<Hyena> hyenas;
  private Link leopard;
  private State state;
  private boolean ok;

  public Bear(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
