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
import org.setms.swe.domain.model.sdlc.eventstorming.Command;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Wireframe extends Artifact {

  private List<@Valid Container> containers;
  private List<@Valid Affordance> affordances;
  private List<@Valid View> views;
  private List<@Valid Feedback> feedbacks;

  public Wireframe(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  public boolean initiates(Command command) {
    return affordances.stream().anyMatch(affordance -> affordance.initiates(command));
  }
}
