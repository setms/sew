package org.setms.sew.core.domain.model.sdlc.architecture;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.HasType;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.domain.model.sdlc.Pointer;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Modules extends NamedObject {

  @NotNull
  @HasType("domain")
  private Pointer mappedTo;

  @NotEmpty @Valid private Collection<Module> modules;

  public Modules(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
