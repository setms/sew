package org.setms.sew.core.domain.model.sdlc;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Event extends NamedObject {

  private Pointer payload;

  public Event(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

}
