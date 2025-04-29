package org.setms.sew.core.stakeholders.inbound.cli;

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
public class UseCase extends NamedObject {

  @NotEmpty private String title;
  private String description;
  private List<Pointer> terms;
  private List<Pointer> captures;
  private List<Scenario> scenarios;

  public UseCase(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  @Getter
  @Setter
  @ToString(callSuper = true)
  @EqualsAndHashCode(callSuper = true)
  public static class Scenario extends NamedObject {

    @NotEmpty private String title;
    @NotEmpty private String description;
    @NotEmpty private List<Pointer> steps;

    public Scenario(FullyQualifiedName fullyQualifiedName) {
      super(fullyQualifiedName);
    }
  }
}
