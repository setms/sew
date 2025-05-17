package org.setms.sew.core.domain.model.sdlc;

import jakarta.validation.constraints.NotEmpty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ExternalSystem extends NamedObject {

  @NotEmpty private String display;

  public ExternalSystem(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
