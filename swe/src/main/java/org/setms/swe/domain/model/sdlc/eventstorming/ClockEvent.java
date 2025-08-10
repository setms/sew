package org.setms.swe.domain.model.sdlc.eventstorming;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;

@Getter
@Setter
public class ClockEvent extends Artifact {

  @NotEmpty private String cron;

  public ClockEvent(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
