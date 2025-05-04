package org.setms.sew.core.domain.model.sdlc;

import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ReadModel extends NamedObject {

  private String display;
  @NotNull private Pointer content;

  public ReadModel(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  public String getDisplay() {
    return Optional.ofNullable(display).orElse(getName());
  }
}
