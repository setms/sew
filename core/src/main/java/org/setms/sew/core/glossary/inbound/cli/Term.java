package org.setms.sew.core.glossary.inbound.cli;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.setms.sew.core.schema.FullyQualifiedName;
import org.setms.sew.core.schema.NamedObject;
import org.setms.sew.core.schema.Pointer;

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
