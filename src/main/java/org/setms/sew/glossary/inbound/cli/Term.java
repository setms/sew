package org.setms.sew.glossary.inbound.cli;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.setms.sew.schema.FullyQualifiedName;
import org.setms.sew.schema.NamedObject;
import org.setms.sew.schema.Pointer;

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
