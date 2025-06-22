package org.setms.sew.core.domain.model.sdlc.design;

import jakarta.validation.constraints.NotEmpty;
import java.util.Collection;
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
public class Field extends NamedObject {

  @NotEmpty private FieldType type;
  private Collection<FieldConstraint> constraints;

  public Field(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
