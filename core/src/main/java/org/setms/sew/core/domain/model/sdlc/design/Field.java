package org.setms.sew.core.domain.model.sdlc.design;

import static org.setms.sew.core.domain.model.validation.Level.ERROR;

import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.sew.core.domain.model.sdlc.Enums;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.domain.model.validation.Diagnostic;
import org.setms.sew.core.domain.model.validation.Location;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Field extends NamedObject {

  @NotNull private FieldType type;
  private Enums<FieldConstraint> constraints = Enums.of(FieldConstraint.class);
  private List<String> values;

  public Field(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  @Override
  public void validate(Location location, Collection<Diagnostic> diagnostics) {
    if (type == FieldType.SELECTION && (values == null || values.size() < 2)) {
      diagnostics.add(new Diagnostic(ERROR, "Selection field needs at least 2 values to select", location));
    }
  }
}
