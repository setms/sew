package org.setms.sew.core.domain.model.sdlc.ddd;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.domain.model.sdlc.Pointer;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Term extends NamedObject {

  @NotEmpty private String display;
  @NotEmpty private String description;
  private List<Pointer> seeAlso;

  public Term(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
