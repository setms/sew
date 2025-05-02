package org.setms.sew.core.domain.model.sdlc;

import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ReadModel extends NamedObject {

  @NotNull private Pointer content;

  public ReadModel(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
