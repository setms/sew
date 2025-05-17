package org.setms.sew.core.domain.model.sdlc;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClockEvent extends NamedObject {

  private String cron;

  public ClockEvent(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
