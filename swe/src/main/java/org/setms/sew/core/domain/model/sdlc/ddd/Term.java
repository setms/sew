package org.setms.sew.core.domain.model.sdlc.ddd;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Term extends Artifact {

  @NotEmpty private String display;
  @NotEmpty private String description;
  private List<Link> seeAlso;

  public Term(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
