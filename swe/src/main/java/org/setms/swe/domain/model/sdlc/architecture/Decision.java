package org.setms.swe.domain.model.sdlc.architecture;

import jakarta.validation.constraints.NotEmpty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;

/** Architecturally significant design decision. */
@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@RegisteredTopicAndChoice
public class Decision extends Artifact {

  private String context;
  @NotEmpty private String topic;
  @NotEmpty private String choice;
  private String rationale;

  public Decision(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
