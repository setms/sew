package org.setms.sew.core.domain.model.sdlc.acceptance;

import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Variable extends NamedObject {

  @NotNull private Object type;
  private Object definition;

  public Variable(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
