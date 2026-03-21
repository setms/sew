package org.setms.swe.domain.model.sdlc.ux;

import jakarta.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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

  public Wireframe(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  public boolean initiates(Command command) {
    return Optional.ofNullable(containers).stream()
        .flatMap(Collection::stream)
        .anyMatch(container -> containsAffordanceThatInitiates(container, command));
  }

  private boolean containsAffordanceThatInitiates(Container container, Command command) {
    return Optional.ofNullable(container.getChildren()).stream()
        .flatMap(Collection::stream)
        .anyMatch(
            child ->
                switch (child) {
                  case Affordance affordance -> affordance.initiates(command);
                  case Container nested -> containsAffordanceThatInitiates(nested, command);
                  case InputField inputField -> false;
                  case View view -> false;
                  case Feedback feedback -> false;
                });
  }
}
