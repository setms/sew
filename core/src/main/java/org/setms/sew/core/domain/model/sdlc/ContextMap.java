package org.setms.sew.core.domain.model.sdlc;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
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
public class ContextMap extends NamedObject {

  @NotEmpty private List<BoundedContext> contexts;

  public ContextMap(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
