package org.setms.swe.domain.model.sdlc.ddd;

import jakarta.validation.constraints.NotEmpty;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Subdomain extends Artifact {

  private DomainClassification classification;
  @NotEmpty private Set<Link> content;
  private Set<Link> dependsOn;

  public Subdomain(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  public Set<Link> dependsOn() {
    return Optional.ofNullable(dependsOn).orElseGet(Collections::emptySet);
  }
}
