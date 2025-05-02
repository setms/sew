package org.setms.sew.core.domain.model.sdlc;

import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Event extends NamedObject {

  private EventType type;
  @NotNull private Pointer payload;

  public Event(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  @RequiredArgsConstructor
  private enum EventType {
    EVENT_CARRIED_STATE_TRANSFER("EventCarriedStateTransfer"),
    NOTIFICATION("Notification");

    private final String name;
  }
}
