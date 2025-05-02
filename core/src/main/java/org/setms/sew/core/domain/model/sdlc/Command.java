package org.setms.sew.core.domain.model.sdlc;

import jakarta.validation.constraints.NotEmpty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Command extends NamedObject {

  @NotEmpty private String display;
  private Pointer payload;
  private CommandProcessing processing;

  public Command(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  @RequiredArgsConstructor
  private enum CommandProcessing {
    SYNCHRONOUS("Synchronous"),
    ASYNCHRONOUS("Asynchronous");

    @Getter private final String display;
  }
}
