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

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Feedback extends Artifact {

  @HasType("event")
  private List<@Valid Link> supports;

  public Feedback(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
