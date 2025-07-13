package org.setms.sew.core.domain.model.sdlc.process;

import jakarta.validation.constraints.NotEmpty;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.domain.model.validation.Location;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Todo extends NamedObject {

  @NotEmpty private String tool;
  private String location;
  @NotEmpty private String message;
  @NotEmpty private String code;
  @NotEmpty private String action;

  public Todo(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  public Tool toTool() {
    try {
      return (Tool) Class.forName(this.tool).getDeclaredConstructor().newInstance();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  public Location toLocation() {
    return Optional.ofNullable(location).map(s -> s.split("/")).map(Location::new).orElse(null);
  }
}
