package org.setms.sew.core.domain.model.sdlc;

import jakarta.validation.constraints.NotEmpty;
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
public class Command extends NamedObject {

  @NotEmpty private String display;
  private Pointer payload;

  public Command(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

}
