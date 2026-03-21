package org.setms.swe.domain.model.sdlc.ui;

import jakarta.validation.constraints.NotBlank;
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
@PropertyName
public class Property extends Artifact {

  @NotBlank private String value;

  public Property(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
