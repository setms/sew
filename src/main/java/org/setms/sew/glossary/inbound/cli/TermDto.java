package org.setms.sew.glossary.inbound.cli;

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
public class TermDto extends NamedObject {

  private String display;
  private String description;
  private List<Pointer> seeAlso;

  public TermDto(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
