package org.setms.sew.core.stakeholders.inbound.cli;

import jakarta.validation.constraints.NotEmpty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.sew.core.schema.FullyQualifiedName;
import org.setms.sew.core.schema.NamedObject;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Stakeholder extends NamedObject {

  @NotEmpty private String display;
  private String person;

  public Stakeholder(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
