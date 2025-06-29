package org.setms.sew.core.domain.model.sdlc.ddd;

import jakarta.validation.constraints.NotEmpty;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.domain.model.sdlc.Pointer;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Subdomain extends NamedObject {

  private DomainClassification classification;
  @NotEmpty private Set<Pointer> content;
  private Set<Pointer> dependsOn;

  public Subdomain(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  public Set<Pointer> dependsOn() {
    return Optional.ofNullable(dependsOn).orElseGet(Collections::emptySet);
  }
}
