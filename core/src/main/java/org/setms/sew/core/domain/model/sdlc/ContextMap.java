package org.setms.sew.core.domain.model.sdlc;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Set;

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

  @NotEmpty private List<BoundedContext> boundedContexts;

  public ContextMap(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  @Getter
  @Setter
  @Accessors(chain = true)
  @ToString(callSuper = true)
  @EqualsAndHashCode(callSuper = true)
  public static class BoundedContext extends NamedObject {

    @NotEmpty private Set<Pointer> content;

    public BoundedContext(FullyQualifiedName fullyQualifiedName) {
      super(fullyQualifiedName);
    }
  }

}
