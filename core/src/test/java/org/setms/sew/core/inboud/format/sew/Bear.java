package org.setms.sew.core.inboud.format.sew;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.domain.model.sdlc.Pointer;

@Getter
@Setter
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Bear extends NamedObject {

  private String dingo;
  private List<String> fox;
  private List<Hyena> hyenas;
  private Pointer leopard;
  private State state;
  private boolean ok;

  public Bear(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
