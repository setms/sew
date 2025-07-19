package org.setms.sew.core.domain.model.sdlc.acceptance;

import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.HasType;
import org.setms.km.domain.model.artifact.Pointer;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Scenario extends Artifact {

  @HasType("variable")
  private Pointer init;

  @NotNull
  @HasType("variable")
  private Pointer command;

  @HasType("variable")
  private Pointer state;

  @HasType("variable")
  private Pointer emitted;

  public Scenario(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
