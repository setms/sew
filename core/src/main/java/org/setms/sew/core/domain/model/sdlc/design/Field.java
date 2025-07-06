package org.setms.sew.core.domain.model.sdlc.design;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.sew.core.domain.model.sdlc.Enums;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Field extends NamedObject {

  @NotNull
  private FieldType type;
  private Enums<FieldConstraint> constraints = Enums.of(FieldConstraint.class);
  private List<String> values;

  public Field(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
