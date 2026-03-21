package org.setms.swe.domain.model.sdlc.ui;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
public class Style extends Artifact {

  @NotEmpty private List<@Valid Property> properties;

  public Style(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
