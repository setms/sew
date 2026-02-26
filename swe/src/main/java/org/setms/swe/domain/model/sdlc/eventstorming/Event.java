package org.setms.swe.domain.model.sdlc.eventstorming;

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
public class Event extends Artifact implements HasPayload {

  @HasType("entity")
  private Link payload;

  public Event(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
