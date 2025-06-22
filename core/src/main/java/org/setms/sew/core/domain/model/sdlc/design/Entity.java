package org.setms.sew.core.domain.model.sdlc.design;

import jakarta.validation.constraints.NotEmpty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;

import java.util.Collection;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Entity extends NamedObject {

  @NotEmpty
  private Collection<Field> fields;

  public Entity(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
