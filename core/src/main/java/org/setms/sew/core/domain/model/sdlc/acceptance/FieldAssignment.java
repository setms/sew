package org.setms.sew.core.domain.model.sdlc.acceptance;

import jakarta.validation.constraints.NotEmpty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.HasType;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.domain.model.sdlc.Pointer;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class FieldAssignment extends NamedObject {

  @NotEmpty
  private String fieldName;

  @HasType("variable")
  private Pointer value;

  public FieldAssignment(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
