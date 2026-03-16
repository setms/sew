package org.setms.swe.domain.model.sdlc.eventstorming;

import jakarta.validation.constraints.NotEmpty;
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
public class Aggregate extends Artifact implements HasPayload {

  @NotEmpty private String display;

  @HasType("entity")
  private Link root;

  public Aggregate(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  @Override
  public Link getPayload() {
    return null;
  }

  public String getDisplay() {
    return Optional.ofNullable(display).orElse(getName());
  }
}
