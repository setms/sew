package org.setms.sew.core.domain.model.sdlc.eventstorming;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Pointer;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ReadModel extends Artifact {

  @NotEmpty private String display;
  @NotNull private Pointer content;

  public ReadModel(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  public String getDisplay() {
    return Optional.ofNullable(display).orElse(getName());
  }
}
