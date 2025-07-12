package org.setms.sew.core.domain.model.sdlc.domainstory;

import jakarta.validation.constraints.NotEmpty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.domain.model.sdlc.Pointer;

import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Sentence extends NamedObject {

  @NotEmpty private List<Pointer> parts;

  public Sentence(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
