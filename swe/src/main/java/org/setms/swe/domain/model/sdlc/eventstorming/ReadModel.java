package org.setms.swe.domain.model.sdlc.eventstorming;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.HasType;
import org.setms.km.domain.model.artifact.Link;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ReadModel extends Artifact {

  @NotEmpty private String display;

  @NotNull
  @HasType("entity")
  private Link content;

  public ReadModel(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  public String getDisplay() {
    return Optional.ofNullable(display).orElse(getName());
  }
}
