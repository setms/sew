package org.setms.swe.domain.model.sdlc.ui;

import jakarta.validation.Valid;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DesignSystem extends Artifact {

  private List<@Valid Style> styles;

  public DesignSystem(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
