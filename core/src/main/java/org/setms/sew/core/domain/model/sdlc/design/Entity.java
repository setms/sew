package org.setms.sew.core.domain.model.sdlc.design;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.domain.model.validation.Diagnostic;
import org.setms.sew.core.domain.model.validation.Location;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Entity extends NamedObject {

  @Valid @NotEmpty private Collection<Field> fields;

  public Entity(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  @Override
  public void validate(Location location, Collection<Diagnostic> diagnostics) {
    fields.forEach(field -> field.validate(location.plus(field), diagnostics));
  }
}
