package org.setms.sew.core.domain.model.sdlc.ddd;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Domain extends NamedObject {

  @NotEmpty private List<Subdomain> subdomains;

  public Domain(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
