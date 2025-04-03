package org.setms.sew.stakeholders.inbound.cli;

import jakarta.validation.constraints.NotEmpty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.setms.sew.schema.FullyQualifiedName;
import org.setms.sew.schema.NamedObject;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Stakeholder extends NamedObject {

  @NotEmpty private String display;
  private String person;

  public Stakeholder(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
