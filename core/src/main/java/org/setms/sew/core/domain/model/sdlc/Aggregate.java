package org.setms.sew.core.domain.model.sdlc;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class Aggregate extends NamedObject {

  @NotEmpty private String display;
  @NotNull private Pointer root;

  public Aggregate(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
