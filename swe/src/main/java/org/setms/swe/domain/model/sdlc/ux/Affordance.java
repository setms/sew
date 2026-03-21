package org.setms.swe.domain.model.sdlc.ux;

import jakarta.validation.Valid;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.HasType;
import org.setms.km.domain.model.artifact.Link;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;

/** Action possibilities that are readily perceivable by an actor. */
@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public non-sealed class Affordance extends Artifact implements WireframeElement {

  @HasType("command")
  Link command;

  @HasType("wireframe")
  Link launch;

  List<@Valid InputField> inputFields;

  public Affordance(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  public boolean initiates(Command command) {
    return this.command.pointsTo(command);
  }
}
