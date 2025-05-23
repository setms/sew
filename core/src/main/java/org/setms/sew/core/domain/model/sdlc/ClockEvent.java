package org.setms.sew.core.domain.model.sdlc;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClockEvent extends NamedObject {

  @NotEmpty private String cron;

  public ClockEvent(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
