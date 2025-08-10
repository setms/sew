package org.setms.swe.domain.model.sdlc.usecase;

import jakarta.validation.constraints.NotEmpty;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class UseCase extends Artifact {

  @NotEmpty private String title;
  private String description;
  private List<Link> terms;
  private List<Link> captures;
  @NotEmpty private List<Scenario> scenarios;

  public UseCase(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  public Stream<Scenario> scenarios() {
    return scenarios.stream();
  }

  @Override
  public void validate(Location location, Collection<Diagnostic> diagnostics) {
    scenarios.forEach(scenario -> scenario.validate(scenario.appendTo(location), diagnostics));
  }
}
