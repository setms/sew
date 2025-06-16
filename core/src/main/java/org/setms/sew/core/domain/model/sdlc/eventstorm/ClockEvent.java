package org.setms.sew.core.domain.model.sdlc.eventstorm;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;

@Getter
@Setter
public class ClockEvent extends NamedObject {

  @NotEmpty private String cron;

  public ClockEvent(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
