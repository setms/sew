package org.setms.sew.core.domain.model.sdlc.acceptance;

import jakarta.validation.constraints.NotEmpty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.HasType;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.Pointer;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class FieldAssignment extends Artifact {

  @NotEmpty private String fieldName;

  @HasType("variable")
  private Pointer value;

  public FieldAssignment(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
