package org.setms.swe.domain.model.sdlc.ux;

import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.swe.domain.model.sdlc.design.FieldType;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class InputField extends Artifact {

  @NotNull private FieldType type;

  public InputField(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
