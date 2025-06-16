package org.setms.sew.core.domain.model.sdlc.eventstorm;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.domain.model.sdlc.Pointer;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ReadModel extends NamedObject {

  @NotEmpty private String display;
  @NotNull private Pointer content;

  public ReadModel(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  public String getDisplay() {
    return Optional.ofNullable(display).orElse(getName());
  }
}
