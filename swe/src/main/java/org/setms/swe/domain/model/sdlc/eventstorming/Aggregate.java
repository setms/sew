package org.setms.swe.domain.model.sdlc.eventstorming;

import jakarta.validation.constraints.NotEmpty;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.HasType;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.nlp.English;
import org.setms.km.domain.model.nlp.NaturalLanguage;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Aggregate extends Artifact implements HasPayload {

  private static final NaturalLanguage LANGUAGE = new English();

  @NotEmpty private String display;

  @HasType("entity")
  private Link root;

  public Aggregate(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  @Override
  public Link getPayload() {
    return root;
  }

  public String getDisplay() {
    return Optional.ofNullable(display).orElse(getName());
  }

  public String domainObjectName() {
    return root != null ? root.getId() : LANGUAGE.singular(getName());
  }
}
