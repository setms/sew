package org.setms.sew.core.domain.model.sdlc.process;

import jakarta.validation.constraints.NotEmpty;
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
public class Todo extends NamedObject {

  @NotEmpty private String tool;
  @NotEmpty private String location;
  @NotEmpty private String message;
  @NotEmpty private String code;
  @NotEmpty private String action;

  public Todo(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
